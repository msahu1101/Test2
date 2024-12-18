package com.mgm.services.booking.room.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.event.EventGridConfig;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.model.event.DataGovernance;
import com.mgm.services.booking.room.model.event.ReservationEventRequestWrapper;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.EventGridProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.models.EventGridEvent;

import lombok.extern.log4j.Log4j2;
import rx.schedulers.Schedulers;

/**
 * Implementation class for ReservationService
 */
@Component
@Log4j2
@Primary
public class ReservationEventPublisherImpl implements EventPublisherService<RoomReservationV2Response> {

	
	private static final String EVENT_SUBJECT = "Room/Reservation";


    @Autowired(required = false)
    private EventGridConfig eventGridConfig;
    
    @Autowired
    private RoomReservationRequestMapper requestMapper;

    private String topic;
    private String schemaVersion;
    private boolean publishEventEnabled;
    private String callbackUrl;
    private String environment;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient.
     * 
     * @param eventGridProperties
     *            EventGrid Properties
     * @param baseEventClient
     *            BaseEventClient
     */
    public ReservationEventPublisherImpl(EventGridProperties eventGridProperties) {
        super();
        this.topic = eventGridProperties.getTopic();
        this.publishEventEnabled = eventGridProperties.isEnabled();
        this.schemaVersion = eventGridProperties.getSchemaVersion();
        this.callbackUrl = eventGridProperties.getCallbackUrl();
        this.environment = eventGridProperties.getEnvironment();
    }

    public boolean isPublishEventEnabled() {
        return publishEventEnabled;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getEnvironment() {
        return environment;
    }

    @LogExecutionTime
    @Override
    public void publishEvent(List<RoomReservationV2Response> roomReservationV2ResponseList, String eventType) {

        if (null != eventGridConfig && isPublishEventEnabled()
                && CollectionUtils.isNotEmpty(roomReservationV2ResponseList)) {
            try {
                EventGridClient eventGridClient = eventGridConfig.getEventGridClient();
                List<EventGridEvent> eventsList = new ArrayList<>();
                StringBuilder confNumString = new StringBuilder("");

                buildEventsMap(roomReservationV2ResponseList).forEach((confNum, data) -> {
                    confNumString.append(confNum).append(ServiceConstant.WHITESPACE_STRING);
                    log.info("Event body to publish {}", CommonUtil.convertObjectToJsonString(data));
                    eventsList.add(new EventGridEvent(UUID.randomUUID().toString(), EVENT_SUBJECT, data, eventType,
                            DateTime.now(), schemaVersion).withTopic(topic));
                });

                String eventGridEndpoint = eventGridConfig.getEventGridEndpoint();
                log.info("Publishing event of type ({}) to EventGrid", eventType);
                eventGridClient.publishEventsAsync(eventGridEndpoint, eventsList).subscribeOn(Schedulers.newThread())
                        .doOnError(throwable -> log.error(
                                "Failed to publish event of type({}) for subject {} and Ids ({}) with error: {}",
                                eventType, EVENT_SUBJECT, confNumString.toString().trim(), throwable.getMessage(),
                                throwable))
                        .doOnCompleted(() -> log.info(
                                "Successfully published an event of type({}) for subject {} to EventGrid", eventType,
                                EVENT_SUBJECT))
                        .subscribe();

            } catch (Exception ex) {
                log.error("Unable to publishing event to EventGrid: ", ex);
            }
        }
    }
        
    private Map<String, Object> buildEventsMap(List<RoomReservationV2Response> roomReservationV2ResponseList) {

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        Map<String, String> headers = new HashMap<>();
        headers.put(ServiceConstant.X_MGM_TRANSACTION_ID, httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID));
        headers.put(ServiceConstant.X_MGM_CORRELATION_ID, httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID));
        headers.put(ServiceConstant.X_MGM_SOURCE, CommonUtil.getSourceHeaderWithFallback(httpRequest));
        headers.put(ServiceConstant.X_MGM_CHANNEL, CommonUtil.getChannelHeaderWithFallback(httpRequest));

        DataGovernance dataGovernance = new DataGovernance();
        dataGovernance.setEventExpiryTime(null);
        dataGovernance.setCatalogId(EVENT_SUBJECT);
        dataGovernance.setContainsPCI(true);
        dataGovernance.setContainsPII(true);
        dataGovernance.setTags(new String[0]);

        return roomReservationV2ResponseList.stream()
                .collect(Collectors.toMap(RoomReservationV2Response::getConfirmationNumber, response -> {
                    ReservationEventRequestWrapper wrappedRequest = new ReservationEventRequestWrapper();
                    ReservationEventRequestWrapper.EventBody body = wrappedRequest.new EventBody();
                    body.setRoomReservation(requestMapper.roomReservationResponseToEventRequest(response));
                    wrappedRequest.setBody(body);
                    wrappedRequest.setHeaders(headers);
                    wrappedRequest.setDataGovernance(dataGovernance);
                    UriTemplate uriTemplate = new UriTemplate(getCallbackUrl());
                    Map<String, String> uriVariables = new HashMap<>();
                    uriVariables.put(ServiceConstant.ENVIRONMENT_PH, getEnvironment());
                    uriVariables.put(ServiceConstant.CONFIRMATION_NUMBER_PH, response.getConfirmationNumber());
                    URI uri = uriTemplate.expand(uriVariables);
                    wrappedRequest.setCallbackUrl(uri.toString());
                    return wrappedRequest;
                }));

    }

}
