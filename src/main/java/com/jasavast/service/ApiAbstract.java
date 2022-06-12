package com.jasavast.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ApiAbstract {
    private ServerHttpResponse httpResponse;
    private ServerHttpRequest httpRequest;
    @Autowired
    private R2dbcEntityTemplate template;
    public void init(ServerHttpRequest request, ServerHttpResponse response){
        this.httpRequest=request;
        this.httpResponse=response;
    }

}
