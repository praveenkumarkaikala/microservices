package com.fundmatrix.authuser.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.fundmatrix.authuser.service..*(..))")
    public Object logServiceCall(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info("-> {} args={}", signature, pjp.getArgs());
        try {
            Object result = pjp.proceed();
            log.info("<- {} completed in {}ms", signature, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("x  {} failed after {}ms: {}", signature, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
