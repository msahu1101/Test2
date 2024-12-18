package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapperImpl;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.RoomRequest;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.reservation.RoomMarket;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.BaseAcrsTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.RequestSourceConfig;
import com.mgm.services.booking.room.util.RequestSourceConfig.SourceDetails;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Log4j2
public class BaseAcrsDAO {

    protected URLProperties urlProperties;
    protected DomainProperties domainProperties;
    protected RestTemplate client;
    protected AcrsProperties acrsProperties;
    protected ApplicationProperties applicationProperties;
    protected ReferenceDataDAOHelper referenceDataDAOHelper;
    protected ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;

	 /**
     * Public cons
     * @param urlProperties
     * @param domainProperties
     * @param restTemplate
     */
    protected BaseAcrsDAO(URLProperties urlProperties, DomainProperties domainProperties, ApplicationProperties applicationProperties, AcrsProperties acrsProperties, 
                          RestTemplate restTemplate, ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl){
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.acrsProperties = acrsProperties;
        this.applicationProperties = applicationProperties;
        this.client = restTemplate;
        this.referenceDataDAOHelper = referenceDataDAOHelper;
        this.acrsOAuthTokenDAOImpl = acrsOAuthTokenDAOImpl;
        this.client.setMessageConverters(Collections.singletonList(ReservationUtil.createHttpMessageConverter()));
    }

    /**
     * Commit pending Room Reservation Request with ACRS
     *
     * @param confirmationNumber
     *          Confirmation number of Room Reservation to Commit
     * @returns true if successfully committed
     * @throws
     */
    protected RoomReservation commitPendingRoomReservation(String confirmationNumber, String propertyId, String source, boolean isPoFlow) throws ParseException {
        // TODO create appropriate headers
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

    	final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
    	final HttpHeaders headers = CommonUtil.createCrsHeadersNoVersion(referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId), acrsVendor);
    	headers.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(), acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(),isPoFlow);
        uriParams.put("confirmationNumber", confirmationNumber);


            log.info("Sending request to ACRS Commit Pending RoomReservation.");
            log.info("Sending request to Commit Pending, Request headers {}:", CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
            log.info("Sending request to Commit Pending, Request body {}: ", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
            log.info("Sending request to Commit Pending, Request query parameters: "+uriParams);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsReservationsConfCommit",
                urlProperties.getAcrsReservationsConfCommit(), start);

        ResponseEntity<ReservationRes> response = client.exchange(domainProperties.getCrs().concat(urlProperties.getAcrsReservationsConfCommit()), HttpMethod.PUT, httpEntity, ReservationRes.class, uriParams);

        setThreadContextAfterAPICall(start, String.valueOf(response.getStatusCodeValue()));

        ReservationRes commitedReservationRes = logAndReturnCrsResponseBody(response, "Commit pending room " +
                "reservation", applicationProperties.isPermanentInfoLogEnabled());
        // merge subTotalServiceCharges into taxList
        SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(commitedReservationRes.getData().getHotelReservation().getSegments());
        RoomReservationTransformer.mergeSubTotalServCrgIntoTaxList(mainSegment);
        RoomReservation roomReservation = RoomReservationTransformer.transform(commitedReservationRes, acrsProperties);
        referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
        return roomReservation;
    }
    protected ReservationRetrieveResReservation retrieveReservationByCnfNumberDirectly(String confirmationNumber,
                                                                               String source) {
        //Fetch reservation directly from ACRS
            final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
            final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
            final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(null, acrsVendor);
            httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

            HttpEntity<?> request = new HttpEntity<>(httpHeaders);
            Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                    acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(), false);
            uriParam.put(ServiceConstant.CRS_RESERVATION_CFNUMBER, confirmationNumber);

            log.info("Sending request to Retrieve directly, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
            log.info("Sending request to Retrieve directly, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
            log.info("Sending request to Retrieve directly, Request query parameters: "+uriParam);


            LocalDateTime start = LocalDateTime.now();
            setThreadContextBeforeAPICall("AcrsRetrieveReservation",
                    urlProperties.getAcrsRetrieveReservation(), start);

            ResponseEntity<ReservationRetrieveResReservation> crsResponse = client.exchange(
                    domainProperties.getCrs() + urlProperties.getAcrsRetrieveReservation(), HttpMethod.GET, request,
                    ReservationRetrieveResReservation.class, uriParam);

            setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

            return logAndReturnCrsResponseBody(crsResponse, "Retrieve Reservation", applicationProperties.isPermanentInfoLogEnabled());

    }
    protected ReservationModifyPendingRes invokeACRSModifyPending(ReservationPartialModifyReq reservationPartialModifyReq,
                                                                String confirmationNumber,
                                                                String propertyCode,
                                                                String iATAId,
                                                                String rRUpSell,
                                                                String source,
                                                                boolean isPoFlow) {

        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders headers = CommonUtil.createCrsHeaders(referenceDataDAOHelper.retrieveAcrsPropertyID(propertyCode),
                acrsVendor, iATAId, rRUpSell);
        headers.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

        RequestSourceConfig.SourceDetails sourceDetails = referenceDataDAOHelper.getRequestSource(source);
        headers.set(ServiceConstant.HEADER_CRS_AMA_CHANNEL_IDENTIFIERS, String.format(ServiceConstant.HEADER_CRS_AMA_CHANNEL_IDENTIFIERS_VALUE,sourceDetails.getAcrsVendor(), sourceDetails.getAcrsChannelClass(), sourceDetails.getAcrsChannelType(),sourceDetails.getAcrsChannel(),sourceDetails.getAcrsSubChannel()));
        HttpEntity<ReservationPartialModifyReq> httpEntity = new HttpEntity<>(reservationPartialModifyReq, headers);
        Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(),isPoFlow);
        uriParams.put("confirmationNumber", confirmationNumber);
        log.info("Sending request to ACRS modify Pending, Request headers {}:", CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
        log.info("Sending request to ACRS modify Pending, Request body {}: ", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
        log.info("Sending request to modify ACRS Pending, Request query parameters: "+uriParams);
        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsReservationsModifyPending",
                urlProperties.getAcrsReservationsCreatePending(), start);
        ResponseEntity<ReservationModifyPendingRes> response = client.exchange(domainProperties.getCrs().concat(urlProperties.getAcrsReservationsConfPending()), HttpMethod.PATCH, httpEntity, ReservationModifyPendingRes.class, uriParams);
        setThreadContextAfterAPICall(start, String.valueOf(response.getStatusCodeValue()));
        return logAndReturnCrsResponseBody(response, "Modify Pending Room Reservation", applicationProperties.isPermanentInfoLogEnabled());

    }


    /**
     * Ignore a pending Reservation Request with ACRS; If a previous version of the reservation exists it will be reverted to that version.
     *
     * @param confirmationNumber
     *      Confirmation Number of pending reservation request to ignore
     * @return
     *      true if successfully processed by ACRS
     */
    protected boolean ignorePendingRoomReservation(String confirmationNumber, String propertyId, String source, boolean isPoFlow) {
        // TODO create appropriate headers
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
        
    	final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
    	final HttpHeaders headers = CommonUtil.createCrsHeadersNoVersion(referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId), acrsVendor);
    	headers.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());
    			
    	final HttpEntity<String> httpEntity = new HttpEntity<>(headers);
    	final Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(), acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(), isPoFlow);
        uriParams.put("confirmationNumber", confirmationNumber);


            log.info("Sending request to ACRS Ignore Pending RoomReservation.");
            log.info("Sending request to Ignore Pending, Request headers {}:", CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
            log.info("Sending request to Ignore Pending, Request body {}: ", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
            log.info("Sending request to Ignore Pending, Request query parameters: "+uriParams);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsReservationsConfPending",
                urlProperties.getAcrsReservationsConfPending(), start);

        ResponseEntity<ReservationRes> response = client.exchange(domainProperties.getCrs().concat(urlProperties.getAcrsReservationsConfPending()), HttpMethod.DELETE, httpEntity, ReservationRes.class, uriParams);

        setThreadContextAfterAPICall(start, String.valueOf(response.getStatusCodeValue()));

        Optional<ReservationRes> ignoreReservationRes = Optional.ofNullable(logAndReturnCrsResponseBody(response,
                "Ignore Pending Room Reservation.", applicationProperties.isPermanentInfoLogEnabled()));

        return ignoreReservationRes.isPresent();
    }

    /**
     * Create Pending Reservation Request with ACRS
     *
     * @param reservationReq
     *          ACRS Room Reservation Request object
     * @return confirmationNumber of pending hotel reservation request
     */
    protected RoomReservation makePendingRoomReservation(ReservationPendingReq reservationReq, String propertyId,
            AgentInfo agentInfo, String rrUpSell, String source) {
        ReservationPendingRes crsResponse = makeACRSPendingRoomReservation(reservationReq, propertyId, agentInfo,
                rrUpSell, source);
        //If ratePlan is blacklisted then throw exception
        if (StringUtils.isNotBlank(source) && !ServiceConstant.ICE.equalsIgnoreCase(source) ){
            SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(crsResponse.getData().getHotelReservation().getSegments());
            List<RoomMarket> marketCodes = BaseAcrsTransformer.createAcrsMarkets(source, mainSegment);
            if(ReservationUtil.isBlackListed(acrsProperties.getWhiteListMarketCodeList(), marketCodes)) {
                throw new BusinessException(ErrorCode.RESERVATION_BLACKLISTED);
            }
        }

        return reservationPendingResTransform(crsResponse);
    }
    
    protected RoomReservation reservationPendingResTransform(ReservationPendingRes crsResponse) {
        RoomReservationPendingResMapperImpl pendingMapping = new RoomReservationPendingResMapperImpl();
        RoomReservation reservation = RoomReservationTransformer
                .transform(pendingMapping.reservationPendingResToReservationRetrieveRes(crsResponse), acrsProperties);
        referenceDataDAOHelper.updateAcrsReferencesToGse(reservation);
        return reservation;
    }
    
    protected ReservationPendingRes makeACRSPendingRoomReservation(ReservationPendingReq reservationReq, String propertyId,
            AgentInfo agentInfo, String rrUpSell,String source) {
        // TODO create appropriate headers
        String iATAId = (null != agentInfo) ? agentInfo.getAgentId() : null;
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
        
        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders headers = CommonUtil.createCrsHeaders(referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId),
                    acrsVendor, iATAId, rrUpSell);
        headers.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());
            
        SourceDetails sourceDetails = referenceDataDAOHelper.getRequestSource(source);
        headers.set(ServiceConstant.HEADER_CRS_AMA_CHANNEL_IDENTIFIERS, String.format(ServiceConstant.HEADER_CRS_AMA_CHANNEL_IDENTIFIERS_VALUE,sourceDetails.getAcrsVendor(), sourceDetails.getAcrsChannelClass(), sourceDetails.getAcrsChannelType(),sourceDetails.getAcrsChannel(),sourceDetails.getAcrsSubChannel()));
        boolean isPoFlow = reservationReq.getData().getHotelReservation().getSegments().get(0).getOffer().getIsPerpetualOffer();
        HttpEntity<ReservationPendingReq> httpEntity = new HttpEntity<>(reservationReq, headers);
       Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(),isPoFlow);
        

            log.info("Sending request to ACRS Make Pending Reservation.");
            log.info("Sending request to Make Pending, Request headers {}:", CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
            log.info("Sending request to Make Pending, Request body {}: ", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
            log.info("Sending request to Make Pending, Request query parameters: "+uriParams);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsReservationsCreatePending",
                urlProperties.getAcrsReservationsCreatePending(), start);

        ResponseEntity<ReservationPendingRes> crsResponse = client.postForEntity(
                domainProperties.getCrs().concat(urlProperties.getAcrsReservationsCreatePending()), httpEntity,
                ReservationPendingRes.class, uriParams);

        setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

        return logAndReturnCrsResponseBody(crsResponse, "Make Pending Room Reservation", applicationProperties.isPermanentInfoLogEnabled());
    }

      
    /**
     * SearchReservations retrieves reservations based on ReservationSearchReq
     *
     * @param reservationSearchReq
     * @return ReservationSearchRes
     */
    protected ReservationSearchResPostSearchReservations searchReservationsByReservationSearchReq(ReservationSearchReq reservationSearchReq,String source) {
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
        
    	final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
    	final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(null, acrsVendor);
    	httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());
    		
    	HttpEntity<ReservationSearchReq> request = new HttpEntity<ReservationSearchReq>(reservationSearchReq, httpHeaders);
        Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getReservationsVersion(), acrsProperties.getChainCode());


            log.info("Sending request to ACRS search Reservations By Reservation Search Req. ");
            log.info("Sending request to Search Reservations, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
            log.info("Sending request to Search Reservations, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
            log.info("Sending request to Search Reservations, Request query parameters: "+uriParam);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsSearchReservations",
                urlProperties.getAcrsSearchReservations(), start);

        ResponseEntity<ReservationSearchResPostSearchReservations> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsSearchReservations(), HttpMethod.POST, request,
                ReservationSearchResPostSearchReservations.class, uriParam);

        setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

        return logAndReturnCrsResponseBody(crsResponse, "Search Reservations", applicationProperties.isPermanentInfoLogEnabled());
    }
    
    /**
     * Retrieve prnding Reservation By Confirmation Number
     * this call will retrieve the latest image of the reservation by confirmation number 
     * @param confirmationNumber
     * @return ReservationRetrieveRes 
     */
    protected ReservationRetrieveResReservation retrievePendingReservationByConfirmationNumber(String confirmationNumber, String source) {
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
        
    	final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
    	final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(null, acrsVendor);
    	httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());
    	
        HttpEntity<?> request = new HttpEntity<>(httpHeaders);
        boolean isPoFlow = false;
        Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(), isPoFlow);
        uriParam.put(ServiceConstant.CRS_RESERVATION_CFNUMBER, confirmationNumber);


            log.info("Sending request to ACRS Retrieve Pending Reservation By Confirmation Number.");
            log.info("Sending request to Retrieve Pending, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
            log.info("Sending request to Retrieve Pending, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
            log.info("Sending request to Retrieve Pending, Request query parameters: "+uriParam);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsRetrievePendingReservation",
                urlProperties.getAcrsRetrievePendingReservation(), start);

        ResponseEntity<ReservationRetrieveResReservation> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsRetrievePendingReservation(), HttpMethod.GET, request,
                ReservationRetrieveResReservation.class, uriParam);

        setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

        return logAndReturnCrsResponseBody(crsResponse, "Retrieve Pending Reservation", applicationProperties.isPermanentInfoLogEnabled());
    }

    protected static RoomRequest createRoomRequest(String requestId) {
        RoomRequest roomRequest = new RoomRequest(requestId);
        roomRequest.setSelected(true);
        return roomRequest;
    }

    /**
     * @param cancelRequest
     * @return
     */
    protected RoomReservation cancelPendingRoomReservation(CancelV2Request cancelRequest) throws ParseException {
        String propertyId = cancelRequest.getPropertyId();

        CancellationReq cancellationReq = new CancellationReq();
        CancellationReqData cancellationReqData = new CancellationReqData();

        if (cancelRequest.getCancellationReason() != null) {
            List<CancellationReason> cancellationReasons = new ArrayList<>();
            cancellationReasons.add(mapCancellationReasonFromCancelRequest(cancelRequest));
            cancellationReqData.setCancellationReasons(cancellationReasons);
            cancellationReq.setData(cancellationReqData);
        }
        
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(cancelRequest.getSource());
        
    	final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
    	final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId), acrsVendor);
    	httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());
    		
        HttpEntity<CancellationReq> request = new HttpEntity<CancellationReq>(cancellationReq, httpHeaders);
        boolean isPoFlow = cancelRequest.isPerpetualPricing();
        Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(), acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(),isPoFlow);
        uriParam.put(ServiceConstant.CRS_RESERVATION_CFNUMBER, cancelRequest.getConfirmationNumber());


            log.info("Sending request to ACRS Cancel Pending Room Reservation.");
            log.info("Sending request to Cancel Pending, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
            log.info("Sending request to Cancel Pending, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
            log.info("Sending request to Cancel Pending, Request query parameters: "+uriParam);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsCancelPendingReservation",
                urlProperties.getAcrsCancelPendingReservation(), start);

        ResponseEntity<ReservationRes> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsCancelPendingReservation(), HttpMethod.PUT, request,
                ReservationRes.class, uriParam);

        setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

        ReservationRes crsReservation = logAndReturnCrsResponseBody(crsResponse, "Cancel Pending Room Reservation", applicationProperties.isPermanentInfoLogEnabled());
        RoomReservation roomReservation = RoomReservationTransformer.transform(crsReservation, acrsProperties);
        referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
        return roomReservation;
    }

    private CancellationReason mapCancellationReasonFromCancelRequest(CancelV2Request cancelRequest) {
        CancellationReason cancellationReason = new CancellationReason();
        cancellationReason.setCode(cancelRequest.getCancellationReason());
        cancellationReason.setWaiveFees(cancelRequest.isOverrideDepositForfeit());
        return cancellationReason;
    }

    protected LinkReservationRes createSharedReservationLink(String primaryResvCnf, List<String> sharedReservations,
            String acrsPropertyCode, String source , boolean isPoFlow) {
        // create header
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(acrsPropertyCode, acrsVendor);
        httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION,
                    ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());           

        LinkReservationReq linkReq = createHotelLinkReservationReq(primaryResvCnf, sharedReservations);
        HttpEntity<LinkReservationReq> httpRequest = new HttpEntity<>(linkReq, httpHeaders);
        Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(), isPoFlow);

        // call ACRS

            log.info("Sending request to ACRS Create Link API.");
            log.info("Sending request to Create Link, Request headers {}:", CommonUtil.convertObjectToJsonString(httpRequest.getHeaders()));
            log.info("Sending request to Create Link, Request body {}: ", CommonUtil.convertObjectToJsonString(httpRequest.getBody()));
            log.info("Sending request to Create Link, Request query parameters: "+uriParams);


        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsCreateReservationLink",
                urlProperties.getAcrsCreateReservationLink(), start);

        ResponseEntity<LinkReservationRes> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsCreateReservationLink(), HttpMethod.POST, httpRequest,
                LinkReservationRes.class, uriParams);

        setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

        return logAndReturnCrsResponseBody(crsResponse, "Create Shared Reservation Link", applicationProperties.isPermanentInfoLogEnabled());
    }

    private LinkReservationReq createHotelLinkReservationReq(String primaryResvCnf, List<String> sharedReservations) {
        LinkReservationReq request = new LinkReservationReq();
        HotelLinkReservationReq hotelLinkReservationReq = new HotelLinkReservationReq();
        List<String> linkedCnfs = new ArrayList<>();
        linkedCnfs.add(primaryResvCnf);
        linkedCnfs.addAll(sharedReservations);
        hotelLinkReservationReq.setCfNumbers(linkedCnfs);
        hotelLinkReservationReq.setSharePricingMethod(SharePricingMethod.FULL);
        hotelLinkReservationReq.setType(LinkType.SHARE);
        LinkReservationReqData linkReservationReqData = new LinkReservationReqData();
        linkReservationReqData.setHotelLinkReservation(hotelLinkReservationReq);
        request.setData(linkReservationReqData);
        return request;
    }
   /**
     * modify reservation link
     * @param linkId
     * @param acrsPropertyCode
     * @param source
     * @param changes
     * @return
     */
    protected LinkReservationRes modifySharedReservationLink(String linkId,
            String acrsPropertyCode, String source,LinkReservationModifyReq changes, boolean isPoFlow) {
        // create header
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(acrsPropertyCode, acrsVendor);
        httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION,
                    ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());
        
        HttpEntity<LinkReservationModifyReq> httpRequest = new HttpEntity<LinkReservationModifyReq>(changes, httpHeaders);
        Map<String, String> uriParams = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getReservationsVersion(), acrsProperties.getChainCode(), isPoFlow);
        uriParams.put(ServiceConstant.CRS_RESERVATION_LINKID, linkId);
        // call ACRS

            log.info("Sending request to ACRS modify link API.");
            log.info("Sending request to Modify Link, Request headers {}:", CommonUtil.convertObjectToJsonString(httpRequest.getHeaders()));
            log.info("Sending request to Modify Link, Request body {}: ", CommonUtil.convertObjectToJsonString(httpRequest.getBody()));
            log.info("Sending request to Modify Link, Request query parameters: "+uriParams);


       LocalDateTime start = LocalDateTime.now();
       setThreadContextBeforeAPICall("AcrsModifyReservationLink",
               urlProperties.getAcrsModifyReservationLink(), start);

        ResponseEntity<LinkReservationRes> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsModifyReservationLink(), HttpMethod.PATCH, httpRequest,
                LinkReservationRes.class, uriParams);

       setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

       return logAndReturnCrsResponseBody(crsResponse, "Modify share reservation link.", applicationProperties.isPermanentInfoLogEnabled());
    }

    protected void setThreadContextBeforeAPICall(String apiName, String apiUrl, LocalDateTime start) {
        ThreadContext.put(ServiceConstant.DEPENDENT_SYSTEM_TYPE, ServiceConstant.ACRS_DEPENDENT_SYSTEM);
        ThreadContext.put(ServiceConstant.API_NAME_TYPE, apiName);
        ThreadContext.put(ServiceConstant.API_URL_TYPE, apiUrl);
        ThreadContext.put(ServiceConstant.TIME_TYPE, start.toString());
    }

    protected void setThreadContextAfterAPICall(LocalDateTime start, String httpStatusCode) {
        long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
        ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
        ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpStatusCode));
        log.info("Custom Dimensions updated after ACRS call");
    }

    
    /**
     * If policies.getDeposit() = null and
     * policies.getGuarantee().getIsRequired()= false then FormOfPAyment is not
     * required in ACRS
     * 
     * @param ReservationPendingRes
     * @return
     */
    protected boolean isCashFormOfPaymentRequired(ReservationPendingRes crsResponse) {
        SegmentResItem mainSegment = BaseAcrsTransformer
                .getMainSegment(crsResponse.getData().getHotelReservation().getSegments());
        return isCashFormOfPaymentRequired(mainSegment);
    }

    /**
     * If policies.getDeposit() = null and
     * policies.getGuarantee().getIsRequired()= false then FormOfPAyment is not
     * required in ACRS
     *
     * @param ReservationModifyPendingRes
     * @return
     */
    protected boolean isCashFormOfPaymentRequired(ReservationModifyPendingRes reservationRes) {
        SegmentResItem mainSegment = BaseAcrsTransformer
                .getMainSegment(reservationRes.getData().getHotelReservation().getSegments());
        return isCashFormOfPaymentRequired(mainSegment);
    }
    protected boolean isCashFormOfPaymentRequired(ReservationRetrieveResReservation existingResv) {
        SegmentResItem mainSegment = BaseAcrsTransformer
                .getMainSegment(existingResv.getData().getHotelReservation().getSegments());
        return isCashFormOfPaymentRequired(mainSegment);
    }
   
    private boolean isCashFormOfPaymentRequired(SegmentResItem mainSegment) {
       boolean isCashFormOfPaymentRequired = false;
       if(null != mainSegment.getOffer().getPolicies()) {
           DepositRes deposit = mainSegment.getOffer().getPolicies().getDeposit();
           isCashFormOfPaymentRequired = Optional.ofNullable(deposit).filter(depositRes -> Double.parseDouble(depositRes.getAmount()) != 0.0).isPresent();
       }
       return isCashFormOfPaymentRequired;
    }

    /**
     * this will set the adultCount for all productUses for all segments
     * @param reservationReq
     * @param adultCount
     */
    protected void updateGuestCount(ReservationPendingReq reservationReq, int adultCount){
        reservationReq.getData().getHotelReservation().getSegments().forEach(segment -> {
            segment.getOffer().getProductUses().forEach(productUse -> {
            Optional<GuestCountReq> adultGuestCount = productUse.getGuestCounts().stream()
                    .filter(guest -> StringUtils.equalsIgnoreCase(guest.getOtaCode(), ServiceConstant.NUM_ADULTS_MAP))
                    .findFirst();
            if (adultGuestCount.isPresent()) {
                adultGuestCount.get().setCount(adultCount);
            }
        });
        });
    }

    protected <T> T logAndReturnCrsResponseBody(ResponseEntity<T> crsResponse, String apiName, boolean isInfoLogEnabled) {
        return ReservationUtil.logAndReturnCrsResponseBody(crsResponse, apiName, log, isInfoLogEnabled);
    }
    protected void logRequestHeaderAndBody(boolean logInfo, String headerStr,String bodyStr, String paramStr, String headerObj, String bodyObj, Map<String, String> paramValues){
       CommonUtil.logRequestHeaderAndBody(logInfo,log,
               headerStr,bodyStr,paramStr,headerObj,bodyObj,paramValues);
    }

    protected <T> T logAndReturnEnrResponseBody(ResponseEntity<T> enrResponse, String apiName, boolean isInfoLogEnabled) {
        return ReservationUtil.logAndReturnEnrResponseBody(enrResponse, apiName, log, isInfoLogEnabled);
    }



}
