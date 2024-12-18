package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.ModifyReservationDAO;
import com.mgm.services.booking.room.dao.OCRSDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.mapper.PreviewModifyRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapperImpl;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapperImpl;
import com.mgm.services.booking.room.mapper.UserProfileInfoRequestMapper;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.ModifyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.PreviewCommitRequest;
import com.mgm.services.booking.room.model.request.ReservationAssociateRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.request.TripDetail;
import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.service.IDUtilityService;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit test class for service methods in ModifyReservationService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ModifyReservationServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private ModifyReservationDAO modifyReservationDAO;

    @InjectMocks
    private ModifyReservationServiceImpl modifyReservationServiceImpl;

    @Mock
    private ItineraryService itineraryService;

    @Mock
    private ItineraryServiceImpl itineraryServiceImpl;

    @Mock
    private RoomReservationRequestMapper requestMapper;

    @Mock
    private RoomReservationResponseMapper responseMapper;

    @Mock
    private PreviewModifyRequestMapper previewMapper;

    @Mock
    private EventPublisherService<?> eventPublisherService;

    @Mock
    private AccertifyInvokeHelper accertifyInvokeHelper;

    @Mock
    private RoomProgramDAO roomProgramDao;

    @Mock
    HttpServletRequest request;

    @Mock
    private ApplicationProperties appProperties;

    @Mock
    private ReservationEmailV2Service emailService;

    @Mock
    private ReservationServiceHelper reservationServiceHelper;

    @Mock
    private FindReservationDAO findReservationDao;
    
    @Mock
    private FindReservationService findResvService;

    @Mock
    private IDUtilityService idUtilityService;
    
    @Mock
    private OCRSDAO ocrsDao;
    
    @Mock
    private UserProfileInfoRequestMapper userProfileInfoRequestMapper;

    @Mock
    private SecretsProperties secretProperties;

    @Mock
    private ServiceConversionHelper serviceConversionHelper;
    
    @Mock
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private <T> T getObject(String fileName, Class<T> target) {
        File file = new File(getClass().getResource(fileName).getPath());
        return convert(file, target);
    }

    private RoomReservation getCommitRoomReservationResponse_ForNoDeposit() {
        return getObject("/paymentwidgetv4/commit/no_deposit/crs-commit-roomreservation-response-nodeposit.json", RoomReservation.class);
    }

    private PaymentRoomReservationRequest getCommitRequest_ForNoDeposit() {
        return getObject("/paymentwidgetv4/commit/no_deposit/commit-v4-nodeposit-request.json", PaymentRoomReservationRequest.class);
    }

    private RoomReservation getCommitRoomReservationResponse_ForAddnDeposit() {
        return getObject("/paymentwidgetv4/commit/addn_deposit/crs-commit-roomreservation-response-addndeposit.json", RoomReservation.class);
    }

    private ModifyRoomReservationResponse getRBSCommitResponse_ForAddnDeposit() {
        return getObject("/paymentwidgetv4/commit/addn_deposit/rbs-commitv4-addndeposit-response.json", ModifyRoomReservationResponse.class);
    }

    private PaymentRoomReservationRequest getCommitRequest_ForAddnDeposit() {
        return getObject("/paymentwidgetv4/commit/addn_deposit/commit-v4-addndeposit-request.json", PaymentRoomReservationRequest.class);
    }

    private RoomReservation getCommitRoomReservationResponse_ForRefundDeposit() {
        return getObject("/paymentwidgetv4/commit/refund_deposit/crs-commit-roomreservation-response-refunddeposit.json", RoomReservation.class);
    }

    private PaymentRoomReservationRequest getCommitRequest_ForRefundDeposit() {
        return getObject("/paymentwidgetv4/commit/refund_deposit/commit-v4-refunddeposit-request.json", PaymentRoomReservationRequest.class);
    }

    private ModifyRoomReservationResponse getRBSCommitResponse_ForRefundDeposit() {
        return getObject("/paymentwidgetv4/commit/refund_deposit/rbs-commitv4-refunddeposit-response.json", ModifyRoomReservationResponse.class);
    }

    private RoomReservation getPendingRoomReservationResponse_ForSingleToSharedUseCase() {
        return getObject("/paymentwidgetv4/modifyPending/singleToShared/modify-pending-dao-roomReservation-response.json", RoomReservation.class);
    }

    private RoomProgramValidateResponse getValidateProgramV2Response() {
        return getObject("/paymentwidgetv4/commit/validateProgramV2-dao-response.json", RoomProgramValidateResponse.class);
    }

    // ModifyController v1 related unit tests

    /**
     * Test preModifyReservation method in the service class for success
     */
    @Test
    public void preModifyReservationSuccessTest() {

        when(modifyReservationDAO.preModifyReservation(Mockito.any(PreModifyRequest.class)))
                .thenReturn(getObject("/modifyreservationdao-premodify-response.json", RoomReservation.class));
        PreModifyRequest preModifyRequest = new PreModifyRequest();
        preModifyRequest.setConfirmationNumber("M00AE6261");
        preModifyRequest.setFirstName("Test");
        preModifyRequest.setFirstName("Test");

        RoomReservation response = modifyReservationServiceImpl.preModifyReservation(preModifyRequest);

        Assertions.assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getPropertyId());
        Assertions.assertEquals("M00AE6261", response.getConfirmationNumber());

    }

    /**
     * Test preModifyReservation method in the service class for invalid dates
     * scenario.
     */
    @Test
    public void preModifyReservationInvalidDatesTest() {
        when(modifyReservationDAO.preModifyReservation(Mockito.any(PreModifyRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.DATES_UNAVAILABLE));
        PreModifyRequest preModifyRequest = new PreModifyRequest();
        preModifyRequest.setConfirmationNumber("M00AE6261");
        preModifyRequest.setFirstName("Test");
        preModifyRequest.setFirstName("Test");

        TripDetail tripDetail = new TripDetail();
        // past dates
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -4);
        tripDetail.setCheckInDate(calendar.getTime());

        // past dates
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -2);
        tripDetail.setCheckOutDate(calendar1.getTime());
        preModifyRequest.setTripDetails(tripDetail);

        try {
            modifyReservationServiceImpl.preModifyReservation(preModifyRequest);
        } catch (BusinessException businessException) {
            Assertions.assertEquals("<_dates_not_available>[ One of more dates are not available ]",
                    businessException.getMessage());
        }
    }

    /**
     * Test modifyReservation method in the service class for successful
     * modification
     */
    @Test
    public void modifyReservationSuccessTest() {

        when(modifyReservationDAO.modifyReservation(Mockito.any(), Mockito.any()))
                .thenReturn(getObject("/modifyreservationdao-modifyReservation-response.json", RoomReservation.class));

        String source = "mgmresorts";
        RoomReservation request = new RoomReservation();

        RoomReservation response = modifyReservationServiceImpl.modifyReservation(source, request);

        Assertions.assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getPropertyId());
        Assertions.assertEquals("M00AE6261", response.getConfirmationNumber());

    }

    /**
     * Test modifyReservation method in the service class for invalid dates
     * scenario.
     */
    @Test
    public void modifyReservationInvalidDatesTest() {

        when(modifyReservationDAO.modifyReservation(Mockito.any(), Mockito.any()))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        String source = "mgmresorts";
        RoomReservation request = new RoomReservation();
        // invalid conf #
        request.setConfirmationNumber("1233");

        assertThatThrownBy(() -> modifyReservationServiceImpl.modifyReservation(source, request))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    // ModifyController v2 related unit tests

    @Test
    public void shouldInvokeupdateCustomerItineraryWhenEnabled() {
        ModifyRoomReservationRequest modifyReservationRequest = new ModifyRoomReservationRequest();
        RoomReservation resv = getObject("/modifyreservationdao-modifyReservation-response.json",
                RoomReservation.class);
        when(requestMapper.roomReservationRequestToModel(Mockito.any())).thenReturn(resv);
        ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", true);
        when(modifyReservationDAO.modifyRoomReservationV2(Mockito.any())).thenReturn(resv);
        when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(new RoomReservationV2Response());
        String[] channels = { "ice" };
        when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
        RoomReservationRequest roomReservation = new RoomReservationRequest();
        modifyReservationRequest.setRoomReservation(roomReservation);
        modifyReservationServiceImpl.modifyRoomReservationV2(modifyReservationRequest);
        verify(itineraryService, Mockito.times(1)).createOrUpdateCustomerItinerary(Mockito.any(),Mockito.any());
    }

    @Test
    public void shouldNotInvokeupdateCustomerItineraryWhenDisabled() {
        ModifyRoomReservationRequest modifyReservationRequest = new ModifyRoomReservationRequest();
        RoomReservation resv = getObject("/modifyreservationdao-modifyReservation-response.json",
                RoomReservation.class);
        when(requestMapper.roomReservationRequestToModel(Mockito.any())).thenReturn(resv);
        ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", false);
        when(modifyReservationDAO.modifyRoomReservationV2(Mockito.any())).thenReturn(resv);
        when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(new RoomReservationV2Response());
        when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
        String[] channels = { "ice" };
        when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        RoomReservationRequest roomReservation = new RoomReservationRequest();
        modifyReservationRequest.setRoomReservation(roomReservation);
        modifyReservationServiceImpl.modifyRoomReservationV2(modifyReservationRequest);
        verify(itineraryService, Mockito.times(0)).createOrUpdateCustomerItinerary(Mockito.any(),Mockito.any());
    }

    @Test
    public void test_modifyRoomReservationV2_withIsNotifyCustomerViaRtcAsTrue_shouldNotInvokeSendConfirmationEmail() {
        ModifyRoomReservationRequest modifyReservationRequest = new ModifyRoomReservationRequest();
        RoomReservation resv = getObject("/modifyreservationdao-modifyReservation-response.json",
                RoomReservation.class);
        when(requestMapper.roomReservationRequestToModel(Mockito.any())).thenReturn(resv);
        when(modifyReservationDAO.modifyRoomReservationV2(Mockito.any())).thenReturn(resv);
        when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(new RoomReservationV2Response());
        when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
        when(reservationServiceHelper.isNotifyCustomerViaRTC(null, false, false)).thenReturn(true);
        RoomReservationRequest roomReservation = new RoomReservationRequest();
        modifyReservationRequest.setRoomReservation(roomReservation);
        modifyReservationServiceImpl.modifyRoomReservationV2(modifyReservationRequest);
        verify(emailService, Mockito.times(0)).sendConfirmationEmail(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void test_modifyRoomReservationV2_withIsNotifyCustomerViaRtcAsFalse_shouldInvokeSendConfirmationEmail() {
        ModifyRoomReservationRequest modifyReservationRequest = new ModifyRoomReservationRequest();
        RoomReservation resv = getObject("/modifyreservationdao-modifyReservation-response.json",
                RoomReservation.class);
        when(requestMapper.roomReservationRequestToModel(Mockito.any())).thenReturn(resv);
        when(modifyReservationDAO.modifyRoomReservationV2(Mockito.any())).thenReturn(resv);
        when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(new RoomReservationV2Response());
        String[] channels = { "web" };
        when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
        when(reservationServiceHelper.isNotifyCustomerViaRTC(null, false, false)).thenReturn(false);
        RoomReservationRequest roomReservation = new RoomReservationRequest();
        modifyReservationRequest.setRoomReservation(roomReservation);
        modifyReservationServiceImpl.modifyRoomReservationV2(modifyReservationRequest);
        verify(emailService, Mockito.times(1)).sendConfirmationEmail(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    /**
     * Test preModifyReservation method in the service class for V2 request.
     */
    @Test
    public void preModifyV2ReservationSuccessTest() {
        PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);
      FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(preModifyV2Request);

        RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",
                RoomReservation.class);
        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
        when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class),
                Mockito.anyString())).thenReturn(true);
        when(reservationServiceHelper.validateLoggedUserOrServiceToken(Mockito.anyString())).thenReturn(false);
        when(reservationServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, findRoomReservation))
                .thenReturn(true);

        RoomReservation reservation = getObject("/preModifyReservation-v2-preModifyReservation.json",
                RoomReservation.class);

        when(modifyReservationDAO.preModifyReservation(preModifyV2Request)).thenReturn(reservation);

        RoomReservationV2Response roomReservationResponse = getObject(
                "/preModifyReservation-v2-roomReservationModelToResponse.json", RoomReservationV2Response.class);

        when(responseMapper.roomReservationModelToResponse(reservation)).thenReturn(roomReservationResponse);

        ModifyRoomReservationResponse response = modifyReservationServiceImpl.preModifyReservation(preModifyV2Request,
                Mockito.anyString());

        Assertions.assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getRoomReservation().getPropertyId());
        Assertions.assertEquals("M03D5C0E1", response.getRoomReservation().getConfirmationNumber());
        double expectedPreviousDesposit = BigDecimal.valueOf(findRoomReservation.getDepositCalc().getAmount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double actualPreviousDesposit = response.getRoomReservation().getRatesSummary().getPreviousDeposit();
        Assertions.assertEquals(expectedPreviousDesposit, actualPreviousDesposit, 0.001);
    }
   
    

    /**
     * Test preModifyv2Reservation method in the service class for without tripDates
     * scenario on v2 modify reservation flow.
     */
    @Test
    public void preModifyV2Reservation_withNoTripDetails_successTest() {
        PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);
        preModifyV2Request.setTripDetails(null);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(preModifyV2Request);

        RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",
                RoomReservation.class);

        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
        when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class),
                Mockito.anyString())).thenReturn(true);
        when(reservationServiceHelper.validateLoggedUserOrServiceToken(Mockito.anyString())).thenReturn(false);
        when(reservationServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, findRoomReservation))
                .thenReturn(true);

        RoomReservation reservation = getObject("/preModifyReservation-v2-preModifyReservation.json",
                RoomReservation.class);

        when(modifyReservationDAO.preModifyReservation(preModifyV2Request)).thenReturn(reservation);

        RoomReservationV2Response roomReservationResponse = getObject(
                "/preModifyReservation-v2-roomReservationModelToResponse.json", RoomReservationV2Response.class);

        when(responseMapper.roomReservationModelToResponse(reservation)).thenReturn(roomReservationResponse);

        ModifyRoomReservationResponse response = modifyReservationServiceImpl.preModifyReservation(preModifyV2Request,
                Mockito.anyString());

        Assertions.assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getRoomReservation().getPropertyId());
        Assertions.assertEquals("M03D5C0E1", response.getRoomReservation().getConfirmationNumber());
        double expectedPreviousDesposit = BigDecimal.valueOf(findRoomReservation.getDepositCalc().getAmount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double actualPreviousDesposit = response.getRoomReservation().getRatesSummary().getPreviousDeposit();
        Assertions.assertEquals(expectedPreviousDesposit, actualPreviousDesposit, 0.001);
    }

    /**
     * Test preModifyv2Reservation method in the service class for invalid dates
     * scenario on v2 modify reservation flow.
     */

    @Test
    public void preModifyRoom_WithNotEligibleForModification_validateErrorMessage() {
        PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(preModifyV2Request);

        RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",
                RoomReservation.class);

        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
        when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class),
                Mockito.anyString())).thenReturn(false);

        try {
            modifyReservationServiceImpl.preModifyReservation(preModifyV2Request, Mockito.anyString());
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.RESERVATION_NOT_MODIFIABLE, e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }
    }

    @Test
    public void preModifyRoom_WithNotMatchingFirstAndLastNames_validateErrorMessage() {
        PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(preModifyV2Request);

        RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",
                RoomReservation.class);

        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
        when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class),
                Mockito.anyString())).thenReturn(true);
        when(reservationServiceHelper.validateLoggedUserOrServiceToken(Mockito.anyString())).thenReturn(false);
        when(reservationServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, findRoomReservation))
                .thenReturn(false);

        try {
            modifyReservationServiceImpl.preModifyReservation(preModifyV2Request, Mockito.anyString());
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.RESERVATION_NOT_FOUND, e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }
    }
    @Test
    public void preModifyReservation_ReservationNotFoundTest() {
        PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);
        FindReservationV2Request findReservationV2Request = reservationServiceHelper.createFindReservationV2Request(preModifyV2Request);
        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(null);
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
        modifyReservationServiceImpl.preModifyReservation(preModifyV2Request, ArgumentMatchers.any()));
        Assertions.assertEquals(ErrorCode.RESERVATION_NOT_FOUND, exception.getErrorCode());
    }
    @Test
     public void preModifyReservation_ReservationNotModifiableTest() {
    	 PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);
         FindReservationV2Request findReservationV2Request = reservationServiceHelper.createFindReservationV2Request(preModifyV2Request);
         RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",RoomReservation.class);
         when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
        RoomReservation mockRoomReservation = new RoomReservation();
        
        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(mockRoomReservation);
        when(reservationServiceHelper.isRequestAllowedToModifyReservation(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(false);

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
        modifyReservationServiceImpl.preModifyReservation(preModifyV2Request, ArgumentMatchers.any()));

        Assertions.assertEquals(ErrorCode.RESERVATION_NOT_MODIFIABLE, exception.getErrorCode());
    }
     

    @Test
    public void previewCommit_WithNotMatchingTotals_validateExistenceOfErrorAndReservationObjects() {

        PreviewCommitRequest commitRequest = getObject("/previewCommitRequest.json", PreviewCommitRequest.class);
        commitRequest.setPreviewReservationTotal(commitRequest.getPreviewReservationTotal() + 1);

        ModifyRoomReservationResponse response = callCommitReservation(commitRequest);

        Assertions.assertNotNull(response.getError(), "Error object must be there");
        Assertions.assertTrue(StringUtils.endsWith(response.getError().getCode(),
                ErrorCode.MODIFY_VIOLATION_PRICE_CHANGE.getNumericCode()), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        Assertions.assertNotNull(response.getRoomReservation(), "Room Reservation object must be there");
    }

    @Test
    public void previewCommit_WithIncreaseinDepositAndNoCVV_validateError() {

        PreviewCommitRequest commitRequest = getObject("/previewCommitRequest.json", PreviewCommitRequest.class);
        commitRequest.setPreviewReservationDeposit(commitRequest.getPreviewReservationDeposit() + 1);
        commitRequest.setCvv(null);

        try {
            callCommitReservation(commitRequest);
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.MODIFY_VIOLATION_NO_CVV, e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }
    }
    
    @Test
    public void previewCommit_WithDecreaseinDepositAndNoCVV_noValidateError() {

        PreviewCommitRequest commitRequest = getObject("/previewCommitRequest.json", PreviewCommitRequest.class);
        commitRequest.setPreviewReservationDeposit(commitRequest.getPreviewReservationDeposit() - 1);
        commitRequest.setCvv(null);

        callCommitReservation(commitRequest);

    }
    
    @Test
    public void previewCommit_WithNoChangeinDepositAndNoCVV_noValidateError() {

        PreviewCommitRequest commitRequest = getObject("/previewCommitRequest.json", PreviewCommitRequest.class);
        commitRequest.setPreviewReservationDeposit(commitRequest.getPreviewReservationDeposit() - 1);
        commitRequest.setCvv(null);

        callCommitReservation(commitRequest);

    }

    @Test
    public void associateReservation_WithNotMatchingFirstAndLastNames_validateErrorMessage() {
        ReservationAssociateRequest request = getObject("/associationRequest.json", ReservationAssociateRequest.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(request);

        RoomReservation findRoomReservation = getObject("/associateReservation-findRoomReservation.json",
                RoomReservation.class);
        findRoomReservation.getProfile().setMlifeNo(0);

        Map<String, String> claims = new HashMap<>();
        claims.put(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM, "Test");
        claims.put(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM, "Test");
        claims.put(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, "79950292");
        when(reservationServiceHelper.getClaims(Mockito.anyString())).thenReturn(claims);

        when(findResvService.findRoomReservation(findReservationV2Request, false)).thenReturn(findRoomReservation);

        when(idUtilityService.isFirstNameLastNameMatching(findRoomReservation.getProfile(),
                claims.get(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM),
                claims.get(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM))).thenReturn(false);

        try {
            modifyReservationServiceImpl.associateReservation(request, Mockito.anyString());
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.ASSOCIATION_VIOLATION_NAME_MISMATCH,
                    e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }
    }

    @Test
    public void associateReservation_WithNotMatchingMlifeNo_validateErrorMessage() {
        ReservationAssociateRequest request = getObject("/associationRequest.json", ReservationAssociateRequest.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(request);

        RoomReservation findRoomReservation = getObject("/associateReservation-findRoomReservation.json",
                RoomReservation.class);

        Map<String, String> claims = new HashMap<>();
        claims.put(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM, "Test");
        claims.put(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM, "Test");
        claims.put(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, "123456");
        when(reservationServiceHelper.getClaims(Mockito.anyString())).thenReturn(claims);

        when(findResvService.findRoomReservation(findReservationV2Request, false)).thenReturn(findRoomReservation);

        try {
            modifyReservationServiceImpl.associateReservation(request, Mockito.anyString());
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.ASSOCIATION_VIOLATION_MLIFE_MISMATCH,
                    e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }
    }
    
    @Test
    public void associateReservation_WithResvAlreadyAssociatedAndTokenWithOnlyMgmId_validateReservationAlreadyAssociatedMessage() {
        
        ReservationAssociateRequest request = getObject("/associationRequest.json", ReservationAssociateRequest.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(request);

        RoomReservation findRoomReservation = getObject("/associateReservation-findRoomReservation.json",
                RoomReservation.class);
        findRoomReservation.getProfile().setMlifeNo(123456);

        Map<String, String> claims = new HashMap<>();
        claims.put(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM, "Test");
        claims.put(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM, "Test");
        claims.put(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM, "123456");
        when(reservationServiceHelper.getClaims(Mockito.anyString())).thenReturn(claims);

        when(findResvService.findRoomReservation(findReservationV2Request, false)).thenReturn(findRoomReservation);

        try {
            modifyReservationServiceImpl.associateReservation(request, Mockito.anyString());
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.ASSOCIATION_VIOLATION_MLIFE_MISMATCH,
                    e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }

    }
    
    @Test
    public void associateReservation_WithResvAlreadyAssociatedWithSameMlife_validateErrorMessage() {

        ReservationAssociateRequest request = getObject("/associationRequest.json", ReservationAssociateRequest.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(request);

        RoomReservation findRoomReservation = getObject("/associateReservation-findRoomReservation.json",
                RoomReservation.class);

        Map<String, String> claims = new HashMap<>();
        claims.put(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM, "Lakshmi");
        claims.put(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM, "T");
        claims.put(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, "79950292");
        when(reservationServiceHelper.getClaims(Mockito.anyString())).thenReturn(claims);

        when(findResvService.findRoomReservation(findReservationV2Request, false)).thenReturn(findRoomReservation);

        try {
            modifyReservationServiceImpl.associateReservation(request, Mockito.anyString());
        } catch (BusinessException e) {
            Assertions.assertEquals(ErrorCode.ASSOCIATION_VIOLATION,
                    e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
        }
    }
    
    @Test
    public void associateReservation_WithResvWithMgmIdAndTokenHasMlifeWithSameOrDifferentMgmId_validateSuccess() {

        ReservationAssociateRequest request = getObject("/associationRequest.json", ReservationAssociateRequest.class);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(request);

        RoomReservation findRoomReservation = getObject("/associateReservation-findRoomReservation.json",
                RoomReservation.class);
        findRoomReservation.getProfile().setMlifeNo(0);

        Map<String, String> claims = new HashMap<>();
        claims.put(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM, "Lakshmi");
        claims.put(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM, "T");
        claims.put(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM, "kjsdfkhjkhjkhk");
        claims.put(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, "123456");
        when(reservationServiceHelper.getClaims(Mockito.anyString())).thenReturn(claims);

        when(findResvService.findRoomReservation(findReservationV2Request, false)).thenReturn(findRoomReservation);
        
        when(idUtilityService.isFirstNameLastNameMatching(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        UpdateProfileInfoRequest infoRequest = new UpdateProfileInfoRequest();
        infoRequest.setUserProfile(new UserProfileRequest());
        when(userProfileInfoRequestMapper.roomReservationModelToUpdateProfileInfoRequest(Mockito.any())).thenReturn(infoRequest);
        when(ocrsDao.updateProfile(Mockito.any())).thenReturn(null);
        when(modifyReservationDAO.updateProfileInfo(Mockito.any())).thenReturn(findRoomReservation);
        when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.anyString())).thenReturn(false);
        modifyReservationServiceImpl.associateReservation(request,
                Mockito.anyString());
    }

    @Test
    public void commitPaymentReservationTest_WithNoDepositSuccess() {
        RoomReservation reservation = getCommitRoomReservationResponse_ForNoDeposit();
        // mock commit payment response from dao layer
        when(modifyReservationDAO.commitPaymentReservation(Mockito.any()))
                .thenReturn(reservation);
        // mock responseMapper
        RoomReservationResponseMapper responseMapperImpl = Mockito.spy(new RoomReservationResponseMapperImpl());
        doNothing().when(responseMapperImpl).updateSpecialRequests(Mockito.any());
        doNothing().when(responseMapperImpl).populateAdditionalDetails(Mockito.any(), Mockito.any());
        RoomReservationV2Response obj = responseMapperImpl.roomReservationModelToResponse(reservation);
        when(responseMapper.roomReservationModelToResponse(Mockito.any()))
                .thenReturn(obj);
        // mock validate program response
        when(roomProgramDao.validateProgramV2(Mockito.any()))
                .thenReturn(getValidateProgramV2Response());

        // create commit request
        PaymentRoomReservationRequest commitRequest = getCommitRequest_ForNoDeposit();
        ModifyRoomReservationResponse response = modifyReservationServiceImpl.commitPaymentReservation(commitRequest);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getRoomReservation());
        RoomReservationV2Response reservationV2Response = response.getRoomReservation();
        Assertions.assertNotNull(reservationV2Response);
        Assertions.assertNotNull(reservationV2Response.getConfirmationNumber());
        Assertions.assertNotNull(reservationV2Response.getProfile());
        Assertions.assertEquals(commitRequest.getFirstName(), reservationV2Response.getProfile().getFirstName());
        Assertions.assertEquals(commitRequest.getLastName(), reservationV2Response.getProfile().getLastName());
        Assertions.assertEquals(ReservationState.Booked, reservationV2Response.getState());
        Assertions.assertNotNull(reservationV2Response.getBookings());
        // extended by 2 days
        Assertions.assertEquals(4, reservation.getBookings().size());
        Assertions.assertNotNull(reservationV2Response.getBilling());
        // 0 amount in billing for No Deposit
        Assertions.assertEquals(0, reservationV2Response.getBilling().get(0).getPayment().getAmount());
        Assertions.assertNotNull(reservationV2Response.getPayments());
        Assertions.assertNotEquals(0, reservationV2Response.getPayments().size());
    }

    @Test
    public void commitPaymentReservationTest_WithAddnDepositSuccess() {
        // mock commit payment response from dao layer
        when(modifyReservationDAO.commitPaymentReservation(Mockito.any()))
                .thenReturn(getCommitRoomReservationResponse_ForAddnDeposit());
        // mock responseMapper
        RoomReservationV2Response obj = getRBSCommitResponse_ForAddnDeposit().getRoomReservation();
        when(responseMapper.roomReservationModelToResponse(Mockito.any()))
                .thenReturn(obj);
        // mock validate program response
        when(roomProgramDao.validateProgramV2(Mockito.any()))
                .thenReturn(getValidateProgramV2Response());

        when(reservationServiceHelper.updatePackageComponentsFlag(Mockito.anyString(), Mockito.any()))
                .thenReturn(obj.getPurchasedComponents());

        // create commit request
        PaymentRoomReservationRequest commitRequest = getCommitRequest_ForAddnDeposit();
        ModifyRoomReservationResponse response = modifyReservationServiceImpl.commitPaymentReservation(commitRequest);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getRoomReservation());
        RoomReservationV2Response reservationV2Response = response.getRoomReservation();
        Assertions.assertNotNull(reservationV2Response);
        Assertions.assertNotNull(reservationV2Response.getConfirmationNumber());
        Assertions.assertNotNull(reservationV2Response.getProfile());
        Assertions.assertEquals(commitRequest.getFirstName(), reservationV2Response.getProfile().getFirstName());
        Assertions.assertEquals(commitRequest.getLastName(), reservationV2Response.getProfile().getLastName());
        Assertions.assertEquals(ReservationState.Booked, reservationV2Response.getState());
        // added component check
        Assertions.assertNotNull(reservationV2Response.getSpecialRequests());
        Assertions.assertEquals(1, reservationV2Response.getSpecialRequests().size());
        Assertions.assertTrue(reservationV2Response.getSpecialRequests().get(0).contains("DOGFEE"));
        Assertions.assertEquals(1, reservationV2Response.getPurchasedComponents().size());
        // additonal payment in billing
        Assertions.assertNotNull(reservationV2Response.getBilling());
        Assertions.assertEquals(56.69, reservationV2Response.getBilling().get(0).getPayment().getAmount());
        // payments[] should not be empty
        Assertions.assertNotNull(reservationV2Response.getPayments());
        Assertions.assertNotEquals(0, reservationV2Response.getPayments().size());
    }

    @Test
    public void commitPaymentReservationTest_WithRefundDepositSuccess() {
        // mock commit payment response from dao layer
        when(modifyReservationDAO.commitPaymentReservation(Mockito.any()))
                .thenReturn(getCommitRoomReservationResponse_ForRefundDeposit());
        // mock responseMapper
        RoomReservationV2Response obj = getRBSCommitResponse_ForRefundDeposit().getRoomReservation();
        when(responseMapper.roomReservationModelToResponse(Mockito.any()))
                .thenReturn(obj);
        // mock validate program response
        when(roomProgramDao.validateProgramV2(Mockito.any()))
                .thenReturn(getValidateProgramV2Response());

        // create commit request
        PaymentRoomReservationRequest commitRequest = getCommitRequest_ForRefundDeposit();
        ModifyRoomReservationResponse response = modifyReservationServiceImpl.commitPaymentReservation(commitRequest);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getRoomReservation());
        RoomReservationV2Response reservationV2Response = response.getRoomReservation();
        Assertions.assertNotNull(reservationV2Response);
        Assertions.assertNotNull(reservationV2Response.getConfirmationNumber());
        Assertions.assertNotNull(reservationV2Response.getProfile());
        Assertions.assertEquals(commitRequest.getFirstName(), reservationV2Response.getProfile().getFirstName());
        Assertions.assertEquals(commitRequest.getLastName(), reservationV2Response.getProfile().getLastName());
        Assertions.assertEquals(ReservationState.Booked, reservationV2Response.getState());
        // removed component check
        Assertions.assertNotNull(reservationV2Response.getSpecialRequests());
        Assertions.assertEquals(0, reservationV2Response.getSpecialRequests().size());
        Assertions.assertEquals(0, reservationV2Response.getPurchasedComponents().size());
        // refund payment in billing
        Assertions.assertNotNull(reservationV2Response.getBilling());
        Assertions.assertEquals(-56.69, reservationV2Response.getBilling().get(0).getPayment().getAmount());
        // payments[] should not be empty
        Assertions.assertNotNull(reservationV2Response.getPayments());
        Assertions.assertNotEquals(0, reservationV2Response.getPayments().size());
    }

    private ModifyRoomReservationResponse callCommitReservation(PreviewCommitRequest commitRequest) {
        PreModifyV2Request preModifyV2Request = commitToPreviewRequest(commitRequest);
        when(previewMapper.commitToPreviewRequest(commitRequest)).thenReturn(preModifyV2Request);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(preModifyV2Request);
        RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",
                RoomReservation.class);
        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);

        when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class),
                Mockito.anyString())).thenReturn(true);
        when(reservationServiceHelper.validateLoggedUserOrServiceToken(Mockito.anyString())).thenReturn(false);
        when(reservationServiceHelper.isRequestAllowedToFindReservation(Mockito.any(PreModifyV2Request.class),
                Mockito.any(RoomReservation.class))).thenReturn(true);

        RoomReservation reservation = getObject("/preModifyReservation-v2-preModifyReservation.json",
                RoomReservation.class);
        when(modifyReservationDAO.preModifyReservation(preModifyV2Request)).thenReturn(reservation);

        RoomReservationV2Response roomReservationResponse = getObject(
                "/preModifyReservation-v2-roomReservationModelToResponse.json", RoomReservationV2Response.class);
        when(responseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class)))
                .thenReturn(roomReservationResponse);

        return modifyReservationServiceImpl.commitReservation(commitRequest, Mockito.anyString());
    }

    private PreModifyV2Request commitToPreviewRequest(PreviewCommitRequest commitRequest) {
        PreModifyV2Request preModifyV2Request = new PreModifyV2Request();

        preModifyV2Request.setSource(commitRequest.getSource());
        preModifyV2Request.setChannel(commitRequest.getChannel());
        preModifyV2Request.setCustomerId(commitRequest.getCustomerId());
        preModifyV2Request.setMlifeNumber(commitRequest.getMlifeNumber());
        preModifyV2Request.setCustomerTier(commitRequest.getCustomerTier());
        preModifyV2Request.setPerpetualPricing(commitRequest.isPerpetualPricing());
        preModifyV2Request.setConfirmationNumber(commitRequest.getConfirmationNumber());
        preModifyV2Request.setTripDetails(commitRequest.getTripDetails());
        preModifyV2Request.setPropertyId(commitRequest.getPropertyId());
        List<String> list = commitRequest.getRoomRequests();
        if (list != null) {
            preModifyV2Request.setRoomRequests(new ArrayList<String>(list));
        }
        preModifyV2Request.setFirstName(commitRequest.getFirstName());
        preModifyV2Request.setLastName(commitRequest.getLastName());

        return preModifyV2Request;

    }
    
    @Test
    public void reservationModifyPendingV4_withNoChangeInDeposit_paymentWidget()  {


    	PreviewCommitRequest previewCommitRequest = getObject("/previewCommitRequest.json", PreviewCommitRequest.class);

    	previewCommitRequest.setSource("ice");
    	previewCommitRequest.setMlifeNumber("123456");
    	
    	PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request.json", PreModifyV2Request.class);
    	when(previewMapper.commitToPreviewRequest(Mockito.any())).thenReturn(preModifyV2Request);

        FindReservationV2Request findReservationV2Request = reservationServiceHelper
                .createFindReservationV2Request(preModifyV2Request);

        RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",
                RoomReservation.class);

        when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
        when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class),
                Mockito.anyString())).thenReturn(true);
        when(reservationServiceHelper.validateLoggedUserOrServiceToken(Mockito.anyString())).thenReturn(false);
        when(reservationServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, findRoomReservation))
                .thenReturn(true);
               
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setRatePlanTags(Collections.singletonList("f1Package"));        
        when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
        RoomReservation reservation = getObject("/preModifyReservation-v2-preModifyReservation.json",
                RoomReservation.class);
        
        when(modifyReservationDAO.preModifyReservation(preModifyV2Request)).thenReturn(reservation);
        when(requestMapper.roomReservationRequestToModel(Mockito.any())).thenReturn(reservation);

        RoomReservationV2Response roomReservationResponse = getObject(
                "/preModifyReservation-v2-roomReservationModelToResponse.json", RoomReservationV2Response.class);
        

        when(responseMapper.roomReservationModelToResponse(reservation)).thenReturn(roomReservationResponse);

        RoomReservationRequest resv = getObject("/previewpending.json",
        		RoomReservationRequest.class);
        when(requestMapper.roomReservationResponseToRequest(Mockito.any())).thenReturn(resv);

        ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", true);
        
        when(modifyReservationDAO.modifyPendingRoomReservationV2(Mockito.any())).thenReturn(reservation);
       
        ModifyRoomReservationResponse response = modifyReservationServiceImpl.reservationModifyPendingV5(previewCommitRequest,
                Mockito.anyString());
        Assertions.assertEquals("M03D5C0E1", response.getRoomReservation().getConfirmationNumber());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getRoomReservation().getPropertyId());
        Assertions.assertEquals("798152609",response.getRoomReservation().getOperaConfirmationNumber());    
        double expectedPreviousDesposit = BigDecimal.valueOf(findRoomReservation.getDepositCalc().getAmount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double actualPreviousDesposit = response.getRoomReservation().getRatesSummary().getPreviousDeposit();
        Assertions.assertEquals(expectedPreviousDesposit, actualPreviousDesposit, 0.001);
    }

    @Test
    public void reservationModifyPendingV4_SingleToSharedSuccessTest() {
        try {
            // mock validate program response
            when(roomProgramDao.validateProgramV2(Mockito.any()))
                    .thenReturn(getValidateProgramV2Response());
            // mock dao response
            when(modifyReservationDAO.modifyPendingRoomReservationV2(Mockito.any()))
                    .thenReturn(new RoomReservation());
            // mock responseMapper
            RoomReservationResponseMapper responseMapperImpl = Mockito.spy(new RoomReservationResponseMapperImpl());
            doNothing().when(responseMapperImpl).updateSpecialRequests(Mockito.any());
            doNothing().when(responseMapperImpl).populateAdditionalDetails(Mockito.any(), Mockito.any());
            RoomReservationV2Response obj =
                    responseMapperImpl.roomReservationModelToResponse(getPendingRoomReservationResponse_ForSingleToSharedUseCase());
            when(responseMapper.roomReservationModelToResponse(Mockito.any()))
                    .thenReturn(obj);

            // create request
            ModifyRoomReservationRequest modifyRoomReservationRequest =
                    getObject("/paymentwidgetv4/modifyPending/singleToShared/rbs-modify-pending-request-singleToShared.json", ModifyRoomReservationRequest.class);

            // mock requestMapper
            RoomReservationRequestMapper requestMapperSpy = Mockito.spy(new RoomReservationRequestMapperImpl());
            Mockito.doNothing().when(requestMapperSpy).updateRoomReservation(Mockito.any(), Mockito.any());
            RoomReservation roomReservation =
                    requestMapperSpy.roomReservationRequestToModel(modifyRoomReservationRequest.getRoomReservation());
            Mockito.doReturn(roomReservation).when(requestMapper).roomReservationRequestToModel(Mockito.any());

            // make call
            ModifyRoomReservationResponse response =
                    modifyReservationServiceImpl.reservationModifyPendingV4(modifyRoomReservationRequest);

            // Assertions
            Assertions.assertNotNull(response);
            Assertions.assertNotNull(response.getRoomReservation());
            RoomReservationV2Response reservationV2Response = response.getRoomReservation();
            Assertions.assertNotNull(reservationV2Response.getConfirmationNumber());
            Assertions.assertNotNull(reservationV2Response.getProfile());
            Assertions.assertEquals(ReservationState.Booked, reservationV2Response.getState());
            // payments[] not empty check
            Assertions.assertNotNull(reservationV2Response.getPayments());
            Assertions.assertNotEquals(0, reservationV2Response.getPayments().size());
            // shareId generated check
            Assertions.assertNotNull(reservationV2Response.getShareId());
            // shareWith cnf no.
            Assertions.assertEquals(1, reservationV2Response.getShareWiths().length);
            // shareWith (secondary) customer profile
            Assertions.assertEquals(1, reservationV2Response.getShareWithCustomers().size());
        }
        catch(Exception e) {
            Assertions.fail("reservationModifyPendingV4_SingleToSharedSuccessTest has Failed");
        }
    }
    

	@Test
	public void commitReservationV2_WithChangeInDeposit_Test() {
		PreviewCommitRequest commitRequest = getObject("/previewCommitRequestV2.json", PreviewCommitRequest.class);
		PreModifyV2Request preModifyV2Request = getObject("/preModifyV2Request-v2.json", PreModifyV2Request.class);
		when(previewMapper.commitToPreviewRequest(Mockito.any())).thenReturn(preModifyV2Request);

		FindReservationV2Request findReservationV2Request = reservationServiceHelper.createFindReservationV2Request(preModifyV2Request);

		RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservationv2.json",RoomReservation.class);

		RoomReservationV2Response roomReservationResponse = getObject("/roomReservationModelToResponse-v2-commit.json", RoomReservationV2Response.class);


		when(findReservationDao.findRoomReservation(findReservationV2Request)).thenReturn(findRoomReservation);
		when(reservationServiceHelper.isRequestAllowedToModifyReservation(Mockito.any(RoomReservation.class), Mockito.anyString())).thenReturn(true);

		when(reservationServiceHelper.validateLoggedUserOrServiceToken(Mockito.anyString())).thenReturn(false);
		when(reservationServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, findRoomReservation)).thenReturn(true);
		when(reservationServiceHelper.updatePackageComponentsFlag(Mockito.anyString(), Mockito.any())).thenReturn(roomReservationResponse.getPurchasedComponents());
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setRatePlanTags(Collections.singletonList("f1Package"));  
		when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

		RoomReservation reservation = getObject("/preModifyReservationv2-commit.json",RoomReservation.class);

		RoomReservationRequest resvRequest = getObject("/commitRequest-v2.json",RoomReservationRequest.class);
		when(requestMapper.roomReservationResponseToRequest(Mockito.any())).thenReturn(resvRequest);

		when(modifyReservationDAO.preModifyReservation(preModifyV2Request)).thenReturn(reservation);
		when(modifyReservationDAO.modifyRoomReservationV2(Mockito.any())).thenReturn(reservation);
		when(requestMapper.roomReservationRequestToModel(Mockito.any())).thenReturn(reservation); 

		when(responseMapper.roomReservationModelToResponse(reservation)).thenReturn(roomReservationResponse);


		ModifyRoomReservationResponse response = modifyReservationServiceImpl.commitReservation(commitRequest,ArgumentMatchers.anyString());
        Assertions.assertNotNull(response);
        //Assertions
		RoomReservationV2Response reservationV2Response = response.getRoomReservation();
        Assertions.assertNotNull(reservationV2Response);
        Assertions.assertNotNull(reservationV2Response.getConfirmationNumber());
        Assertions.assertNotNull(reservationV2Response.getProfile());
        Assertions.assertEquals(commitRequest.getFirstName(),reservationV2Response.getProfile().getFirstName());
        Assertions.assertEquals(commitRequest.getLastName(),reservationV2Response.getProfile().getLastName());
        Assertions.assertEquals(ReservationState.Booked, reservationV2Response.getState());
        
        Assertions.assertNotNull(reservationV2Response.getSpecialRequests());
        Assertions.assertEquals(1, reservationV2Response.getSpecialRequests().size());
        Assertions.assertEquals(1, reservationV2Response.getPurchasedComponents().size());
        // billing
        Assertions.assertNotNull(reservationV2Response.getBilling());
        Assertions.assertEquals(272.51, reservationV2Response.getBilling().get(0).getPayment().getAmount());
        // payments
        Assertions.assertNotNull(reservationV2Response.getPayments());
        Assertions.assertNotEquals(0, reservationV2Response.getPayments().size());

		Assertions.assertEquals("5777VG3W7D", response.getRoomReservation().getConfirmationNumber());
		double expectedPreviousDesposit = BigDecimal.valueOf(findRoomReservation.getDepositCalc().getAmount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
		double actualPreviousDesposit = response.getRoomReservation().getRatesSummary().getPreviousDeposit();
		Assertions.assertEquals(expectedPreviousDesposit, actualPreviousDesposit, 0.001);

	}
	@Test
	public void commit_WithIncreaseinDepositAndNoCVV_validateError() {

		PreviewCommitRequest commitRequest = getObject("/previewCommitRequest.json", PreviewCommitRequest.class);
		commitRequest.setPreviewReservationDeposit(commitRequest.getPreviewReservationDeposit() + 1);
		commitRequest.setCvv(null);

		try {
			callCommitReservation(commitRequest);
		} catch (BusinessException e) {
			Assertions.assertEquals(ErrorCode.MODIFY_VIOLATION_NO_CVV, e.getErrorCode(), TestConstant.ERROR_CODE_SHOULD_MATCH_MSG);
		}
	}
}
