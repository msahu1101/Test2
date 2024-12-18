package com.mgm.services.booking.room.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.logging.AppInsightsTelemetryInitializer;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.DoubleRoundSerializer;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for defining beans which application intends to be
 * available for auto wiring.
 */
@Configuration
@EnableScheduling
@Log4j2
public class BeanConfiguration {

    private static final int THREAD_POOL = 10;

    private static final int MAX_TOTAL_CONNECTIONS = 500;
    private static final int MAX_PER_ROUTE = 50;
    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 20 * 1000;
    private static final int CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS = 30;
    // Determines the timeout in milliseconds until a connection is established.
    private static final int CONNECT_TIMEOUT = 5000;

    // The timeout when requesting a connection from the connection manager.
    private static final int REQUEST_TIMEOUT = 10000;

    // The timeout for waiting for data
    private static final int SOCKET_TIMEOUT = 10000;

    @Autowired
    private ApplicationProperties appProps;

    @Bean
    public EhCacheManagerFactoryBean getEhCacheManagerFactoryBean() {
        return new EhCacheManagerFactoryBean();
    }

    @Bean
    public ScheduledExecutorService getScheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(THREAD_POOL);
    }
    
    @Bean
    public AppInsightsTelemetryInitializer appInsightsInitializer() {

        TelemetryConfiguration.getActive().getTelemetryInitializers().add(new AppInsightsTelemetryInitializer());
        return new AppInsightsTelemetryInitializer();
    }

    @Bean
    public PoolingHttpClientConnectionManager poolingConnectionManager() {
        return CommonUtil.poolingConnectionManager(appProps.isSslInsecure(),MAX_TOTAL_CONNECTIONS,MAX_PER_ROUTE);
    }

    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return CommonUtil.connectionKeepAliveStrategy(appProps.getCommonRestTTL());
        
    }

    @Bean
    public CloseableHttpClient httpClient() {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(appProps.getReadTimeOut())
                .setConnectTimeout(appProps.getConnectionTimeout()).setSocketTimeout(appProps.getSocketTimeOut()).build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                //.evictExpiredConnections()
                .setConnectionManager(poolingConnectionManager())
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .build();
    }

    @Bean
    public Runnable idleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
        
        return new Runnable() {
            @Override
            @Scheduled(
                    fixedDelay = 10000)
            public void run() {
                try {
                    if (connectionManager != null) {
                        log.trace("run IdleConnectionMonitor - Closing expired and idle connections...");
                        connectionManager.closeExpiredConnections();
                        connectionManager.closeIdleConnections(CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS, TimeUnit.SECONDS);
                    } else {
                        log.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
                    }
                } catch (Exception e) {
                    log.error("run IdleConnectionMonitor - Exception occurred. msg={}, e={}", e.getMessage(), e);
                }
            }
        };
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient());
        return clientHttpRequestFactory;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    /**
     * Defining ObjectMapper bean to add custom serializer for formatting double
     * value types.
     * 
     * @return Object Mapper
     */
    @Bean
    public ObjectMapper jsonObjectMapper() {
        final ArrayList<Module> modules = new ArrayList<>();

        final SimpleModule module = new SimpleModule();
        // adding our custom serializer
        module.addSerializer(double.class, new DoubleRoundSerializer());
        module.addSerializer(Double.class, new DoubleRoundSerializer());

        // support for ACRS (De)serializing.
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());

        modules.add(module);
        modules.add(new JavaTimeModule());

        return Jackson2ObjectMapperBuilder.json().modules(modules).build()
                .setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.amadeus.xml.ahp.rates");
        return marshaller;
    }

    @Bean(name = "restTemplateRetryTemplate")
    public RetryTemplate restTemplateRetryTemplate() {
        return createRestTemplateRetryTemplate();
    }

    private RetryTemplate createRestTemplateRetryTemplate() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(ResourceAccessException.class, true);
        retryableExceptions.put(HttpServerErrorException.ServiceUnavailable.class, true);
        retryableExceptions.put(HttpServerErrorException.BadGateway.class, true);
        retryableExceptions.put(HttpServerErrorException.GatewayTimeout.class, true);
        retryableExceptions.put(HttpClientErrorException.TooManyRequests.class, true);
        retryableExceptions.put(HttpClientErrorException.BadRequest.class, true);
        return createRetryTemplate(retryableExceptions);
    }

    private RetryTemplate createRetryTemplate(Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy = new ExponentialRandomBackOffPolicy();
        exponentialRandomBackOffPolicy.setInitialInterval(200);
        exponentialRandomBackOffPolicy.setMaxInterval(400);
        exponentialRandomBackOffPolicy.setMultiplier(2);
        retryTemplate.setBackOffPolicy(exponentialRandomBackOffPolicy);
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, retryableExceptions));
        return retryTemplate;
    }

}
