
package com.mgm.services.booking.room.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.CancelReservationDAO;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Unit test class for service methods in CancelService.
 *
 */
@RunWith(MockitoJUnitRunner.class)

public class CancelV2ServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private CancelReservationDAO cancelDao;
    
    @Mock
    private EventPublisherService<?> eventPublisherService;

    @Mock
    private RoomReservationResponseMapper roomReservationResponseMapper;
    
    @Mock
    private ItineraryService itineraryService;
    
    @InjectMocks
    CancelServiceImpl cancelServiceImpl;

    @Mock
    ApplicationProperties appProperties;

    @Mock
	private ReservationEmailV2Service emailService;

    @Mock
    private SecretsProperties secretProperties;

    @Mock
    private ReservationServiceHelper reservationServiceHelper;
    
    @Mock
    private AccertifyInvokeHelper accertifyInvokeHelper;

    @Before
    public void setup() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");
    }

    private RoomReservation getCancelResponse(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());
        return convert(file, RoomReservation.class);
    }

    private RoomReservationV2Response getCancelReservationResponse(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());
        RoomReservationV2Response data = convert(file, RoomReservationV2Response.class);
        return data;
    }

    /**
     * Test cancelReservation for success.
     */
    @Test
    public void cancelReservationSuccessTest() {

        when(cancelDao.cancelReservation(Mockito.any(CancelV2Request.class)))
                .thenReturn(getCancelResponse("/cancelled-reservation.json"));

        when(roomReservationResponseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class)))
                .thenReturn(getCancelReservationResponse("/cancelled-reservation.json"));
        
        doNothing().when(eventPublisherService).publishEvent(Mockito.any(), Mockito.any());

        CancelV2Request cancelRequest = new CancelV2Request();
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setItineraryId("389741569");

        CancelRoomReservationV2Response responseV2 = cancelServiceImpl.cancelReservation(cancelRequest, false);
        assertNotNull(responseV2);
        assertEquals("M00AE5151", responseV2.getRoomReservation().getConfirmationNumber());
        assertEquals("Cancelled", responseV2.getRoomReservation().getState().toString());

    }

    /**
     * Test cancelReservation for reservation not found scenario.
     */
    @Test
    public void cancelReservationFailedTest() {

        when(cancelDao.cancelReservation(Mockito.any(CancelV2Request.class)))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        CancelV2Request cancelRequest = new CancelV2Request();
        cancelRequest.setConfirmationNumber("Foobar");
        cancelRequest.setItineraryId("389741569");

        assertThatThrownBy(() -> cancelServiceImpl.cancelReservation(cancelRequest, false))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    public void test_cancelReservation_withIsNotifyCustomerViaRtcAsTrue_shouldNotInvokeSendCancellationEmail() {

        when(cancelDao.cancelReservation(Mockito.any(CancelV2Request.class)))
                .thenReturn(getCancelResponse("/cancelled-reservation.json"));

        when(roomReservationResponseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class)))
                .thenReturn(getCancelReservationResponse("/cancelled-reservation.json"));

        doNothing().when(eventPublisherService).publishEvent(Mockito.any(), Mockito.any());

        CancelV2Request cancelRequest = new CancelV2Request();
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setItineraryId("389741569");

        when(reservationServiceHelper.isNotifyCustomerViaRTC("66964e2b-2550-4476-84c3-1a4c0c5c067f", false, false))
                .thenReturn(true);

        cancelServiceImpl.cancelReservation(cancelRequest, false);
        verify(emailService, Mockito.times(0)).sendCancellationEmail(Mockito.any(), Mockito.any());

    }

    @Test
    public void test_cancelReservation_withIsNotifyCustomerViaRtcAsFalse_shouldInvokeSendCancellationEmail() {

        when(cancelDao.cancelReservation(Mockito.any(CancelV2Request.class)))
                .thenReturn(getCancelResponse("/cancelled-reservation.json"));

        when(roomReservationResponseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class)))
                .thenReturn(getCancelReservationResponse("/cancelled-reservation.json"));

        doNothing().when(eventPublisherService).publishEvent(Mockito.any(), Mockito.any());

        CancelV2Request cancelRequest = new CancelV2Request();
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setItineraryId("389741569");

        String[] channels = { "web" };
        when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        when(reservationServiceHelper.isNotifyCustomerViaRTC("66964e2b-2550-4476-84c3-1a4c0c5c067f", false, false))
                .thenReturn(false);

        cancelServiceImpl.cancelReservation(cancelRequest, false);
        verify(emailService, Mockito.times(1)).sendCancellationEmail(Mockito.any(), Mockito.any());

    }

}
//