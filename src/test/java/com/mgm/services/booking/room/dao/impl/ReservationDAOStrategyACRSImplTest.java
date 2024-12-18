package com.mgm.services.booking.room.dao.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.dao.RefDataDAO;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapper;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapperImpl;
import com.mgm.services.booking.room.model.crs.reservation.DailyRateDetails;
import com.mgm.services.booking.room.model.crs.reservation.ExtCfNumber;
import com.mgm.services.booking.room.model.crs.reservation.GuestCountRes;
import com.mgm.services.booking.room.model.crs.reservation.GuestsRes;
import com.mgm.services.booking.room.model.crs.reservation.GuestsResItem;
import com.mgm.services.booking.room.model.crs.reservation.HotelRes;
import com.mgm.services.booking.room.model.crs.reservation.HotelReservationPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.HotelReservationRes;
import com.mgm.services.booking.room.model.crs.reservation.HotelReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.crs.reservation.LinkReservationRes;
import com.mgm.services.booking.room.model.crs.reservation.LoyaltyProgram;
import com.mgm.services.booking.room.model.crs.reservation.OfferRes;
import com.mgm.services.booking.room.model.crs.reservation.Period;
import com.mgm.services.booking.room.model.crs.reservation.PersonName;
import com.mgm.services.booking.room.model.crs.reservation.PointOfSale;
import com.mgm.services.booking.room.model.crs.reservation.ProductUseRates;
import com.mgm.services.booking.room.model.crs.reservation.ProductUseRes;
import com.mgm.services.booking.room.model.crs.reservation.ProductUseResItem;
import com.mgm.services.booking.room.model.crs.reservation.RateChange;
import com.mgm.services.booking.room.model.crs.reservation.RatePlanRes;
import com.mgm.services.booking.room.model.crs.reservation.ResStatus;
import com.mgm.services.booking.room.model.crs.reservation.ReservationIdsRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationModifyPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationModifyPendingResData;
import com.mgm.services.booking.room.model.crs.reservation.ReservationPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResreservationData;
import com.mgm.services.booking.room.model.crs.reservation.SegmentRes;
import com.mgm.services.booking.room.model.crs.reservation.SegmentResItem;
import com.mgm.services.booking.room.model.paymentservice.AuthResponse;
import com.mgm.services.booking.room.model.paymentservice.CaptureResponse;
import com.mgm.services.booking.room.model.refdata.AlertAndTraceSearchRefDataRes;
import com.mgm.services.booking.room.model.refdata.RoutingInfoResponseList;
import com.mgm.services.booking.room.model.request.ModificationChangesRequest;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.request.TripDetail;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.PartyRoomReservation;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.RequestSourceConfig;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.authorization.AuthorizationTransactionDetails;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;

class ReservationDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {

    private static RestTemplate client;
    private static DomainProperties domainProperties;
    private static RestTemplateBuilder restTemplateBuilder;
    private static AcrsProperties acrsProperties;
    private static RoomPriceDAO roomPriceDAO;
    private static IDMSTokenDAO idmsTokenDAO;
    private static PaymentDAO paymentDao;
    private static RefDataDAO refDataDAO;
    private static ReservationDAOStrategyACRSImpl reservationDAOStrategyACRSImpl;
    ModifyReservationDAOStrategyACRSImpl modifyReservationDAOStrategyACRSImpl;
    static Logger logger = LoggerFactory.getLogger(ReservationDAOStrategyACRSImplTest.class);
    private final RoomReservationPendingResMapper pendingResMapper = new RoomReservationPendingResMapperImpl();
    private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

    @Mock
   	private static ReservationDAOStrategyACRSImpl acrsStrategy;
    private RoomReservation roomReservation;
    private RoomCartRequest roomCartRequest;
    @InjectMocks
	protected static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;
 
    private ResponseEntity<RoutingInfoResponseList> getRefDataRIResponse(String jsonFileName){
        return new ResponseEntity<>(
                convertCrs(jsonFileName, RoutingInfoResponseList.class),
                HttpStatus.OK);
    }
    private ResponseEntity<AlertAndTraceSearchRefDataRes> getRefDataResponse(String jsonFileName){
        return new ResponseEntity<>(
                convertCrs(jsonFileName, AlertAndTraceSearchRefDataRes.class),
                HttpStatus.OK);
    }
    private HttpEntity<ReservationModifyPendingRes> modifyPendingFormOfPaymentResponse(String jsonFileName){
        return new ResponseEntity<>(
                convertCrs(jsonFileName, ReservationModifyPendingRes.class),
                HttpStatus.OK);
    }
    private ResponseEntity<LinkReservationRes> createMockShareLink(String jsonFileName){
        return new ResponseEntity<>(
                convertCrs(jsonFileName, LinkReservationRes.class),
                HttpStatus.OK);
    }
    private HttpEntity<ReservationPendingRes> makePendingRoomReservationResponse(String jsonFileName) {
        return new ResponseEntity<>(
                convertCrs(jsonFileName, ReservationPendingRes.class),
                HttpStatus.CREATED);
    }

    private ResponseEntity<ReservationRes> pendingCommitReservationResponse(String jsonFileName) {
        return new ResponseEntity<>(convertCrs(jsonFileName, ReservationRes.class),
                HttpStatus.OK);
    }
    private ResponseEntity<ReservationRes> pendingIgnoreReservationResponse(String jsonFileName) {
        return new ResponseEntity<>(convertCrs(jsonFileName, ReservationRes.class),
                HttpStatus.OK);
    }

    private void setMockForCreatePendingSuccess(ResponseEntity<ReservationPendingRes> response) {
        when(client.postForEntity(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(),
                ArgumentMatchers.<Class<ReservationPendingRes>>any(), Mockito.anyMap()))
                .thenReturn(response);
    }


    private void setMockForPendingCommitSuccess(ResponseEntity<ReservationRes> commitPendingResponse) {
        String cnfNumber = commitPendingResponse.getBody().getData().getHotelReservation().getReservationIds().getCfNumber();
        Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(), acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(),false);
        uriParams.put("confirmationNumber", cnfNumber);
         when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>>any(), Mockito.eq(uriParams)))
                .thenReturn(commitPendingResponse);
    }

    private void setMockForPendingIgnoreSuccess(ResponseEntity<ReservationRes> ignorePendingResponse) {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>>any(), Mockito.anyMap()))
                .thenReturn(ignorePendingResponse);
    }


    private void setMockForShareLinkSuccess(ResponseEntity<LinkReservationRes> response) {
        when(client.exchange(ArgumentMatchers.contains("link"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<LinkReservationRes>>any(), Mockito.anyMap())).thenReturn(response);
    }

    private void mockForRouting(String fileName){
        ResponseEntity<RoutingInfoResponseList> response = getRefDataRIResponse(fileName);
        setMockForGetRefDataRISuccess(response);
    }
    private void mockForRefData(String fileName){
        ResponseEntity<AlertAndTraceSearchRefDataRes> response = getRefDataResponse(fileName);
        setMockForGetRefDataSuccess(response);
    }

    private void setMockForGetRefDataSuccess(ResponseEntity<AlertAndTraceSearchRefDataRes> response ) {
        when(refDataDAO.searchRefDataEntity(ArgumentMatchers.any())).thenReturn(response.getBody());
    }
    private void setMockForGetRefDataRISuccess(ResponseEntity<RoutingInfoResponseList> response ) {
        when(refDataDAO.getRoutingInfo(ArgumentMatchers.any())).thenReturn(response.getBody());
    }
    private HotelReservationRes hotelReservationResResponse() {
        RoomReservationPendingResMapperImpl impl = new RoomReservationPendingResMapperImpl();
        ReservationModifyPendingRes response = (ReservationModifyPendingRes) makePendingRoomReservationResponse()
                .getBody();
        HotelReservationPendingRes pendingRes = response.getData().getHotelReservation();
        return impl.pendingResvToHotelReservationRes(pendingRes);
    }
    private HttpEntity<?> makePendingRoomReservationResponse() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/acrs/modifyreservation/crs-modify-pending.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    protected void setMockAuthToken() {
		setMockAuthToken("token", "ICECC");
	}

	protected void setMockAuthToken(String token, String channel) {
		Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = new HashMap<>();
		ACRSAuthTokenResponse tokenRes = new ACRSAuthTokenResponse();
		tokenRes.setToken(token);
		acrsAuthTokenResponseMap.put(channel, tokenRes);
		when(acrsOAuthTokenDAOImpl.generateToken()).thenReturn(acrsAuthTokenResponseMap);
	}

    @BeforeEach
    public void init() {
        super.init();
        client = Mockito.mock(RestTemplate.class);
        roomPriceDAO = Mockito.mock(RoomPriceDAO.class);
        idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
        paymentDao = Mockito.mock(PaymentDAO.class);
        refDataDAO = Mockito.mock(RefDataDAO.class);
        domainProperties = new DomainProperties();
        domainProperties.setCrs("https://cfts.hospitality.api.amadeus.com");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        roomPriceDAOStrategyACRSImpl = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
        modifyReservationDAOStrategyACRSImpl = Mockito.mock(ModifyReservationDAOStrategyACRSImpl.class);
        acrsProperties = new AcrsProperties();
        acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
        acrsProperties.setModifyPartyConfirmationNumberPath("modifyPartyConfirmationNumberPath");
        acrsProperties.setModifyAddOnPaymentInfoPath("/data/hotelReservation/segments[id=%s]/formOfPayment/paymentInfo");
        acrsProperties.setLiveCRS(true);
        acrsProperties.setMaxAcrsCommentLength(51);
        acrsProperties.setDeleteProductUsePath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]");
        acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
        
        // Initialize test data
        roomCartRequest = new RoomCartRequest();
        roomCartRequest.setCheckInDate(LocalDate.now());
        roomCartRequest.setCheckOutDate(LocalDate.now().plusDays(1));
        roomCartRequest.setRoomTypeId("exampleRoomTypeId");
        roomCartRequest.setPropertyId("examplePropertyId");
        roomCartRequest.setProgramId("exampleProgramId");
        roomCartRequest.setNumGuests(2);

        roomReservation = new RoomReservation();
        roomReservation.setConfirmationNumber("exampleConfirmationNumber");
        roomReservation.setPropertyId("examplePropertyId");
        roomReservation.setAgentInfo(new AgentInfo());
        roomReservation.setRrUpSell("exampleRrUpSell");
        roomReservation.setSource("exampleSource");
        roomReservation.setPerpetualPricing(false);
        RoomReservationPendingResMapper pendingResMapper = new RoomReservationPendingResMapperImpl();

        
        // set urlProperties
        urlProperties.setAcrsReservationsCreatePending(
                "/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/pending");
        urlProperties.setAcrsReservationsConfCommit(
                "/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/{confirmationNumber}/commit");
        urlProperties.setAcrsReservationsConfPending(
                "/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/reservations/{acrsChainCode}/confirmationNumber/pending");
        urlProperties.setAcrsCreateReservationLink(
                "/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/reservations/{acrsChainCode}/link");
        CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
        when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
                acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),1,applicationProperties.getCrsRestTTL())).thenReturn(client);

        doNothing().when(loyaltyDao).updatePlayerPromo(ArgumentMatchers.any());
        try {
            reservationDAOStrategyACRSImpl = new ReservationDAOStrategyACRSImpl(urlProperties, domainProperties,
                    applicationProperties, restTemplateBuilder, roomPriceDAO, acrsProperties, referenceDataDAOHelper,
                    acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);

            reservationDAOStrategyACRSImpl.setIdmsTokenDAO(idmsTokenDAO);
            reservationDAOStrategyACRSImpl.setPaymentDao(paymentDao);
            reservationDAOStrategyACRSImpl.setRefDataDAO(refDataDAO);
            reservationDAOStrategyACRSImpl.setLoyaltyDao(loyaltyDao);
            reservationDAOStrategyACRSImpl.setPendingResMapper(pendingResMapper);
            reservationDAOStrategyACRSImpl.client = client;

            ReflectionTestUtils.setField(reservationDAOStrategyACRSImpl, "accertifyInvokeHelper", accertifyInvokeHelper);
            ReflectionTestUtils.setField(reservationDAOStrategyACRSImpl, "modifyReservationDAOStrategyACRSImpl", modifyReservationDAOStrategyACRSImpl);
        } catch (SSLException e) {
            logger.error(e.getMessage());
            logger.error("Caused " + e.getCause());
        }
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }


    /**
     * This will assert the common attributes trip date, bookings , price.
     * @param request
     * @param response
     */
    private void checkAssertions(RoomReservation request, RoomReservation response) {
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getConfirmationNumber());
        //validate status
        Assertions.assertEquals(ReservationState.Booked, response.getState());
       Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), response.getCheckInDate()));
       Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), response.getCheckOutDate()));
        // check room
        Assertions.assertEquals(request.getRoomTypeId(), response.getRoomTypeId());
       // check prices
        Assertions.assertNotNull(response.getBookings());
        response.getBookings().forEach(booking -> {
           RoomPrice bookObj= request.getBookings().stream().filter(x -> x.getDate().equals(booking.getDate())).findFirst().get();// check prices
           //check program
            Assertions.assertEquals(bookObj.getProgramId(), booking.getProgramId());
            //check price
            Assertions.assertEquals(bookObj.getPrice(), booking.getPrice());
            // check override program
            Assertions.assertEquals(bookObj.getOverrideProgramId(), booking.getOverrideProgramId());
            // check override prices
            Assertions.assertEquals(bookObj.getOverridePrice(), booking.getOverridePrice());
        }
         );
        // check deposit
        if(CollectionUtils.isNotEmpty(request.getCreditCardCharges())) {
            // deposit by card
            Assertions.assertNotNull(response.getDepositCalc());
            Assertions.assertEquals(request.getDepositPolicyCalc().isDepositRequired(), response.getDepositPolicyCalc().isDepositRequired());
            Assertions.assertEquals(request.getDepositCalc().getAmount(), response.getDepositCalc().getAmount());
            Assertions.assertEquals(request.getDepositCalc().getForfeitAmount(), response.getDepositCalc().getForfeitAmount());
            Assertions.assertEquals(request.getDepositCalc().getOverrideAmount(), response.getDepositCalc().getOverrideAmount());

            //validate payments
            // for modify flow ACRS doesn't send any payments related info in response
            if(StringUtils.isBlank( request.getConfirmationNumber())) {
                Double depositAmount = request.getDepositCalc().getOverrideAmount() > 0 ? request.getDepositCalc().getOverrideAmount() : request.getDepositCalc().getAmount();
                Assertions.assertEquals(request.getCreditCardCharges(), response.getCreditCardCharges());
                Assertions.assertNotNull(response.getPayments());
                Double sumOfPayments = response.getPayments().stream().mapToDouble(Payment::getChargeAmount).sum();
                //total deposit paid will be same as deposit amount
                Assertions.assertEquals(depositAmount, sumOfPayments);
                Double cardAmount = response.getCreditCardCharges().stream().mapToDouble(CreditCardCharge::getAmount).sum();
                Assertions.assertEquals(depositAmount, cardAmount);
            }
         }else{
            // cash payment
            Assertions.assertTrue(CollectionUtils.isEmpty(response.getCreditCardCharges()));
        }

        // check special requests
        Assertions.assertEquals(request.getSpecialRequests().size(), response.getSpecialRequests().size());
        //validate special req values
        if(request.getSpecialRequests().size() >0){
            Assertions.assertEquals(request.getSpecialRequests(), response.getSpecialRequests());
            // check addons
            Optional<String> addOns = request.getSpecialRequests().stream().filter(x -> ACRSConversionUtil.isAcrsComponentCodeGuid(x)).findAny();
            if(addOns.isPresent()){
                boolean hasComponentCharge = response.getChargesAndTaxesCalc().getCharges().stream()
                        .map(RoomChargeItem::getItemized)
                        .flatMap(Collection::stream)
                        .anyMatch(itemized -> RoomChargeItemType.ComponentCharge.equals(itemized.getItemType()));
                Assertions.assertTrue(hasComponentCharge);
                boolean hasComponentTax = response.getChargesAndTaxesCalc().getTaxesAndFees().stream()
                        .map(RoomChargeItem::getItemized)
                        .flatMap(Collection::stream)
                        .anyMatch(itemized -> RoomChargeItemType.ComponentChargeTax.equals(itemized.getItemType()));

                Assertions.assertTrue(hasComponentTax);
            }
        }
         // check perpetualPricing flag
        Assertions.assertEquals(request.isPerpetualPricing(), response.isPerpetualPricing());
        // check RIs
        if(CollectionUtils.isNotEmpty(request.getRoutingInstructions())){
            Assertions.assertEquals(request.getRoutingInstructions(), response.getRoutingInstructions());
        }
        //check groupCode
        if(ACRSConversionUtil.isAcrsGroupCodeGuid(request.getProgramId())){
            Assertions.assertTrue(response.getIsGroupCode());

        }
        // check share
        if(CollectionUtils.isNotEmpty(request.getShareWithCustomers())){
            Assertions.assertNotNull(response.getShareId());
            Assertions.assertNotNull(response.getShareWiths());
        }
        // check for party
        if(request.getNumRooms()>1){
            Assertions.assertNotNull(response.getPartyConfirmationNumber());
        }
        //Check alerts
        //check traces

    }


    /**
     * This will mock ACRS make pending, modify pending, create, payment auth, get ACRS tokens APIS.
     * @param propertyCode
     * @param createPendingResJsonFile
     * @param modifyPendingResJsonFile
     * @param commitReservationResJsonFile
     * @return
     */
    public void mockForSuccessOperations(RoomReservation request, String propertyCode, String createPendingResJsonFile, String modifyPendingResJsonFile, String commitReservationResJsonFile){
        {
            //Room,property,rate Id map
            setMockForRefDataHelper(request, propertyCode, "ICECC");
            //idms token mock
            mockIdmsToken();
            //token for ACRS
            setMockAuthToken();
            //pps mock
            mockPaymentAuth(request,ServiceConstant.APPROVED);
            //Capture Mocks
            mockCapturePayment(ServiceConstant.APPROVED);
            // ACRS MOCK
            mockACRSResponse(createPendingResJsonFile, modifyPendingResJsonFile, commitReservationResJsonFile);
        }
    }

    private void mockACRSResponse(String createPendingResJsonFile, String modifyPendingResJsonFile, String commitReservationResJsonFile) {
        // Mock the create Pending response
        if(StringUtils.isNotBlank(createPendingResJsonFile)) {
            ResponseEntity<ReservationPendingRes> pendingResponse =
                    (ResponseEntity<ReservationPendingRes>) makePendingRoomReservationResponse(createPendingResJsonFile);
            setMockForCreatePendingSuccess(pendingResponse);
        }
        // ModifyPending mock response through PET
        ReservationModifyPendingRes reservationPendingRes =
                modifyPendingFormOfPaymentResponse(modifyPendingResJsonFile).getBody();
        String cnfNumber = reservationPendingRes.getData().getHotelReservation().getReservationIds().getCfNumber();

        Mockito.doReturn(reservationPendingRes).when(paymentDao)
                .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(), ArgumentMatchers.any(),
                        ArgumentMatchers.any(), Mockito.eq(cnfNumber), ArgumentMatchers.anyBoolean());

        // commit pending mock response
        ResponseEntity<ReservationRes> commitPendingRes =
                pendingCommitReservationResponse(commitReservationResJsonFile);
        setMockForPendingCommitSuccess(commitPendingRes);
    }

    public void mockForPaymentAuthFailedOps(RoomReservation request, String propertyCode, String createPendingResJsonFile, String ignorePendingResJsonFile){
        {
            //Room,property,rate Id map
            setMockForRefDataHelper(request, propertyCode, "ICECC");
            //idms token mock
            mockIdmsToken();
            //token for ACRS
            setMockAuthToken();
            // Mock the create Pending response
            ResponseEntity<ReservationPendingRes> pendingResponse =
                    (ResponseEntity<ReservationPendingRes>)makePendingRoomReservationResponse(createPendingResJsonFile);
            setMockForCreatePendingSuccess(pendingResponse);
            //pps mock
            mockPaymentAuth(request,null);
            //mock for ignore resv
            setMockForPendingIgnoreSuccess(pendingIgnoreReservationResponse(ignorePendingResJsonFile));

        }
    }

    private void mockIdmsToken(){
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("dummyValue");
        Mockito.doReturn(tokenResponse).when(idmsTokenDAO).generateToken();
    }

    private void mockPaymentAuth(RoomReservation request, String approved){
        // PPS Authorize
        Mockito.doReturn(true).when(accertifyInvokeHelper).performAFSCheck(request);
        AuthorizationTransactionRequest authorizeRequest = new AuthorizationTransactionRequest();
        authorizeRequest.setTransaction(new AuthorizationTransactionDetails());
        Mockito.doReturn(authorizeRequest).when(accertifyInvokeHelper).createAuthorizeRequest(request,request.getInSessionReservationId());
        AuthorizationTransactionResponse authorizationTransactionResponse = new AuthorizationTransactionResponse();
        authorizationTransactionResponse.setAuthorized(true);
        Mockito.doReturn(authorizationTransactionResponse).when(paymentDao).afsAuthorize(ArgumentMatchers.any());
        AuthResponse authResponse = new AuthResponse();
        authResponse.setStatusMessage(approved);
        authResponse.setAuthRequestId("fsdrw4356");
        Mockito.doReturn(authResponse).when(paymentDao).authorizePayment(ArgumentMatchers.any());

    }
    private void mockCapturePayment(String approved){
        CaptureResponse captureResponse = new CaptureResponse();
        captureResponse.setStatusMessage(approved);
        Mockito.doReturn(captureResponse).when(paymentDao).capturePayment(ArgumentMatchers.any());

    }
    protected void setMockForRefDataHelper(RoomReservation requestResponse, String propertyCode, String acrsVendor) {

        when(referenceDataDAOHelper.retrieveAcrsPropertyID(ArgumentMatchers.any())).thenReturn(propertyCode);
        if(ACRSConversionUtil.isAcrsRoomCodeGuid(requestResponse.getRoomTypeId())) {
            when(referenceDataDAOHelper.retrieveRoomTypeDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(requestResponse.getRoomTypeId())))
                    .thenReturn(ACRSConversionUtil.getRoomCode(requestResponse.getRoomTypeId()) );
            when(referenceDataDAOHelper.retrieveRoomTypeDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(ACRSConversionUtil.getRoomCode(requestResponse.getRoomTypeId()))))
                    .thenReturn(ACRSConversionUtil.getRoomCode(requestResponse.getRoomTypeId()) );
        }else {
            when(referenceDataDAOHelper.retrieveRoomTypeDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(requestResponse.getRoomTypeId())))
                    .thenReturn(ACRSConversionUtil.createRoomCodeGuid(requestResponse.getRoomTypeId(),propertyCode) );
        }
        setMockForRatePlanGroupRefData(requestResponse.getProgramId(), propertyCode, requestResponse.getIsGroupCode());

        RequestSourceConfig.SourceDetails source = new RequestSourceConfig.SourceDetails();
        when(referenceDataDAOHelper.getRequestSource((ArgumentMatchers.any()))).thenReturn(source);
        when(referenceDataDAOHelper.getAcrsVendor(Mockito.any())).thenReturn(acrsVendor);
        when(referenceDataDAOHelper.retrieveGsePropertyID(Mockito.any())).thenReturn(requestResponse.getPropertyId());
        requestResponse.getBookings().stream().forEach(booking->{
            setMockForRatePlanGroupRefData(booking.getProgramId(), propertyCode, requestResponse.getIsGroupCode());
            if(StringUtils.isNotBlank(booking.getOverrideProgramId())) {
                setMockForRatePlanGroupRefData(booking.getOverrideProgramId(), propertyCode, requestResponse.getIsGroupCode());
            }
        });
    }

    private void setMockForRatePlanGroupRefData(String programId, String propertyCode, boolean isGroup) {
        if(ACRSConversionUtil.isAcrsGroupCodeGuid(programId)){
            when(referenceDataDAOHelper.createFormatedGroupCode(ArgumentMatchers.any(), ArgumentMatchers.eq(programId)))
                    .thenReturn(ACRSConversionUtil.getGroupCode(programId));
        }else if(ACRSConversionUtil.isAcrsRatePlanGuid(programId)){
            when(referenceDataDAOHelper.retrieveRatePlanDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(programId)))
                    .thenReturn(ACRSConversionUtil.getRatePlanCode(programId));
            when(referenceDataDAOHelper.retrieveRatePlanDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(ACRSConversionUtil.getRatePlanCode(programId))))
                    .thenReturn(ACRSConversionUtil.getRatePlanCode(programId));
        }else if(isGroup){
            when(referenceDataDAOHelper.retrieveRatePlanDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(programId)))
                    .thenReturn(ACRSConversionUtil.createGroupCodeGuid(programId,propertyCode));
        }else{
            when(referenceDataDAOHelper.retrieveRatePlanDetail(ArgumentMatchers.any(), ArgumentMatchers.eq(programId)))
                    .thenReturn(ACRSConversionUtil.createRatePlanCodeGuid(programId,propertyCode));
        }
    }
    
    
    private HotelReservationPendingRes getHotelReservationPendingRes() {
		ReservationModifyPendingRes response = (ReservationModifyPendingRes) makePendingRoomReservationResponse()
				.getBody();
		return response.getData().getHotelReservation();
    }
   
    private void setReservationModifyPendingRes() {
		ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
		Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRequestToPaymentExchangeToken(
				ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.anyBoolean());
	}

	private HttpEntity<ReservationModifyPendingRes> makePendingRoomReservationResponseFromPayment() {
		ResponseEntity<ReservationModifyPendingRes> response = new ResponseEntity<ReservationModifyPendingRes>(
				getReservationModifyPendingRes(), HttpStatus.OK);
		return response;
	}

	private ReservationModifyPendingRes getReservationModifyPendingRes() {
		HotelReservationPendingRes pendingRes = getHotelReservationPendingRes();

		ReservationModifyPendingRes reservationModifyPendingRes = new ReservationModifyPendingRes();
		ReservationModifyPendingResData data = new ReservationModifyPendingResData();
		PointOfSale creator = new PointOfSale();
		creator.setOrigin("US");
		creator.setVendorCode("VendorCode");
		creator.setVendorName("VendorName");
		data.setCreator(creator);

		PointOfSale requestor = new PointOfSale();
		requestor.setOrigin("US");
		requestor.setVendorCode("VendorCode");
		requestor.setVendorName("VendorName");
		data.setRequestor(requestor);

		data.setHotelReservation(pendingRes);
		reservationModifyPendingRes.setData(data);
		return reservationModifyPendingRes;
	}

    @Test
    //done
    public void createTransientReservation_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-acrs-res.json";
             // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);

            // mock
            mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithCashPayment failed with exception: {}", e);
            Assertions.fail("createTransientReservation_Success Failed");
        }
    }
    @Test
    //done
    public void createTransientReservationWithCashPayment_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientCashResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-cash-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-cash-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-cash-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-cash-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientCashResvReqJsonFile, RoomReservation.class);
            // mock
            mockForSuccessOperations(request, propertyCode,  createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);

            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithCashPayment failed with exception: {}", e);
            Assertions.fail("createTransientReservationWithCashPayment Failed");
        }
    }
    @Test
    //Done
    public void createTransientReservationWithOverriddenDeposit_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-overriddendeposit-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-overriddendeposit-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-overriddendeposit-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-overriddendeposit-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
             mockForSuccessOperations(request,propertyCode,createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            double overriddenDepositAmt = request.getDepositCalc().getOverrideAmount();
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            request.getDepositCalc().setOverrideAmount(overriddenDepositAmt);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithOverriddenDeposit_Success failed with exception: {}", e);
            Assertions.fail("createTransientReservationWithOverriddenDeposit_Success Failed");
        }
    }
    //done
    @Test
    public void createTransientReservationOverridePriceNProgram_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-overrideprogramprice-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-overrideprogramprice-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-overrideprogramprice-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-overrideprogramprice-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
            mockForSuccessOperations(request,propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);

            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationOverridePriceNProgram_Success failed with exception: {}", e);
            Assertions.fail("createTransientReservationOverridePriceNProgram_Success Failed");
        }
    }
    //done
    @Test
    public void createTransientReservationWithGroup_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-group-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-group-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-group-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-group-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
            mockForSuccessOperations(request, propertyCode,  createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);

            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithGroup_Success failed with exception: {}", e);
            Assertions.fail("createTransientReservationWithGroup_Success Failed");
        }
    }
    /**
     * Transient reservation with  Manual RI
     */
    @Test
    //done
    public void createTransientReservationWithManualRI_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-manualRI-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-manualRI-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-manualRI-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-manualRI-acrs-res.json";
            String getRefDataRIResponseJsonFile = "/acrs/createreservation/response/refDataRI-response.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
            mockForSuccessOperations(request, propertyCode,  createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            mockForRouting(getRefDataRIResponseJsonFile);
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithManualRI_Success failed with exception: {}", e);
            Assertions.fail("createTransientReservationWithManualRI_Success Failed");
        }
    }

    // PO reservation
    @Test
    //done
    public void createTransientPartyReservation_Success() {
        try {
            // ACRS primary res
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-party-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-party-withoutsplict-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-party-withoutsplict-acrs-res.json";
            // ACRS sec res
            String createPendingSecResJsonFile= "/acrs/createreservation/response/transient-create-pending-party-sec-acrs-res.json";
            String modifyPendingSecResJsonFile= "/acrs/createreservation/response/transient-modify-pending-party-withoutsplict-sec-acrs-res.json";
            String commitPendingSecResJsonFile= "/acrs/createreservation/response/transient-create-commit-party-withoutsplict-sec-acrs-res.json";
            // Mock ACRS create pending for primary and  secondary
            ResponseEntity<ReservationPendingRes> pendingPrimaryResponse =
                    (ResponseEntity<ReservationPendingRes>)makePendingRoomReservationResponse(createPendingResJsonFile);
            ResponseEntity<ReservationPendingRes> pendingSecResponse =
                    (ResponseEntity<ReservationPendingRes>)makePendingRoomReservationResponse(createPendingSecResJsonFile);

            when(client.postForEntity(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(),
                    ArgumentMatchers.<Class<ReservationPendingRes>>any(), Mockito.anyMap()))
                    .thenReturn(pendingPrimaryResponse)
                    .thenReturn(pendingSecResponse);

            //mock ACRS for secondary reservation modify pending and commit
            mockACRSResponse(null,modifyPendingSecResJsonFile,commitPendingSecResJsonFile);
            partyReservation(false,null, modifyPendingResJsonFile, commitPendingResJsonFile);


        } catch (Exception e) {
            logger.error("createTransientPartyReservation_Success failed with exception: {}", e);
            Assertions.fail("createTransientPartyReservation_Success Failed");
        }
    }

    @Test
    public void createTransientPartyReservation_with_Split_Card_Success() {
        try {
            // ACRS primary res
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-party-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-party-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-party-acrs-res.json";
            // ACRS sec res
            String createPendingSecResJsonFile= "/acrs/createreservation/response/transient-create-pending-party-sec-acrs-res.json";
            String modifyPendingSecResJsonFile= "/acrs/createreservation/response/transient-modify-pending-party-sec-acrs-res.json";
            String commitPendingSecResJsonFile= "/acrs/createreservation/response/transient-create-commit-party-sec-acrs-res.json";
            // Mock ACRS create pending for primary and  secondary
            ResponseEntity<ReservationPendingRes> pendingPrimaryResponse =
                    (ResponseEntity<ReservationPendingRes>)makePendingRoomReservationResponse(createPendingResJsonFile);
            ResponseEntity<ReservationPendingRes> pendingSecResponse =
                    (ResponseEntity<ReservationPendingRes>)makePendingRoomReservationResponse(createPendingSecResJsonFile);

            when(client.postForEntity(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(),
                    ArgumentMatchers.<Class<ReservationPendingRes>>any(), Mockito.anyMap()))
                    .thenReturn(pendingPrimaryResponse)
                    .thenReturn(pendingSecResponse);

            //mock ACRS for secondary reservation modify pending and commit
            mockACRSResponse(null,modifyPendingSecResJsonFile,commitPendingSecResJsonFile);
            partyReservation(true,null, modifyPendingResJsonFile, commitPendingResJsonFile);
        } catch (Exception e) {
            logger.error("createTransientPartyReservation_Success failed with exception: {}", e);
            Assertions.fail("createTransientPartyReservation_Success Failed");
        }
    }
    private void partyReservation(boolean splitCreditCardDetails,String createPendingResJsonFile, String modifyPendingResJsonFile, String commitPendingResJsonFile){
        String propertyCode = "MV021";
        String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-party-req.json";
          // Create Request
        RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
        int numberOfRoom = request.getNumRooms();
        List<CreditCardCharge> creditCardCharges = request.getCreditCardCharges();
        // mock
        mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);

        // Send Request
        PartyRoomReservation reservationRes = reservationDAOStrategyACRSImpl.makePartyRoomReservation(request,splitCreditCardDetails);
        //Assert number of success reservation
        Assertions.assertEquals(numberOfRoom,reservationRes.getRoomReservations().size());
        // Assert Success
        reservationRes.getRoomReservations().forEach(party ->{
            //if its secondary reservation and splitCreditCardDetails = false, then its cash payment
            if(splitCreditCardDetails){
                request.setCreditCardCharges(creditCardCharges);
            }else if(party.getPartyConfirmationNumber().contains(party.getConfirmationNumber())){
                // primary reservation
                request.setCreditCardCharges(creditCardCharges);
            }else{
                request.setCreditCardCharges(null);
                party.setCreditCardCharges(null);
            }
            checkAssertions(request, party);
        });

    }
    //share reservation
    @Test
    //done
    public void createTransientShareReservation_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-share-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-share-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-share-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-share-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
             mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            //share link mock
            ResponseEntity<LinkReservationRes> shareLinkRes = createMockShareLink("/acrs/createreservation/response/transient-sharelink-acrs-res.json");
            setMockForShareLinkSuccess(shareLinkRes);
            // card details backup
            List<CreditCardCharge> backupCCInfo = request.getCreditCardCharges();
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            request.setCreditCardCharges(backupCCInfo);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientShareReservation_Success failed with exception: {}", e);
            Assertions.fail("createTransientShareReservation_Success Failed");
        }
    }

    //Alerts-Spacial request
    @Test
    //done
    public void createTransientWithTracesAndSplReqReservation_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-traces-splreq-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-traces-splreq-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-traces-splreq-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-traces-splreq-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
             mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            // mock for refData(traces and special request) API call
            mockForRefData("/acrs/createreservation/response/refData-response.json");
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientWithTracesAndSplReqReservation_Success failed with exception: {}", e);
            Assertions.fail("createTransientWithTracesAndSplReqReservation_Success Failed");
        }
    }
    //Special request-Addons
    @Test
    //done
    public void createTransientReservationWithAddons_Success() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-addons-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-addons-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-addons-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-addons-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);

            // mock
            mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithAddons_Success failed with exception: {}", e);
            Assertions.fail("createTransientReservationWithAddons_Success Failed");
        }
    }
    //Checkout reservation
    @Test
    //done
    public void checkoutReservation_Success() {
        try {
            String checkoutResvReqJsonFile = "/acrs/createreservation/request/checkout-reservation-req.json";
            String checkoutModifyResJsonFile= "/acrs/createreservation/response/checkout-modify-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(checkoutResvReqJsonFile, RoomReservation.class);

            // mock
            ReservationRes modifyRes = pendingCommitReservationResponse(checkoutModifyResJsonFile).getBody();
            RoomReservation checkoutRes = RoomReservationTransformer.transform(modifyRes, acrsProperties);
            Mockito.doReturn(checkoutRes).when(modifyReservationDAOStrategyACRSImpl).modifyRoomReservationV2(request);
             // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("checkoutReservation_Success failed with exception: {}", e);
            Assertions.fail("checkoutReservation_Success Failed");
        }
    }
    @Test
    public void resetPerpetualPricing() throws Exception {
        final RoomReservation reservation = new RoomReservation();
        reservation.setPerpetualPricing(true);
        reservation.setPromo("CMLIFE");

        // With Promo
        reservationDAOStrategyACRSImpl.resetPerpetualPricing(reservation);
        Assert.assertFalse(reservation.isPerpetualPricing());

        // With programid as PO
        reservation.setPerpetualPricing(true);
        reservation.setPromo(null);
        reservation.setProgramId("RPCD-v-COMPS002-d-PROP-v-MV021");
        reservationDAOStrategyACRSImpl.resetPerpetualPricing(reservation);
        Assert.assertTrue(reservation.isPerpetualPricing());

        // With booking programid as PO
        reservation.setPerpetualPricing(true);
        reservation.setProgramId("RPCD-v-BAR-d-PROP-v-MV021");
        reservation.setBookings(new ArrayList<>());
        RoomPrice booking = new RoomPrice();
        reservation.getBookings().add(booking);
        booking.setProgramId("RPCD-v-COMPS002-d-PROP-v-MV021");
        reservationDAOStrategyACRSImpl.resetPerpetualPricing(reservation);
        Assert.assertTrue(reservation.isPerpetualPricing());

        // With programid as non PO
        booking.setProgramId("RPCD-v-BAR-d-PROP-v-MV021");
        reservationDAOStrategyACRSImpl.resetPerpetualPricing(reservation);
        Assert.assertFalse(reservation.isPerpetualPricing());
    }
    @Test
    void createReservationWithPreDefinedPkgTestSuccess() {
        try {
            String propertyCode = "MV275";
            String propertyId = "e0f70eb3-7e27-4c33-8bcd-f30bf3b1103a";
            String roomTypeId = "ROOMCD-v-DRST-d-PROP-v-MV275";
            String programId = "RPCD-v-PFABT-d-PROP-v-MV275";
            String createPendingResJsonFile= "/acrs/premadePackages/crs-premadePackage-create-pending-response.json";
            String modifyPendingResJsonFile= "/acrs/premadePackages/crs-premadePackage-modify-pending-formOfPayment-response.json";
            String commitPendingResJsonFile= "/acrs/premadePackages/crs-premadePackage-commit-pending-response.json";
            // Create Request
            // Create Request
            RoomReservation request = createRoomReservation(fmt.parse("2022-09-08"), fmt.parse("2022-09-11"),
                    propertyId, roomTypeId, programId, new ArrayList<>());
            // Set prices and Deposit
            request.getBookings().forEach(booking -> booking.setPrice(158.00));
            request.getDepositCalc().setAmount(172.45);
            request.getCreditCardCharges().get(0).setAmount(172.45);
            // mock
            mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
            // Send Request
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            // Assert Success
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createReservationWithPreDefinedPkgTestSuccess failed with exception: {}", e);
            Assertions.fail("createReservationWithPreDefinedPkgTestSuccess Failed");
        }
    }
    //save reservation
    //pkg program
    //Failed test cases
    //Payment failed
    // payment failed
    @Test
    public void createReservationTestPaymentFailed() {

        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-acrs-res.json";
            String ignorePendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-acrs-res.json";
            // Create Request
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            // mock
            mockForPaymentAuthFailedOps(request, propertyCode, createPendingResJsonFile, ignorePendingResJsonFile);
             // Assert Success
            assertThatThrownBy(() -> reservationDAOStrategyACRSImpl.makeRoomReservationV2(request)).isInstanceOf(BusinessException.class)
                    .hasMessage(getErrorMessage(ErrorCode.PAYMENT_AUTHORIZATION_FAILED));
        } catch (Exception e) {
            Assert.fail("createReservationTestPaymentFailed Failed");
        }
    }
    //ACRS commit failed
    
    // Test cases for CBSR-1918 Payment Widget Changes
    @Test
    public void createTransientReservation_skipPaymentProcess() {
        try {
            String propertyCode = "MV021";
            String createTransientResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-skipPaymentProcess-req.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-acrs-res.json";
            
            RoomReservation request = convertRBSReq(createTransientResvReqJsonFile, RoomReservation.class);
            mockForSuccessOperations(request, propertyCode, createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);
 
            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservation_skipPaymentProcess failed with exception: {}", e);
            Assertions.fail("createTransientReservation_skipPaymentProcess Failed");
        }
    }
    
    @Test
    public void createTransientReservationWithCashPayment__skipPaymentProcess() {
        try {
            String propertyCode = "MV021";
            String createTransientCashResvReqJsonFile = "/acrs/createreservation/request/transient-create-reservation-cash-req-skipPaymentProcess.json";
            String createPendingResJsonFile= "/acrs/createreservation/response/transient-create-pending-cash-acrs-res.json";
            String modifyPendingResJsonFile= "/acrs/createreservation/response/transient-modify-pending-cash-acrs-res.json";
            String commitPendingResJsonFile= "/acrs/createreservation/response/transient-create-commit-cash-acrs-res.json";
            
            RoomReservation request = convertRBSReq(createTransientCashResvReqJsonFile, RoomReservation.class);
            mockForSuccessOperations(request, propertyCode,  createPendingResJsonFile, modifyPendingResJsonFile, commitPendingResJsonFile);

            RoomReservation reservationRes = reservationDAOStrategyACRSImpl.makeRoomReservationV2(request);
            checkAssertions(request, reservationRes);

        } catch (Exception e) {
            logger.error("createTransientReservationWithCashPayment__skipPaymentProcess failed with exception: {}", e);
            Assertions.fail("createTransientReservationWithCashPayment__skipPaymentProcess Failed");
        }
    }
       
	@Test
	void saveRoomReservation_Success(){
		setMockAuthToken();
		setReservationModifyPendingRes();

		RoomReservation reservation = new RoomReservation();
		reservation.setConfirmationNumber("123456");
		reservation.setPropertyId("propertyId");
		reservation.setAgentInfo(new AgentInfo());
		reservation.setRrUpSell("test");
		reservation.setSource("exampleSource");

		final ReservationRetrieveResReservation existingReservation = new ReservationRetrieveResReservation();
		final ReservationRetrieveResreservationData data = new ReservationRetrieveResreservationData();

		final HotelReservationRetrieveResReservation hotelReservation = new HotelReservationRetrieveResReservation();
		data.setHotelReservation(hotelReservation);
		existingReservation.setData(data);

		final List<HotelRes> hotels = new ArrayList<>();
		HotelRes hotel = new HotelRes();
		hotel.setPropertyCode("PropertyCode");
		hotels.add(hotel);
		hotelReservation.setHotels(hotels);

		final SegmentRes segmentRes = new SegmentRes();
		final SegmentResItem segment = new SegmentResItem();
		segment.setSegmentHolderId(1);
		final OfferRes offer = new OfferRes();
		offer.setPromoCode("PTRN123456");
		segment.setStart(LocalDate.now());
		segment.setEnd(LocalDate.now().plusDays(1));
		segmentRes.add(segment);
		hotelReservation.setSegments(segmentRes);

		final GuestsRes userProfiles = new GuestsRes();
		final GuestsResItem profile = new GuestsResItem();
		final LoyaltyProgram loyaltyProgram = new LoyaltyProgram();
		String mLifeNo = "987877474";
		loyaltyProgram.setLoyaltyId(mLifeNo);
		profile.setId(1);
		profile.setLoyaltyProgram(loyaltyProgram);
		PersonName personName = new PersonName();
		personName.setTitle("Miss");
		profile.setPersonName(personName);
		userProfiles.add(profile);
		ReservationIdsRes reservationIdsRes = new ReservationIdsRes();
		reservationIdsRes.setCfNumber("123");
		reservationIdsRes.setExtCxNumber("987");
		ExtCfNumber extCxNumber = new ExtCfNumber();
		extCxNumber.setNumber("678");
		reservationIdsRes.setExtCfNumber(extCxNumber);
		reservationIdsRes.setPmsCfNumber("675");
		hotelReservation.setUserProfiles(userProfiles);
		hotelReservation.setReservationIds(reservationIdsRes);
		ResStatus status = ResStatus.BK;
		hotelReservation.setStatus(status);
		hotelReservation.setSegments(segmentRes);
		
		ProductUseRes productUseRes=new ProductUseRes();
		ProductUseResItem productUseResItem = new ProductUseResItem();
		
		List<GuestCountRes> guestCountResList = new ArrayList<>();
		GuestCountRes guestCountRes = new GuestCountRes();
		guestCountRes.setOtaCode("AQC10");
		guestCountRes.count(4);
		guestCountResList.add(guestCountRes);
		productUseResItem.setGuestCounts(guestCountResList);
		productUseResItem.setQuantity(10);
		productUseResItem.setAssignedRoom("ABC");
		ProductUseRates productUseRates = new ProductUseRates();
		productUseRates.setHasRateChange(true);

		List<RateChange> rateChangeList = new ArrayList<>();
		RateChange rateChange = new RateChange();
		rateChange.setStart(LocalDate.now());
		rateChange.setEnd(LocalDate.now().plusDays(3));
		
		DailyRateDetails dailyRateDetails = new DailyRateDetails();
		dailyRateDetails.setBsAmt("126");
		rateChange.setDailyTotalRate(dailyRateDetails);
		rateChangeList.add(rateChange);

		productUseRates.setDailyRates(rateChangeList);
		productUseResItem.setProductRates(productUseRates);
		Period period = new Period();
		period.setStart(LocalDate.now());
		period.setEnd(LocalDate.now().plusDays(3));
		
		productUseResItem.setPeriod(period);
		productUseResItem.setIsMainProduct(true);
		productUseResItem.setRatePlanCode("8796");
		productUseRes.add(productUseResItem);

		offer.setProductUses(productUseRes);
		offer.setGroupCode("89");
		segment.setOffer(offer); 
		
		List<RatePlanRes> ratePlanResList = new ArrayList<>();
		RatePlanRes ratePlanRes= new RatePlanRes();
		ratePlanRes.setActiveDays("2");
		ratePlanResList.add(ratePlanRes);
		hotelReservation.setRatePlans(ratePlanResList);

		ModificationChangesRequest modificationChangesRequest = new ModificationChangesRequest();
		TripDetail tripDetail = new TripDetail();
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -4);
		tripDetail.setCheckInDate(calendar.getTime());

		Calendar calendar1 = Calendar.getInstance();
		calendar1.add(Calendar.DATE, -2);
		tripDetail.setCheckOutDate(calendar1.getTime());
		modificationChangesRequest.setTripDetails(tripDetail);

		when(referenceDataDAOHelper.retrieveAcrsPropertyID(reservation.getPropertyId())).thenReturn("PropertyId");
		when(referenceDataDAOHelper.getAcrsVendor(Mockito.anyString())).thenReturn("ICECC");

		ResponseEntity<ReservationRetrieveResReservation> crsResponse = ResponseEntity.ok(existingReservation);

		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.eq(HttpMethod.GET),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(), Mockito.anyMap()))
		.thenReturn(crsResponse);

		when(referenceDataDAOHelper.updateAcrsReferencesFromGse(Mockito.any(ModificationChangesRequest.class))).thenReturn(modificationChangesRequest);

		reservationDAOStrategyACRSImpl.saveRoomReservation(reservation);
		
		// Assertion
		assertNotNull(existingReservation); // Ensuring that an existing reservation is not null after retrieval
		assertEquals("PropertyCode", existingReservation.getData().getHotelReservation().getHotels().get(0).getPropertyCode()); // Ensuring that the property code matches
		assertEquals("PTRN123456", existingReservation.getData().getHotelReservation().getSegments().get(0).getOffer().getPromoCode()); // Ensuring that the promo code matches
		assertEquals("Miss", existingReservation.getData().getHotelReservation().getUserProfiles().get(0).getPersonName().getTitle()); // Ensuring that the title matches
		assertEquals("987877474", existingReservation.getData().getHotelReservation().getUserProfiles().get(0).getLoyaltyProgram().getLoyaltyId()); // Ensuring that the loyalty ID matches
		assertEquals("BK", existingReservation.getData().getHotelReservation().getStatus().toString()); // Ensuring that the reservation status is as expected
		assertEquals(1, existingReservation.getData().getHotelReservation().getRatePlans().size()); // Ensuring that the rate plan list is not empty
		assertEquals("2", existingReservation.getData().getHotelReservation().getRatePlans().get(0).getActiveDays()); // Ensuring that the active days for the rate plan are as expected
	}
	
    @Test
	void testHasError() throws IOException {
    	ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);

		// Assertions
		assertTrue(result);
	}
    
    @Test
    void testHandleErrorSystemExceptionSYSTEM_ERROR() throws IOException {
    	ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionElse() throws IOException {
    	ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("Some Error Occured");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionDATES_UNAVAILABLE() throws IOException {
    	ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("<UnableToPriceTrip>");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.DATES_UNAVAILABLE, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionRESERVATION_NOT_FOUND() throws IOException {
    	ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("<BookingNotFound>");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		
		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
    }
}
