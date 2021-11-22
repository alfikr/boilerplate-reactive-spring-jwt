package com.jasavast.api.coba;

import com.jasavast.core.annotation.DeleteExecution;
import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.annotation.PutExecution;
import com.jasavast.service.ApiAbstract;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("apiCobaAja")
public class CobaAja extends ApiAbstract {
    @GetExecution
    public Mono<JSONObject> getMethod(){
        return Mono.justOrEmpty(getDefault(new JSONObject().put("message","Get method success").put("data",reqData)));
    }
    @PostExecution
    public Mono<JSONObject> postMethod(){
        return Mono.justOrEmpty(getDefault(new JSONObject().put("message","Post method success").put("data",reqData)));
    }
    @PutExecution
    public Mono<JSONObject> putMethod(){
        return Mono.justOrEmpty(getDefault(new JSONObject().put("message","Put method success").put("data",reqData)));
    }
    @DeleteExecution
    public Mono<JSONObject> deleteMethod(){
        return Mono.justOrEmpty(getDefault(new JSONObject().put("message","Delete method success").put("data",reqData)));
    }
    private JSONObject getDefault(JSONObject data){
        return new JSONObject()
                .put("success",true)
                .put("data",data).put("errors",new JSONArray());
    }
}
