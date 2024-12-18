/**
 * 
 */
package com.mgm.services.booking.room.event;

import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.EventGridProperties;
import com.mgm.services.booking.room.properties.SecretPropertiesAzure;
import com.microsoft.azure.AzureResponseBuilder;
import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.RestClient;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

/**
 * Base Event Client class for initializing the EventGridClient for registering
 * an event in Azure EventGrid
 * 
 * @author vararora
 *
 */
@Component
@Data
@ConditionalOnMissingAwsCloudEnvironment
@Log4j2
public class EventGridConfig {

    private String eventGridEndpoint;

    private EventGridClient eventGridClient;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient.
     * 
     * @param domainProperties
     *            Domain Properties
     * @param eventGridProperties
     *            EventGrid Properties
     * @param secretProperties
     *            Secret Properties
     */
    public EventGridConfig(DomainProperties domainProperties, EventGridProperties eventGridProperties,
            SecretPropertiesAzure secretProperties) {
        this.eventGridEndpoint = domainProperties.getEventGrid();
        long startTime = System.currentTimeMillis();
        String topicKey = secretProperties.getSecretValue(eventGridProperties.getTopicCredentialsKey());
        log.info("Time taken to fetch secret value {} ms", System.currentTimeMillis() - startTime);
        TopicCredentials topicCredentials = new TopicCredentials(topicKey);
        RestClient restClient = new RestClient.Builder()
                .withBaseUrl(eventGridEndpoint)
                .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
                .withSerializerAdapter(new AzureJacksonAdapter()).withCredentials(topicCredentials)
                .withRetryStrategy(new CustomRetryStrategy(eventGridProperties.getRetryCount(),
                        eventGridProperties.getRetryInterval())).build();
        this.eventGridClient = new EventGridClientImpl(restClient);
    }

}
