package com.jasavast.resource;

import com.jasavast.core.error.BadRequestAlertException;
import com.jasavast.service.ApiAbstract;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.CorePublisher;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

@RestController
@RequiredArgsConstructor
public class CoreApi {
    private final ApplicationContext context;
    @PostMapping("/{moduleName}/{subModuleName}/{actionName}")
    public CorePublisher<Object> defaultApiHandler(@PathVariable("moduleName") String moduleName,
                                                   @PathVariable ("subModuleName") String subModuleName,
                                                   @PathVariable("actionName") String actionName,
                                                   ServerHttpRequest request, ServerHttpResponse response) throws Exception {
        String beanName = "api"+moduleName+subModuleName;
        ApiAbstract bean = (ApiAbstract) context.getBean(beanName);
        if (bean==null){
            return Mono.error(new BadRequestAlertException("kelas "+beanName+" tidak ditemukan",beanName,"404"));
        }
        bean.init(request,response);
        Method method = bean.getClass().getMethod(actionName);
        if(method==null){
            return Mono.error(new BadRequestAlertException("method "+actionName+" tidak ditemukan",beanName,"404"));
        }
        CorePublisher<Object> apiResult=null;
        if (method.getReturnType().equals(Void.TYPE)){
            method.invoke(bean);
        }else {
            apiResult = (CorePublisher<Object>) method.invoke(bean);
        }
        return apiResult;
    }
}
