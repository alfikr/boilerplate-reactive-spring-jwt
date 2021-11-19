package com.jasavast.resource;

import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.annotation.PutExecution;
import com.jasavast.core.error.MethodNotAllowedException;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.resource.vm.ReqVM;
import com.jasavast.service.ApiAbstract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
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
    @PostMapping("/api/request")
    public Mono<JSONObject> postMethod(@RequestBody @Valid ReqVM req) throws Exception{
        log.info("user request {}",req);
        String[] methods = req.getMethod().split("\\.");
        if(methods.length!=2){
            throw new MethodNotFoundException();
        }
        String beanName = methods[0];
        log.info("bean name {}",beanName);
        Object bean = context.getBean(beanName);
        if (bean==null){
            log.info("bean {} is null",beanName);
            throw new MethodNotFoundException();
        }
        if (ApiAbstract.class.isAssignableFrom(bean.getClass())){
            log.info("start invoke method");
            ((ApiAbstract) bean).init(req.toJson());
        }
        log.info("class api {} method api {}",bean.getClass().getName(),methods[1]);
        Class cl = bean.getClass().getSuperclass();
        Method m = cl.getMethod(methods[1]);
        if(!m.isAnnotationPresent(PostExecution.class)){
            throw new MethodNotAllowedException();
        }
        Mono<JSONObject> result = (Mono<JSONObject>) m.invoke(bean);
        return result;
    }
    @PutMapping("/api/request")
    public Mono<JSONObject> putMethod(@RequestParam(required = false) String community,@RequestBody @Valid ReqVM req) throws Exception{
        String[] methods = req.getMethod().split("\\.");
        if(methods.length!=2){
            throw new MethodNotFoundException();
        }
        String beanName = methods[0];
        Object bean = context.getBean(beanName);
        if (bean==null){
            log.info("bean {} is null",beanName);
            throw new MethodNotFoundException();
        }
        if (ApiAbstract.class.isAssignableFrom(bean.getClass())){
            log.info("start invoke method");
            ((ApiAbstract) bean).init(req.toJson());
        }
        log.info("class api {}",bean.getClass().getName());
        Class cl = bean.getClass().getSuperclass();
        Method m = cl.getMethod(methods[1]);
        if(!m.isAnnotationPresent(PutExecution.class)){
            throw new MethodNotAllowedException();
        }
        Mono<JSONObject> result = (Mono<JSONObject>) m.invoke(bean);
        return result;
    }
    @DeleteMapping("/api/request")
    public Mono<JSONObject> deleteMethod(ServerHttpRequest request) throws Exception {
        Map map= request.getQueryParams().toSingleValueMap();
        log.info("request param {}",map);
        JSONObject req = new JSONObject(map);
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
        Method m = cl.getMethod(method[1]);
        log.info("support http method get  {}",
                m.isAnnotationPresent(GetExecution.class));
        if(!m.isAnnotationPresent(GetExecution.class)){
            throw new MethodNotAllowedException();
        }
        Mono<JSONObject> result = (Mono<JSONObject>) m.invoke(bean);
        return result;
    }
}
