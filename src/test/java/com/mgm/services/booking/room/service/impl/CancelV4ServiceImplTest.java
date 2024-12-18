package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.CancelReservationDAO;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.CancelV4Request;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.IDUtilityService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyPkgComponentCacheService;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Unit test class for service methods in CancelService.
 *
 */

@ExtendWith(MockitoExtension.class)
public class CancelV4ServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private CancelReservationDAO cancelDao;

    @InjectMocks
    CancelServiceImpl cancelServiceImpl;
    
    @Mock
    FindReservationDAO findReservationDAO;
    
    @Mock
    ReservationServiceHelper reservationServiceHelper;
    
    @Mock
    FindReservationServiceHelper findReservationServiceHelper;
    
    @Mock
    private IDUtilityService idUtilityService;

    @Mock
    ApplicationProperties appProperties;
    
    @Mock
    private RoomReservationResponseMapper roomReservationResponseMapper;
    
    @Mock
    private RoomReservationRequestMapper roomReservationRequestMapper;
    
    @Mock
    private PropertyPkgComponentCacheService propertyPkgComponentCacheService;
    
    @Mock
    private EventPublisherService<?> eventPublisherService;
    
    @Mock
	private ReservationEmailV2Service emailService;

    @BeforeEach
    public void setup() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    
    private CancelV4Request getCancelPreviewV4Request() {
        File file = new File(getClass().getResource("/cancelPreview-v4-request.json").getPath());
        return convert(file, CancelV4Request.class);

    }
    
    private CancelV2Request getCancelPreviewV2Request() {
        File file = new File(getClass().getResource("/cancelPreview-v4-request.json").getPath());
        return convert(file, CancelV2Request.class);

    }
    
    private CancelRoomReservationV2Response getCancelPreviewResponse() {
        File file = new File(getClass().getResource("/cancelPreview-v4-response.json").getPath());
        return convert(file, CancelRoomReservationV2Response.class);

    }
    
    private CancelV4Request getCancelCommitV4Request() {
        File file = new File(getClass().getResource("/cancelCommit-v4-Request.json").getPath());
        return convert(file, CancelV4Request.class);

    }
    
    private CancelV2Request getCancelCommitV2Request() {
        File file = new File(getClass().getResource("/cancelCommit-v4-Request.json").getPath());
        return convert(file, CancelV2Request.class);

    }
    
    private CancelRoomReservationV2Response getCancelCommitResponse() {
        File file = new File(getClass().getResource("/cancelCommit-v4-response.json").getPath());
        return convert(file, CancelRoomReservationV2Response.class);

    }
    
    private FindReservationV2Request getFindReservationV2Request() {
        File file = new File(getClass().getResource("/cancelCommit-v4-response.json").getPath());
        return convert(file, FindReservationV2Request.class);

    }
    
    private RoomReservation getReservationResponse() {
        File file = new File(getClass().getResource("/find-resv-v4-response.json").getPath());
        return convert(file, RoomReservation.class);

    }
    

    @Test
    public void cancelPreviewReservationV4SuccessTest() {
    	
        when(reservationServiceHelper.createFindReservationV2Request(Mockito.any(CancelV4Request.class))).thenReturn(getFindReservationV2Request());
        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationV2Request.class))).thenReturn(getReservationResponse());
        when(idUtilityService.isFirstNameLastNameMatching(Mockito.any(ReservationProfile.class), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(reservationServiceHelper.isRequestAllowedToCancelReservation(Mockito.any(RoomReservation.class))).thenReturn(true);
        when(roomReservationResponseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class))).thenReturn(getCancelPreviewResponse().getRoomReservation());
        when(cancelDao.cancelPreviewReservation(Mockito.any(CancelV2Request.class))).thenReturn(getReservationResponse());
        when(reservationServiceHelper.createCancelV2Request(Mockito.any(CancelV4Request.class), Mockito.any(RoomReservation.class))).thenReturn(getCancelPreviewV2Request());
        
        CancelV4Request cancelV4Request = getCancelPreviewV4Request();
        CancelRoomReservationV2Response response = cancelServiceImpl.cancelPreviewReservation(cancelV4Request, null);
        response.getRoomReservation().setPurchasedComponents(getReservationResponse().getPurchasedComponents());

        runAssertions(response);
    }


    @Test
    public void cancelPreviewV4ReservationNotFoundTest() {

        CancelV4Request cancelV4Request = getCancelPreviewV4Request();
        assertThatThrownBy(() -> cancelServiceImpl.cancelPreviewReservation(cancelV4Request, null))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    
    @Test
    public void cancelCommitReservationV4SuccessTest() {
    	
        when(reservationServiceHelper.createFindReservationV2Request(Mockito.any(CancelV4Request.class))).thenReturn(getFindReservationV2Request());
        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationV2Request.class))).thenReturn(getReservationResponse());
        when(findReservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue())).thenReturn(true);
        when(reservationServiceHelper.isRequestAllowedToCancelReservation(Mockito.any(RoomReservation.class))).thenReturn(true);
        when(roomReservationResponseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class))).thenReturn(getCancelCommitResponse().getRoomReservation());
        when(cancelDao.cancelCommitReservation(Mockito.any(CancelV2Request.class))).thenReturn(getReservationResponse());
        when(reservationServiceHelper.createCancelV2Request(Mockito.any(CancelV4Request.class), Mockito.any(RoomReservation.class))).thenReturn(getCancelCommitV2Request());
        doNothing().when(eventPublisherService).publishEvent(Mockito.any(), Mockito.any());

        CancelV4Request cancelV4Request = getCancelCommitV4Request();
        CancelRoomReservationV2Response response = cancelServiceImpl.cancelCommitReservation(cancelV4Request, null);
        response.getRoomReservation().setPurchasedComponents(getReservationResponse().getPurchasedComponents());
        runAssertions(response);

    }
    
    @Test
    public void cancelCommitV4ReservationNotFoundTest() {

        CancelV4Request cancelV4Request = getCancelPreviewV4Request();
        assertThatThrownBy(() -> cancelServiceImpl.cancelCommitReservation(cancelV4Request, null))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }
    
    private void runAssertions(CancelRoomReservationV2Response response) {
    	assertNotNull(response);
    	assertEquals("15T3R6HP40", response.getRoomReservation().getConfirmationNumber());
        assertEquals("Cancelled", response.getRoomReservation().getState().toString());
        assertEquals("Settled", response.getRoomReservation().getPayments().get(0).getStatus().toString());
        assertEquals("COMPONENTCD-v-CFBP1-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CFBP1",response.getRoomReservation().getPurchasedComponents().get(0).getId());
        assertEquals(true, response.getRoomReservation().getPurchasedComponents().get(0).isNonEditable());
        assertEquals(true, response.getRoomReservation().getPurchasedComponents().get(0).getIsPkgComponent());
    }
}
