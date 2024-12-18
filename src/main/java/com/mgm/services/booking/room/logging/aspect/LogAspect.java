package com.mgm.services.booking.room.logging.aspect;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.context.annotation.Configuration;

import com.microsoft.applicationinsights.TelemetryClient;

import lombok.extern.log4j.Log4j2;


@Aspect
@Configuration
@Log4j2
@ConditionalOnMissingAwsCloudEnvironment
public class LogAspect {
    
   @Autowired
    TelemetryClient telemetryClient;

    @Around(value = "@annotation(com.mgm.services.booking.room.logging.annotation.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        final long start = System.currentTimeMillis();
        final Object proceed = joinPoint.proceed();
        final long executionTime = System.currentTimeMillis() - start;

        Map <String, Double> metrics = new HashMap<>();
        Map <String, String> properties = new HashMap<>();

        metrics.put("ProcessingTime", (double)executionTime);
        properties.put("ClassName", joinPoint.getTarget().getClass().toString());

        telemetryClient.trackEvent(joinPoint.getSignature().getName(), properties, metrics);
        log.info(" #Method: " + joinPoint.getSignature().getName() + " #Time: " + executionTime );
        return proceed;
    }
}
