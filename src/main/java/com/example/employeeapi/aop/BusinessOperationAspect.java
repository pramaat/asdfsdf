package com.example.employeeapi.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BusinessOperationAspect {
    private static final Logger logger = LoggerFactory.getLogger(BusinessOperationAspect.class);

    @Around("@annotation(auditedOperation)")
    public Object recordOperation(ProceedingJoinPoint joinPoint, AuditedOperation auditedOperation) throws Throwable {
        long startedAt = System.nanoTime();
        String actor = currentActor();
        try {
            Object result = joinPoint.proceed();
            logger.info("Business operation completed operation={} actor={} durationMs={}",
                    auditedOperation.value(), actor, elapsedMillis(startedAt));
            return result;
        } catch (Throwable exception) {
            logger.warn("Business operation failed operation={} actor={} durationMs={} exceptionType={}",
                    auditedOperation.value(), actor, elapsedMillis(startedAt), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? "anonymous"
                : authentication.getName();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
