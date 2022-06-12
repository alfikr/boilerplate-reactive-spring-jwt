package com.jasavast.core.annotation;

import com.jasavast.domain.AuditEventModel;
import com.jasavast.repository.AuditAspectRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@Aspect
public class AspectConfiguration {

    @Autowired
    private AuditAspectRepository auditAspectRepository;

    @Around("@annotation(com.jasavast.core.annotation.GetExecution) || " +
            "@annotation(com.jasavast.core.annotation.PostExecution ) ||" +
            "@annotation(com.jasavast.core.annotation.PutExecution) || " +
            "@annotation(com.jasavast.core.annotation.DeleteExecution)")
    public Object proses(ProceedingJoinPoint joinPoint) throws Throwable{
        long start = System.currentTimeMillis();
        try {
            Object o = joinPoint.proceed();
            long end = System.currentTimeMillis();
            auditAspectRepository.save(AuditEventModel.builder()
                    .logType("log")
                    .executionTime((end-start))
                    .executionStart(start)
                    .executionEnd(end)
                    .className(joinPoint.getSignature().getDeclaringTypeName())
                    .methodName(joinPoint.getSignature().getName())
                    .message(String.format("request {}",joinPoint.getArgs()))
                    .title("executed method")
                    .build());
            return o;
        }catch (Throwable e){
            long end = System.currentTimeMillis();
            int line =e.getStackTrace().length>0?e.getStackTrace()[0].getLineNumber():-1;
            auditAspectRepository.save(AuditEventModel.builder()
                    .error(true)
                    .logType("log")
                    .executionTime((end-start))
                    .executionStart(start)
                    .executionEnd(end)
                    .className(joinPoint.getSignature().getDeclaringTypeName())
                    .methodName(joinPoint.getSignature().getName())
                    .errorMessage(e.getMessage())
                    .message(String.format("request {} error on line {}",joinPoint.getArgs(),line))
                    .title("executed method")
                    .errorMessage(e.getMessage())
                    .build());
            throw e;
        }
    }
}
