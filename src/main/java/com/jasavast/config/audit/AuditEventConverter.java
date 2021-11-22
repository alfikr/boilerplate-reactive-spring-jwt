package com.jasavast.config.audit;

import com.jasavast.domain.AuditEventModel;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;
@Component
public class AuditEventConverter {
    private Map<String,Object> convertDataToObjects(Map<String,String> data){
        Map<String,Object> result = new HashMap();
        if (data!=null){
            for (Map.Entry<String,String> entry:data.entrySet()){
                result.put(entry.getKey(),entry.getValue());
            }
        }
        return result;
    }
    public Map<String, String> convertDataToStrings(Map<String, Object> data) {
        Map<String, String> results = new HashMap<>();

        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                // Extract the data that will be saved.
                if (entry.getValue() instanceof WebAuthenticationDetails) {
                    WebAuthenticationDetails authenticationDetails = (WebAuthenticationDetails) entry.getValue();
                    results.put("remoteAddress", authenticationDetails.getRemoteAddress());
                    results.put("sessionId", authenticationDetails.getSessionId());
                } else {
                    results.put(entry.getKey(), Objects.toString(entry.getValue()));
                }
            }
        }
        return results;
    }
    public List<AuditEvent> convertToAuditEvent(Iterable<AuditEventModel> persistentAuditEvents){
        if (persistentAuditEvents==null){
            return Collections.emptyList();
        }
        List<AuditEvent> auditEvents= new ArrayList<>();
        persistentAuditEvents.forEach(x->{
            auditEvents.add(convertToAuditEvent(x));
        });

        return auditEvents;
    }
    public AuditEvent convertToAuditEvent(AuditEventModel auditEvent){
        if (auditEvent==null){
            return null;
        }
        return new AuditEvent(auditEvent.getAuditEventDate().atZone(ZoneId.systemDefault()).toInstant(),auditEvent.getPrincipal(),
                auditEvent.getAuditEventType(),
                convertDataToObjects(auditEvent.getData()));
    }
}
