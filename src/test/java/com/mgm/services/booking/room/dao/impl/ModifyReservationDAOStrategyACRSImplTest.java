package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.dao.helper.ReservationDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapper;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapperImpl;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapperImpl;
import com.mgm.services.booking.room.model.DestinationHeader;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.paymentservice.AuthResponse;
import com.mgm.services.booking.room.model.paymentservice.CaptureResponse;
import com.mgm.services.booking.room.model.paymentservice.RefundResponse;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.transformer.BaseAcrsTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Log4j2
public class ModifyReservationDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {
    @Mock
    private static RestTemplate client;

    @Mock
    private static DomainProperties domainProperties;

    @Mock
    private static RestTemplateBuilder restTemplateBuilder;

    private static AcrsProperties acrsProperties;

    @Mock
    private static IDMSTokenDAO idmsTokenDAO;

    @Mock
    private static PaymentDAO paymentDao;
    
    @Mock
    private static BaseAcrsDAO baseAcrsDao;

    @Mock
    private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

    private RoomReservationPendingResMapper pendingResMapper = new RoomReservationPendingResMapperImpl();

    @InjectMocks
    private static ModifyReservationDAOStrategyACRSImpl modifyReservationDAOStrategyACRSImpl;
    
    @Mock
    private ReservationDAOHelper reservationDAOHelper;

    private HotelReservationRes hotelReservationResResponse() {
        RoomReservationPendingResMapperImpl impl = new RoomReservationPendingResMapperImpl();
        ReservationModifyPendingRes response = (ReservationModifyPendingRes) makePendingRoomReservationResponse()
                .getBody();
        HotelReservationPendingRes pendingRes = response.getData().getHotelReservation();
        return impl.pendingResvToHotelReservationRes(pendingRes);
    }

    private HotelReservationRes hotelReservationResResponse_NoDeposit() {
        ReservationModifyPendingRes response = (ReservationModifyPendingRes) makePendingRoomReservationResponse_NoDeposit().getBody();
        HotelReservationPendingRes pendingRes = response.getData().getHotelReservation();
        return pendingResMapper.pendingResvToHotelReservationRes(pendingRes);
    }

	private ResponseEntity<ReservationRetrieveResReservation> retrieveReservationResponse() {
		return new ResponseEntity<>(
                convertCrs("/acrs/modifyreservation/crs_retrieve_resv.json", ReservationRetrieveResReservation.class),
                HttpStatus.OK);
    }

	private ResponseEntity<ReservationRetrieveResReservation> retrieveReservationResponseWithMultipleNonMainProductUses() {
		return new ResponseEntity<>(
				convertCrs("/acrs/modifyreservation/crs_retrieve_resv_multiple_non_main_products.json",
						ReservationRetrieveResReservation.class), HttpStatus.OK);
	}
    
    private HttpEntity<?> retrieveReservationExcludeComponentResponse() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/acrs/modifyreservation/crs_retrieveReservation_excludeComponent.json", ReservationRetrieveResReservation.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> makePendingRoomReservationResponse() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/acrs/modifyreservation/crs-modify-pending.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }
    
    private HttpEntity<?> makePendingIgnoreRoomReservationResponse() {
        ResponseEntity<?> response = new ResponseEntity<ReservationRes>(new ReservationRes(),
                HttpStatus.OK);
        return response;
    }
    
    private HttpEntity<ReservationModifyPendingRes> makePendingRoomReservationResponseFromPayment() {
        ResponseEntity<ReservationModifyPendingRes> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/acrs/modifyreservation/crs-modify-pending.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> makePendingRoomReservationFailedResponse() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/acrs/modifyreservation/crs-modify-pending-error.json", ReservationModifyPendingRes.class),
                HttpStatus.BAD_REQUEST);
        return response;
    }

    private HttpEntity<?> pendingCommitReservationResponse() {
        ResponseEntity<?> response = new ResponseEntity<ReservationRes>(
                convertCrs("/acrs/modifyreservation/crs-pending-commit.json", ReservationRes.class), HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> makePendingRoomReservationResponse_NoDeposit() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/paymentwidgetv4/commit/no_deposit/crs-modify-pending-nodeposit.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> retrivePendingReservationResponse_NoDeposit() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/commit/no_deposit/crs_retrieve_pending_resv_nodeposit.json", ReservationRetrieveResReservation.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> pendingCommitReservationResponse_NoDeposit() {
        ResponseEntity<?> response = new ResponseEntity<ReservationRes>(
                convertCrs("/paymentwidgetv4/commit/no_deposit/crs-pending-commit-nodeposit.json", ReservationRes.class), HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> searchReservationResponse_NoDeposit() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/commit/no_deposit/crs_search_resv_nodeposit.json", ReservationSearchResPostSearchReservations.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> makePendingRoomReservationResponse_AddnDeposit() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/paymentwidgetv4/commit/addn_deposit/crs-modify-pending-addndeposit.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> retrivePendingReservationResponse_AddnDeposit() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/commit/addn_deposit/crs_retrieve_pending_resv_addndeposit.json", ReservationRetrieveResReservation.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> pendingCommitReservationResponse_AddnDeposit() {
        ResponseEntity<?> response = new ResponseEntity<ReservationRes>(
                convertCrs("/paymentwidgetv4/commit/addn_deposit/crs-pending-commit-addndeposit.json", ReservationRes.class), HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> searchReservationResponse_AddnDeposit() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/commit/addn_deposit/crs_search_resv_addndeposit.json", ReservationSearchResPostSearchReservations.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> makePendingRoomReservationResponse_RefundDeposit() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/paymentwidgetv4/commit/refund_deposit/crs-modify-pending-refunddeposit.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> retrivePendingReservationResponse_RefundDeposit() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/commit/refund_deposit/crs_retrieve_pending_resv_refunddeposit.json", ReservationRetrieveResReservation.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> pendingCommitReservationResponse_RefundDeposit() {
        ResponseEntity<?> response = new ResponseEntity<ReservationRes>(
                convertCrs("/paymentwidgetv4/commit/refund_deposit/crs-pending-commit-refunddeposit.json", ReservationRes.class), HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> searchReservationResponse_RefundDeposit() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/commit/refund_deposit/crs_search_resv_refunddeposit.json", ReservationSearchResPostSearchReservations.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> makePendingRoomReservationResponse_SingleToShared() {
        ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
                convertCrs("/paymentwidgetv4/modifyPending/singleToShared/crs-modify-pending-singleToShared.json", ReservationModifyPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> retrivePendingReservationResponse_SingleToShared() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/modifyPending/singleToShared/crs-retrieve-pending-resv-singleToShared.json", ReservationRetrieveResReservation.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> pendingCommitReservationResponse_SingleToShared() {
        ResponseEntity<?> response = new ResponseEntity<ReservationRes>(
                convertCrs("/paymentwidgetv4/modifyPending/singleToShared/crs-pending-commit-singleToShared.json", ReservationRes.class), HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> pendingReservationResponse_SingleToShared() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/modifyPending/singleToShared/crs-resv-pending-singleToShared.json", ReservationPendingRes.class),
                HttpStatus.OK);
        return response;
    }

    private HttpEntity<?> sharedLinkReservationResponse_SingleToShared() {
        ResponseEntity<?> response = new ResponseEntity<>(
                convertCrs("/paymentwidgetv4/modifyPending/singleToShared/crs-shared-resv-link-response-singleToShared.json", LinkReservationRes.class),
                HttpStatus.OK);
        return response;
    }

    private void setMockForAddnDepositPendingRetriveSuccess() {
        when(client.exchange(ArgumentMatchers.contains("last"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRetrieveResReservation>) retrivePendingReservationResponse_AddnDeposit());
    }

    private void setMockForAddnDepositModifyPendingSuccess() {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationModifyPendingRes>) makePendingRoomReservationResponse_AddnDeposit());
    }

    private void setMockForAddnDepositPendingCommitSuccess() {
        when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRes>) pendingCommitReservationResponse_AddnDeposit());
    }

    private void setMockForAddnDepositSearchSuccess() {
        when(client.exchange(ArgumentMatchers.contains("search"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationSearchResPostSearchReservations>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationSearchResPostSearchReservations>) searchReservationResponse_AddnDeposit());
    }

    private void setMockForRefundDepositPendingRetriveSuccess() {
        when(client.exchange(ArgumentMatchers.contains("last"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRetrieveResReservation>) retrivePendingReservationResponse_RefundDeposit());
    }

    private void setMockForRefundDepositModifyPendingSuccess() {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationModifyPendingRes>) makePendingRoomReservationResponse_RefundDeposit());
    }

    private void setMockForRefundDepositPendingCommitSuccess() {
        when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRes>) pendingCommitReservationResponse_RefundDeposit());
    }

    private void setMockForRefundDepositSearchSuccess() {
        when(client.exchange(ArgumentMatchers.contains("search"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationSearchResPostSearchReservations>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationSearchResPostSearchReservations>) searchReservationResponse_RefundDeposit());
    }

    private void setMockForNoDepositPendingRetriveSuccess() {
        when(client.exchange(ArgumentMatchers.contains("last"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRetrieveResReservation>) retrivePendingReservationResponse_NoDeposit());
    }

    private void setMockForNoDepositModifyPendingSuccess() {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationModifyPendingRes>) makePendingRoomReservationResponse_NoDeposit());
    }

    private void setMockForNoDepositPendingCommitSuccess() {
        when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRes>) pendingCommitReservationResponse_NoDeposit());
    }

    private void setMockForNoDepositSearchSuccess() {
        when(client.exchange(ArgumentMatchers.contains("search"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationSearchResPostSearchReservations>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationSearchResPostSearchReservations>) searchReservationResponse_NoDeposit());
    }

    private void setMockForSingleToSharedPendingRetriveSuccess() {
        when(client.exchange(ArgumentMatchers.contains("last"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRetrieveResReservation>) retrivePendingReservationResponse_SingleToShared());
    }

    private void setMockForSingleToSharedModifyPendingSuccess() {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationModifyPendingRes>) makePendingRoomReservationResponse_SingleToShared());
    }

    private void setMockForSingleToSharedPendingCommitSuccess() {
        when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationRes>) pendingCommitReservationResponse_SingleToShared());
    }

    private void setMockForSingleToSharedLinkReservationResponseSucces() {
        when(client.exchange(ArgumentMatchers.contains("links"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<LinkReservationRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<LinkReservationRes>) sharedLinkReservationResponse_SingleToShared());
    }

    private void setMockForSingleToSharedPendingReservationSuccess() {
        when(client.postForEntity(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(),
                ArgumentMatchers.<Class<ReservationPendingRes>> any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationPendingRes>) pendingReservationResponse_SingleToShared());
    }


    private void setMockForRetrieveSuccess() {
        when(client.exchange(ArgumentMatchers.contains("cardExpireDate"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>> any(), Mockito.anyMap()))
				.thenReturn(retrieveReservationResponse());
    }

    private void setMockForModifyPendingSuccess() {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>> any(), Mockito.anyMap()))
                        .thenReturn((ResponseEntity<ReservationModifyPendingRes>) makePendingRoomReservationResponse());
    }

    private void setMockForPendingCommitSuccess() {
        when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>> any(), Mockito.anyMap()))
                        .thenReturn((ResponseEntity<ReservationRes>) pendingCommitReservationResponse());
    }

    private void setMockForModifyPendingFailed() {
        when(client.exchange(
                ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class), ArgumentMatchers.any(),
                ArgumentMatchers.<Class<ReservationModifyPendingRes>> any(), Mockito.anyMap())).thenReturn(
                        (ResponseEntity<ReservationModifyPendingRes>) makePendingRoomReservationFailedResponse());
    }
    
    private void setMockForModifyIgnorePendingSuccess() {
        when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>> any(), Mockito.anyMap()))
                        .thenReturn((ResponseEntity<ReservationRes>) makePendingIgnoreRoomReservationResponse());
    }

    private void setMockReferenceDataDAOHelper() {
        setMockForRoomPropertyCode();
        // Do nothing and return parameter as inputted
        when(referenceDataDAOHelper.updateAcrsReferencesFromGse(Mockito.any(ModificationChangesRequest.class))).thenAnswer(i -> i.getArguments()[0]);
    }

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		runOnceBeforeClass();
	}

	@BeforeEach
    @Override
    public void init() {
        super.init();
		client = Mockito.mock(RestTemplate.class);
        idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
        paymentDao = Mockito.mock(PaymentDAO.class);
        roomPriceDAOStrategyACRSImpl = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
        domainProperties = new DomainProperties();
        domainProperties.setCrs("");
        domainProperties.setCrsUcpRetrieveResv("");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties.setCrsUcpRetrieveResvEnvironment("test");
		acrsProperties = new AcrsProperties();
        acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
		acrsProperties.setChainCode("chainCode");
        acrsProperties.setReservationsVersion("v2");
        acrsProperties.setEnvironment("test");
		acrsProperties.setLiveCRS(false);
		acrsProperties.setModifyDateStartPath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]/period/start");
		acrsProperties.setModifyDateEndPath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]/period/end");
		acrsProperties.setModifyRoomTypeIdPath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]/inventoryTypeCode");
		acrsProperties.setModifyProgramIdPath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]/ratePlanCode");
		acrsProperties.setDeleteProductUsePath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]");
		acrsProperties.setAddProductUsePath("/data/hotelReservation/segments[id=1]/offer/productUses");
		acrsProperties.setModifyGuestCountsPath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]");
		acrsProperties.setModifyRequestedRatesPath("/data/hotelReservation/segments[id=1]/offer/productUses[id=%s]/requestedProductRates");
		acrsProperties.setMaxAcrsCommentLength(51);
		acrsProperties.setSuppresWebComponentPatterns(Arrays.asList("ICE"));
		acrsProperties.setUpdateComponentStatusPath("/data/hotelReservation/segments[id=%s]/segmentStatus");
		acrsProperties.setUpdateComponentCancelReasonPath("/data/hotelReservation/segments[id=%s]/cancellationReasons");
		acrsProperties.setModifyAddOnPaymentInfoPath("/data/hotelReservation/segments[id=%s]/formOfPayment/paymentInfo");
        acrsProperties.setPseudoExceptionProperties(Arrays.asList("MV002","MV003","MV305","MV931"));
        // payment guarantee code map
		Map<String, PaymentType> guaranteeCodeMapMock = new HashMap<>();
		guaranteeCodeMapMock.put("CC", PaymentType.NUMBER_44);
		acrsProperties.setPaymentTypeGuaranteeCodeMap(guaranteeCodeMapMock);
        urlProperties.setAcrsRetrieveReservation("/hotel-platform/cit/mgm/v6/hotel/reservations/MGM/{cfNumber}");
        urlProperties.setAcrsReservationsCreatePending(
                "/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/pending");
        urlProperties.setAcrsReservationsConfCommit(
                "/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/{confirmationNumber}/commit");
        urlProperties.setAcrsReservationsConfPending(
                "/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/reservations/{acrsChainCode}/confirmationNumber/pending");
        urlProperties.setCrsUcpRetrieveResvUrl("/hotel-platform/cit/mgm/v6/hotel/reservations/MGM/{cfNumber}");
        urlProperties.setAcrsSearchReservations("/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/reservations/{acrsChainCode}/search");
        urlProperties.setAcrsRetrievePendingReservation("/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/reservations/{acrsChainCode}/{cfNumber}/last");
        urlProperties.setAcrsCreateReservationLink("/hotel-platform/{acrsEnvironment}/{routingCode}/{acrsVersion}/reservation/chains/{acrsChainCode}/links");
        fmt.setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
        try {
            modifyReservationDAOStrategyACRSImpl = new ModifyReservationDAOStrategyACRSImpl(urlProperties,
                    domainProperties, applicationProperties, restTemplateBuilder, acrsProperties,
                    referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);

            modifyReservationDAOStrategyACRSImpl.setIdmsTokenDAO(idmsTokenDAO);
            modifyReservationDAOStrategyACRSImpl.setPaymentDao(paymentDao);
            modifyReservationDAOStrategyACRSImpl.setPendingResMapper(pendingResMapper);
            modifyReservationDAOStrategyACRSImpl.setLoyaltyDao(loyaltyDao);
            modifyReservationDAOStrategyACRSImpl.client = client;
            ReflectionTestUtils.setField(modifyReservationDAOStrategyACRSImpl, "accertifyInvokeHelper", accertifyInvokeHelper);
            reservationDAOHelper = Mockito.mock(ReservationDAOHelper.class);
            ReflectionTestUtils.setField(modifyReservationDAOStrategyACRSImpl, "reservationDAOHelper", reservationDAOHelper);
        } catch (SSLException e) {
			log.error("ModifyReservationDAOStrategyACRSImplTest failed to initialize with following exception: ", e);
			Assertions.fail("ModifyReservationDAOStrategyACRSImplTest failed during initialization with exception.");
        }
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    }

    private RoomReservation mockReqForUpdateRoomReservation(boolean refund) throws ParseException {
        double chargeAmount = 100.00;
        if (refund) {
            chargeAmount *= -1;
        }

        RoomReservation request = new RoomReservation();
        request.setSource("mgmresorts");
        request.setConfirmationNumber("1271274619");
        request.setPropertyId("MV021");
        request.setRoomTypeId("DMLQ");
        request.setProgramId("CATST");
        List<RoomPrice> bookings = new ArrayList<>();
        RoomPrice booking = new RoomPrice();
        booking.setProgramId("CATST");
        bookings.add(booking);
        request.setBookings(bookings);
        
        ReservationProfile profile = new ReservationProfile();
        profile.setId(1000);
        profile.setFirstName("John");
        profile.setLastName("Doe");
        
        ProfileAddress profileAddress = new ProfileAddress();
        profileAddress.setStreet1("street");
        profileAddress.setStreet2("street2");
        profileAddress.setCity("city");
        profile.setAddresses(Collections.singletonList(profileAddress));
        
        ProfilePhone profilePhone = new ProfilePhone();
        profilePhone.setNumber("787263547334");
        profilePhone.setType("Home");
        profile.setPhoneNumbers(Collections.singletonList(profilePhone));
        
        request.setProfile(profile);        

        request.setCheckInDate(fmt.parse("2021-02-23"));
        request.setCheckOutDate(fmt.parse("2021-02-24"));
        // setPayments
        // setCreditcard with exp date
        CreditCardCharge creditCardCharge = new CreditCardCharge();
        creditCardCharge.setExpiry(fmt.parse("2026-02-23"));
        creditCardCharge.setAmount(chargeAmount);
        creditCardCharge.setNumber("55555544444");
        creditCardCharge.setType("Mastercard");
        List<CreditCardCharge> cards = new ArrayList<CreditCardCharge>();
        cards.add(creditCardCharge);
        request.setCreditCardCharges(cards);
        List<String> splReqs = new ArrayList<>();
        splReqs.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
        request.setSpecialRequests(splReqs);
        
        Deposit depositCalc = new Deposit();
        depositCalc.setAmount(100.0);        
        request.setDepositCalc(depositCalc);

        DepositPolicy depositPolicy = new DepositPolicy();
        depositPolicy.setDepositRequired(true);
        depositPolicy.setCreditCardRequired(true);
        request.setDepositPolicyCalc(depositPolicy);
        
        return request;
    }

    @Test
    public void modifyReservationTestSuccessRefund() {
        try {
        	setMockAuthToken();
        	setMockReferenceDataDAOHelper();
            // retrive success
			setMockForRetrieveSuccess();
            // pending success
            setMockForModifyPendingSuccess();
            // payment success
            TokenResponse tknRes = new TokenResponse();
            tknRes.setAccessToken("1234");
            AuthResponse authRes = new AuthResponse();
            authRes.setStatusMessage(ServiceConstant.APPROVED);
            authRes.setAuthRequestId("1531653");
            RefundResponse refundRes = new RefundResponse();
            refundRes.setStatusMessage(ServiceConstant.APPROVED);
            refundRes.setAmount("100"); // TODO 100 or -100
            CaptureResponse capRes = new CaptureResponse();
            capRes.setStatusMessage(ServiceConstant.APPROVED);
            Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();            
            Mockito.doReturn(refundRes).when(paymentDao).refundPayment(ArgumentMatchers.any());
            ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
            .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationResponse().getBody();
            Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            // commit success
            setMockForPendingCommitSuccess();
            RoomReservation request = mockReqForUpdateRoomReservation(true);
            Date checkInDate = fmt.parse("2020-11-29");
            Date checkOutDate = fmt.parse("2020-12-08");
            request.setCheckInDate(checkInDate);
            request.setCheckOutDate(checkOutDate);
            request.setBookings(createGenericBookings(checkInDate, checkOutDate, "CATST", 100.00));

            RoomReservation reservation = modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(request);
			Assertions.assertNotNull(reservation);
			Assertions.assertNotNull(reservation.getConfirmationNumber());
			Assertions.assertEquals(request.getConfirmationNumber(), reservation.getConfirmationNumber());
			Assertions.assertEquals(ReservationState.Booked, reservation.getState());
			Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
			Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
			Assertions.assertEquals(request.getPropertyId(), reservation.getPropertyId());
			Assertions.assertEquals(request.getRoomTypeId(), reservation.getRoomTypeId());
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Caused " + e.getCause());
			Assertions.fail("ModifyRoomReservationTestSuccessRefund has Failed");
        }
    }

    @Test
    public void modifyReservationTestSuccessAdditionalCharge() {
        try {
        	setMockAuthToken();
        	setMockReferenceDataDAOHelper();
            // retrive success
			setMockForRetrieveSuccess();
            // pending success
            setMockForModifyPendingSuccess();

            // payment success
            TokenResponse tknRes = new TokenResponse();
            tknRes.setAccessToken("1234");
            AuthResponse authRes = new AuthResponse();
            authRes.setStatusMessage(ServiceConstant.APPROVED);
            authRes.setAuthRequestId("1531653");
            CaptureResponse capRes = new CaptureResponse();
            capRes.setStatusMessage(ServiceConstant.APPROVED);
            Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
            Mockito.doReturn(authRes).when(paymentDao).authorizePayment(ArgumentMatchers.any());
            Mockito.doReturn(capRes).when(paymentDao).capturePayment(ArgumentMatchers.any());
            ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
            .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(ReservationPartialModifyReq.class), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
            ReservationRetrieveResReservation reservationRetrieveRes = retrieveReservationResponse().getBody();
            Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            // commit success
            setMockForPendingCommitSuccess();
            RoomReservation request = mockReqForUpdateRoomReservation(false);
            Date checkInDate = fmt.parse("2020-11-29");
            Date checkOutDate = fmt.parse("2020-12-08");
            request.setCheckInDate(checkInDate);
            request.setCheckOutDate(checkOutDate);

            // create bookings for these dates (programId: CATST)
            request.setBookings(createGenericBookings(checkInDate, checkOutDate, "CATST", 100.00));

            RoomReservation reservation = modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(request);
			Assertions.assertNotNull(reservation);
			Assertions.assertNotNull(reservation.getConfirmationNumber());
			Assertions.assertEquals(request.getConfirmationNumber(), reservation.getConfirmationNumber());
			Assertions.assertEquals(ReservationState.Booked, reservation.getState());
			Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
			Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
			Assertions.assertEquals(request.getPropertyId(), reservation.getPropertyId());
			Assertions.assertEquals(request.getRoomTypeId(), reservation.getRoomTypeId());
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Caused " + e.getCause());
			Assertions.fail("modifyReservationTestSuccessAdditionalCharge has Failed");
        }
    }

    private List<RoomPrice> createGenericBookings(Date checkInDate, Date checkOutDate, String programId, double price) {
        List<RoomPrice> roomPriceList = new ArrayList<>();
        Calendar cal = new GregorianCalendar();
        cal.setTime(checkInDate);

        while (cal.getTime().before(checkOutDate)) {
            RoomPrice roomPrice = new RoomPrice();
            roomPrice.setPrice(price);
            roomPrice.setDate(cal.getTime());
            roomPrice.setProgramId(programId);

            roomPriceList.add(roomPrice);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return roomPriceList;
    }

    // price is not available
    // pending failed
    @Test
    public void modifyReservationPendingFailed() {
        try {
        	setMockAuthToken();
        	setMockReferenceDataDAOHelper();
            // retrive success
			setMockForRetrieveSuccess();
            // pending failed
            setMockForModifyPendingFailed();
            // payment success
            TokenResponse tknRes = new TokenResponse();
            tknRes.setAccessToken("1234");
            Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
           
            // pending failed
            when(paymentDao
            .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean())).thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL));
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationResponse().getBody();
            Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            RoomReservation request = mockReqForUpdateRoomReservation(false);
            Date checkInDate = fmt.parse("2021-02-23");
            Date checkOutDate = fmt.parse("2021-02-24");
            request.setBookings(createGenericBookings(checkInDate, checkOutDate, "CATST", 100.00));

            assertThatThrownBy(() -> modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_SUCCESSFUL));
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Caused " + e.getCause());
			Assertions.fail("modifyReservationPendingFailed Failed");
        }
    }

    // reservation is not available

    @Test
    public void invalidReservation() {

        try {
            setMockAuthToken();
            setMockReferenceDataDAOHelper();
            // retrieve failed;
            when(paymentDao.sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

            // payment success
            TokenResponse tknRes = new TokenResponse();
            tknRes.setAccessToken("1234");
            Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
            RoomReservation request = mockReqForUpdateRoomReservation(false);
            assertThatThrownBy(() -> modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(request))
                    .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
        } catch (Exception e) {
            log.error("Exception " + e.getMessage());
            log.error("Caused " + e.getCause());
			Assertions.fail("invalidReservation Failed");
        }
    }

    // payment failed
    @Test
    public void modifyReservationTestPaymentFailed() {

        try {
        	setMockAuthToken();
        	setMockReferenceDataDAOHelper();
			setMockForRetrieveSuccess();
            // pending success
            setMockForModifyPendingSuccess();
            // payment success
            TokenResponse tknRes = new TokenResponse();
            tknRes.setAccessToken("1234");
            AuthResponse authRes = new AuthResponse();
            authRes.setStatusMessage("NOTARRROVED");
            Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
            Mockito.doReturn(authRes).when(paymentDao).authorizePayment(ArgumentMatchers.any());
            ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
            .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationResponse().getBody();
            Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            RoomReservation request = mockReqForUpdateRoomReservation(false);
            Date checkInDate = fmt.parse("2021-03-06");
            Date checkOutDate = fmt.parse("2021-03-07");
            request.setCheckInDate(checkInDate);
            request.setCheckOutDate(checkOutDate);
            request.setBookings(createGenericBookings(checkInDate, checkOutDate, "CATST", 100.00));

            assertThatThrownBy(() -> modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(getErrorMessage(ErrorCode.PAYMENT_AUTHORIZATION_FAILED));
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Caused " + e.getCause());
			Assertions.fail("modifyReservationTestPaymentFailed Failed");
        }
    }

    @Test
    public void modifyPreviewV2ChangeDatesTest() throws ParseException {
    	setMockAuthToken();
    	setMockReferenceDataDAOHelper();
		setMockForRetrieveSuccess();
        setMockForModifyPendingSuccess();

        // Create request
        TripDetailsRequest newTripDetails = new TripDetailsRequest();
        Date newCheckInDate = fmt.parse("2020-11-29");
        Date newCheckOutDate = fmt.parse("2020-12-08");
        newTripDetails.setCheckInDate(newCheckInDate);
        newTripDetails.setCheckOutDate(newCheckOutDate);

        PreModifyV2Request request = new PreModifyV2Request();
        request.setTripDetails(newTripDetails);
        request.setConfirmationNumber("1271274619");
        request.setSource("ICECC");
        // payment success
        TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
        ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
        Mockito.doReturn(reservationPendingRes).when(paymentDao)
        .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());
		ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationResponse().getBody();
        Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        RoomReservation fetchedReservation = RoomReservationTransformer.transform(reservationRetrieveRes, acrsProperties);
        referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);
        request.setFindResvResponse(fetchedReservation);

        // Do test
        RoomReservation roomReservation = null;

        try {
            roomReservation = modifyReservationDAOStrategyACRSImpl.preModifyReservation(request);
        } catch (Exception e){
            log.error("Unexpected Exception occurred during modifyPreviewV2Test. ", e);
            Assertions.fail("modifyPreviewV2Test has failed due to above unexpected error.");
        }

        // Assertions on response
        Assertions.assertNotNull(roomReservation);
		Assertions.assertTrue(DateUtils.isSameDay(newCheckInDate, roomReservation.getCheckInDate()));
		Assertions.assertTrue(DateUtils.isSameDay(newCheckOutDate, roomReservation.getCheckOutDate()));
	}

	@Test
	public void createAcrsProductUseModificationChangesExistingOverrideExtendCheckOutTest() throws ParseException {
		// Arrange
		setMockAuthToken();
		setMockReferenceDataDAOHelper();
		TokenResponse tknRes = new TokenResponse();
		tknRes.setAccessToken("1234");
		Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();

		// Setting up mock retrieve response
		ResponseEntity<ReservationRetrieveResReservation> mockRetrieveResponse = retrieveReservationResponse();

		ProductUseResItem productUseRes = mockRetrieveResponse.getBody().getData().getHotelReservation().getSegments().get(0)
				.getOffer().getProductUses().get(0);
		ProductUseRates productRates = productUseRes.getProductRates();
		List<RateRes> requestedRates = productRates.getRequestedRates();
		List<RateChange> dailyRates = productRates.getDailyRates();

		LocalDate maxDate = productUseRes.getPeriod().getEnd();
		String overridePrice = "13.00";
		Optional<RateRes> requestedRateToModify = requestedRates.stream()
				.filter(rateRes -> maxDate.isEqual(rateRes.getEnd()))
				.findFirst();

		if (requestedRateToModify.isPresent()) {
			RateRes rateRes = requestedRateToModify.get();
			BaseRes baseRes = rateRes.getBase();
			OriginalBaseRate originalBaseRate = new OriginalBaseRate();
			originalBaseRate.setBsAmt(baseRes.getAmount());
			originalBaseRate.setStart(rateRes.getStart());
			originalBaseRate.setEnd(rateRes.getEnd());

			//Do the override
			baseRes.setAmount(overridePrice);
			baseRes.setOverrideInd(true);
			baseRes.setOriginalBaseRates(Collections.singletonList(originalBaseRate));
		}

		Optional<RateChange> dailyRateToModify = dailyRates.stream()
				.filter(rateChange -> maxDate.isEqual(rateChange.getEnd()))
				.findFirst();

		if (dailyRateToModify.isPresent()) {
			RateChange rateChange = dailyRateToModify.get();
			rateChange.getDailyTotalRate().setBsAmt(overridePrice);
		}

		ReservationRetrieveResReservation reservationRetrieveRes = mockRetrieveResponse.getBody();
		SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(reservationRetrieveRes.getData().getHotelReservation().getSegments());
		RoomReservation fetchedReservation = RoomReservationTransformer.transform(reservationRetrieveRes, acrsProperties);
		referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);


		// Create preModifyCharges request
		ModificationChangesRequest request = modifyReservationDAOStrategyACRSImpl.populatePreModifyRequest(fetchedReservation);
		// add 1 day to check out date
		TripDetail newTripDetails = createTripDetail("2020-11-29", "2020-12-09", 1, 1);
		request.setTripDetails(newTripDetails);
		// remove override price
		request.getBookings()
				.forEach(roomPrice -> roomPrice.setOverridePrice(-1.0));
		//Add new booking object for 2020-12-08
		RoomPrice newbookingObject = new RoomPrice();
		newbookingObject.setDate(fmt.parse("2020-12-08"));
		newbookingObject.setPrice(89.91);
		newbookingObject.setProgramId("TDAAA");
		newbookingObject.setResortFee(37.0);
		request.getBookings().add(newbookingObject);

		// Do test
		ModificationChanges modificationChanges = null;

		try {
			modificationChanges = modifyReservationDAOStrategyACRSImpl.createAcrsProductUseModificationChanges(request, fetchedReservation, mainSegment);
		} catch (Exception e) {
			log.error("Unexpected Exception occurred during modifyChargesV2ExistingOverrideChangeDatesTest. ", e);
			Assertions.fail("modifyChargesV2ExistingOverrideChangeDatesTest has failed due to above unexpected error.");
		}

		// Assertions on response
		String modifyRequestedRatesPathString = String.format(acrsProperties.getModifyRequestedRatesPath(),
				1);
		String modifyDateEndPathString = String.format(acrsProperties.getModifyDateEndPath(), 1);
		Assertions.assertNotNull(modificationChanges);
		Assertions.assertEquals(2, modificationChanges.size());
		// validate order as it matters with extension and override change that extension happens first.
		Assertions.assertEquals(modifyDateEndPathString, modificationChanges.get(0).getPath());
		Assertions.assertEquals(modifyRequestedRatesPathString, modificationChanges.get(1).getPath());
		Optional<ModificationChangesItem> removeRequestedProductRatesItemOptional = modificationChanges.stream()
				.filter(item -> modifyRequestedRatesPathString.equalsIgnoreCase(item.getPath()))
				.findFirst();
		Assertions.assertTrue(removeRequestedProductRatesItemOptional.isPresent());
		ModificationChangesItem removeRequestedProductRatesItem = removeRequestedProductRatesItemOptional.get();
		Assertions.assertEquals(ModificationChangesItem.OpEnum.UPSERT, removeRequestedProductRatesItem.getOp());
		Assertions.assertTrue(removeRequestedProductRatesItem.getValue() instanceof ArrayList<?>);
		List<?> rateReqsList = (ArrayList<?>) (removeRequestedProductRatesItem.getValue());
		Assertions.assertTrue(rateReqsList.get(0) instanceof RateReq);
		RateReq removeOverrideRateReq = (RateReq) rateReqsList.get(0);
		Assertions.assertEquals("89.91", removeOverrideRateReq.getBase().getAmount());
		Assertions.assertEquals(Boolean.FALSE, removeOverrideRateReq.getBase().getOverrideInd());


		Optional<ModificationChangesItem> extendCheckOutDateItemOptional = modificationChanges.stream()
				.filter(item -> modifyDateEndPathString.equalsIgnoreCase(item.getPath()))
				.findFirst();
		Assertions.assertTrue(extendCheckOutDateItemOptional.isPresent());
		ModificationChangesItem extendCheckOutDateItem = extendCheckOutDateItemOptional.get();
		Assertions.assertEquals(ModificationChangesItem.OpEnum.UPSERT, extendCheckOutDateItem.getOp());
		Assertions.assertTrue(extendCheckOutDateItem.getValue() instanceof LocalDate);
		Assertions.assertTrue(LocalDate.of(2020, 12, 9).isEqual((LocalDate) extendCheckOutDateItem.getValue()));
	}

	@Test
	public void createAcrsProductUseModificationChangesOverrideExistingHasMultipleNonMainProductUses()
			throws ParseException {
		// Arrange baseline mocks needed
		setMockAuthToken();
		setMockReferenceDataDAOHelper();
		TokenResponse tknRes = new TokenResponse();
		tknRes.setAccessToken("1234");
		Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();

		// Arrange mock retrieve response
		ResponseEntity<ReservationRetrieveResReservation> mockRetrieveResponse =
				retrieveReservationResponseWithMultipleNonMainProductUses();

		ReservationRetrieveResReservation reservationRetrieveRes = mockRetrieveResponse.getBody();
		SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(
				reservationRetrieveRes.getData().getHotelReservation().getSegments());
		RoomReservation fetchedReservation = RoomReservationTransformer.transform(reservationRetrieveRes, acrsProperties);
		referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);

		// Arrange  preModifyCharges request
		ModificationChangesRequest request = modifyReservationDAOStrategyACRSImpl.populatePreModifyRequest(
				fetchedReservation);
		// fetched reservation trip dates is 2024-12-17 to 2024-12-20
		TripDetail newTripDetails = createTripDetail("2024-12-17", "2024-12-20", 2, 0);
		request.setTripDetails(newTripDetails);

		// We want to override 2024-12-17 and 2024-12-18 to $30.00
		Date overrideNight1 = fmt.parse("2024-12-17");
		Date overrideNight2 = fmt.parse("2024-12-18");
		request.getBookings().stream()
				.filter(roomPrice -> ReservationUtil.areDatesEqualExcludingTime(roomPrice.getDate(), overrideNight1) ||
						ReservationUtil.areDatesEqualExcludingTime(roomPrice.getDate(), overrideNight2))
				.forEach(roomPrice -> roomPrice.setOverridePrice(30.00));

		// Act on method being tested
		ModificationChanges modificationChanges = null;
		try {
			modificationChanges = modifyReservationDAOStrategyACRSImpl.createAcrsProductUseModificationChanges(request,
					fetchedReservation, mainSegment);
		} catch (Exception e) {
			log.error("Unexpected Exception occurred during " +
					"createAcrsProductUseModificationChangesOverrideExistingHasMultipleNonMainProductUses. ", e);
			Assertions.fail("createAcrsProductUseModificationChangesOverrideExistingHasMultipleNonMainProductUses has " +
					"failed due to above unexpected error.");
		}

		// Assert on response
		String modifyRequestedRatesPathString = String.format(acrsProperties.getModifyRequestedRatesPath(),
				1);
		Assertions.assertNotNull(modificationChanges);
		Assertions.assertEquals(1, modificationChanges.size());
		Optional<ModificationChangesItem> removeRequestedProductRatesItemOptional = modificationChanges.stream()
				.filter(item -> modifyRequestedRatesPathString.equalsIgnoreCase(item.getPath()))
				.findFirst();
		Assertions.assertTrue(removeRequestedProductRatesItemOptional.isPresent());
		ModificationChangesItem removeRequestedProductRatesItem = removeRequestedProductRatesItemOptional.get();
		Assertions.assertEquals(ModificationChangesItem.OpEnum.UPSERT, removeRequestedProductRatesItem.getOp());
		Assertions.assertTrue(removeRequestedProductRatesItem.getValue() instanceof ArrayList<?>);
		List<?> rateReqsList = (ArrayList<?>) (removeRequestedProductRatesItem.getValue());
		Assertions.assertEquals(2, rateReqsList.size());
		Assertions.assertTrue(rateReqsList.get(0) instanceof RateReq);
		RateReq firstRateRequest = (RateReq) rateReqsList.get(0);
		Assertions.assertEquals("30.0", firstRateRequest.getBase().getAmount());
		Assertions.assertEquals(Boolean.TRUE, firstRateRequest.getBase().getOverrideInd());
		Assertions.assertTrue(LocalDate.of(2024, 12, 17).isEqual(firstRateRequest.getStart()));

		Assertions.assertTrue(rateReqsList.get(1) instanceof RateReq);
		RateReq secondRateRequest = (RateReq) rateReqsList.get(1);
		Assertions.assertEquals("30.0", secondRateRequest.getBase().getAmount());
		Assertions.assertEquals(Boolean.TRUE, secondRateRequest.getBase().getOverrideInd());
		Assertions.assertTrue(LocalDate.of(2024, 12, 18).isEqual(secondRateRequest.getStart()));
	}

	private TripDetail createTripDetail(String checkInDate, String checkOutDate, int numAdults, int numChildren)
			throws ParseException {
		TripDetail newTripDetails = new TripDetail();
		Date newCheckInDate = fmt.parse(checkInDate);
		Date newCheckOutDate = fmt.parse(checkOutDate);
		newTripDetails.setCheckInDate(newCheckInDate);
		newTripDetails.setCheckOutDate(newCheckOutDate);
		newTripDetails.setNumAdults(numAdults);
		newTripDetails.setNumChildren(numChildren);
		return newTripDetails;
	}

	@Test
	public void testValidateMissingWebSuppressComponents_When_FindResvResponse_isNonNull() {
		try {
			List<String> specialRequests = Arrays.asList("Request1", "Request2");

			PreModifyV2Request request = new PreModifyV2Request();
			TokenResponse tknRes = new TokenResponse();
			tknRes.setAccessToken("1234");
			Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
			ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment()
					.getBody();
			Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRequestToPaymentExchangeToken(
					ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
					ArgumentMatchers.anyBoolean());
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationExcludeComponentResponse()
					.getBody();
			Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(
					ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class),
					ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
			RoomReservation fetchedReservation = RoomReservationTransformer.transform(reservationRetrieveRes,
					acrsProperties);
			referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);
			request.setFindResvResponse(fetchedReservation);
			request.setRoomRequests(specialRequests);
			assertThatThrownBy(() -> modifyReservationDAOStrategyACRSImpl.preModifyReservation(request))
					.isInstanceOf(BusinessException.class)
					.hasMessage(getErrorMessage(ErrorCode.NON_EDITABLE_COMPONENTS_MISSING));
		} catch (Exception e) {
			log.error(e.getMessage());
			log.error("Caused " + e.getCause());
			Assertions.fail("ValidateTestMissingWebSuppressComponents Failed");
		}
    }
    @Test
    public void testUpdateCreditCardTokens() {
    	setMockAuthToken();
    	setMockReferenceDataDAOHelper();
    	setMockForModifyIgnorePendingSuccess();
        List<String> specialRequests = Arrays.asList("COMPONENTCD-v-ICEFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-ICEFEE");

			PreModifyV2Request request = new PreModifyV2Request();
			TokenResponse tknRes = new TokenResponse();
			tknRes.setAccessToken("1234");
			Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
			ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment()
					.getBody();
			Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRequestToPaymentExchangeToken(
					ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
					ArgumentMatchers.anyBoolean());
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationExcludeComponentResponse()
					.getBody();
			Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(
					ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class),
					ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
			RoomReservation fetchedReservation = RoomReservationTransformer.transform(reservationRetrieveRes,
					acrsProperties);
			referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);
			request.setFindResvResponse(fetchedReservation);
			request.setRoomRequests(specialRequests);
      request.setSource("ICECC");
			// Do test
	        RoomReservation roomReservation = null;
		try {
            roomReservation = modifyReservationDAOStrategyACRSImpl.preModifyReservation(request);
        } catch (Exception e){
            log.error("Unexpected Exception occurred during modifyPreviewV2Test. ", e);
            Assertions.fail("modifyPreviewV2Test has failed due to above unexpected error.");
        }
		 // Assertions on response
        Assertions.assertNotNull(roomReservation);
    }

    @Test
    public void modifyReservationTestSuccessRefund_skipPaymentProcess() {
        try {
        	setMockAuthToken();
        	setMockReferenceDataDAOHelper();
			setMockForRetrieveSuccess();
            setMockForModifyPendingSuccess();
            TokenResponse tknRes = new TokenResponse();
            tknRes.setAccessToken("1234");
            AuthResponse authRes = new AuthResponse();
            authRes.setStatusMessage(ServiceConstant.APPROVED);
            authRes.setAuthRequestId("1531653");
            RefundResponse refundRes = new RefundResponse();
            refundRes.setStatusMessage(ServiceConstant.APPROVED);
            refundRes.setAmount("100");
            CaptureResponse capRes = new CaptureResponse();
            capRes.setStatusMessage(ServiceConstant.APPROVED);
            Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();            
            Mockito.doReturn(refundRes).when(paymentDao).refundPayment(ArgumentMatchers.any());
            ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
            .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrieveReservationResponse().getBody();
            Mockito.doReturn(reservationRetrieveRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.anyString(), ArgumentMatchers.any(DestinationHeader.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
 
            setMockForPendingCommitSuccess();
            RoomReservation request = mockReqForUpdateRoomReservation(true);
            Date checkInDate = fmt.parse("2020-11-29");
            Date checkOutDate = fmt.parse("2020-12-08");
            request.setCheckInDate(checkInDate);
            request.setCheckOutDate(checkOutDate);
            request.setBookings(createGenericBookings(checkInDate, checkOutDate, "CATST", 100.00));
            // skip payment and skip fraud check
            request.setSkipFraudCheck(true);
            request.setSkipPaymentProcess(true);
            
            RoomReservation reservation = modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(request);
			Assertions.assertNotNull(reservation);
			Assertions.assertNotNull(reservation.getConfirmationNumber());
			Assertions.assertEquals(request.getConfirmationNumber(), reservation.getConfirmationNumber());
			Assertions.assertEquals(ReservationState.Booked, reservation.getState());
			Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
			Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
			Assertions.assertEquals(request.getPropertyId(), reservation.getPropertyId());
			Assertions.assertEquals(request.getRoomTypeId(), reservation.getRoomTypeId());
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Caused " + e.getCause());
			Assertions.fail("ModifyRoomReservationTestSuccessRefund_skipPaymentProcess has Failed");
        }
    }
    
    @Test
	void testHasError() throws IOException {
    	ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
        Assertions.assertTrue(result);
	}
    
    @Test
    void testHandleErrorSystemExceptionSYSTEM_ERROR() throws IOException {
    	ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertions
		SystemException ex = Assertions.assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
        Assertions.assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionElse() throws IOException {
    	ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("Some Error Occured");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);

		// Assertions
		BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
        Assertions.assertSame(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionDATES_UNAVAILABLE() throws IOException {
    	ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("<UnableToPriceTrip>");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
        Assertions.assertSame(ErrorCode.DATES_UNAVAILABLE, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionRESERVATION_NOT_FOUND() throws IOException {
    	ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("<BookingNotFound>");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
        Assertions.assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionUNMODIFIABLE_RESV_STATUS() throws IOException {
    	ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new ModifyReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("Reservation is already cancelled");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
		
		// Assertions
		BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
        Assertions.assertSame(ErrorCode.UNMODIFIABLE_RESV_STATUS, ex.getErrorCode());
	}

    @Test
    void commitPaymentReservationTest_ModifyDate_WithNoDepositSuccess() {
        try {
            // set initial mocks
            setMockAuthToken();
            setMockReferenceDataDAOHelper();
            setMockForNoDepositSearchSuccess();
            setMockForNoDepositPendingRetriveSuccess();
            setMockForNoDepositPendingCommitSuccess();
            setMockForNoDepositModifyPendingSuccess();

            ReservationModifyPendingRes reservationPendingRes = (ReservationModifyPendingRes) makePendingRoomReservationResponse_NoDeposit().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
                    .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());

            // create commit request
            PaymentRoomReservationRequest commitRequest =
                    convertRBSReq("/paymentwidgetv4/commit/no_deposit/commit-v4-nodeposit-request.json", PaymentRoomReservationRequest.class);

            RoomReservationRequestMapper requestMapper = new RoomReservationRequestMapperImpl();
            CommitPaymentDTO commitResvRequest = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(commitRequest);

            // make call
            RoomReservation reservation =
                    modifyReservationDAOStrategyACRSImpl.commitPaymentReservation(commitResvRequest);

            // Assertions
            Assertions.assertNotNull(reservation);
            Assertions.assertNotNull(reservation.getConfirmationNumber());
            Assertions.assertNotNull(reservation.getProfile());
            Assertions.assertEquals(commitResvRequest.getFirstName(), reservation.getProfile().getFirstName());
            Assertions.assertEquals(commitResvRequest.getLastName(), reservation.getProfile().getLastName());
            Assertions.assertEquals(ReservationState.Booked, reservation.getState());
            // extended by 2 days
            Assertions.assertNotNull(reservation.getBookings());
            Assertions.assertEquals(4, reservation.getBookings().size());
            // 0 amount in billing for No Deposit
            Assertions.assertNotNull(reservation.getCreditCardCharges());
            Assertions.assertEquals(0, reservation.getCreditCardCharges().get(0).getAmount());
            // payments[] should not be empty
            Assertions.assertNotNull(reservation.getPayments());
            Assertions.assertNotEquals(0, reservation.getPayments().size());
        }
        catch (Exception e) {
            log.error(e.getMessage());
			Assertions.fail("commitPaymentReservationTest_ModifyDateWithNoDepositSuccess has Failed");
        }
    }

    @Test
    void commitPaymentReservationTest_AddSplReq_WithAdditionalDepositSuccess() {
        try {
            // set initial mocks
            setMockAuthToken();
            setMockReferenceDataDAOHelper();
            setMockForAddnDepositSearchSuccess();
            setMockForAddnDepositPendingRetriveSuccess();
            setMockForAddnDepositPendingCommitSuccess();
            setMockForAddnDepositModifyPendingSuccess();

            ReservationModifyPendingRes reservationPendingRes = (ReservationModifyPendingRes) makePendingRoomReservationResponse_AddnDeposit().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
                    .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());

            // create commit request
            PaymentRoomReservationRequest commitRequest =
                    convertRBSReq("/paymentwidgetv4/commit/addn_deposit/commit-v4-addndeposit-request.json", PaymentRoomReservationRequest.class);

            RoomReservationRequestMapper requestMapper = new RoomReservationRequestMapperImpl();
            CommitPaymentDTO commitResvRequest = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(commitRequest);

            // make call
            RoomReservation reservation =
                    modifyReservationDAOStrategyACRSImpl.commitPaymentReservation(commitResvRequest);

            // Assertions
            Assertions.assertNotNull(reservation);
            Assertions.assertNotNull(reservation.getConfirmationNumber());
            Assertions.assertNotNull(reservation.getProfile());
            Assertions.assertEquals(commitResvRequest.getFirstName(), reservation.getProfile().getFirstName());
            Assertions.assertEquals(commitResvRequest.getLastName(), reservation.getProfile().getLastName());
            Assertions.assertEquals(ReservationState.Booked, reservation.getState());
            // added component check
            Assertions.assertNotNull(reservation.getSpecialRequests());
            Assertions.assertEquals(1, reservation.getSpecialRequests().size());
            Assertions.assertTrue(reservation.getSpecialRequests().get(0).contains("DOGFEE"));
            Assertions.assertEquals(1, reservation.getPurchasedComponents().size());
            // additional payment in billing
            Assertions.assertNotNull(reservation.getCreditCardCharges());
            Assertions.assertEquals(56.69, reservation.getCreditCardCharges().get(0).getAmount());
            // payments[] should not be empty
            Assertions.assertNotNull(reservation.getPayments());
            Assertions.assertNotEquals(0, reservation.getPayments().size());
        }
        catch (Exception e) {
            log.error(e.getMessage());
			Assertions.fail("commitPaymentReservationTest_AddSplReq_WithAdditionalDepositSuccess has Failed");
        }
    }

    @Test
    void commitPaymentReservationTest_RemoveSplReq_WithRefundDepositSuccess() {
        try {
            // set initial mocks
            setMockAuthToken();
            setMockReferenceDataDAOHelper();
            setMockForRefundDepositSearchSuccess();
            setMockForRefundDepositPendingRetriveSuccess();
            setMockForRefundDepositPendingCommitSuccess();
            setMockForRefundDepositModifyPendingSuccess();

            ReservationModifyPendingRes reservationPendingRes = (ReservationModifyPendingRes) makePendingRoomReservationResponse_RefundDeposit().getBody();
            Mockito.doReturn(reservationPendingRes).when(paymentDao)
                    .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());

            // create commit request
            PaymentRoomReservationRequest commitRequest =
                    convertRBSReq("/paymentwidgetv4/commit/refund_deposit/commit-v4-refunddeposit-request.json", PaymentRoomReservationRequest.class);

            RoomReservationRequestMapper requestMapper = new RoomReservationRequestMapperImpl();
            CommitPaymentDTO commitResvRequest = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(commitRequest);

            // make call
            RoomReservation reservation =
                    modifyReservationDAOStrategyACRSImpl.commitPaymentReservation(commitResvRequest);

            // Assertions
            Assertions.assertNotNull(reservation);
            Assertions.assertNotNull(reservation.getConfirmationNumber());
            Assertions.assertNotNull(reservation.getProfile());
            Assertions.assertEquals(commitResvRequest.getFirstName(), reservation.getProfile().getFirstName());
            Assertions.assertEquals(commitResvRequest.getLastName(), reservation.getProfile().getLastName());
            Assertions.assertEquals(ReservationState.Booked, reservation.getState());
            // removed component check
            Assertions.assertNotNull(reservation.getSpecialRequests());
            Assertions.assertEquals(0, reservation.getSpecialRequests().size());
            Assertions.assertEquals(0, reservation.getPurchasedComponents().size());
            // refund payment in billing
            Assertions.assertNotNull(reservation.getCreditCardCharges());
            Assertions.assertEquals(-56.69, reservation.getCreditCardCharges().get(0).getAmount());
            // payments[] should not be empty
            Assertions.assertNotNull(reservation.getPayments());
            Assertions.assertNotEquals(0, reservation.getPayments().size());
        }
        catch (Exception e) {
            log.error(e.getMessage());
			Assertions.fail("commitPaymentReservationTest_RemoveSplReq_WithRefundDepositSuccess has Failed");
        }
    }

    @Test
    void modifyPendingRoomReservationV2Test_ModifySingleResvToSharedSuccess() {
        try {
            // set initial mocks
            setMockAuthToken();
            setMockReferenceDataDAOHelper();
            setMockForSingleToSharedPendingRetriveSuccess();
            setMockForSingleToSharedPendingReservationSuccess();
            setMockForSingleToSharedModifyPendingSuccess();
            setMockForSingleToSharedPendingCommitSuccess();
            setMockForSingleToSharedLinkReservationResponseSucces();

            ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) retrivePendingReservationResponse_SingleToShared().getBody();
            Mockito.doReturn(reservationRetrieveRes).when(paymentDao)
                    .sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any());

            ReservationModifyPendingRes reservationModifyPendingRes = (ReservationModifyPendingRes) makePendingRoomReservationResponse_SingleToShared().getBody();
            // return null on first time call
            Mockito.doReturn(null, reservationModifyPendingRes).when(paymentDao)
                    .sendRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.anyBoolean());

            // create modify request
            ModifyRoomReservationRequest modifyRoomReservationRequest =
                    convert("/paymentwidgetv4/modifyPending/singleToShared/rbs-modify-pending-request-singleToShared.json", ModifyRoomReservationRequest.class);
            RoomReservationRequestMapper requestMapper = Mockito.spy(new RoomReservationRequestMapperImpl());
            Mockito.doNothing().when(requestMapper).updateRoomReservation(Mockito.any(), Mockito.any());
            RoomReservation roomReservationRequest =
                    requestMapper.roomReservationRequestToModel(modifyRoomReservationRequest.getRoomReservation());

            // make call
            RoomReservation reservation =
                    modifyReservationDAOStrategyACRSImpl.modifyPendingRoomReservationV2(roomReservationRequest);

            // Assertions
            Assertions.assertNotNull(reservation);
            Assertions.assertNotNull(reservation.getConfirmationNumber());
            Assertions.assertNotNull(reservation.getProfile());
            Assertions.assertEquals(ReservationState.Booked, reservation.getState());
            // payments[] not empty check
            Assertions.assertNotNull(reservation.getPayments());
            Assertions.assertNotEquals(0, reservation.getPayments().size());
            // shareId generated check
            Assertions.assertNotNull(reservation.getShareId());
            // shareWith cnf no.
            Assertions.assertEquals(1, reservation.getShareWiths().length);
            // shareWith (secondary) customer profile
            Assertions.assertEquals(1, reservation.getShareWithCustomers().size());
        }
        catch (Exception e) {
            log.error(e.getMessage());
            Assertions.fail("modifyPendingRoomReservationV2Test_ModifySingleResvToSharedSuccess has Failed");
        }
    }

    @Test
    void testRefundModificationChangesWithExpiredCard() {
        // Arrange Acrs properties used for test
        acrsProperties.setModifyForcedSellPath("ModifyForcedSellPath");
        acrsProperties.setModifyDepositPaymentsPath("ModifyDepositPaymentsPath");
        // Arrange mock reservation as null for this test case
        RoomReservation reservation = null;
        // Arrange mock creditCardAuthorizationMap
        List<CreditCardCharge> creditCardCharges = createMockRefundCreditCardChargesWithExpiredCard();
        Map<CreditCardCharge, String> creditCardAuthorizationMap = creditCardCharges.stream()
                .collect(Collectors.toMap(Function.identity(), auth -> "MockAuthId"));
        try {
            // Act
            List<ModificationChangesItem> modificationChangesItemList =
                    modifyReservationDAOStrategyACRSImpl.getModificationChangesForPaymentTxns(reservation,
                            creditCardAuthorizationMap);
            // Assert response values
            Assertions.assertTrue(CollectionUtils.isNotEmpty(modificationChangesItemList));

            Optional<ModificationChangesItem> paymentModificationChangeOptional = modificationChangesItemList.stream()
                    .filter(item -> "ModifyDepositPaymentsPath".equalsIgnoreCase(item.getPath()))
                    .findFirst();
            Assertions.assertTrue(paymentModificationChangeOptional.isPresent());
            Assertions.assertTrue(paymentModificationChangeOptional.get().getValue() instanceof PaymentTransactionReq);

            PaymentTransactionReq paymentTransactionReq = (PaymentTransactionReq) paymentModificationChangeOptional.get()
                    .getValue();
            Assertions.assertEquals(PaymentStatus.PAYMENT_RECEIVED, paymentTransactionReq.getPaymentStatus());
            Assertions.assertEquals(PaymentIntent.REFUND, paymentTransactionReq.getPaymentIntent());
            Assertions.assertEquals("100.0", paymentTransactionReq.getAmount());
            Assertions.assertEquals("1223", paymentTransactionReq.getPaymentCard().getExpireDate());
        } catch (Exception e) {
            Assertions.fail("testRefundModificationChangesWithExpiredCard failed with unexpected exception: ", e);
        }
    }
}