package com.vanshika.api_rate_limiter_service.aspect;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Spring AOP Logging Aspect.
 *
 * This single class automatically logs method entry, exit, execution time,
 * and exceptions for ALL controllers and services — without touching any
 * business logic code. This is the core principle of Aspect-Oriented Programming.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut: Target everything inside the controller package.
     */
    @Pointcut("within(com.vanshika.api_rate_limiter_service.controller..*)")
    public void controllerMethods() {}

    /**
     * Pointcut: Target everything inside the service package.
     */
    @Pointcut("within(com.vanshika.api_rate_limiter_service.service..*)")
    public void serviceMethods() {}

    /**
     * @Around advice wraps every controller method.
     * Logs the method name, arguments, return value, and how long it took.
     * This fires automatically for every single controller method — no manual log.info needed.
     */
    @Around("controllerMethods()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

        log.debug("[AOP] → Entering {}.{}() with args: {}",
                className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // actually runs the method
        long timeTaken = System.currentTimeMillis() - startTime;

        log.debug("[AOP] ← Exiting {}.{}() — completed in {}ms",
                className, methodName, timeTaken);

        return result;
    }

    /**
     * @Around advice wraps every service method.
     * At DEBUG level, shows method name and execution time.
     * At TRACE level, also shows the method arguments.
     */
    @Around("serviceMethods()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

        log.trace("[AOP] → Entering {}.{}() with args: {}",
                className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long timeTaken = System.currentTimeMillis() - startTime;

        log.debug("[AOP] ← {}.{}() finished in {}ms", className, methodName, timeTaken);

        return result;
    }

    /**
     * @AfterThrowing advice fires whenever any service or controller throws an exception.
     * This captures exception details without needing try-catch blocks everywhere.
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        log.error("[AOP] Exception in {}.{}() — {}: {}",
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
    }
}

