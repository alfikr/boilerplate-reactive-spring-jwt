package com.jasavast.service;

import org.json.JSONObject;

public class ApiAbstract {
    protected JSONObject req;
    protected JSONObject reqData;
    public void init(JSONObject req){
        this.req=req;
        this.reqData=req.getJSONObject("data");
    }

}
