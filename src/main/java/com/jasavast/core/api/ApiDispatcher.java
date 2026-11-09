package com.jasavast.core.api;

import com.jasavast.core.annotation.DeleteExecution;
import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.annotation.PutExecution;
import com.jasavast.core.error.MethodNotAllowedException;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.resource.vm.ReqVM;
import com.jasavast.service.ApiAbstract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Resolves {@code "<beanName>.<operation>"} to an annotated method on an {@link ApiAbstract} bean.
 * <p>
 * The registry is built once at startup:
 * <ul>
 *   <li>only beans extending {@link ApiAbstract} are reachable (not arbitrary context beans);</li>
 *   <li>signatures are validated eagerly ({@code Mono<?> op(ReqVM)}), so a wrong handler fails
 *       startup instead of returning 500 at runtime;</li>
 *   <li>the method is invoked on the Spring proxy so aspects and {@code @PreAuthorize} apply.</li>
 * </ul>
 * The operation name is the annotation's {@code id()} when set, otherwise the Java method name.
 */
@Component
@Slf4j
public class ApiDispatcher {

    private static final Map<Class<? extends Annotation>, HttpMethod> VERBS = Map.of(
            GetExecution.class, HttpMethod.GET,
            PostExecution.class, HttpMethod.POST,
            PutExecution.class, HttpMethod.PUT,
            DeleteExecution.class, HttpMethod.DELETE);

    private static final Map<Class<? extends Annotation>, Function<Annotation, String>> IDS = Map.of(
            GetExecution.class, a -> ((GetExecution) a).id(),
            PostExecution.class, a -> ((PostExecution) a).id(),
            PutExecution.class, a -> ((PutExecution) a).id(),
            DeleteExecution.class, a -> ((DeleteExecution) a).id());

    private record Endpoint(Object bean, Method method) {
    }

    /** key: "bean.operation" -> (http verb -> endpoint) */
    private final Map<String, Map<HttpMethod, Endpoint>> registry;

    public ApiDispatcher(ApplicationContext context) {
        Map<String, Map<HttpMethod, Endpoint>> map = new HashMap<>();
        context.getBeansOfType(ApiAbstract.class).forEach((beanName, bean) -> register(map, beanName, bean));
        this.registry = Collections.unmodifiableMap(map);
        log.info("API dispatcher registered {} operation(s): {}", registry.size(), registry.keySet());
    }

    private static void register(Map<String, Map<HttpMethod, Endpoint>> map, String beanName, Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (Method method : targetClass.getMethods()) {
            for (Map.Entry<Class<? extends Annotation>, HttpMethod> verb : VERBS.entrySet()) {
                Annotation annotation = method.getAnnotation(verb.getKey());
                if (annotation == null) {
                    continue;
                }
                validateSignature(beanName, method);
                String id = IDS.get(verb.getKey()).apply(annotation);
                String operation = beanName + "." + (id.isBlank() ? method.getName() : id);
                Endpoint previous = map.computeIfAbsent(operation, k -> new HashMap<>(4))
                        .putIfAbsent(verb.getValue(), new Endpoint(bean, method));
                if (previous != null) {
                    throw new IllegalStateException("Duplicate API operation " + verb.getValue() + " " + operation);
                }
            }
        }
    }

    private static void validateSignature(String beanName, Method method) {
        boolean ok = Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && Mono.class.isAssignableFrom(method.getReturnType())
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == ReqVM.class;
        if (!ok) {
            throw new IllegalStateException("API operation " + beanName + "." + method.getName()
                    + " must have signature: public Mono<?> " + method.getName() + "(ReqVM request)");
        }
    }

    public Set<String> operations() {
        return registry.keySet();
    }

    /**
     * @return the operation result; errors (unknown operation, wrong verb, handler failure) are
     * signalled through the Mono, never thrown.
     */
    public Mono<?> dispatch(HttpMethod httpMethod, ReqVM request) {
        return Mono.defer(() -> {
            Map<HttpMethod, Endpoint> byVerb = request.getMethod() == null ? null : registry.get(request.getMethod());
            if (byVerb == null) {
                return Mono.error(new MethodNotFoundException());
            }
            Endpoint endpoint = byVerb.get(httpMethod);
            if (endpoint == null) {
                return Mono.error(new MethodNotAllowedException());
            }
            try {
                return (Mono<?>) endpoint.method().invoke(endpoint.bean(), request);
            } catch (InvocationTargetException e) {
                return Mono.error(e.getTargetException());
            } catch (IllegalAccessException e) {
                ReflectionUtils.handleReflectionException(e);
                return Mono.error(e); // unreachable
            }
        });
    }
}
