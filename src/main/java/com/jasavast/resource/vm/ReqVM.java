package com.jasavast.resource.vm;

import lombok.Data;
import org.json.JSONObject;

import javax.validation.constraints.NotBlank;

@Data
public class ReqVM {
    @NotBlank
    private String method;
    private Object data;

    public JSONObject toJson(){
        return new JSONObject().put("method",this.method)
                .put("data",this.data);
    }
}
