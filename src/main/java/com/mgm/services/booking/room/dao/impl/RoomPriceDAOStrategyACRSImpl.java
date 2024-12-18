package com.mgm.services.booking.room.dao.impl;

import com.google.common.collect.Lists;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.RoomPriceDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.exception.ACRSSearchOffersErrorDetails;
import com.mgm.services.booking.room.exception.ACRSSearchOffersErrorRes;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.crs.calendarsearches.*;
import com.mgm.services.booking.room.model.crs.calendarsearches.Loyalty.PlayDominanceOverrideEnum;
import com.mgm.services.booking.room.model.crs.groupretrieve.GroupProductUseResGroupBookingRetrieve;
import com.mgm.services.booking.room.model.crs.groupretrieve.GroupRetrieveResGroupBookingRetrieve;
import com.mgm.services.booking.room.model.crs.groupretrieve.GroupRetrieveResgroupBookingRetrieveData;
import com.mgm.services.booking.room.model.crs.searchoffers.IncludeSellStrategyMulti;
import com.mgm.services.booking.room.model.crs.searchoffers.Loyalty;
import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.model.crs.searchoffers.RequestedGuestCounts;
import com.mgm.services.booking.room.model.crs.searchoffers.OptionsMulti.IncludePublicRatesEnum;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.AuroraPriceV3Request;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.*;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.CalendarAvailabilityTransformer;
import com.mgm.services.booking.room.transformer.RoomAvailabilityTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation class providing DAO services to retrieve room prices by
 * invoking aurora API calls.
 */
@Component
@Log4j2
public class RoomPriceDAOStrategyACRSImpl extends BaseAcrsDAO implements RoomPriceDAOStrategy {

    private URLProperties urlProperties;
    private DomainProperties domainProperties;
    private RestTemplate client;
    private AcrsProperties acrsProperties;
    private GroupSearchDAOImpl groupSearchDAOImpl;
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    private ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;
	private	SecretsProperties secretsProperties;

	// Unable to price denial identifiers:
	// 99 - Requested check-out date is after the promo code stay end date
	final List<Integer> UNABLE_TO_PRICE_DENIAL_IDENTIFIERS = Arrays.asList(99);
	private final boolean  doTempInfoLogging ;
	@Autowired
    public RoomPriceDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
            ApplicationProperties applicationProperties, AcrsProperties acrsProperties, RestTemplateBuilder builder,
            GroupSearchDAOImpl groupSearchDAOImpl, ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, SecretsProperties secretsProperties) {
		super(urlProperties, domainProperties, applicationProperties, acrsProperties, CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getAcrsPricingConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsPricingMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(),
				applicationProperties.getSocketTimeOut(),1,
				applicationProperties.getCrsRestTTL()), referenceDataDAOHelper, acrsOAuthTokenDAOImpl);

		this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getAcrsPricingConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsPricingMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeoutACRS(),
				applicationProperties.getReadTimeOutACRS(),
				applicationProperties.getSocketTimeOutACRS(),
				1,
				applicationProperties.getCrsRestTTL());
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.acrsProperties = acrsProperties;
        this.groupSearchDAOImpl = groupSearchDAOImpl;
        this.referenceDataDAOHelper = referenceDataDAOHelper;
        this.acrsOAuthTokenDAOImpl = acrsOAuthTokenDAOImpl;
		this.secretsProperties = secretsProperties;

		this.client.setMessageConverters(Collections.singletonList(ReservationUtil.createHttpMessageConverter()));
		this.doTempInfoLogging = CommonUtil.isTempLogEnabled(secretsProperties.getSecretValue(applicationProperties.getTempInfoLogEnabled()));
    }

	/**
	 * Invokes ACRS pricing API for the request provided and returns pricing
	 * response returned based on input criteria.
	 *
	 * @param pricingRequest Aurora pricing request object
	 * @return List of aurora pricing response
	 */
	@Override
	public List<AuroraPriceResponse> getRoomPrices(AuroraPriceRequest pricingRequest) {
		if(doTempInfoLogging){
			log.info("Incoming Request for getRoomPrices: {}", CommonUtil.convertObjectToJsonString(pricingRequest));
		}else if (log.isDebugEnabled()) {
			log.debug("Incoming Request for getRoomPrices: {}", CommonUtil.convertObjectToJsonString(pricingRequest));
		}

		List<AuroraPriceResponse> auroraPriceResponseList;
		boolean isGroupCode = ACRSConversionUtil.isAcrsGroupCodeGuid(pricingRequest.getProgramId());
		boolean forcePerpetualOffer = pricingRequest.isPerpetualOffer();
		if (null != pricingRequest.getProgramId()) {
			String aCrsRatePlanCode = referenceDataDAOHelper.retrieveRatePlanDetail(pricingRequest.getPropertyId(),
					pricingRequest.getProgramId());

			forcePerpetualOffer = ACRSConversionUtil.isPORatePlan(aCrsRatePlanCode);
		}

		final String aCrsPropertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(pricingRequest.getPropertyId());
		String computedBaseRatePlan = acrsProperties.getBaseRatePlan(aCrsPropertyCode.toUpperCase());

		SuccessfulSingleAvailability originalAvailabilityResponse = getAcrsSingleAvailability(pricingRequest);
		SuccessfulSingleAvailability basePriceAvailabilityResponse = null;
		if (forcePerpetualOffer && isCrsResponseCompCash(originalAvailabilityResponse.getData().getRatePlans())) {
			// Fill in BAR rate prices if PO flow. If an error is thrown, there will be no BAR prices returned and
			// the flow without strike through pricing.
			AuroraPriceRequest basePriceRequest = buildBasePriceRequest(pricingRequest, computedBaseRatePlan);
			try {
				basePriceAvailabilityResponse = getAcrsSingleAvailability(basePriceRequest);
			} catch (BusinessException e) {
				log.warn("Unable to price using BAR rateplan for {}. Proceeding with the requested rateplan.", pricingRequest.getPropertyId());
				log.debug("Bar prices failing due to exception:", e);
			}
		}

		if (!CollectionUtils.isEmpty(originalAvailabilityResponse.getData().getDenials())){
			Optional<Denial> denialOptional = originalAvailabilityResponse.getData().getDenials().stream()
					.filter(denial -> UNABLE_TO_PRICE_DENIAL_IDENTIFIERS.contains(denial.getIdentifier()))
					.findFirst();
			if(denialOptional.isPresent()){
				Denial denial = denialOptional.get();
				throw new BusinessException(ErrorCode.UNABLE_TO_PRICE, denial.getReason());
			}
		}

		auroraPriceResponseList = RoomAvailabilityTransformer.transform(originalAvailabilityResponse,
				pricingRequest, computedBaseRatePlan, forcePerpetualOffer,
				basePriceAvailabilityResponse);

		referenceDataDAOHelper.updateAcrsReferencesToGse(auroraPriceResponseList, isGroupCode);
		return auroraPriceResponseList;
	}

	private AuroraPriceRequest buildBasePriceRequest(AuroraPriceRequest pricingRequest, String computedBaseRatePlan) {
		return AuroraPriceRequest.builder()
				.isPerpetualOffer(false)
				.checkInDate(pricingRequest.getCheckInDate())
				.checkOutDate(pricingRequest.getCheckOutDate())
				.programId(referenceDataDAOHelper.retrieveRatePlanDetail(pricingRequest.getPropertyId(),
						computedBaseRatePlan, true))
				.propertyId(pricingRequest.getPropertyId())
				.numGuests(pricingRequest.getNumGuests())
				.numChildren(pricingRequest.getNumChildren())
				.source(pricingRequest.getSource())
				.includeDefaultRatePlans(false)
				.build();
	}

	private boolean isCrsResponseCompCash(List<RatePlanSingle> acrsRatePlanList) {
        Optional<RatePlanSingle> acrsRatePlan = acrsRatePlanList.stream()
                .filter(ratePlan -> ServiceConstant.COMP.equals(ratePlan.getGamingBucket()) ||
						ServiceConstant.CASH.equals(ratePlan.getGamingBucket())).findFirst();
		return acrsRatePlan.isPresent();
	}

    /**
     * Invokes ACRS pricing API for the request provided and returns Availability details like taxes charges with pricing.
     * response returned based on input criteria.
     * @param pricingRequest
     * @return
     */

    public SuccessfulSingleAvailability getAcrsSingleAvailability (AuroraPriceRequest pricingRequest) {
		BodyParameterSingle bodyParameterSingle =
				RoomAvailabilityTransformer.getBodyParameterSingleFromPricingRequest(pricingRequest, referenceDataDAOHelper);
		return acrsSingleAvailability(pricingRequest, bodyParameterSingle);
    }

	private SuccessfulSingleAvailability acrsSingleAvailability(AuroraPriceRequest pricingRequest, BodyParameterSingle bodyParameterSingle) {
		final boolean forcePerpetualOffer = pricingRequest.isPerpetualOffer();
		final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(pricingRequest.getSource());
		final String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(pricingRequest.getPropertyId());
        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(propertyCode, acrsVendor);

        //Adding these logs to identify Null pointer exception.
        if (log.isDebugEnabled()) {
            log.debug("Acrs availability API : Printing ACRS Vendor Code " + acrsVendor);
            log.debug("Acrs availability API : Printing token " + CommonUtil.convertObjectToJsonString(acrsAuthTokenResponseMap));
        }

        httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION,
                    ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());

        HttpEntity<BodyParameterSingle> request = new HttpEntity<>(bodyParameterSingle, httpHeaders);
        Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(), acrsProperties.getAvailabilityVersion(), acrsProperties.getChainCode(), forcePerpetualOffer);

        uriParam.put(ServiceConstant.CRS_PROPERTY_CODE, propertyCode);
        uriParam.put(ServiceConstant.CRS_START_DATE, pricingRequest.getCheckInDate().toString());

        long noOfNights = Duration.between(pricingRequest.getCheckInDate().atStartOfDay(),
                pricingRequest.getCheckOutDate().atStartOfDay()).toDays();

        uriParam.put(ServiceConstant.CRS_DURATION, noOfNights + "D");
		logRequestHeaderAndBody(doTempInfoLogging,
				"Sending request to Availability, Request headers {}:",
				"Sending request to Availability, Request body {}: ",
				"Sending request to Availability, Request query parameters: {}",
				CommonUtil.convertObjectToJsonString(request.getHeaders()),
				CommonUtil.convertObjectToJsonString(request.getBody()),
				uriParam
		);

		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("AcrsAvailabilityReservation",
				urlProperties.getAcrsAvailabilityReservation(), start);

		ResponseEntity<SuccessfulSingleAvailability> crsResponse = client.exchange(
				domainProperties.getCrs() + urlProperties.getAcrsAvailabilityReservation(), HttpMethod.POST,
				request, SuccessfulSingleAvailability.class, uriParam);

		setThreadContextAfterAPICall(start, HttpStatus.OK.toString());

		return logAndReturnCrsResponseBody(crsResponse, "Single Availability", doTempInfoLogging);
    }


	/**
	 * this will return price offer with room and components offers
	 * @param bodyParameterPricingRequest
	 * @param acrsPropertyCode
	 * @param ratePlanCode
	 * @param startDate
	 * @param duration
	 * @return
	 */
	SuccessfulPricing acrsSearchOffers(BodyParameterPricing bodyParameterPricingRequest, String acrsPropertyCode,
									   String ratePlanCode, String startDate, String duration, String source,
									   boolean isPoFlow) {
		final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

		final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
		final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(acrsPropertyCode,acrsVendor);
		httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

		HttpEntity<BodyParameterPricing> httpRequest = new HttpEntity<>(bodyParameterPricingRequest, httpHeaders);
		Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
				acrsProperties.getAvailabilityVersion(), acrsProperties.getChainCode(), isPoFlow);
		uriParam.put(ServiceConstant.CRS_PROPERTY_CODE, acrsPropertyCode);
		uriParam.put("ratePlanCode", ratePlanCode);
		uriParam.put(ServiceConstant.CRS_START_DATE, startDate);
		uriParam.put(ServiceConstant.CRS_DURATION, duration + "D");
		logRequestHeaderAndBody(doTempInfoLogging,
				"Sending request to Search Offer, Request headers {}:",
				"Sending request to Search Offer, Request body {}: ",
				"Sending request to Search Offer, Request query parameters: {}",
				CommonUtil.convertObjectToJsonString(httpRequest.getHeaders()),
				CommonUtil.convertObjectToJsonString(httpRequest.getBody()),
				uriParam
		);
		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("AcrsSearchOffers",
				urlProperties.getAcrsSearchOffers(), start);

		ResponseEntity<SuccessfulPricing> crsResponse = client.exchange(
				domainProperties.getCrs() + urlProperties.getAcrsSearchOffers(), HttpMethod.POST, httpRequest,
				SuccessfulPricing.class, uriParam);

		setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

		return logAndReturnCrsResponseBody(crsResponse, "Search Offers", doTempInfoLogging);
	}

    @Override
    public List<AuroraPriceResponse> getCalendarPrices(AuroraPriceRequest pricingRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request from Client for getCalendarPrices: {}", CommonUtil.convertObjectToJsonString(pricingRequest));
        }

        return getIterableCalendarPrices(pricingRequest);
    }

    @Override
	public List<AuroraPriceResponse> getIterableCalendarPrices(AuroraPriceRequest pricingRequest) {
		if (log.isDebugEnabled()) {
			log.debug("Incoming Request for getIterableCalendarPrices: {}",
					CommonUtil.convertObjectToJsonString(pricingRequest));
		}
		final SuccessfulSingleAvailability successfulSingleAvailability = getSingleAvailResponseFromGroupSearch(pricingRequest);
		final List<AuroraPriceResponse> singleAvailResponseList = successfulSingleAvailability != null ? RoomAvailabilityTransformer
				.transform(successfulSingleAvailability, pricingRequest.getNumRooms()) : null;

		List<SuccessfulCalendarAvailability> crsResponseList = getCRSCalendarAvailabilityResponseList(pricingRequest, 1);

		final List<AuroraPriceResponse> calendarPricesResponseList = CalendarAvailabilityTransformer
				.transformCalendarAvailability(crsResponseList, pricingRequest.getNumRooms(), pricingRequest.isPerpetualOffer());
		referenceDataDAOHelper.updateAcrsReferencesToGse(calendarPricesResponseList, false);
		return aggregateResponse(singleAvailResponseList, calendarPricesResponseList);
	}

	private List<SuccessfulCalendarAvailability> getCRSCalendarAvailabilityResponseList(AuroraPriceRequest pricingRequest, int tripLength) {
		final String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(pricingRequest.getPropertyId());

		BodyParameterCalendar bodyParameterCalendar = createBodyParameterCalendar(propertyCode,pricingRequest);

        final long noOfNights = Duration.between(pricingRequest.getCheckInDate().atStartOfDay(),
                pricingRequest.getCheckOutDate().atStartOfDay()).toDays();

        List<AuroraPriceRequest> tempAuroraPriceRequestList = new ArrayList<>();

       if (noOfNights <= ServiceConstant.ACRS_DAY_LIMIT) {
            tempAuroraPriceRequestList.add(pricingRequest);
        } else {
            tempAuroraPriceRequestList = splitPriceRequest(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate());
        }

        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(pricingRequest.getSource());

        return acrsCalendarAvailability(pricingRequest, tripLength, propertyCode, bodyParameterCalendar, tempAuroraPriceRequestList, acrsVendor);
    }

	private BodyParameterCalendar createBodyParameterCalendar(String propertyCode, AuroraPriceRequest pricingRequest){

		final BodyParameterCalendar bodyParameterCalendar = new BodyParameterCalendar();
		final RequestedRatesCalendar requestedRatesCalendar = new RequestedRatesCalendar();

		final List<RequestedProductCalendar> products = new ArrayList<>();
		OptionsCalendar options = new OptionsCalendar();
		if(!CollectionUtils.isEmpty(pricingRequest.getRoomTypeIds())){
			pricingRequest.getRoomTypeIds().forEach(id -> {
				final RequestedProductCalendar product = new RequestedProductCalendar();
				product.setProductCode(referenceDataDAOHelper.retrieveRoomTypeDetail(propertyCode, id));
				products.add(product);

			});
			bodyParameterCalendar.setProducts(products);
			//Pass Never when room types are requested.
			//This will help ACRS to provide only what we requested for.
			//If we don't pass this, then ACRS will return all the rate plan and room types
			options.setIncludePublicRates(com.mgm.services.booking.room.model.crs.calendarsearches.OptionsCalendar.IncludePublicRatesEnum.NEVER);
		}

		//inc-4
		if (pricingRequest.getPromo() != null) {
			requestedRatesCalendar.setPromoCode(pricingRequest.getPromo());
		} else if (pricingRequest.getProgramId() != null) { //programId - rates.ratePlanCodes
			final List<String> ratePlanCodes = new ArrayList<>();
			ratePlanCodes.add(referenceDataDAOHelper.retrieveRatePlanDetail(propertyCode, pricingRequest.getProgramId()));
			requestedRatesCalendar.setRatePlanCodes(ratePlanCodes);
		}else if(org.apache.commons.collections.CollectionUtils.isNotEmpty(pricingRequest.getProgramIds())){
			final List<String> ratePlanCodes = pricingRequest.getProgramIds().stream()
					.map(programId -> referenceDataDAOHelper.retrieveRatePlanDetail(propertyCode, programId))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
				requestedRatesCalendar.setRatePlanCodes(ratePlanCodes);
		}

		bodyParameterCalendar.setRates(requestedRatesCalendar);

		// perpetualOffer - mlifeNumber

		com.mgm.services.booking.room.model.crs.calendarsearches.Loyalty loyalty
				= new com.mgm.services.booking.room.model.crs.calendarsearches.Loyalty();

		//inc-3
		if (StringUtils.isNotEmpty(pricingRequest.getMlifeNumber())) {
			loyalty.setLoyaltyId(pricingRequest.getMlifeNumber());
		}

		if (pricingRequest.isPerpetualOffer()) {
			if (pricingRequest.getMlifeNumber() != null) {
				options.setPerpetualOffer(true);
			} else {
				log.error("If perpetual offer, the loyaltyId must be provided");
				throw new BusinessException(ErrorCode.MLIFE_NUMBER_NOT_FOUND);
			}
		}

		//inc-3
		//customer rank
		if(pricingRequest.getCustomerRank() > 0) {
			loyalty.setValueTierOverride(String.valueOf(pricingRequest.getCustomerRank()));
		}

		//inc-3
		//customer dominant play
		if (StringUtils.isNotBlank(pricingRequest.getCustomerDominantPlay())) {
			if (ServiceConstant.SLOT.equalsIgnoreCase(pricingRequest.getCustomerDominantPlay())) {
				loyalty.setPlayDominanceOverride(PlayDominanceOverrideEnum.SLOTS);
			} else {
				loyalty.setPlayDominanceOverride(PlayDominanceOverrideEnum.valueOf(pricingRequest.getCustomerDominantPlay().toUpperCase()));
			}
		}

		options.setLoyalty(loyalty);
		bodyParameterCalendar.setOptions(options);

		//inc-4
		List<com.mgm.services.booking.room.model.crs.calendarsearches.RequestedGuestCounts> reqGuestCounts = new ArrayList<>();
		com.mgm.services.booking.room.model.crs.calendarsearches.RequestedGuestCounts aqC10 = new com.mgm.services.booking.room.model.crs.calendarsearches.RequestedGuestCounts();
		aqC10.setCount(pricingRequest.getNumGuests());
		aqC10.setOtaCode(ServiceConstant.NUM_ADULTS_MAP);
		com.mgm.services.booking.room.model.crs.calendarsearches.RequestedGuestCounts aqC8 = new com.mgm.services.booking.room.model.crs.calendarsearches.RequestedGuestCounts();
		aqC8.setCount(pricingRequest.getNumChildren());
		aqC8.setOtaCode(ServiceConstant.NUM_CHILD_MAP);
		reqGuestCounts.add(aqC10);
		reqGuestCounts.add(aqC8);
		bodyParameterCalendar.setGuestCounts(reqGuestCounts);
		return bodyParameterCalendar;
	}

    private List<SuccessfulCalendarAvailability> acrsCalendarAvailability(AuroraPriceRequest pricingRequest, int tripLength,
																		  String propertyCode, BodyParameterCalendar bodyParameterCalendar, List<AuroraPriceRequest> tempAuroraPriceRequestList, String acrsVendor) {
        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(pricingRequest.getPropertyId(), acrsVendor);
        httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());

        return tempAuroraPriceRequestList.parallelStream()
				.map(auroraPriceRequest -> makeAcrsCalendarAvailabilitySearch(tripLength, propertyCode,
						auroraPriceRequest.getCheckInDate(), auroraPriceRequest.getCheckOutDate(),
						bodyParameterCalendar,	httpHeaders))
				.collect(Collectors.toList());
    }

	private SuccessfulCalendarAvailability makeAcrsCalendarAvailabilitySearch(int tripLength,
																			  String propertyCode,
																			  LocalDate checkInDate,
																			  LocalDate checkOutDate,
																			  BodyParameterCalendar bodyParameterCalendar,
																			  HttpHeaders httpHeaders) {
		final HttpEntity<BodyParameterCalendar> request = new HttpEntity<>(bodyParameterCalendar, httpHeaders);

		final Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
				acrsProperties.getSearchVersion(), acrsProperties.getChainCode());
		uriParam.put(ServiceConstant.CRS_PROPERTY_CODE, propertyCode);
		uriParam.put(ServiceConstant.CRS_START_DATE, checkInDate.toString());
		uriParam.put(ServiceConstant.CRS_END_DATE, checkOutDate.toString());
		uriParam.put(ServiceConstant.CRS_DURATION, tripLength + "D");
		logRequestHeaderAndBody(doTempInfoLogging,
				"Sending request to Calendar Availability, Request headers {}:",
				"Sending request to Calendar Availability, Request body {}: ",
				"Sending request to Calendar Availability, Request query parameters: {}",
				CommonUtil.convertObjectToJsonString(request.getHeaders()),
				CommonUtil.convertObjectToJsonString(request.getBody()),
				uriParam
		);

		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("AcrsCalendarAvailabilitySearch",
				urlProperties.getAcrsCalendarAvailabilitySearch(), start);

		final ResponseEntity<SuccessfulCalendarAvailability> crsResponse = client.exchange(
				domainProperties.getCrs() + urlProperties.getAcrsCalendarAvailabilitySearch(), HttpMethod.POST, request,
				SuccessfulCalendarAvailability.class, uriParam);

		setThreadContextAfterAPICall(start, HttpStatus.OK.toString());

		return logAndReturnCrsResponseBody(crsResponse, "Calendar Availability", doTempInfoLogging);
	}

	/**
     * This method returns the base price for each date calculated from the Prevail rate codes.
     * Call Calendar availability in case its a PO Pricing Request. Retrieve the base price from normal pricing call for non PO flow.
     * @param request
     * @param crsResponses
     * @param tripLength
     * @return
     */

    private Map<LocalDate, Double> getCRSCalendarAvailabilityBasePrices(AuroraPriceRequest request,
                                                                        List<SuccessfulCalendarAvailability> crsResponses ,
                                                                        int tripLength) {
        if (request.isPerpetualOffer()) {
            final String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
            final BodyParameterCalendar requestBody = createRequestBody(propertyCode);

            final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(request.getSource());

        	final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        	final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(request.getPropertyId(), acrsVendor);
        	httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

            crsResponses = splitPriceRequest(request.getCheckInDate(), request.getCheckOutDate()).parallelStream()
					.map(priceRequest -> makeAcrsCalendarAvailabilitySearch(tripLength, propertyCode,
									priceRequest.getCheckInDate(), priceRequest.getCheckOutDate(), requestBody, httpHeaders))
					.collect(Collectors.toList());
        }

        final String acrsPropertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
        String computedBaseRatePlan = acrsProperties.getBaseRatePlan(acrsPropertyCode.toUpperCase());

        return CalendarAvailabilityTransformer.transformCalendarBasePrice(crsResponses, computedBaseRatePlan);
    }

    private BodyParameterCalendar createRequestBody(String propertyCode) {
        final BodyParameterCalendar requestBody = new BodyParameterCalendar();

		String computedBaseRatePlan = acrsProperties.getBaseRatePlan(propertyCode.toUpperCase());

        //ratePlan - PREVL
        final RequestedRatesCalendar requestedRatesCalendar = new RequestedRatesCalendar();
        final List<String> ratePlanCodes = new ArrayList<>();
        ratePlanCodes.add(computedBaseRatePlan);
        requestedRatesCalendar.setRatePlanCodes(ratePlanCodes);
        requestBody.setRates(requestedRatesCalendar);

        final OptionsCalendar options = new OptionsCalendar();
        options.setPerpetualOffer(false);
        options.setIncludeROCandROHProductsOnly(false);
        requestBody.setOptions(options);
        return requestBody;
    }

    private SuccessfulSingleAvailability getSingleAvailResponseFromGroupSearch(AuroraPriceRequest pricingRequest) {

    	if(ACRSConversionUtil.isAcrsGroupCodeGuid(pricingRequest.getProgramId())) {

            //call Search group with the group code, start date, end date
            final GroupSearchV2Request groupSearchRequest = new GroupSearchV2Request();
            groupSearchRequest.setSource(pricingRequest.getSource());
            groupSearchRequest.setStartDate(pricingRequest.getCheckInDate().format(DateTimeFormatter.ISO_DATE));
            groupSearchRequest.setEndDate(pricingRequest.getCheckOutDate().format(DateTimeFormatter.ISO_DATE));
            groupSearchRequest.setPropertyId(referenceDataDAOHelper.retrieveAcrsPropertyID(pricingRequest.getPropertyId()));
            groupSearchRequest.setId(pricingRequest.getProgramId());
            final List<GroupSearchV2Response> groupSearchResponseList = groupSearchDAOImpl.searchGroup(groupSearchRequest);
            if(groupSearchResponseList.size() > 1) {
                log.error("Received multiple GroupSearchResponse for a single GroupCode -"+pricingRequest.getProgramId());
                throw new BusinessException(ErrorCode.FAILURE_GROUP_SEARCH);
            }

            //call Single avail with group code, group start date and group end date            
            final RoomAvailabilityV2Request roomAvailabilityV2Request = new RoomAvailabilityV2Request();
            roomAvailabilityV2Request.setSource(pricingRequest.getSource());
            roomAvailabilityV2Request.setPropertyId(pricingRequest.getPropertyId());
            roomAvailabilityV2Request.setIsGroupCode(pricingRequest.isGroupCode());
            roomAvailabilityV2Request.setNumAdults(pricingRequest.getNumGuests());
            roomAvailabilityV2Request.setExcludeNonOffer(pricingRequest.isProgramRate());

            final GroupSearchV2Response groupSearchV2Response = groupSearchResponseList.get(0);
            roomAvailabilityV2Request.setCheckInDate(pricingRequest.getCheckInDate());
            roomAvailabilityV2Request.setCheckOutDate(pricingRequest.getCheckOutDate());
            roomAvailabilityV2Request.setProgramId(groupSearchV2Response.getId());
            final AuroraPriceRequest pricingRequestToCallSingleAvail = AuroraPriceRequestTransformer.getAuroraPriceV2Request(roomAvailabilityV2Request, roomAvailabilityV2Request.isExcludeNonOffer());
            return getAcrsSingleAvailability(pricingRequestToCallSingleAvail);

        }
		return null;
	}
	/**combine singleAvailResponse and  calendarPricesResponse
     * sort final response based to Date
     */
    private List<AuroraPriceResponse> aggregateResponse(List<AuroraPriceResponse> singleAvailResponseList,
            List<AuroraPriceResponse> calendarPricesResponseList) {

    	Stream<AuroraPriceResponse> aggregatedStreamResponse = calendarPricesResponseList.stream();
        if(!CollectionUtils.isEmpty(singleAvailResponseList)){
            final Map<Date, AuroraPriceResponse> dateVsSingleAvailResMap = singleAvailResponseList.stream()
                .collect(Collectors.toMap(AuroraPriceResponse::getDate, e -> e));

            calendarPricesResponseList.removeIf(a -> dateVsSingleAvailResMap.containsKey(a.getDate()));
            aggregatedStreamResponse = Stream.concat(singleAvailResponseList.stream(), calendarPricesResponseList.stream());
        }

        final Stream<AuroraPriceResponse> aggregatedSortedStreamResponse = aggregatedStreamResponse.sorted((p1, p2)->p1.getDate().compareTo(p2.getDate()));

        return aggregatedSortedStreamResponse.collect(Collectors.toList());

    }

    private List<AuroraPriceV3Response> aggregateV3Response(List<AuroraPriceV3Response> singleAvailResponseList,
            List<AuroraPriceV3Response> calendarPricesResponseList) {

    	Stream<AuroraPriceV3Response> aggregatedStreamResponse = calendarPricesResponseList.stream();
        if(!CollectionUtils.isEmpty(singleAvailResponseList)){
            final Map<Date, AuroraPriceV3Response> dateVsSingleAvailResMap = singleAvailResponseList.stream()
                .collect(Collectors.toMap(AuroraPriceV3Response::getDate, e -> e));

            calendarPricesResponseList.removeIf(a -> dateVsSingleAvailResMap.containsKey(a.getDate()));

            aggregatedStreamResponse = Stream.concat(singleAvailResponseList.stream(), calendarPricesResponseList.stream());
        }

        final Stream<AuroraPriceV3Response> aggregatedSortedStreamResponse = aggregatedStreamResponse.sorted((p1, p2)->p1.getDate().compareTo(p2.getDate()));

        return aggregatedSortedStreamResponse.collect(Collectors.toList());

    }

    /**split AuroraPriceRequest based on date rage as ACRS supports 62 days at a time
     */
    private static List<AuroraPriceRequest> splitPriceRequest(LocalDate checkInDate, LocalDate checkOutDate) {
        final List<AuroraPriceRequest> allRequests = new ArrayList<>();
        LocalDate maxEndDate = checkInDate.plusDays(ServiceConstant.ACRS_DAY_LIMIT);
        LocalDate currentEnd;
		LocalDate workingStart = checkInDate;
        LocalDate workingEnd = maxEndDate.isBefore(checkOutDate) ? maxEndDate : checkOutDate;

        do {
            allRequests.add(AuroraPriceRequest.builder()
                    .checkInDate(workingStart)
                    .checkOutDate(workingEnd)
                    .build());
            currentEnd = workingEnd;
            workingStart = workingEnd.plusDays(1);
            maxEndDate = workingStart.plusDays(ServiceConstant.ACRS_DAY_LIMIT);
            workingEnd = maxEndDate.isBefore(checkOutDate) ? maxEndDate : checkOutDate;
        } while(currentEnd.isBefore(checkOutDate));

        return allRequests;
    }

    /**
     * Invokes ACRS Multi Availability API for the request provided and returns pricing
     * response returned based on input criteria.
     *
     * @param pricingRequest
     *            Aurora pricing request object
     * @return List of AuroraPriceResponse Response
     */
    public List<AuroraPriceResponse> getResortPrices(AuroraPriceRequest pricingRequest) {

		List<String> propertyIds = pricingRequest.getPropertyIds();
		//If number of properties <= max only one call is triggered
		if(propertyIds.size() <= acrsProperties.getMaxPropertiesForResortPricing()){
			return getResortsPricesByProperties(pricingRequest, propertyIds);
		} else{
			//If number of properties > max, then parallel calls are triggered with max properties per call
			List<List<String>> propSubLists = Lists.partition(propertyIds, acrsProperties.getMaxPropertiesForResortPricing());
			ExecutorService executor = Executors.newFixedThreadPool(propSubLists.size());
			try{
				List<CompletableFuture<List <AuroraPriceResponse>>> listOfCompletableFutures = propSubLists.stream()
						.map(propList -> CompletableFuture.supplyAsync(
								() -> getResortsPricesByProperties(pricingRequest, propList),executor)
								.exceptionally(ex -> {
				                    log.error(ex.getMessage());
				                    return null;
				                }))
								.collect(Collectors.toList());
				// TODO refactor by combining the following 2 lines into one
				List<List <AuroraPriceResponse>> listOfPriceResponses = listOfCompletableFutures.stream()
						.map(CompletableFuture::join)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				return listOfPriceResponses.stream().flatMap(Collection::stream).collect(Collectors.toList());
			} finally {
				executor.shutdown();
			}
		}
    }
    /**
     *
     * @param pricingRequest
     * @param propertyIds
     * @return
     */
    private List<AuroraPriceResponse> getResortsPricesByProperties(AuroraPriceRequest pricingRequest,
            List<String> propertyIds) {
        List<String> acrsPropertyIds = propertyIds.stream()
                .map(propertyId -> referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId))
                .collect(Collectors.toList());
        List<AuroraPriceResponse> prices = getResortsPricesByProperties(pricingRequest, acrsPropertyIds, false);
        List<String> unAvailablePropIds = prices.stream()
				.filter(price -> AvailabilityStatus.NOARRIVAL.equals(price.getStatus()))
				.map(AuroraPriceResponse::getPropertyId)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(unAvailablePropIds) && pricingRequest.getProgramId() != null) {
            prices.addAll(getResortsPricesByProperties(pricingRequest, unAvailablePropIds, true));
        }
        referenceDataDAOHelper.updateAcrsReferencesToGse(prices,false);
        return prices;

    }
    /**
     * Invokes ACRS Multi Availability API for the request provided and returns pricing
     * @param pricingRequest
     * @param propertyIds
     * @param returnedNoPrices
     * @return
     */

    private List<AuroraPriceResponse> getResortsPricesByProperties(AuroraPriceRequest pricingRequest,
            List<String> propertyIds, boolean returnedNoPrices) {

        BodyParameterMulti bodyParameterMulti = new BodyParameterMulti();
        DataRqMulti data = new DataRqMulti();
        RequestedRatesMulti requestedRatesMulti = new RequestedRatesMulti();
        OptionsMulti options = new OptionsMulti();
        if(StringUtils.isNotEmpty(pricingRequest.getProgramId())) {
            List<String> ratePlanCodes = new ArrayList<>();
            ratePlanCodes.add(pricingRequest.getProgramId());
            requestedRatesMulti.setRatePlanCodes(ratePlanCodes);
            requestedRatesMulti.setPromoCode(pricingRequest.getPromo());
            //If rateplan/program is passed, make sure we send Sell Strategy as Never.
            //If all the properties prices are not returned, for the remaining properties, 
            //RBS has to make a second api call to ACRS without Sell Strategy as Never, 
            //so that it gets the lowest prices for those properties based on sell strategy. 
            if(!returnedNoPrices) {
                options.setIncludeSellStrategy(IncludeSellStrategyMulti.NEVER);
                options.setIncludePublicRates(IncludePublicRatesEnum.NEVER);
                data.setOptions(options);
            }
        }
        data.setRates(requestedRatesMulti);

		List<RequestedGuestCounts> reqGuestCounts =
				RoomAvailabilityTransformer.getRequestedGuestCountsListFromPricingRequest(pricingRequest);

        data.setProducts(Collections.singletonList(createMultiProductFromRoomTypeId(null, reqGuestCounts,
                pricingRequest.getPropertyId(), pricingRequest.getNumRooms())));
        
        options.setIncludeTaxAverageDetails(true);
	    options.setDisabilityMode(DisabilityRequest.ACCESSIBLEANDNONACCESSIBLE);
        if (StringUtils.isNotBlank(pricingRequest.getMlifeNumber()) && Integer.valueOf(pricingRequest.getMlifeNumber()) > 0) {
        	Loyalty loyalty = new Loyalty();
        	loyalty.setLoyaltyId(pricingRequest.getMlifeNumber());
        	options.setLoyalty(loyalty);
        }
        data.setOptions(options);
        bodyParameterMulti.setData(data);

        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(pricingRequest.getSource());
        SuccessfulMultiAvailability crsResponse = acrsMultiAvailabilityHttpEntity(pricingRequest, propertyIds, bodyParameterMulti, acrsVendor);

        return RoomAvailabilityTransformer.transformMultiAvailability(crsResponse, pricingRequest);
    }

    private SuccessfulMultiAvailability acrsMultiAvailabilityHttpEntity(AuroraPriceRequest pricingRequest, List<String> propertyIds, BodyParameterMulti bodyParameterMulti, String acrsVendor) {
        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(null, acrsVendor);
        httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());

        HttpEntity<BodyParameterMulti> request = new HttpEntity<>(bodyParameterMulti, httpHeaders);
        List<String> acrsPropertyIds = propertyIds.stream()
                .map(propertyId -> referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId))
                .collect(Collectors.toList());
        Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getAvailabilityVersion(), acrsProperties.getChainCode());
        uriParam.put(ServiceConstant.CRS_PROPERTY_CODE, String.join(",", acrsPropertyIds));
        uriParam.put(ServiceConstant.CRS_START_DATE, pricingRequest.getCheckInDate().toString());
        uriParam.put(ServiceConstant.CRS_VIEW, "minMaxOfferPerHotel");

        long noOfNights = Duration.between(pricingRequest.getCheckInDate().atStartOfDay(),
                pricingRequest.getCheckOutDate().atStartOfDay()).toDays();

        uriParam.put(ServiceConstant.CRS_DURATION, noOfNights + "D");
		logRequestHeaderAndBody(doTempInfoLogging,
				"Sending request to Multi Availability, Request headers {}:",
				"Sending request to Multi Availability, Request body {}: ",
				"Sending request to Multi Availability, Request query parameters: {}",
				CommonUtil.convertObjectToJsonString(request.getHeaders()),
				CommonUtil.convertObjectToJsonString(request.getBody()),
				uriParam
		);

        LocalDateTime start = LocalDateTime.now();
        setThreadContextBeforeAPICall("AcrsMultiAvailabilityReservation",
                urlProperties.getAcrsMultiAvailabilityReservation(), start);

        ResponseEntity<SuccessfulMultiAvailability> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsMultiAvailabilityReservation(), HttpMethod.POST,
                request, SuccessfulMultiAvailability.class, uriParam);

        setThreadContextAfterAPICall(start, HttpStatus.OK.toString());

		return logAndReturnCrsResponseBody(crsResponse, "Multi Availability", doTempInfoLogging);
    }


    @Override
    public List<AuroraPriceResponse> getCalendarPricesV2(AuroraPriceRequest pricingRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for getCalendarPricesV2: {}", CommonUtil.convertObjectToJsonString(pricingRequest));
        }

        return getCalendarPrices(pricingRequest);
    }

    private RequestedProductMulti createMultiProductFromRoomTypeId(String roomTypeId, List<RequestedGuestCounts> guestCounts,
            String propertyId, int roomNum) {
        RequestedProductMulti requestedProductMulti = new RequestedProductMulti();
        if ( null != roomTypeId ) {
            requestedProductMulti.setProductCode(referenceDataDAOHelper.retrieveRoomTypeDetail(propertyId, roomTypeId));
        }
        requestedProductMulti.setGuestCounts(guestCounts);
        requestedProductMulti.setQuantity((roomNum > 0) ? roomNum : 1);
        return requestedProductMulti;
    }
    @Override
    public List<AuroraPriceResponse> getIterableCalendarPricesV2(AuroraPriceRequest pricingRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for getIterableCalendarPricesV2: {}", CommonUtil.convertObjectToJsonString(pricingRequest));
        }

        return getIterableCalendarPrices(pricingRequest);
    }

    @Override
    public AuroraPricesResponse getRoomPricesV2(AuroraPriceRequest pricingRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for getRoomPricesV2: {}", CommonUtil.convertObjectToJsonString(pricingRequest));
        }

        AuroraPricesResponse auroraPricesResponse = new AuroraPricesResponse();
		auroraPricesResponse.setMrdPricing(pricingRequest.isEnableMrd());
		auroraPricesResponse.setAuroraPrices(getRoomPrices(pricingRequest));

        return auroraPricesResponse;
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

		@Override
		public void handleError(ClientHttpResponse httpResponse) throws IOException {
            try {
                LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
                long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
                ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
				ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
                log.info("Custom Dimensions updated after ACRS call");
            } catch (Exception e) {
                // Do nothing
            }

			// Handle error response
			String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
			log.error("Error received from Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(),
					response);
			ACRSSearchOffersErrorRes acrsSearchOffersErrorRes = ACRSErrorUtil.parseACRSSearchOffersErrorDetails(response);

			int httpStatusCode = (null == acrsSearchOffersErrorRes.getError()) ? 500 :
					acrsSearchOffersErrorRes.getError().getHttpStatus();
			if (httpStatusCode >= 500) {
				throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			} else if (httpStatusCode >= 400) {
				ErrorCode errorCode = getErrorCodeFromErrorDetail(acrsSearchOffersErrorRes);
				throw new BusinessException(errorCode);
			} else {
				throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			}
		}

		private ErrorCode getErrorCodeFromErrorDetail(ACRSSearchOffersErrorRes errorRes) {
			ACRSSearchOffersErrorDetails errorDetails = errorRes.getError();
			int errorCode = errorDetails.getCode();
			ErrorCode returnErrorCode;
			switch(errorCode) {
				case 50001:
					// Invalid number of products
				case 50002:
					// Product doesn't exist
				case 50003:
					// invalid product format
				case 50004:
					// group does not exist
				case 50005:
					// group not available
				case 50009:
					// invalid rate plan format
				case 50006:
					// invalid rate plan
					returnErrorCode = ErrorCode.OFFER_NOT_AVAILABLE;
					break;

				case 50037:
					// Property not active
				case 50010:
					// invalid property code
				case 50014:
					// invalid property format
					returnErrorCode = ErrorCode.INVALID_PROPERTY;
					break;

				case 50018:
					// Invalid checkin date
				case 50019:
					// Invalid Duration
				case 50027:
					// Invalid System Range (startDate)
					returnErrorCode = ErrorCode.INVALID_DATES;
					break;

				case 50021:
					// no product available for requested parameters
					Optional<ErrorCode> denialErrorCode = getErrorCodeFromDenials(errorRes);
					returnErrorCode = denialErrorCode.orElse(ErrorCode.UNABLE_TO_PRICE);
					break;

				case 50038:
					// Restriction on property
				case 50011:
					// no availability for property
				case 50013:
					// Closed restriction on property
				case 50012:
					// min and max los of the property are not respected
				case 50034:
					// Restriction on Inventory Type (RoomType)
				case 50008:
					// restriction on rate
				case 50022:
					// no availability but waitlist available
				case 50023:
					// No availability but there is for less occupancy
				case 50024:
					// Hurdle point not reached
				case 50025:
					// no availability for number of products requested
					returnErrorCode = ErrorCode.UNABLE_TO_PRICE;
					break;

				case 50026:
					// Dates outside inventory period
					returnErrorCode = ErrorCode.DATES_UNAVAILABLE;
					break;

				case 50016:
					// invalid number of guest adult
				case 50030:
					// Invalid number of guest adult (children but no adult)
					returnErrorCode = ErrorCode.INVALID_NUM_ADULTS;
					break;

				case 50068:
					// Invalid Guest Profile Data
					returnErrorCode = ErrorCode.INVALID_CUSTOMERID_OR_PROFILEID;
					break;

				case 50020:
					// Invalid channel
				case 50007:
					// Rates and themes/elements cannot be requested at the same time
				case 50028:
					// Unable to process
				default:
					returnErrorCode = ErrorCode.SYSTEM_ERROR;
			}
			return returnErrorCode;
		}

		private Optional<ErrorCode> getErrorCodeFromDenials(ACRSSearchOffersErrorRes errorRes) {
			Optional<ErrorCode> errorCodeOptional = Optional.empty();
			if (null != errorRes && null != errorRes.getData() &&
			!CollectionUtils.isEmpty(errorRes.getData().getDenials())) {
				// Grab the first Denial for ErrorCode
				Integer denialIdentity = errorRes.getData().getDenials().stream()
						.map(Denial::getIdentifier)
						.findFirst()
						.orElse(-1);
				if (35 == denialIdentity) {
					errorCodeOptional = Optional.of(ErrorCode.MLIFE_NUMBER_NOT_FOUND);
				}
			}
			return errorCodeOptional;
		}
	}

    @Override
    public List<AuroraPriceV3Response> getLOSBasedCalendarPrices(AuroraPriceV3Request request) {
        if (log.isDebugEnabled()) {
			log.debug("Incoming Request for getIterableCalendarPrices: {}", CommonUtil.convertObjectToJsonString(request));
		}
		String ratePlanCode = null;
		AuroraPriceV3Request localRequest = request;
		if (StringUtils.isNotEmpty(request.getProgramId())) {
			ratePlanCode = referenceDataDAOHelper.retrieveRatePlanDetail(request.getPropertyId(),
					request.getProgramId());
			boolean isPerpetualOffer = isPerpetualOfferRatePlan(ratePlanCode);
			if (isPerpetualOffer != request.isPerpetualOffer()) {
				localRequest = request.toBuilder()
						.isPerpetualOffer(isPerpetualOffer)
						.build();
			}
		}

    	final SuccessfulSingleAvailability successfulSingleAvailability = getSingleAvailResponseFromGroupSearch(localRequest);
		final List<AuroraPriceV3Response> singleAvailResponseList = successfulSingleAvailability != null ? RoomAvailabilityTransformer
				.transformV3(successfulSingleAvailability, localRequest.getNumRooms()) : null;

		final int lengthOfStay = localRequest.getTripLength();
		//CBSR-655 - ACRS returns results for End date while GSE does not, hence subtracting 1 day to keep the response in sync
		AuroraPriceV3Request calendarRequest = localRequest.toBuilder()
				.checkOutDate(localRequest.getCheckOutDate().minusDays(1))
				.build();
        final List<SuccessfulCalendarAvailability> crsResponseList = getCRSCalendarAvailabilityResponseList(calendarRequest, lengthOfStay);
        final Map<LocalDate, Double> basePrices = getCRSCalendarAvailabilityBasePrices(calendarRequest, crsResponseList, lengthOfStay);

        final String acrsPropertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(localRequest.getPropertyId());
        String computedBaseRatePlan = acrsProperties.getBaseRatePlan(acrsPropertyCode.toUpperCase());

        final List<AuroraPriceV3Response> calendarPricesResponseList = CalendarAvailabilityTransformer
				.transformCalendarAvailabilityV3(crsResponseList, basePrices, localRequest.getNumRooms(), lengthOfStay,
						localRequest.isPerpetualOffer(), computedBaseRatePlan, ratePlanCode);
		referenceDataDAOHelper.updateAcrsReferencesToGseV3(calendarPricesResponseList, localRequest.getPropertyId());
		return aggregateV3Response(singleAvailResponseList, calendarPricesResponseList);
    }

	@Override
	public List<AuroraPriceResponse> getGridAvailabilityForSoldOut(AuroraPriceRequest auroraPriceRequest) {
		final String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(auroraPriceRequest.getPropertyId());
		final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(auroraPriceRequest.getSource());
		List<AuroraPriceResponse> pricesResponseList = null;
		if(StringUtils.isNotBlank(auroraPriceRequest.getGroupCnfNumber())){
			GroupRetrieveResGroupBookingRetrieve retrieveGroupRes = retrieveGroup(auroraPriceRequest.getGroupCnfNumber(), propertyCode, auroraPriceRequest.getSource());
			// create List<AuroraPriceResponse> for SO rooms
			pricesResponseList = getGroupAvailabilityForSoRooms(auroraPriceRequest,retrieveGroupRes);
			referenceDataDAOHelper.updateAcrsReferencesToGse(pricesResponseList, true);
		}else {
			BodyParameterCalendar bodyParameterCalendar = createBodyParameterCalendar(propertyCode, auroraPriceRequest);
			final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
			final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(auroraPriceRequest.getPropertyId(), acrsVendor);
			httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());

			SuccessfulCalendarAvailability calRes = makeAcrsCalendarAvailabilitySearch(1, propertyCode,
					auroraPriceRequest.getCheckInDate(), auroraPriceRequest.getCheckOutDate(),
					bodyParameterCalendar, httpHeaders);

			pricesResponseList = CalendarAvailabilityTransformer
					.transformCalendarAvailabilityForSoldOut(calRes, auroraPriceRequest.getNumRooms(), auroraPriceRequest.isPerpetualOffer());
			referenceDataDAOHelper.updateAcrsReferencesToGse(pricesResponseList, false);
		}

		return pricesResponseList;
	}

	private List<AuroraPriceResponse> getGroupAvailabilityForSoRooms(AuroraPriceRequest auroraPriceRequest, GroupRetrieveResGroupBookingRetrieve retrieveGroupRes) {
		List<AuroraPriceResponse> priceResponseList = new ArrayList<>();
		GroupRetrieveResgroupBookingRetrieveData data = retrieveGroupRes.getData();
		if(null != data && null != data.getGroup()) {
			String groupCode = data.getGroup().getGroupIds().getGroupCode();
			List<String> soRoomCodes = auroraPriceRequest.getRoomTypeIds().stream().map(ACRSConversionUtil::getRoomCode).collect(Collectors.toList());
			List<GroupProductUseResGroupBookingRetrieve> productUsesForSoRooms = data.getGroup().getOffer().getProductUses().stream()
					.filter(pu -> soRoomCodes.contains(pu.getInventoryTypeCode()))
					.collect(Collectors.toList());
			LocalDate start = auroraPriceRequest.getCheckInDate();
			final LocalDate end = auroraPriceRequest.getCheckOutDate();
			while (!start.isAfter(end)) {
				LocalDate finalStart = start;
				List<GroupProductUseResGroupBookingRetrieve> matchedProductUses = productUsesForSoRooms.stream()
						.filter(pu -> null != pu.getCountersPerInventoryType())
						.filter(pu -> null != pu.getCountersPerInventoryType().getLiveCounters())
						.filter(pu -> CommonUtil.isDateInADateRange(finalStart, pu.getPeriod().getStart(), pu.getPeriod().getEnd()))
						.filter(pu -> pu.getCountersPerInventoryType().getLiveCounters().stream()
								.filter(lc -> null != lc.getAvailable())
								.filter(lc -> CommonUtil.isDateInADateRange(finalStart, lc.getPeriod().getStart(), lc.getPeriod().getEnd()))
								.anyMatch(lc -> lc.getAvailable() > auroraPriceRequest.getNumRooms())
						).collect(Collectors.toList());
				if (matchedProductUses.size() > 1) {
					log.warn("Unable to price date while processing group retrieve for grid pricing; Multiple product uses matched date: '{}'", finalStart);
				} else if(matchedProductUses.size() == 1) {
					String inventoryTypeCode = matchedProductUses.get(0).getInventoryTypeCode();
					priceResponseList.add(RoomAvailabilityTransformer.auroraPriceResFromGroupReterieveRes(auroraPriceRequest.getPropertyId(),finalStart, inventoryTypeCode, groupCode));
				}
				start = start.plusDays(1);
			}
		}
		return priceResponseList;
	}


	private boolean isPerpetualOfferRatePlan(String ratePlanCode){
		return ACRSConversionUtil.isPORatePlan(ratePlanCode);
	}

	private GroupRetrieveResGroupBookingRetrieve retrieveGroup(String groupCnfNumber,String hotelCode,String source){
		final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

		final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
		final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(hotelCode, acrsVendor);
		httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

		HttpEntity<?> request = new HttpEntity<>(httpHeaders);
		Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
				acrsProperties.getGroupSearchVersion(), acrsProperties.getChainCode(), false);
		uriParam.put(ServiceConstant.CRS_RESERVATION_CFNUMBER, groupCnfNumber);
		log.info("Sending request to ACRS Retrieve group By Confirmation Number.");
		log.info("Sending request to Retrieve group, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
		log.info("Sending request to Retrieve group, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
		log.info("Sending request to Retrieve group, Request query parameters: "+uriParam);
		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("AcrsRetrieveGroup",
				urlProperties.getAcrsGroupRetrieve(), start);

		ResponseEntity<GroupRetrieveResGroupBookingRetrieve> crsResponse = client.exchange(
				domainProperties.getCrs() + urlProperties.getAcrsGroupRetrieve(), HttpMethod.GET, request,
				GroupRetrieveResGroupBookingRetrieve.class, uriParam);

		setThreadContextAfterAPICall(start, String.valueOf(crsResponse.getStatusCodeValue()));

		return logAndReturnCrsResponseBody(crsResponse, "ACRS Group Retrieve", applicationProperties.isPermanentInfoLogEnabled());

	}

}
