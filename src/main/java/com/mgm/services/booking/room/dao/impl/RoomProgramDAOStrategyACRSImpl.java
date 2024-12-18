package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.CVSDao;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.LoyaltyDao;
import com.mgm.services.booking.room.dao.RoomProgramDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.crs.searchoffers.BodyParameterPricing;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.*;
import com.mgm.services.booking.room.service.cache.rediscache.service.ENRRatePlanRedisService;
import com.mgm.services.booking.room.transformer.GroupSearchTransformer;
import com.mgm.services.booking.room.transformer.RoomRatePlanResponseTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.JSonMapper;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation class for RoomProgram Dao which connects to other function apps and source systems to get relevant reference data.
 */
@Component
@Log4j2
public class RoomProgramDAOStrategyACRSImpl extends BaseAcrsDAO implements RoomProgramDAOStrategy {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected JSonMapper mapper = new JSonMapper();

    @Autowired
    @Setter
    private IDMSTokenDAO idmsTokenDAO;

    @Autowired(required=false)
    @Setter
    protected CVSDao cvsDao;

    @Autowired
    @Setter
    protected LoyaltyDao loyaltyDao;

    private SecretsProperties secretsProperties;

    @Autowired
    @Setter
    private GroupSearchDAOStrategyACRSImpl groupSearch;

    private  boolean  doTempInfoLogging;

    @Autowired
    private ENRRatePlanRedisService enrRatePlanRedisService;

    @Autowired
    private ReferenceDataDAOHelper refDataDAOHelper;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     *
     * @param urlProperties         URL Properties
     * @param domainProperties      Domain Properties
     * @param applicationProperties Application Properties
     * @param builder               Spring's auto-configured rest template builder
     * @throws SSLException Throws SSL Exception
     */
    protected RoomProgramDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                             ApplicationProperties applicationProperties, AcrsProperties acrsProperties, RestTemplateBuilder builder, ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, SecretsProperties secretsProperties) throws SSLException {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties, CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getEnrConnectionPerRouteDaoImpl(),
                applicationProperties.getEnrMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeoutENR(),
                applicationProperties.getReadTimeOutENR(),
                applicationProperties.getSocketTimeOutENR(),
                1,
                applicationProperties.getEnrRestTTL()), referenceDataDAOHelper, acrsOAuthTokenDAOImpl);
        this.secretsProperties = secretsProperties;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.doTempInfoLogging = CommonUtil.isTempLogEnabled(secretsProperties.getSecretValue(applicationProperties.getTempInfoLogEnabled()));
    }
    
    @Override
    public ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramRequestDTO request){
        CVSResponse customerValues = null;
        if (StringUtils.isNotEmpty(request.getMlifeNumber())) {
            customerValues = cvsDao.getCustomerValues(request.getMlifeNumber());
        }
        ApplicableProgramsResponse applicableProgramsResponse = null;
        ENRRatePlanSearchResponse[] rateplanArray = new ENRRatePlanSearchResponse[1];

        if(isENRRedisIntegrationEnabled()) {
        	rateplanArray = getApplicableProgramsFromRedis(request);
        }else {
        	rateplanArray = getApplicableProgramsFromENR(request);
        }
        applicableProgramsResponse = RoomRatePlanResponseTransformer.getRoomRatePlanResponse(
                    customerValues, rateplanArray, request.getMlifeNumber());
        updateProgramApplicableGuid(applicableProgramsResponse);
        return applicableProgramsResponse;
    }

  
    private boolean isENRRedisIntegrationEnabled() {
    	String checkEnrRedisEnabledValue = secretsProperties.getSecretValue(String.format(applicationProperties.getRbsENRRedisIntegrationEnabled(),applicationProperties.getRbsEnv()));
    	return StringUtils.isNotBlank(checkEnrRedisEnabledValue) && Boolean.parseBoolean(checkEnrRedisEnabledValue);
	}

	public ENRRatePlanSearchResponse[] getApplicableProgramsFromRedis(ApplicableProgramRequestDTO request) {
    	// get applicable programs from redis
    	try {
    		final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
            return enrRatePlanRedisService.searchRatePlans(request,vendorCode);
		} catch (Exception e) {
			return getApplicableProgramsFromENR(request);
		}
            
    }

    private ENRRatePlanSearchResponse[] getApplicableProgramsFromENR(ApplicableProgramRequestDTO request){

        final String uri = createURI(true, true, null, null, null, null, request.getPropertyId(), request.getRoomTypeId(), request.getBookDate(), request.getTravelDate(),
                request.getCheckInDate(), request.getCheckOutDate());

        final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
        log.debug("Sending Get Applicable Programs to ENR Service : {}, source : {}", uri, vendorCode);

        final ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = client.exchange(
                domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, null)), ENRRatePlanSearchResponse[].class, createURIParams());

        logAndReturnEnrResponseBody(enrSearchResponse, "Applicable Programs", doTempInfoLogging);

        return enrSearchResponse.getBody();
    }
    
    public CustomerOfferResponse getCustomerOffers(CustomerOffersRequestDTO request) {
       final String uri = createURI(null, false, null,null, null, null, request.getPropertyId(), null, new Date(), new Date(), null, null);

       final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);

       Level logLevel = (doTempInfoLogging) ? Level.INFO : Level.DEBUG;

       log.log(logLevel, "Sending Get Customer Offers to ENR Service : {}, source : {}", uri, vendorCode);

       final ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = client.exchange(
        		domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, null)), ENRRatePlanSearchResponse[].class, createURIParams());

       logAndReturnEnrResponseBody(enrSearchResponse, "Customer Offers", doTempInfoLogging);
        
       CustomerOfferResponse customerOfferResponse = null;
       final int status = enrSearchResponse.getStatusCodeValue();
       if (status >= 200 && status < 300) {
           customerOfferResponse = RoomRatePlanResponseTransformer.getCustomerOfferRatePlanResponse(enrSearchResponse.getBody(), request.getCustomerId(), request.getMlifeNumber());
           updateCustomerOfferGuid(customerOfferResponse);
       }
       return customerOfferResponse;
	}

    private void updateCustomerOfferGuid(CustomerOfferResponse customerOfferResponse) {
        if (null != customerOfferResponse) {

            // Iterating here to set the GSE/ACRS GUID for the rate plans.
            final List<CustomerOfferDetail> filteredOffers = customerOfferResponse.getOffers().stream().filter(
                    offer -> {
                        final String ratePlanCode = ACRSConversionUtil.getRatePlanCode(offer.getId());
                        if (!StringUtils.isAnyEmpty(offer.getPropertyId(), ratePlanCode)) {
                            String ratePlanGuid = referenceDataDAOHelper.retrieveRatePlanDetail(offer.getPropertyId(), ratePlanCode, false);
                            if (null != ratePlanGuid) {
                                offer.setId(ratePlanGuid);
                                return true;
                            }
                        }
                        return false;
                    }).collect(Collectors.toList());
            customerOfferResponse.setOffers(filteredOffers);
        }
    }

    private void updateProgramApplicableGuid(ApplicableProgramsResponse applicableProgramsResponse) {
        if (null != applicableProgramsResponse) {

            // Iterating here to set the GSE/ACRS GUID for the rate plans.
            final List<ApplicableRoomProgram> filteredPrograms = applicableProgramsResponse.getPrograms().stream().filter(
                    program -> {
                        final String ratePlanCode = ACRSConversionUtil.getRatePlanCode(program.getId());
                        if (!StringUtils.isAnyEmpty(program.getPropertyId(), ratePlanCode)) {
                            final String ratePlanGuid = referenceDataDAOHelper.retrieveRatePlanDetail(program.getPropertyId(), ratePlanCode, false);
                            if (null != ratePlanGuid) {
                                program.setId(ratePlanGuid);
                                return true;
                            }
                        }
                        return false;
                    }
            ).collect(Collectors.toList());

            applicableProgramsResponse.setPrograms(filteredPrograms);
            List<String> filteredProgramIds = filteredPrograms.stream()
                    .map(ApplicableRoomProgram::getId)
                    .distinct()
                    .collect(Collectors.toList());
            applicableProgramsResponse.setProgramIds(filteredProgramIds);
        }
    }

    @Override
    public String getProgramByPromoCode(String propertyId, String promoCode) {
    	String programId = null;
    	try {
    		
    		if(isENRRedisIntegrationEnabled()) {
    			programId = getProgramByPromoCodeRedis(propertyId, promoCode);
    		}else {
    			programId = getProgramByPromoCodeENR(propertyId,promoCode);
    		}
    	    
    	} catch (Exception e) {
    		// Ignoring Data not found error
    		log.warn("Unable to find ENR ratePlan response {}", ExceptionUtils.getStackTrace(e));
    	}

    	return programId;
    }

    private String getProgramByPromoCodeRedis(String propertyId, String promoCode) {
    	try {
    		String source = CommonUtil.getSourceHeader();
    		final String vendorCode = referenceDataDAOHelper.getAcrsVendor(source, true);
            return enrRatePlanRedisService.searchPromoByCode(propertyId, promoCode,vendorCode);
		} catch (Exception e) {
			return getProgramByPromoCodeENR(propertyId, promoCode);
		}
	}

	private String getProgramByPromoCodeENR(String propertyId, String promoCode) {
		String programId = null;
		final String uri = createURI(true, true, SEARCH_TYPE_PROMO_VALUE, promoCode, null, null, propertyId, null, null, null, null, null);
    	
		String channel = CommonUtil.getChannelHeader();
		String source = CommonUtil.getSourceHeader();
		final String vendorCode = referenceDataDAOHelper.getAcrsVendor(source, true);
		log.debug("Sending Validate Program V2 to ENR Service : {}, source : {}", uri, vendorCode);

		ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = null;

		enrSearchResponse = client.exchange(
				domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, channel)),
				ENRRatePlanSearchResponse[].class, createURIParams());
		
		if(enrSearchResponse!=null && enrSearchResponse.getBody()!=null && enrSearchResponse.getBody().length>0) {
			programId = enrSearchResponse.getBody()[0].getRatePlanId();
		}

        logAndReturnEnrResponseBody(enrSearchResponse, "Program by PromoCode", doTempInfoLogging);
        return programId;
	}

	@Override
    public List<RoomProgram> getRoomOffers(RoomProgramRequest offersRequest) {
        //Dont need to implement as this is used as v1 program apis with GSE only
        throw new NotImplementedException("RoomProgramDAO>getRoomOffers method is not available in ACRS Impl.");
    }

    @Override
    public RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest validateRequest) {
        //TODO: Fill me when the functionality will be available from ACRS
        final RoomProgramValidateResponse response = new RoomProgramValidateResponse();
        response.setEligible(true);
        return response;
    }

    @Override
    public RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest request) {
        ENRRatePlanSearchResponse[] ratePlans = null;
        // Search RatePlan ENR if not a group code and (promo activated or promo not present)
        if (!ACRSConversionUtil.isAcrsGroupCodeGuid(request.getProgramId()) &&
                (acrsProperties.isPromoFeedActivated() || StringUtils.isEmpty(request.getPromo()))) {
            String programId = request.getProgramId();
            String propertyCode = null;
            if (StringUtils.isNotEmpty(request.getPropertyId())) {
                propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
            }
            if (StringUtils.isEmpty(propertyCode) && ACRSConversionUtil.isAcrsRatePlanGuid(programId)) {
                propertyCode = ACRSConversionUtil.getPropertyCode(request.getProgramId());
            }
            if(StringUtils.isNotEmpty(request.getPromoCode())) {
                String updatedPromoCode = CommonUtil.normalizePromoCode(request.getPromoCode(), request.getPropertyId());
                programId = ACRSConversionUtil.createRatePlanCodeGuid(updatedPromoCode, propertyCode);
            }
            final List<String> ratePlanIds = StringUtils.isNotEmpty(programId) ? Arrays.asList(programId) : Arrays.asList();
            final String uri = createURI(null, true, null, request.getPromo(), ratePlanIds,
                    null, propertyCode, null, null, null, null, null);

            // fetch rateplans from redis or enr on basis of flag
            if(isENRRedisIntegrationEnabled()) {
            	ratePlans = validateProgramV2FromRedis(request, ratePlanIds, propertyCode, uri);
            }else {
            	ratePlans = validateProgramV2FromENR(request, ratePlanIds, propertyCode, uri);
            }
            
        }

        final RoomProgramValidateResponse response = RoomRatePlanResponseTransformer.getValidateProgramResponse(request, ratePlans, acrsProperties.isPromoFeedActivated());
        if(ACRSConversionUtil.isPatronPromo(request.getPromoCode()) && !request.isModifyFlow()) {
            validatePatronEligibility(request, response);
        }

        return response;
    }
    
    private ENRRatePlanSearchResponse[] validateProgramV2FromENR(RoomProgramValidateRequest request, List<String> ratePlanIds, String propertyCode, String uri) {
    	ENRRatePlanSearchResponse[] ratePlansENR = null;
    	
    	final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
        log.debug("Sending Validate Program V2 to ENR Service : {}, source : {}", uri, vendorCode);

        final ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = client.exchange(
        		domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, request.getChannel())),
                ENRRatePlanSearchResponse[].class, createURIParams());

        logAndReturnEnrResponseBody(enrSearchResponse, "Validate Program", doTempInfoLogging);

        final int status = enrSearchResponse.getStatusCodeValue();
        if (status >= 200 && status < 300) {
        	ratePlansENR = enrSearchResponse.getBody();
        }
    	
    	return ratePlansENR;
    }
    
    private ENRRatePlanSearchResponse[] validateProgramV2FromRedis(RoomProgramValidateRequest request, List<String> ratePlanIds, String propertyCode, String uri) {
    	try {
    		final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
            return enrRatePlanRedisService.searchRatePlansToValidate(request,vendorCode, ratePlanIds, propertyCode);
		} catch (Exception e) {
			return validateProgramV2FromENR(request, ratePlanIds, propertyCode, uri);
		}
    }

    private void validatePatronEligibility(RoomProgramValidateRequest request, RoomProgramValidateResponse response) {

        final String aCrsPromo = response.getPromo();
        if (response.isPatronProgram() && StringUtils.isNotEmpty(aCrsPromo)) {
            boolean isEligible = false;
            final String mlifeNumber = request.getMlifeNumber();
            if (StringUtils.isNotEmpty(mlifeNumber)) {
                final List<CustomerPromotion> patronPromos = loyaltyDao.getPlayerPromos(mlifeNumber);
                Optional<CustomerPromotion> patronFound = patronPromos.stream()
                        .filter(promo ->
                                StringUtils.equalsIgnoreCase(promo.getPromoId(), String.valueOf(ACRSConversionUtil.getPatronPromoId(aCrsPromo)))
                                        && StringUtils.equalsIgnoreCase(promo.getPropertyId(), response.getPropertyId()))
                        .findAny();
                if (patronFound.isPresent()) {
                    isEligible = response.isEligible();
                }
            }
            response.setEligible(isEligible);
        }

    }

    private Map<String, String> createURIParams() {
        final Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        return uriParams;
    }

    private String createURI(Boolean searchActive, boolean isPromoSearch, String promoSearchType, String promoCode,
                             List<String> ratePlanIds, String ratePlanCode, String propertyId, String roomTypeId,
                             Date bookDate, Date travelDate, Date checkInDate, Date checkOutDate) {

        final String urlPostfix = "-POSTFIX";
        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(urlPostfix);

        if (null != searchActive) {
            uriBuilder.queryParam(ACTIVE_ATTRIBUTE, searchActive);
        }

        if (StringUtils.isNotEmpty(promoCode)) {
            uriBuilder.queryParam(PROMOS_ATTRIBUTE, promoCode);
        }

        if (StringUtils.isNotEmpty(promoSearchType)) {
            uriBuilder.queryParam(PROMOS_SEARCH_TYPE, promoSearchType);
        }

        if (CollectionUtils.isNotEmpty(ratePlanIds)) {
            uriBuilder.queryParam(IDS_ATTRIBUTE, ratePlanIds.stream().collect(Collectors.joining(",")));
        }

        if (StringUtils.isNotEmpty(ratePlanCode)) {
            uriBuilder.queryParam(CODE_ATTRIBUTE, ratePlanCode);
        }

        if (StringUtils.isNotEmpty(propertyId)) {
            final String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId);
            uriBuilder.queryParam(PROPERTY_CODE_ATTRIBUTE, propertyCode);
            if (StringUtils.isNotEmpty(roomTypeId)) {
                final String roomTypeCode = referenceDataDAOHelper.retrieveRoomTypeDetail(propertyCode, roomTypeId);
                if (null != roomTypeCode) {
                    uriBuilder.queryParam(ROOM_TYPE_ATTRIBUTE, roomTypeCode);
                }
            }
        }

        if (null != travelDate && (checkInDate == null || travelDate.before(checkInDate))) {
                // If travel date is before than than check-in date then consider travel date to validate instead
                checkInDate = travelDate;
            
        }
        if (null != checkInDate) {
            uriBuilder.queryParam(TRAVEL_START_DATE_ATTRIBUTE, dateFormat.format(checkInDate));
        }

        if (null != checkOutDate) {
            uriBuilder.queryParam(TRAVEL_END_DATE_ATTRIBUTE, dateFormat.format(checkOutDate));
        }

        if (null != bookDate) {
            uriBuilder.queryParam(BOOK_DATE_ATTRIBUTE, dateFormat.format(bookDate));
        }

        return uriBuilder.toUriString().replace(urlPostfix, (isPromoSearch || StringUtils.isNotEmpty(promoCode)) ?
                urlProperties.getEnrPromoChannelRatePlanSearch() : urlProperties.getEnrChannelRatePlanSearch());
    }

    public HttpHeaders createHeaders(String source, String channel) {
        final String authToken = idmsTokenDAO.generateToken().getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+authToken);
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        if (StringUtils.isNotEmpty(source)) {
            headers.set(ServiceConstant.X_MGM_SOURCE, source);
        }
        if (StringUtils.isNotEmpty(channel)) {
            headers.set(ServiceConstant.X_MGM_CHANNEL, channel);
        }
        return headers;
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            final String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received ENR Rate plan Search: status code: {}, header: {}, body: {}",
                    httpResponse.getStatusCode().value(), httpResponse.getHeaders().toString(), response);
            ACRSErrorDetails acrsError = ACRSErrorUtil.getACRSErrorDetailsFromACRSErrorRes(response);
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }else if (httpResponse.getStatusCode().value() == 404) {
                throw new BusinessException(ErrorCode.OFFER_NOT_AVAILABLE);
            } else if (httpResponse.getStatusCode().value() == 400) {
                throw new BusinessException(ErrorCode.INVALID_DATES);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION,acrsError.getTitle());

            }
        }
    }

    @Override
    public List<RoomProgramBasic> findProgramsByRatePlanCode(String ratePlanCode, String source) {
        return StringUtils.isNotEmpty(ratePlanCode) ?
                getRoomBasicPrograms(createURI(null, false, null,null,null, ratePlanCode,null, null, null,
                        null,null, null), source, ratePlanCode) : Collections.emptyList();
    }

    @Override
    public List<RoomProgramBasic> findProgramsIfSegment(String programId, String source) {
        final String ratePlanCode = ACRSConversionUtil.getRatePlanCode(programId);
        return StringUtils.isNotEmpty(ratePlanCode) ?
                getRoomBasicPrograms(createURI(null, false, null,null, null, ratePlanCode,
                null, null, null, null,null, null), source, ratePlanCode) : Collections.emptyList();
    }

    @Override
    public List<RoomProgramBasic> findProgramsByGroupCode(String groupCode,LocalDate checkInDate, LocalDate checkOutDate, String source) {
        GroupSearchV2Request groupSearchReq = new GroupSearchV2Request();
        groupSearchReq.setSource(source);
        groupSearchReq.setStartDate(checkInDate.toString());
        groupSearchReq.setEndDate(checkOutDate.toString());
        groupSearchReq.setId(groupCode);
        
         List<GroupSearchV2Response> grpSearchRes = groupSearch.searchGroup(groupSearchReq);
         log.info("Group search response : {}",CommonUtil.convertObjectToJsonString(grpSearchRes));
         updateGroupSearchResponse(grpSearchRes);
         log.info("Group search response after update: {}",CommonUtil.convertObjectToJsonString(grpSearchRes));
         return grpSearchRes.stream().map(GroupSearchTransformer::transform).collect(Collectors.toList());
     }

	private List<RoomProgramBasic> getRoomBasicPrograms(String uri, String source, String ratePlanCode) {

		ENRRatePlanSearchResponse[] ratePlans = null;

		final Map<String, RoomProgramBasic> programs = new HashMap<>();
		log.debug("Sending Programs by Rate Plan Code to ENR Service : {}", uri);

		try {
			if (isENRRedisIntegrationEnabled()) {
				ratePlans = getRoomBasicProgramsRedis(uri, source, ratePlanCode);
			} else {
				ratePlans = getRoomBasicProgramsENR(uri, source, ratePlanCode);
			}

			if (ArrayUtils.isNotEmpty(ratePlans)) {
				Arrays.stream(ratePlans).forEach(rp -> {
					final String id = rp.getRatePlanId();
					final String propertyId = rp.getPropertyId();
					// Only ACRS enabled properties ratePlans will be considered
					if (referenceDataDAOHelper.isPropertyManagedByAcrs(propertyId)
							&& !isMyVegasRatePlanCheck(rp.getRateCode(), rp.getRatePlanTags(), ratePlanCode)) {
						programs.put(rp.getRatePlanId(),
								new RoomProgramBasic(id, propertyId, rp.getRateCode(), rp.getIsActive(),
										rp.getBookableOnline(),
										ReservationUtil.convertStringDateTimeToDate(rp.getBookingStartDate()),
										ReservationUtil.convertStringDateTimeToDate(rp.getBookingEndDate()),
										ReservationUtil.convertStringDateTimeToDate(rp.getTravelStartDate()),
										ReservationUtil.convertStringDateTimeToDate(rp.getTravelEndDate()),
										CollectionUtils.isNotEmpty(rp.getRatePlanTags())
												? rp.getRatePlanTags().toArray(new String[0])
												: null,
										null));
					}
				});
			}

		} catch (BusinessException e) {
			// Returning empty ratePlan List if in case its not found.
			log.warn("Cant find the ratePlan with URI {} | Error : {}", uri, ExceptionUtils.getStackTrace(e));
		}

		// Return if multiple ratePlans are present so that resort pricing can use them
		return !programs.isEmpty() ? new ArrayList<>(programs.values()) : Collections.emptyList();
	}

    private ENRRatePlanSearchResponse[] getRoomBasicProgramsENR(String uri, String source, String ratePlanCode) {
    	ENRRatePlanSearchResponse[] ratePlansENR = null;
    	final String vendorCode = referenceDataDAOHelper.getAcrsVendor(source, true);
        log.debug("Sending Validate Program V2 to ENR Service : {}, source : {}", uri, vendorCode);

        final ResponseEntity<ENRRatePlanSearchResponse[]> response = client.exchange(
        		domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, null)), ENRRatePlanSearchResponse[].class, createURIParams());

        logAndReturnEnrResponseBody(response, "Room Basic Programs", doTempInfoLogging);

        final int status = response.getStatusCodeValue();
        if (status >= 200 && status < 300) {
        	ratePlansENR = response.getBody();
        }
		return ratePlansENR;
	}

	private ENRRatePlanSearchResponse[] getRoomBasicProgramsRedis(String uri, String source, String ratePlanCode) {
		try {
    		final String vendorCode = referenceDataDAOHelper.getAcrsVendor(source, true);
            return enrRatePlanRedisService.searchRatePlanByCode(ratePlanCode, vendorCode);
		} catch (Exception e) {
			return getRoomBasicProgramsENR(uri, source, ratePlanCode);
		}
	}

	//CBSR-1522
    private boolean isMyVegasRatePlanCheck(String rateCode, List<String> ratePlanTags, String requestRatePlanCode) {
        if ((StringUtils.isNotEmpty(rateCode) && rateCode.startsWith(ServiceConstant.ACRS_MYVEGAS_RATEPLAN)) ||
                (CollectionUtils.isNotEmpty(ratePlanTags) && ratePlanTags.contains(ServiceConstant.ACRS_MYVEGAS_RATEPLAN))) {
            return !rateCode.equalsIgnoreCase(requestRatePlanCode);
        } else {
            return false;
        }
    }

    @Override
    public String getRatePlanCodeByProgramId(String programId) {
        return ACRSConversionUtil.getRatePlanCode(programId);
    }

    @Override
    public RoomProgramsResponseDTO getRoomPrograms(RoomProgramsRequestDTO request, List<CustomerPromotion> patronOffers, CVSResponse customerValues) {
        RoomProgramsResponseDTO roomProgramsResponseDTO = new RoomProgramsResponseDTO();
        if(StringUtils.isNotEmpty(request.getPropertyId())){
            roomProgramsResponseDTO = getRoomENRPrograms(request,patronOffers,customerValues);
        }else{
            // get all ACRS properties
            String acrsEnabledProperties = secretsProperties.getSecretValue(acrsProperties.getAcrsPropertyListSecretKey());

            if(StringUtils.isNotEmpty(acrsEnabledProperties)){
                List<RoomProgramsRequestDTO> offerRequestList = Arrays.stream(acrsEnabledProperties.split(","))
                    .map(propertyCode -> createRequest(request, propertyCode))
                    .collect(Collectors.toList());
               ExecutorService executor =  Executors.newFixedThreadPool(offerRequestList.size());
                //parallel call for each ACRS properties
               try {

                   CVSResponse finalCustomerValues = customerValues;
                   List<CompletableFuture<RoomProgramsResponseDTO>> roomProgramsResponseFutures = offerRequestList.stream()
                           .map(req -> CompletableFuture.supplyAsync(
                                   () -> getRoomENRPrograms(req, patronOffers, finalCustomerValues), executor)
                           .exceptionally(ex -> {
                               log.warn("getRoomENRPrograms failed for ACRS property {} with exception: ", req.getPropertyId(), ex);
                               return null;
                           }))
                           .collect(Collectors.toList());
                   List<RoomProgramsResponseDTO> roomProgramsResponseDTOList = roomProgramsResponseFutures.stream()
                           .map(CompletableFuture::join)
                           .filter(Objects::nonNull)
                           .collect(Collectors.toList());
                   roomProgramsResponseDTO = mergeAllOffersRefactor(roomProgramsResponseDTOList);
               }finally {
                   executor.shutdown();
               }
            }
        }
        return roomProgramsResponseDTO;
    }

    private static RoomProgramsResponseDTO mergeAllOffersRefactor(List<RoomProgramsResponseDTO> roomProgramsResponseDTOList) {
        RoomProgramsResponseDTO response = new RoomProgramsResponseDTO();
        response.setPoPrograms(roomProgramsResponseDTOList.stream()
                .flatMap(res -> res.getPoPrograms().stream())
                .collect(Collectors.toList()));
        response.setPatronPrograms(roomProgramsResponseDTOList.stream()
                .flatMap(res -> res.getPatronPrograms().stream())
                .collect(Collectors.toList()));
        response.setIceChannelPrograms(roomProgramsResponseDTOList.stream()
                .flatMap(res -> res.getIceChannelPrograms().stream())
                .collect(Collectors.toList()));
        return response;
    }

    private RoomProgramsRequestDTO createRequest(RoomProgramsRequestDTO request, String propertyCode){
        return new RoomProgramsRequestDTO(request.getSource(),
                request.getChannel(),propertyCode,request.getCustomerId(),request.getMlifeNumber(),
                request.isPerpetualPricing(),request.isResortPricing());
    }


    private RoomProgramsResponseDTO getRoomENRPrograms(RoomProgramsRequestDTO request, List<CustomerPromotion> patronOffers,  CVSResponse customerValues){
    	ENRRatePlanSearchResponse[] rateplanArray = new ENRRatePlanSearchResponse[1];
    	
    	if(isENRRedisIntegrationEnabled()) {
    		rateplanArray = getRoomProgramsFromRedis(request);
    	}else {
    		rateplanArray = getRoomENRProgramsFromENR(request);
		}
    	RoomProgramsResponseDTO response = new RoomProgramsResponseDTO();
    	if(ArrayUtils.isNotEmpty(rateplanArray)) {
    		response = RoomRatePlanResponseTransformer.getCustomerOffersResponse(rateplanArray,
                    request, customerValues, patronOffers);
    	}
        
		return response;
    }

    private ENRRatePlanSearchResponse[] getRoomENRProgramsFromENR(RoomProgramsRequestDTO request) {
    	// Retrieve Rate plan and Promo data
        final String uri = createURI(null, true, null,null, null, null, request.getPropertyId(), null, new Date(), null,null, null);

        final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
        log.debug("Sending Get Customer Offers to ENR Service : {}, source : {}", uri, vendorCode);

        final ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = client.exchange(
                domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, null)), ENRRatePlanSearchResponse[].class, createURIParams());

        logAndReturnEnrResponseBody(enrSearchResponse, "Room ENR Programs", doTempInfoLogging);

        ENRRatePlanSearchResponse[] rateplanArrayENR = new ENRRatePlanSearchResponse[1];
        
        final int status = enrSearchResponse.getStatusCodeValue();
        if (status >= 200 && status < 300) {
        	rateplanArrayENR =  enrSearchResponse.getBody();
        }
        return rateplanArrayENR;
	}

	private ENRRatePlanSearchResponse[] getRoomProgramsFromRedis(RoomProgramsRequestDTO request) {
		try {
			final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
	        return enrRatePlanRedisService.searchRatePlansRoomProgram(request,vendorCode);
		} catch (Exception e) {
			return getRoomENRProgramsFromENR(request);
		}
	}

	@Override
    public List<RoomOfferDetails> getRatePlanById(RoomProgramV2Request request) {

        final List<RoomOfferDetails> allRatePlans = new ArrayList<>();
        final List<String> programIds = new ArrayList<>();
        HashSet<String> propertyCodes = new HashSet<>();
        for (String ratePlanId : request.getProgramIds()) {
            if (ACRSConversionUtil.isAcrsGroupCodeGuid(ratePlanId)) {
                log.warn("Group code `{}` found. Group codes cannot be looked up via ENR.", ratePlanId);
            } else {
                programIds.add(ratePlanId);
                if (ACRSConversionUtil.isAcrsRatePlanGuid(ratePlanId)) {
                    String propertyCode = ACRSConversionUtil.getPropertyCode(ratePlanId);
                    propertyCodes.add(propertyCode);
                }
            }
        }
        if(!programIds.isEmpty()) {
            String propertyCode = propertyCodes.size() == 1 ? propertyCodes.stream().findFirst().get() : null;
            ENRRatePlanSearchResponse[] rateplanArray = new ENRRatePlanSearchResponse[1];
        	if(isENRRedisIntegrationEnabled()) {
        		rateplanArray = getRatePlanByIdFromRedis(request, propertyCode, programIds);
        	}else {
        		rateplanArray = getRatePlanByIdFromENR(request, propertyCode, programIds);
    		}
            final ENRRatePlanSearchResponse[] ratePlans = rateplanArray;
            allRatePlans.addAll(RoomRatePlanResponseTransformer.buildRoomProgramDetail(ratePlans, request.getProgramIds()));
        }
        return allRatePlans;
    }

    private ENRRatePlanSearchResponse[] getRatePlanByIdFromENR(RoomProgramV2Request request, String propertyCode, List<String> programIds) {
    	
    	final String uri = createURI(null, request.isPromoSearch(), null, null, programIds,
                null, propertyCode, null, null, null, null, null);
    	
    	final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
        log.debug("Sending Validate Program V2 to ENR Service : {}, source : {}", uri, vendorCode);

        final ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = client.exchange(
                domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, request.getChannel())),
                ENRRatePlanSearchResponse[].class, createURIParams());

        logAndReturnEnrResponseBody(enrSearchResponse, "Rate Plan Search by Id", doTempInfoLogging);
		return enrSearchResponse.getBody();
	}

	private ENRRatePlanSearchResponse[] getRatePlanByIdFromRedis(RoomProgramV2Request request, String propertyCode, List<String> programIds) {
		try {
			final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
	        return enrRatePlanRedisService.searchRatePlanById(request,vendorCode, propertyCode, programIds);
		} catch (Exception e) {
			return getRatePlanByIdFromENR(request, propertyCode, programIds);
		}
	}

	@Override
    public boolean isSegmentGUID(String programId) {
        // dummy implementation, will never be called
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.RoomProgramDAOStrategy#isProgramPO(java
     * .lang.String)
     */
    @Override
    public boolean isProgramPO(String programId) {
        return ACRSConversionUtil.isPORatePlan(ACRSConversionUtil.getRatePlanCode(programId));
    }

    /**
     * This method adds up all combinations of programIds to promo for GSE
     * @param request
     * @return
     */
    @Override
    public Map<String, String> getProgramPromoAssociation(RoomProgramPromoAssociationRequest request) {

    	ENRRatePlanSearchResponse[] ratePlans = null;
    	
        final Map<String, String> associationMap = new HashMap<>();
        final List<String> programIds = request.getProgramIds();

        if (CollectionUtils.isNotEmpty(programIds)) {
            String propertyCode = null;
            if (StringUtils.isNotEmpty(request.getPropertyId())) {
                propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
            }
            
            if(isENRRedisIntegrationEnabled()) {
            	ratePlans = getProgramPromoAssociationRedis(programIds,propertyCode,request,SEARCH_TYPE_PROMO_VALUE);
            }else {
            	ratePlans = getProgramPromoAssociationENR(programIds,propertyCode,request);
            }

            associationMap.putAll(RoomRatePlanResponseTransformer.transformProgramToPromoAssociation(programIds, ratePlans));
        }

        return associationMap;
    }

    private ENRRatePlanSearchResponse[] getProgramPromoAssociationRedis(List<String> programIds, String propertyCode,
			RoomProgramPromoAssociationRequest request, String searchType) {
    	 try {
     		final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
             return enrRatePlanRedisService.searchPromoRatePlans(request,vendorCode, programIds, propertyCode, searchType);
 		} catch (Exception e) {
 			return getProgramPromoAssociationENR(programIds,propertyCode,request);
 		}
	}

	private ENRRatePlanSearchResponse[] getProgramPromoAssociationENR(List<String> programIds, String propertyCode,
			RoomProgramPromoAssociationRequest request) {
		ENRRatePlanSearchResponse[] ratePlansENR = null;
		final String uri = createURI(null, true, SEARCH_TYPE_PROMO_VALUE,
				request.getPromo(), programIds, null, propertyCode,
                null, null, null, null, null);

        final String vendorCode = referenceDataDAOHelper.getAcrsVendor(request.getSource(), true);
        log.debug("Sending Validate Program V2 to ENR Service : {}, source : {}", uri, vendorCode);

        ResponseEntity<ENRRatePlanSearchResponse[]> enrSearchResponse = null;
        try {
            enrSearchResponse = client.exchange(
                    domainProperties.getEnrSearch() + uri, HttpMethod.GET, new HttpEntity<BodyParameterPricing>(createHeaders(vendorCode, request.getChannel())),
                    ENRRatePlanSearchResponse[].class, createURIParams());

            logAndReturnEnrResponseBody(enrSearchResponse, "Program Promo Association", doTempInfoLogging);
        } catch (Exception e) {
            // Ignoring Data not found error
            log.warn("Unable to find ENR ratePlan for promo {} with exception: ", request.getPromo(), e);
        }
        if(null != enrSearchResponse) {
        	ratePlansENR = enrSearchResponse.getBody();
        }
		return ratePlansENR;
	}

	private void updateGroupSearchResponse(List<GroupSearchV2Response> grpSearchRes) {
        List<GroupSearchV2Response> pseudoPropertyResp = new ArrayList<>();
        for (GroupSearchV2Response response : grpSearchRes) {
            if (StringUtils.isNotEmpty(response.getPropertyId())) {
                String pseudoPropertyCode = referenceDataDAOHelper.getPseudoPropertyCode(response.getPropertyId());
                if (StringUtils.isNotEmpty(pseudoPropertyCode)) {
                    log.info("Pseudo property Code {} found for property: {}", pseudoPropertyCode, response.getPropertyId());
                    String pseudoPropertyId = referenceDataDAOHelper.retrieveGsePropertyID(pseudoPropertyCode);
                    GroupSearchV2Response pseudoPropResp = GroupSearchTransformer.pseudoPropertyGroupResponse(response, pseudoPropertyCode, pseudoPropertyId);
                    pseudoPropertyResp.add(pseudoPropResp);
                } else {
                    log.info("No Pseudo property Code for property: {}", response.getPropertyId());
                }
            }
        }
        if (CollectionUtils.isNotEmpty(pseudoPropertyResp)) {
            grpSearchRes.addAll(pseudoPropertyResp);
        }
    }
}
