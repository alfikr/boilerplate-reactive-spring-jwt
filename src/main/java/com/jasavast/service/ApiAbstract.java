package com.jasavast.service;

import org.json.JSONObject;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ApiAbstract {
    protected JSONObject req;
    protected JSONObject reqData;
    public void init(JSONObject req){
        this.req=req;
        this.reqData=req.getJSONObject("data");
    }

}
