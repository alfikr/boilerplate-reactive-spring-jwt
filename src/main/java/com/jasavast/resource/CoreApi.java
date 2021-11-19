package com.jasavast.resource;

import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.error.MethodNotAllowedException;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.service.ApiAbstract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CoreApi {
    private final ApplicationContext context;
    @GetMapping("/api/request")
    public Mono<JSONObject> getMethod(ServerHttpRequest request) throws Exception {
        Map map= request.getQueryParams().toSingleValueMap();
        JSONObject req = new JSONObject(map);
        log.info("request param {}",req);
        if(!req.has("method")){
            throw new MethodNotFoundException();
        }
        String [] method = req.getString("method").split("\\.");
        if(method.length!=2){
            throw new MethodNotFoundException();
        }
        Object bean = context.getBean(method[0]);
        if (bean==null){
            throw new MethodNotFoundException();
        }
        if (ApiAbstract.class.isAssignableFrom(bean.getClass())){
            ((ApiAbstract) bean).init(req);
        }
        log.info("class api {}",bean.getClass().getName());
        Class cl = bean.getClass().getSuperclass();
        Method m =  cl.getMethod(method[1]);
        log.info("support http method get  {}",
                m.isAnnotationPresent(GetExecution.class));
        if(!m.isAnnotationPresent(GetExecution.class)){
            throw new MethodNotAllowedException();
        }
        Mono<JSONObject> result = (Mono<JSONObject>) m.invoke(bean);

        return result;
    }
}
