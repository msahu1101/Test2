package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.event.EventGridConfig;
import com.mgm.services.booking.room.event.ReservationEventType;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.EventGridProperties;
import com.mgm.services.booking.room.properties.SecretPropertiesAzure;
import com.microsoft.azure.eventgrid.EventGridClient;

import rx.Observable;

/**
 * Unit test class to validate the publishing of event
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReservationEventPublisherImplTest {

    @InjectMocks
    private static ReservationEventPublisherImpl reservationEventPublisher;

    @InjectMocks
    private static EventGridProperties eventGridProperties;

    @InjectMocks
    private static DomainProperties domainProperties;

    @Mock
    private static SecretPropertiesAzure secretProperties;

    @InjectMocks
    private static EventGridConfig eventGridConfig;

    @Mock
    private RoomReservationRequestMapper requestMapper;

    @Mock
    private static EventGridClient eventGridClient;

    private static MockHttpServletRequest request;

    @BeforeClass
    public static void init() {
        eventGridProperties = new EventGridProperties();
        eventGridProperties.setEnabled(true);
        eventGridProperties.setTopic("testTopic");
        eventGridProperties.setSchemaVersion("1.0");
        eventGridProperties.setEnvironment("dev");
        eventGridProperties.setTopicCredentialsKey("key");
        eventGridProperties.setCallbackUrl("callbackUrl");

        domainProperties = new DomainProperties();
        domainProperties.setEventGrid("https://{topichostname}/");

        secretProperties = mock(SecretPropertiesAzure.class);
        when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("topikKey");
        reservationEventPublisher = new ReservationEventPublisherImpl(eventGridProperties);
        eventGridConfig = new EventGridConfig(domainProperties, eventGridProperties, secretProperties);

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        eventGridClient = mock(EventGridClient.class);

        ReflectionTestUtils.setField(eventGridConfig, "eventGridClient", eventGridClient);
        ReflectionTestUtils.setField(reservationEventPublisher, "eventGridConfig", eventGridConfig);

    }

    @Test
    public void shouldPublishEventSuccessfully() throws InterruptedException {
        AtomicBoolean state = new AtomicBoolean(false);
        Void resp = mock(Void.class);
        when(eventGridClient.publishEventsAsync(Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Observable.just(resp).doOnCompleted(() -> state.set(true)));

        RoomReservationV2Response reservationResponse = new RoomReservationV2Response();
        reservationResponse.setConfirmationNumber("confirmationNumber");
        reservationEventPublisher.publishEvent(Collections.singletonList(reservationResponse),
                ReservationEventType.CREATE.toString());
        Thread.sleep(1000);
        assertTrue("State should be changed", state.get());
    }

    @Test
    public void shouldThrowExceptionOnPublishEvent() throws InterruptedException {
        AtomicBoolean state = new AtomicBoolean(false);
        Observable<Void> resp = Observable.error(new RuntimeException("unexcepted"));
        when(eventGridClient.publishEventsAsync(Mockito.anyString(), Mockito.anyList()))
                .thenReturn(resp.doOnError(throwable -> state.set(true)));

        RoomReservationV2Response reservationResponse = new RoomReservationV2Response();
        reservationResponse.setConfirmationNumber("confirmationNumber");
        reservationEventPublisher.publishEvent(Collections.singletonList(reservationResponse),
                ReservationEventType.CREATE.toString());
        Thread.sleep(1000);
        assertTrue("State should be changed", state.get());
    }

    @Test
    public void shouldNotPublishEventWhenToggleIsDisabled() {
        ReflectionTestUtils.setField(reservationEventPublisher, "publishEventEnabled", false);
        RoomReservationV2Response reservationResponse = new RoomReservationV2Response();
        reservationResponse.setConfirmationNumber("confirmationNumber");
        reservationEventPublisher.publishEvent(Collections.singletonList(reservationResponse),
                ReservationEventType.CREATE.toString());
        verify(eventGridConfig.getEventGridClient(), never());
    }

}
