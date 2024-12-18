package com.mgm.services.booking.room.config;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Aspect configuration class to monitor performance for all methods in DAO
 * classes.
 * 
 */
@Configuration
@EnableAspectJAutoProxy
@Aspect
public class PerformanceMetricsConfiguration {

    /**
     * Monitoring pointcut.
     */
    @Pointcut("execution(* com.mgm.services.booking.room.dao.impl.*.*(..))")
    public void monitor() {
        // No need to implement
    }

    /**
     * Creates instance of performance monitor interceptor.
     * 
     * @return performance monitor interceptor
     */
    @Bean
    public PerformanceMonitorInterceptor performanceMonitorInterceptor() {
        return new PerformanceMonitorInterceptor(false);
    }

    /**
     * Creates instance of performance monitor advisor.
     * 
     * @return performance monitor advisor
     */
    @Bean
    public Advisor performanceMonitorAdvisor() {
        final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("com.mgm.services.booking.room.config.PerformanceMetricsConfiguration.monitor()");
        return new DefaultPointcutAdvisor(pointcut, performanceMonitorInterceptor());
    }
}
