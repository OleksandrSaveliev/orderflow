package com.my.orderflow.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingAspect.class);
    private static final String REQUEST_ID = "requestId";

    @Around("execution(* com.my.orderflow.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("Request: {} | args: {}", method, args);

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String requestId = MDC.get(REQUEST_ID);

            if (exception != null) {
                log.warn("Response: {} | duration: {}ms | requestId: {} | error: {}",
                        method, duration, requestId, exception.getMessage());
            } else {
                log.info("Response: {} | duration: {}ms | requestId: {}", method, duration, requestId);
            }
        }
    }
}