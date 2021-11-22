package com.jasavast.api.coba;

import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.service.ApiAbstract;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component("apiCobaAja")
public class CobaAja extends ApiAbstract {

        @GetExecution
        public Mono<JSONObject> sampleAja(){
            return Mono.just(new JSONObject()
                    .put("success",true)
                    .put("data",new JSONObject().put("method","Get Method success")));
        }
        @PostExecution(id = "SMP001")
        public Mono<JSONObject> samplePost(){
            return Mono.just(new JSONObject()
                    .put("success",true).put("data",new JSONObject("method","Post Method success")));
        }
}
