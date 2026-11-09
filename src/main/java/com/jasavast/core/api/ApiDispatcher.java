package com.jasavast.core.api;

import com.jasavast.core.annotation.DeleteExecution;
import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.annotation.PutExecution;
import com.jasavast.core.error.MethodNotAllowedException;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.service.ApiAbstract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Resolves {@code /api/{module}/{subModule}/{action}} to bean {@code "api" + module + subModule}
 * (case-insensitive) and to its method {@code action} annotated for the HTTP verb.
 * <p>
 * The registry is built once at startup:
 * <ul>
 *   <li>only beans extending {@link ApiAbstract} are reachable, and they must be named {@code api...};</li>
 *   <li>signatures are validated eagerly ({@code public Mono<?> action(ApiRequest)}), so a wrong
 *       handler fails startup instead of failing at runtime;</li>
 *   <li>the method is invoked on the Spring proxy so aspects and {@code @PreAuthorize} apply.</li>
 * </ul>
 * The action name is the annotation's {@code id()} when set, otherwise the Java method name
 * (case-sensitive).
 */
@Component
@Slf4j
public class ApiDispatcher {

    static final String BEAN_PREFIX = "api";

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

    /** lower-case bean name -> action -> http verb -> endpoint */
    private final Map<String, Map<String, Map<HttpMethod, Endpoint>>> registry;

    public ApiDispatcher(ApplicationContext context) {
        Map<String, Map<String, Map<HttpMethod, Endpoint>>> map = new HashMap<>();
        Map<String, String> originalNames = new HashMap<>();
        context.getBeansOfType(ApiAbstract.class).forEach((beanName, bean) -> {
            String key = beanName.toLowerCase(Locale.ROOT);
            if (!key.startsWith(BEAN_PREFIX) || key.length() == BEAN_PREFIX.length()) {
                throw new IllegalStateException("ApiAbstract bean '" + beanName
                        + "' must be named 'api<Module><SubModule>' (e.g. @Component(\"apiCobaAja\"))");
            }
            String clash = originalNames.putIfAbsent(key, beanName);
            if (clash != null) {
                throw new IllegalStateException("API beans '" + clash + "' and '" + beanName + "' differ only by case");
            }
            map.put(key, register(beanName, bean));
        });
        this.registry = Collections.unmodifiableMap(map);
        log.info("API dispatcher registered: {}", operations());
    }

    private static Map<String, Map<HttpMethod, Endpoint>> register(String beanName, Object bean) {
        Map<String, Map<HttpMethod, Endpoint>> actions = new HashMap<>();
        for (Method method : AopUtils.getTargetClass(bean).getMethods()) {
            for (Map.Entry<Class<? extends Annotation>, HttpMethod> verb : VERBS.entrySet()) {
                Annotation annotation = method.getAnnotation(verb.getKey());
                if (annotation == null) {
                    continue;
                }
                validateSignature(beanName, method);
                String id = IDS.get(verb.getKey()).apply(annotation);
                String action = id.isBlank() ? method.getName() : id;
                Endpoint previous = actions.computeIfAbsent(action, k -> new HashMap<>(4))
                        .putIfAbsent(verb.getValue(), new Endpoint(bean, method));
                if (previous != null) {
                    throw new IllegalStateException("Duplicate API operation " + verb.getValue() + " " + beanName + "." + action);
                }
            }
        }
        return actions;
    }

    private static void validateSignature(String beanName, Method method) {
        boolean ok = Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && Mono.class.isAssignableFrom(method.getReturnType())
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == ApiRequest.class;
        if (!ok) {
            throw new IllegalStateException("API operation " + beanName + "." + method.getName()
                    + " must have signature: public Mono<?> " + method.getName() + "(ApiRequest request)");
        }
    }

    /** e.g. {@code "GET apicobaaja.getMethod"}, sorted; for logs and tests. */
    public Set<String> operations() {
        Set<String> result = new TreeSet<>();
        registry.forEach((bean, actions) -> actions.forEach((action, verbs) ->
                verbs.keySet().forEach(verb -> result.add(verb.name() + " " + bean + "." + action))));
        return result;
    }

    /**
     * @return the operation result; errors (unknown operation, wrong verb, handler failure) are
     * signalled through the Mono, never thrown.
     */
    public Mono<?> dispatch(HttpMethod httpMethod, String module, String subModule, String action, ApiRequest request) {
        return Mono.defer(() -> {
            String beanKey = (BEAN_PREFIX + module + subModule).toLowerCase(Locale.ROOT);
            Map<String, Map<HttpMethod, Endpoint>> actions = registry.get(beanKey);
            Map<HttpMethod, Endpoint> byVerb = actions == null ? null : actions.get(action);
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
                return Mono.error(new IllegalStateException(e));
            }
        });
    }
}
