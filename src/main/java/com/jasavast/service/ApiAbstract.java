package com.jasavast.service;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ApiAbstract {
    private ServerHttpResponse httpResponse;
    private ServerHttpRequest httpRequest;
    public void init(ServerHttpRequest request, ServerHttpResponse response){
        this.httpRequest=request;
        this.httpResponse=response;
    }

}
