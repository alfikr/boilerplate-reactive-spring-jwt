package com.jasavast.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ApiAbstract {
    private ServerHttpResponse httpResponse;
    private ServerHttpRequest httpRequest;
    @Autowired
    private ReactiveMongoTemplate template;
    public void init(ServerHttpRequest request, ServerHttpResponse response){
        this.httpRequest=request;
        this.httpResponse=response;
    }

}
