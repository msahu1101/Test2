package com.mgm.services.booking.room;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.platform.commons.PreconditionViolationException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.exception.ErrorResponseBuilder;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.OktaResponse;
import com.mgm.services.booking.room.model.PriceV2Itemized;
import com.mgm.services.booking.room.model.RoomAvailabilityResponse;
import com.mgm.services.booking.room.model.TestData;
import com.mgm.services.booking.room.model.ValidAvailabilityData;
import com.mgm.services.booking.room.model.request.CreatePartyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.request.ItineraryServiceRequest;
import com.mgm.services.booking.room.model.request.RoomPaymentDetailsRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.request.TripParams;
import com.mgm.services.booking.room.model.response.CreateItineraryResponse;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RatePlanV2Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.validator.MyVegasTokenScopes;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public abstract class BaseRoomBookingV2IntegrationTest extends BaseIntegrationTest {

    protected static WebClient identityClient;
    protected static WebClient oktaRealClient;
    protected static WebTestClient client;
    protected static WebClient realClient;

    protected static String testDataFileName = "/test-data.json";
    protected static TestData defaultTestData;

    protected static int MAX_DAYS = 100;

    protected static String BEARER_TOKEN;

    protected static final String V2_RESERV_URI = "/v2/reservation";
    protected static final String V2_RESERVE_PARTY_URI = "/v2/reservation/party";
    protected static final String V2_AVAILABILITY_TRIP = "/v2/availability/trip";
    protected static final String V2_PREVIEW_RESERVATION_API = "/v2/reservation/preview";
    protected static final String V2_ASSOCIATE_RESERVATION_API = "/v2/reservation/associate";

    protected static String testCasinoPromoCode;
    protected static String testCasinoPropertyId;
    protected static String testTransientProgramId;

    protected static String itineraryId = null;
    protected static String customerId = null;
    private static final String ITINERARY_SCOPE_CREATE = "itinerary:create";

    private static final RestTemplate restClient = new RestTemplateBuilder().build();

    private static Map<String, String> azureKeyVaultSecrets = new HashMap<>();
    private static Map<Integer, ValidAvailabilityData> availabilityMap = new HashMap<>();
    private static String idmsUrl;
    private static String itineraryUrl;
    private static String oauthScope;

    @BeforeClass
    public static void setup() throws IOException {

        envPrefix = System.getenv("envPrefix");
        deploymentEnv = System.getenv(TestConstant.DEPLOYMENT_ENV);
        
        if (System.getProperty("spring.profiles.active") != null
                && System.getProperty("spring.profiles.active").contains("local")) {
            loadLocalVariables();
        } else {
            initializeEnvVariables();
        }

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = "http://localhost:8080";
        }
        
        log.info("Environment: {}", envPrefix);
        defaultTestData = getDefaultTestData(testDataFileName, TestData.class);

        initializeSecretsManager();

        identityClient = WebClient.builder().baseUrl(idmsUrl).build();
        oktaRealClient = WebClient.builder().baseUrl(TestConstant.OKTA_BASE_URL).build();
        client = WebTestClient.bindToServer().baseUrl(baseUrl).responseTimeout(Duration.ofMillis(500000)).build();
        realClient = WebClient.builder().baseUrl(baseUrl).build();

        availabilityMap.put(1, getAvailability(1));
        availabilityMap.put(2, getAvailability(2));
        availabilityMap.put(3, getAvailability(3));

    }
    
    protected static void initializeSecretsManager() {
        log.info("value of {} is {}", TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE,
                System.getProperty(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE));
        azureKeyVaultSecrets.put(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE,
                System.getProperty(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE));
    }
    
    protected static void initializeEnvVariables() {

        baseUrl = System.getenv("baseUrl");

        idmsUrl = (System.getenv("idmsUrl") != null ? System.getenv("idmsUrl")
                : "https://azdeapi-dev.mgmresorts.com/stg/identity/authorization/v1/mgmsvc/token");
        itineraryUrl = (System.getenv("itinerarySvcUrl") != null ? System.getenv("itinerarySvcUrl")
                : "https://azdeapi-dev.mgmresorts.com/service/dev/v3/itinerary");
        oauthScope = System.getenv("oauthScope");

        customerId = (System.getenv(TestConstant.CUSTOMER_ID) != null ? System.getenv(TestConstant.CUSTOMER_ID)
                : defaultTestData.getV2CustomerId());
        testCasinoPromoCode = (System.getenv(TestConstant.CASINO_PROMO_CODE) != null
                ? System.getenv(TestConstant.CASINO_PROMO_CODE)
                : defaultTestData.getCasinoPromoCode());
        testTransientProgramId = (System.getenv(TestConstant.TRANSIENT_PROGRAM_ID) != null
                ? System.getenv(TestConstant.TRANSIENT_PROGRAM_ID)
                : defaultTestData.getCasinoProgramId());
        testCasinoPropertyId = (System.getenv(TestConstant.CASINO_PROPERTY_ID) != null
                ? System.getenv(TestConstant.CASINO_PROPERTY_ID)
                : defaultTestData.getCasinoPropertyId());
    }
    
    protected static void loadLocalVariables() {
        baseUrl = "http://localhost:8080";
        
        idmsUrl = "https://azdeapi-dev.mgmresorts.com/stg/identity/authorization/v1/mgmsvc/token";
        itineraryUrl = "https://azdeapi-dev.mgmresorts.com/service/qa/v3/itinerary";

        //customerId = "923673427969"; // dev
        customerId = "1028175167489"; // qa
        //testCasinoPromoCode = "NOTBOOKPC"; // dev
        testCasinoPromoCode = "ICECASNEGO"; // qa
        testTransientProgramId = "14caa425-8ed7-4530-bb48-d7068d3e367e"; //qa
        testCasinoPropertyId = "66964e2b-2550-4476-84c3-1a4c0c5c067f";
        envPrefix = "qa";
    }

    protected static ValidAvailabilityData getAvailabilityTestData(int numberOfNights, String customerId) {
        RoomAvailabilityResponse availabilityResponse = null;
        // setting to future date
        int counter = 5;
        boolean isAvailabileRoomFound = false;

        String checkInDate = null;
        String checkOutDate = null;
        RatePlanV2Response firstAvailablePlan = null;

        while (counter < MAX_DAYS) {
            checkInDate = getFutureDate(counter);
            checkOutDate = getFutureDate(counter + numberOfNights);
            counter += numberOfNights;
            availabilityResponse = getRoomAvailability(checkInDate, checkOutDate, customerId);

            if (null != availabilityResponse) {
                RoomAvailabilityV2Response firstAvailableRoom = null;
                if (!availabilityResponse.getAvailability().isEmpty()) {
                    Optional<RoomAvailabilityV2Response> firstAvailableRoomOption = availabilityResponse
                            .getAvailability()
                            .stream()
                            .findFirst();
                    if (firstAvailableRoomOption.isPresent()) {
                        firstAvailableRoom = firstAvailableRoomOption.get();
                        isAvailabileRoomFound = true;
                    }
                } else if (null != availabilityResponse.getRatePlans()
                        && !availabilityResponse.getRatePlans().isEmpty()) {
                    Optional<RatePlanV2Response> firstAvailablePlanOption = availabilityResponse.getRatePlans().stream()
                            .filter(ratePlan -> ratePlan.getRooms().stream().filter(rooms -> !rooms.isUnavailable())
                                    .count() > 0)
                            .findFirst();
                    if (firstAvailablePlanOption.isPresent()) {
                        firstAvailablePlan = firstAvailablePlanOption.get();
                        Optional<RoomAvailabilityV2Response> firstAvailableRoomOption = firstAvailablePlan.getRooms()
                                .stream().findFirst();
                        if (firstAvailableRoomOption.isPresent()) {
                            firstAvailableRoom = firstAvailableRoomOption.get();
                            isAvailabileRoomFound = true;
                        }
                    }
                }

                if (isAvailabileRoomFound) {
                    return populateAvailabilityData(checkInDate, checkOutDate, defaultTestData.getPropertyId(), firstAvailableRoom,
                            numberOfNights, customerId);
                }
            }

        }
        
        throw new PreconditionViolationException("No room found to be available for next few weeks. Number of nights: "
                + numberOfNights + ", Customer Id: " + customerId);

    }
    
    private static ValidAvailabilityData populateAvailabilityData(String checkInDate, String checkOutDate,
            String propertyId, RoomAvailabilityV2Response firstAvailableRoom, int numNights, String customerId) {

        ValidAvailabilityData availData = new ValidAvailabilityData();
        availData.setCheckInDate(checkInDate);
        availData.setCheckOutDate(checkOutDate);
        availData.setRoomTypeId(firstAvailableRoom.getRoomTypeId());
        availData.setPropertyId(propertyId);
        availData.setCustomerId(customerId);

        Optional<PriceV2Itemized> itemizedPrice = firstAvailableRoom.getPrice().getItemized().stream().findFirst();

        if (itemizedPrice.isPresent()) {
            PriceV2Itemized price = itemizedPrice.get();
            availData.setProgramId(price.getProgramId());
            availData.setPricingRuleId(price.getPricingRuleId());
            availData.setProgramIdIsRateTable(price.getProgramIdIsRateTable());

        }

        log.info("Availabile test data - {}", availData);

        return availData;
    }

    public abstract ApiDetails getApiDetails();

    public BaseRoomBookingV2IntegrationTest() {
        super();
    }

    @Test
    public void api_WithNoHeaders_validateHeaderMissingError() {
        ApiDetails apiDetails = getApiDetails();
        if (apiDetails.getHttpMethod().equals(ApiDetails.Method.GET)) {
            validateGetRequestNoHeaderTest(apiDetails.getBaseServiceUrl(), apiDetails.getDefaultQueryParams());
        } else if (apiDetails.getHttpMethod().equals(ApiDetails.Method.POST)) {
            validatePostRequestNoHeaderTest(apiDetails.getBaseServiceUrl(), apiDetails.getDefaultRequest());
        } else if (apiDetails.getHttpMethod().equals(ApiDetails.Method.PUT)) {
            validatePutRequestNoHeaderTest(apiDetails.getBaseServiceUrl(), apiDetails.getDefaultRequest());
        }
    }

    @Test
    public void api_ResponseHeaderValidation() {
        ApiDetails apiDetails = getApiDetails();
        if (apiDetails.getHttpMethod().equals(ApiDetails.Method.GET)) {
            validateGetResponseHeaders(apiDetails.getBaseServiceUrl(), apiDetails.getDefaultQueryParams());
        } else if (apiDetails.getHttpMethod().equals(ApiDetails.Method.POST)) {
            validatePostResponseHeaders(apiDetails.getBaseServiceUrl(), apiDetails.getDefaultRequest());
        } else if (apiDetails.getHttpMethod().equals(ApiDetails.Method.PUT)) {
            validatePutResponseHeaders(apiDetails.getBaseServiceUrl(), apiDetails.getDefaultRequest());
        }
    }

    public static void addAllHeaders(HttpHeaders headers, String sourceHeader, String channelHeader,
            String transactionIdHeader) {
        // Adds x-mgm-source, x-mgm-channel, x-mgm-transaction-id headers
        if (StringUtils.isNoneBlank(sourceHeader)) {
            headers.add(TestConstant.HEADER_SOURCE_V2, sourceHeader);
            headers.add(TestConstant.HEADER_SOURCE_V1, sourceHeader);
        }
        if (StringUtils.isNoneBlank(channelHeader)) {
            headers.add(TestConstant.HEADER_CHANNEL_V2, channelHeader);
            headers.add(TestConstant.HEADER_CHANNEL_V1, channelHeader);
        }
        if (StringUtils.isNoneBlank(transactionIdHeader)) {
            String transactionId = String.format("%s-%s", transactionIdHeader, UUID.randomUUID().toString());
            log.info("Generated Transaction Id :" + transactionId);
            headers.add(ServiceConstant.HEADER_TRANSACTION_ID, transactionId);
        }
    }

    public static void addAllHeaders(HttpHeaders headers, String sourceHeader, String channelHeader,
            String transactionIdHeader, String scope) {
        addAllHeaders(headers, sourceHeader, channelHeader, transactionIdHeader);
        if (StringUtils.isNotBlank(scope)) {
            headers.add(ServiceConstant.HEADER_AUTHORIZATION,
                    ServiceConstant.HEADER_AUTH_BEARER + createBearerToken(scope));
        } else {
            headers.add(ServiceConstant.HEADER_AUTHORIZATION,
                    ServiceConstant.HEADER_AUTH_BEARER + createBearerToken(getAllScopes()));
        }
    }

    public static void addAllHeadersWithGuestToken(HttpHeaders headers, String sourceHeader, String channelHeader,
            String transactionIdHeader, String clientId, String clientSecret) {
        addAllHeadersWithGuestToken(headers, sourceHeader, channelHeader, transactionIdHeader, clientId, clientSecret,
                getAllScopes());
    }

    public static void addAllHeadersWithGuestToken(HttpHeaders headers, String sourceHeader, String channelHeader,
            String transactionIdHeader, String clientId, String clientSecret, String scope) {
        addAllHeaders(headers, sourceHeader, channelHeader, transactionIdHeader);
        headers.add(ServiceConstant.HEADER_AUTHORIZATION,
                getAuthorizationHeaderForGuest(clientId, clientSecret, scope));
    }

    /**
     * Creates a guest token with the given details and contact that with the
     * string "Bearer ", which can be directly used as a Authorization header's
     * value.
     * 
     * @param clientId
     *            client id
     * @param clientSecret
     *            client secret
     * @param scope
     *            scope for new token
     * @return authorization header value for guest, as a string
     */
    public static String getAuthorizationHeaderForGuest(String clientId, String clientSecret, String scope) {
        LinkedMultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
        paramsMap.add("grant_type", "password");
        paramsMap.add("username", clientId);
        paramsMap.add("password", clientSecret);
        paramsMap.add("scope", scope);
        return ServiceConstant.HEADER_AUTH_BEARER + getOktaAccessToken(paramsMap);
    }

    public static void addAdditionalHeader(HttpHeaders headers, String headerName, String headerValue) {
        headers.add(headerName, headerValue);
    }

    /**
     * Create token using IDMS API.
     * 
     * @param scope
     *            scope for token
     * @return token as bearer token.
     */
    private static String createBearerToken(String scope) {
        /*
         * Environment variables: oauthGrantType, oauthScope VM Arguements:
         * rbs-oauth-client-id, rbs-oauth-client-secret
         */
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.set(ServiceConstant.HEADER_CLIENT_ID, System.getProperty("rbs-oauth-client-id"));
        body.add(ServiceConstant.HEADER_CLIENT_SECRET, System.getProperty("rbs-oauth-client-secret"));
        body.add(ServiceConstant.HEADER_GRANT_TYPE, "client_credentials");
        // if scope param null, then scope picked from env variable.
        body.add(ServiceConstant.HEADER_SCOPE, (scope != null ? scope : oauthScope));

        TokenResponse result = identityClient.post().body(BodyInserters.fromFormData(body))
                .header(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED).exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(TokenResponse.class)).log().block();
        // if the scope is null, preserve the token with static variable for
        // subsequence calls.
        if (scope == null) {
            BEARER_TOKEN = result.getAccessToken();
        }
        return result.getAccessToken();
    }

    /**
     * Invokes an overloaded method by passing <i>addSourceHeader</i> and
     * <i>addChannelHeader</i> as true.
     * 
     * @param uri
     *            - uri
     * @param queryParams
     *            - query params
     * @param errorCode
     *            - error code
     * @param errorMsg
     *            - error message
     * 
     */
    protected void validateGetRequestErrorDetails(String uri, MultiValueMap<String, String> queryParams,
            String errorCode, String errorMsg) {
        validateGetRequestErrorDetails(uri, queryParams, errorCode, errorMsg, TestConstant.ICE, TestConstant.ICE,
                TestConstant.DUMMY_TRANSACTION_ID);
    }

    /**
     * Call the given <i>uri</i> using the web client along with the headers
     * passed and compare the result against the passed <i>errocode</i> and
     * <i>erroMsg</i>
     *
     * @param uri
     *            uri
     * @param errorCode
     *            error code
     * @param errorMsg
     *            error message
     * @param sourceHeader
     *            source header string
     * @param channelHeader
     *            channel header string
     * @param transactionIdHeader
     *            transaction id header string
     */
    protected void validateGetRequestErrorDetails(String uri, MultiValueMap<String, String> queryParams,
            String errorCode, String errorMsg, String sourceHeader, String channelHeader, String transactionIdHeader) {
        BodyContentSpec body = client.get().uri(builder -> builder.path(uri).queryParams(queryParams).build()).headers(
                httpHeaders -> addAllHeaders(httpHeaders, sourceHeader, channelHeader, transactionIdHeader, null))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.error.code").isEqualTo(errorCode).jsonPath("$.error.message").isEqualTo(errorMsg);
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json responce: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    /**
     * Make calls to the given <i>uri</i> without passing any headers
     */
    protected void validateGetRequestNoHeaderTest(String uri, MultiValueMap<String, String> queryParams) {
        ErrorResponse errorResponse = getErrorResponse(ErrorCode.RUNTIME_MISSING_SOURCE_HEADER,
                ErrorTypes.VALIDATION_ERROR);
        validateGetRequestErrorDetails(uri, queryParams, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), null, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID);
        errorResponse = getErrorResponse(ErrorCode.INVALID_CHANNEL_HEADER, ErrorTypes.VALIDATION_ERROR);
        validateGetRequestErrorDetails(uri, queryParams, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), TestConstant.ICE, null, TestConstant.DUMMY_TRANSACTION_ID);
        errorResponse = getErrorResponse(ErrorCode.INVALID_TRANSACTIONID_HEADER, ErrorTypes.VALIDATION_ERROR);
        validateGetRequestErrorDetails(uri, queryParams, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), TestConstant.ICE, TestConstant.ICE, null);
    }

    protected void validatePostRequestErrorDetails(String uri, Object request, String errorCode, String errorMsg,
            String addSourceHeader, String addChannelHeader, String transactionIdHeader) {
        BodyContentSpec body = client.post().uri(uri).body(BodyInserters.fromValue(request)).headers(
                httpHeaders -> addAllHeaders(httpHeaders, addSourceHeader, addChannelHeader, transactionIdHeader, null))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.error.code").isEqualTo(errorCode).jsonPath("$.error.message").isEqualTo(errorMsg);
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json responce: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    protected void validatePutRequestNoHeaderTest(String uri, Object request) {
        ErrorResponse errorResponse = getErrorResponse(ErrorCode.RUNTIME_MISSING_SOURCE_HEADER,
                ErrorTypes.VALIDATION_ERROR);
        validatePutRequestErrorDetails(uri, request, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), null, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID);
        errorResponse = getErrorResponse(ErrorCode.INVALID_CHANNEL_HEADER, ErrorTypes.VALIDATION_ERROR);
        validatePutRequestErrorDetails(uri, request, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), TestConstant.ICE, null, TestConstant.DUMMY_TRANSACTION_ID);
        errorResponse = getErrorResponse(ErrorCode.INVALID_TRANSACTIONID_HEADER, ErrorTypes.VALIDATION_ERROR);
        validatePutRequestErrorDetails(uri, request, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), TestConstant.ICE, TestConstant.ICE, null);

    }

    protected void validatePutRequestErrorDetails(String uri, Object request, String errorCode, String errorMsg,
            String addSourceHeader, String addChannelHeader, String transactionIdHeader) {
        BodyContentSpec body = client
                .put().uri(uri).body(BodyInserters.fromValue(request)).headers(httpHeaders -> addAllHeaders(httpHeaders,
                        addSourceHeader, addChannelHeader, transactionIdHeader, null))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.error.code").isEqualTo(errorCode).jsonPath("$.error.message").isEqualTo(errorMsg);
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json responce: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    protected void validatePostRequestErrorDetails(String uri, Object request, String errorCode, String errorMsg) {
        validatePostRequestErrorDetails(uri, request, errorCode, errorMsg, TestConstant.ICE, TestConstant.ICE,
                TestConstant.DUMMY_TRANSACTION_ID);
    }

    protected void validatePostRequestNoHeaderTest(String uri, Object request) {
        ErrorResponse errorResponse = getErrorResponse(ErrorCode.RUNTIME_MISSING_SOURCE_HEADER,
                ErrorTypes.VALIDATION_ERROR);
        validatePostRequestErrorDetails(uri, request, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), null, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID);
        errorResponse = getErrorResponse(ErrorCode.INVALID_CHANNEL_HEADER, ErrorTypes.VALIDATION_ERROR);
        validatePostRequestErrorDetails(uri, request, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), TestConstant.ICE, null, TestConstant.DUMMY_TRANSACTION_ID);
        errorResponse = getErrorResponse(ErrorCode.INVALID_TRANSACTIONID_HEADER, ErrorTypes.VALIDATION_ERROR);
        validatePostRequestErrorDetails(uri, request, errorResponse.getError().getCode(),
                errorResponse.getError().getMessage(), TestConstant.ICE, TestConstant.ICE, null);
    }

    protected static void validateSuccessResponse(ClientResponse clientResponse, String baseURL) {
        if (!clientResponse.statusCode().is2xxSuccessful()) {
            throw new TestExecutionException("The request is unsuccessful for url " + baseURL + ", Response code: "
                    + clientResponse.statusCode() + ", Transaction ID: "
                    + clientResponse.headers().header("x-mgm-transaction-id"));
        }
    }
    
    protected static void validate4XXFailureResponse(ClientResponse clientResponse, String baseURL) {
        if (!clientResponse.statusCode().is4xxClientError()) {
            throw new TestExecutionException("The response should be 4xx message for " + baseURL + ", Response code: "
                    + clientResponse.statusCode());
        }
    }

    protected String createItineraryId() {
        File requestFile = new File(getClass().getResource("/create-customer-itinerary.json").getPath());
        ItineraryServiceRequest itineraryRequest = convert(requestFile, ItineraryServiceRequest.class);
        TripParams tripParams = new TripParams();
        tripParams.setNumAdults(2);
        tripParams.setArrivalDate(getFutureDate(5));
        tripParams.setDepartureDate(getFutureDate(10));
        itineraryRequest.getItinerary().setTripParams(tripParams);
        itineraryRequest.getItinerary().setCustomerId(customerId);
        
        log.info("Itinerary Request: {}", CommonUtil.convertObjectToJsonString(itineraryRequest));

        HttpHeaders headers = new HttpHeaders();
        headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
        headers.add(ServiceConstant.HEADER_AUTHORIZATION,
                ServiceConstant.HEADER_AUTH_BEARER + createBearerToken(ITINERARY_SCOPE_CREATE));
        headers.add(ServiceConstant.X_MGM_CORRELATION_ID, UUID.randomUUID().toString());

        HttpEntity<ItineraryServiceRequest> request = new HttpEntity<>(itineraryRequest, headers);
        
        log.info("Itinerary Url: {}", itineraryUrl);
        
        try {
            final ResponseEntity<CreateItineraryResponse> response = restClient.exchange(
                    itineraryUrl, HttpMethod.POST, request, CreateItineraryResponse.class);
            itineraryId = response.getBody().getItinerary().getItineraryId();
            log.info("Created Itinerary Id:" + itineraryId);
        } catch (RestClientException e) {
            log.error("Error occured while fetching itineraryId, ", e);
            itineraryId = defaultTestData.getTestItineraryId();
        }
        return itineraryId;
    }

    private CreateRoomReservationResponse findRoomReservation(String confNumber) {

        return realClient
                .get().uri(builder -> builder.path(V2_RESERV_URI)
                        .queryParam(TestConstant.CONFIRMATION_NUMBER, confNumber).queryParam("cacheOnly", true).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on finding reservation : " + V2_RESERV_URI
                            + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(CreateRoomReservationResponse.class)).block();
    }

    public CreateRoomReservationResponse makeReservationV2AndValidate(
            CreateRoomReservationRequest createRoomReservationRequest) {
        CreateRoomReservationResponse reservation = makeReservationV2(createRoomReservationRequest, TestConstant.ICE,
                TestConstant.ICE);

        int count = 0;
        boolean resvFound = false;
        while (count < 10) {
            addDelay(5000);

            CreateRoomReservationResponse findReservation = findRoomReservation(
                    reservation.getRoomReservation().getConfirmationNumber());

            if (StringUtils.isNotEmpty(findReservation.getRoomReservation()
                    .getOperaConfirmationNumber()) && StringUtils.isNotEmpty(
                            findReservation.getRoomReservation()
                                    .getPostingState())
                    && findReservation.getRoomReservation()
                            .getPostingState()
                            .equalsIgnoreCase("Success")) {
                resvFound = true;
                break;
            }

            count++;
        }

        // aborting the tests, if the reservation is not syched to Opera
        Assume.assumeTrue("Reservation with confirmation number: "
                + reservation.getRoomReservation().getConfirmationNumber() + " not posted to Opera yet.", resvFound);
        return reservation;
    }

    public CreateRoomReservationResponse makeReservationV2(CreateRoomReservationRequest createRoomReservationRequest) {
        return makeReservationV2(createRoomReservationRequest, TestConstant.ICE, TestConstant.ICE);
    }

    public CreateRoomReservationResponse makeReservationV2(CreateRoomReservationRequest createRoomReservationRequest,
            String sourceHeader, String channelHeader) {
        createRoomReservationRequest.getRoomReservation().setItineraryId(createItineraryId());
        log.info("Reservation Request: {}", CommonUtil.convertObjectToJsonString(createRoomReservationRequest));
        Mono<CreateRoomReservationResponse> resultPost = realClient.post()
                .uri(builder -> builder.path(V2_RESERV_URI).build())
                .body(BodyInserters.fromValue(createRoomReservationRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, sourceHeader, channelHeader,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on reserving room for endpoint " + V2_RESERV_URI
                            + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateSuccessResponse(clientResponse, V2_RESERV_URI);
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("Reservation Response: {}", entity.getBody());
                        return convert(entity.getBody(), CreateRoomReservationResponse.class);
                    });

                });

        CreateRoomReservationResponse response = resultPost.block();

        log.info("Room reservation response: {}", CommonUtil.convertObjectToJsonString(response));

        return response;
    }

    public CreateRoomReservationResponse makeReservationV2(CreateRoomReservationRequest createRoomReservationRequest,
            String sourceHeader, String channelHeader, String clientId, String clientSecret, String scope,
            Map<String, String> additionalHeaders) {
        createRoomReservationRequest.getRoomReservation().setItineraryId(createItineraryId());
        log.info("Reservation Request JSON: {}", createRoomReservationRequest);
        Mono<CreateRoomReservationResponse> resultPost = realClient.post()
                .uri(builder -> builder.path(V2_RESERV_URI).build())
                .body(BodyInserters.fromValue(createRoomReservationRequest)).headers(httpHeaders -> {
                    if (StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(clientSecret)) {
                        addAllHeadersWithGuestToken(httpHeaders, sourceHeader, channelHeader,
                                TestConstant.DUMMY_TRANSACTION_ID, clientId, clientSecret, scope);
                    } else {
                        addAllHeaders(httpHeaders, sourceHeader, channelHeader, TestConstant.DUMMY_TRANSACTION_ID,
                                scope);
                    }
                    additionalHeaders.forEach((k, v) -> httpHeaders.add(k, v));
                }).exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on reserving room for endpoint " + V2_RESERV_URI
                            + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateSuccessResponse(clientResponse, V2_RESERV_URI);
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("Reservation Response: {}", entity.getBody());
                        return convert(entity.getBody(), CreateRoomReservationResponse.class);
                    });

                });

        CreateRoomReservationResponse response = resultPost.block();

        log.info("Room reservation response::{}", response);

        return response;
    }

    protected void validatePostResponseHeaders(String uri, Object request) {

        realClient.post().uri(builder -> builder.path(uri).build()).body(BodyInserters.fromValue(request))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException(
                            "Error on reserving room for endpoint " + uri + ", Error message : " + error.getMessage(),
                            error);
                }).doOnSuccess(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateResponseHeaders(clientResponse);
                }).block();

    }

    protected void validateResponseHeaders(ClientResponse clientResponse) {
        if (System.getProperty("spring.profiles.active") == null
                || !System.getProperty("spring.profiles.active").contains("local")) {
            List<String> missedHeader = new ArrayList<>();
            for (String header : TestConstant.headers) {
                if (!clientResponse.headers().asHttpHeaders().containsKey(header)) {
                    missedHeader.add(header);
                }
            }
            assertTrue("Response should contain '" + missedHeader + "'", missedHeader.isEmpty());
        }

    }

    protected void validatePutResponseHeaders(String uri, Object request) {

        realClient.put().uri(builder -> builder.path(uri).build()).body(BodyInserters.fromValue(request))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException(
                            "Error on reserving room for endpoint " + uri + ", Error message : " + error.getMessage(),
                            error);
                }).doOnSuccess(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateResponseHeaders(clientResponse);
                }).block();
    }

    protected void validateGetResponseHeaders(String uri, MultiValueMap<String, String> queryParams) {

        realClient.get().uri(builder -> builder.path(uri).queryParams(queryParams).build())
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException(
                            "Error on reserving room for endpoint " + uri + ", Error message : " + error.getMessage(),
                            error);
                }).doOnSuccess(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    if (!clientResponse.statusCode()
                            .isError()) {
                        validateResponseHeaders(clientResponse);
                    }
                }).block();

    }

    /**
     * Create CreateRoomReservationRequest object by data from the
     * <i>filePath</i> given and override the dates for a single night stay.
     * 
     * @param filePath
     *            path of the request object
     * @param numNights
     *            number of nights
     * @return CreateRoomReservationRequest object
     */
    public CreateRoomReservationRequest createRequestBasic(String filePath) {
        return updateRequestWithTestData(
                convert(new File(getClass().getResource(filePath).getPath()), CreateRoomReservationRequest.class), 1);
    }

    /**
     * Create CreateRoomReservationRequest object by data from the
     * <i>filePath</i> given and override the dates.
     * 
     * @param filePath
     *            path of the request object
     * @return CreateRoomReservationRequest object
     */
    public CreateRoomReservationRequest createRequestMultiNight(String filePath, int numNights) {
        File requestFile = new File(getClass().getResource(filePath).getPath());
        CreateRoomReservationRequest createRoomReservationRequest = convert(requestFile,
                CreateRoomReservationRequest.class);
        updateRequestWithTestData(createRoomReservationRequest, numNights);

        ValidAvailabilityData availData = getAvailability(numNights);
        Date checkInDatePlusOne = addDays(availData.getCheckInDate(), 1);
        createRoomReservationRequest.getRoomReservation().getBookings().get(1).setDate(checkInDatePlusOne);
        createRoomReservationRequest.getRoomReservation().getBookings().get(1).setProgramId(availData.getProgramId());
        createRoomReservationRequest.getRoomReservation().getBookings().get(1)
                .setPricingRuleId(availData.getPricingRuleId());
        createRoomReservationRequest.getRoomReservation().getBookings().get(1)
                .setProgramIdIsRateTable(availData.isProgramIdIsRateTable());
        createRoomReservationRequest.getRoomReservation().getChargesAndTaxes().getCharges().get(1)
                .setDate(checkInDatePlusOne);
        createRoomReservationRequest.getRoomReservation().getChargesAndTaxes().getTaxesAndFees().get(1)
                .setDate(checkInDatePlusOne);
        return createRoomReservationRequest;
    }

    /**
     * Create CreateRoomReservationRequest object by data from the
     * <i>filePath</i> given and override the dates.
     * 
     * @param filePath
     *            path of the request object
     * @return CreateRoomReservationRequest object
     */
    public CreateRoomReservationRequest createRequestAlert(String filePath, int numNights) {
        File requestFile = new File(getClass().getResource(filePath).getPath());
        CreateRoomReservationRequest createRoomReservationRequest = convert(requestFile,
                CreateRoomReservationRequest.class);
        updateRequestWithTestData(createRoomReservationRequest, numNights);

        TripDetailsRequest tripDetails = createRoomReservationRequest.getRoomReservation().getTripDetails();
        Date checkInDate = tripDetails.getCheckInDate();
        Date checkOutDate = tripDetails.getCheckInDate();

        createRoomReservationRequest.getRoomReservation().getTraces().get(0).setDate(checkInDate);
        createRoomReservationRequest.getRoomReservation().getTraces().get(1).setDate(checkInDate);
        createRoomReservationRequest.getRoomReservation().getRoutingInstructions().get(0).setStartDate(checkInDate);
        createRoomReservationRequest.getRoomReservation().getRoutingInstructions().get(0).setEndDate(checkOutDate);
        return createRoomReservationRequest;
    }

    protected CreateRoomReservationRequest updateRequestWithTestData(CreateRoomReservationRequest request,
            int numNights) {
        String reservationId = String.valueOf(System.currentTimeMillis());
        log.info("Generated reservationId : {}", reservationId);

        ValidAvailabilityData availData = getAvailability(numNights);

        Date checkInDate = getDate(availData.getCheckInDate());
        Date checkOutDate = getDate(availData.getCheckOutDate());

        RoomReservationRequest reservationRequest = request.getRoomReservation();
        if (null != customerId) {
            reservationRequest.setCustomerId(NumberUtils.toLong(customerId));
            if (null != reservationRequest.getProfile()) {
                reservationRequest.getProfile().setId(NumberUtils.toLong(customerId));
            }
        }
        reservationRequest.setId(reservationId);
        reservationRequest.setRoomTypeId(availData.getRoomTypeId());
        reservationRequest.getTripDetails().setCheckInDate(checkInDate);
        reservationRequest.getTripDetails().setCheckOutDate(checkOutDate);
        reservationRequest.getBookings().get(0).setDate(checkInDate);
        reservationRequest.getBookings().get(0).setProgramId(availData.getProgramId());
        reservationRequest.getBookings().get(0).setPricingRuleId(availData.getPricingRuleId());
        reservationRequest.getBookings().get(0).setProgramIdIsRateTable(availData.isProgramIdIsRateTable());
        reservationRequest.getChargesAndTaxes().getCharges().get(0).setDate(checkInDate);
        reservationRequest.getChargesAndTaxes().getTaxesAndFees().get(0).setDate(checkInDate);
        reservationRequest.getDepositDetails().setDueDate(checkInDate);
        reservationRequest.getDepositDetails().setForfeitDate(checkInDate);
        updateCcToken(reservationRequest.getBilling());
        request.setRoomReservation(reservationRequest);

        return request;
    }

    protected void updateCcToken(List<RoomPaymentDetailsRequest> paymentDetailList) {
        String[] tokenDetails = defaultTestData.getTokenDetails().split("\\|");
        Optional.ofNullable(paymentDetailList).ifPresent(payments -> {
            payments.forEach(billing -> {
                billing.getPayment().setType(tokenDetails[0]);
                billing.getPayment().setCcToken(tokenDetails[1]);
            });
        });
    }

    public CreatePartyRoomReservationResponse makePartyRoomReservation(
            CreatePartyRoomReservationRequest createPartyRoomReservationRequest) {
        createPartyRoomReservationRequest.getRoomReservation().setItineraryId(createItineraryId());
        Mono<CreatePartyRoomReservationResponse> resultPost = realClient.post()
                .uri(builder -> builder.path(V2_RESERVE_PARTY_URI).build())
                .body(BodyInserters.fromValue(createPartyRoomReservationRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on reserving room for endpoint " + V2_RESERVE_PARTY_URI
                            + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateSuccessResponse(clientResponse, V2_RESERVE_PARTY_URI);
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("Reservation Response: {}", entity.getBody());
                        return convert(entity.getBody(), CreatePartyRoomReservationResponse.class);
                    });

                });

        CreatePartyRoomReservationResponse response = resultPost.block();

        log.info("Party Room reservation response::{}", response);

        return response;
    }

    /**
     * Create CreateRoomReservationRequest object by data from the
     * <i>filePath</i> given and override the dates for a single night stay.
     * 
     * @param filePath
     *            path of the request object
     * @return CreateRoomReservationRequest object
     */
    public CreatePartyRoomReservationRequest createPartyRequestBasic(String filePath) {
        File requestFile = new File(getClass().getResource(filePath).getPath());
        CreatePartyRoomReservationRequest createPartyRoomReservationRequest = convert(requestFile,
                CreatePartyRoomReservationRequest.class);
        RoomReservationRequest reservationRequest = createPartyRoomReservationRequest.getRoomReservation();
        createPartyRoomReservationRequest.setRoomReservation(updateRequestWithTestData(reservationRequest, 1));
        
        log.info("Party Reservation Request: {}", CommonUtil.convertObjectToJsonString(createPartyRoomReservationRequest));
        return createPartyRoomReservationRequest;
    }

    /**
     * Create CreatePartyRoomReservationRequest object by data from the
     * <i>filePath</i> given and override the dates.
     * 
     * @param filePath
     *            path of the request object
     * @return CreatePartyRoomReservationRequest object
     */
    public CreatePartyRoomReservationRequest createPartyRequestMultiNight(String filePath, int numNights) {
        File requestFile = new File(getClass().getResource(filePath).getPath());
        CreatePartyRoomReservationRequest partyRequest = convert(requestFile, CreatePartyRoomReservationRequest.class);
        RoomReservationRequest reservationRequest = partyRequest.getRoomReservation();
        updateRequestWithTestData(reservationRequest, numNights);

        ValidAvailabilityData availData = getAvailability(numNights);
        reservationRequest.getBookings().get(1).setProgramId(availData.getProgramId());
        reservationRequest.getBookings().get(1)
                .setPricingRuleId(availData.getPricingRuleId());
        reservationRequest.getBookings().get(1)
                .setProgramIdIsRateTable(availData.isProgramIdIsRateTable());

        Date checkInDatePlusOne = addDays(availData.getCheckInDate(), 1);
        partyRequest.getRoomReservation().getBookings().get(1).setDate(checkInDatePlusOne);
        partyRequest.getRoomReservation().getChargesAndTaxes().getCharges().get(1).setDate(checkInDatePlusOne);
        partyRequest.getRoomReservation().getChargesAndTaxes().getTaxesAndFees().get(1).setDate(checkInDatePlusOne);
        return partyRequest;
    }

    protected RoomReservationRequest updateRequestWithTestData(RoomReservationRequest roomReservationRequest,
            int numNights) {
        String reservationId = String.valueOf(System.currentTimeMillis());
        log.info("Generated reservationId : {}", reservationId);

        ValidAvailabilityData availData = getAvailability(numNights);

        Date checkInDate = getDate(availData.getCheckInDate());
        Date checkOutDate = getDate(availData.getCheckOutDate());

        if (null != customerId) {
            roomReservationRequest.setCustomerId(NumberUtils.toLong(customerId));
            if (null != roomReservationRequest.getProfile()) {
                roomReservationRequest.getProfile().setId(NumberUtils.toLong(customerId));
            }
        }
        roomReservationRequest.setId(reservationId);
        roomReservationRequest.setRoomTypeId(availData.getRoomTypeId());
        roomReservationRequest.getTripDetails().setCheckInDate(checkInDate);
        roomReservationRequest.getTripDetails().setCheckOutDate(checkOutDate);
        roomReservationRequest.getBookings().get(0).setProgramId(availData.getProgramId());
        roomReservationRequest.getBookings().get(0).setPricingRuleId(availData.getPricingRuleId());
        roomReservationRequest.getBookings().get(0).setProgramIdIsRateTable(availData.isProgramIdIsRateTable());

        roomReservationRequest.getBookings().get(0).setDate(checkInDate);
        roomReservationRequest.getChargesAndTaxes().getCharges().get(0).setDate(checkInDate);
        roomReservationRequest.getChargesAndTaxes().getTaxesAndFees().get(0).setDate(checkInDate);
        roomReservationRequest.getDepositDetails().setDueDate(checkInDate);
        roomReservationRequest.getDepositDetails().setForfeitDate(checkInDate);
        updateCcToken(roomReservationRequest.getBilling());
        return roomReservationRequest;
    }

    protected ErrorResponse getErrorResponse(ErrorCode errorCode, ErrorTypes errorType) {
        return new ErrorResponseBuilder()
                .buildErrorResponse(String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                        errorType.errorTypeCode(), errorCode.getNumericCode()), errorCode.getDescription());
    }

    /**
     * Create a call to availability trip v2 service with the given params.
     * 
     * @param checkInDate
     *            check in date
     * @param checkOutDate
     *            check out date
     * @param customerId
     *            customerId
     * @return RoomAvailabilityCombinedResponse object
     */
    public static RoomAvailabilityResponse getRoomAvailability(String checkInDate, String checkOutDate,
            String customerId) {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", checkInDate);
        queryParams.add("checkOutDate", checkOutDate);
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        queryParams.add(ServiceConstant.CUSTOMER_ID, customerId);
        queryParams.add("programId", testTransientProgramId);

        RoomAvailabilityResponse availabilityResponse = null;

        String response = realClient.get()
                .uri(builder -> builder.path(V2_AVAILABILITY_TRIP).queryParams(queryParams).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null)).retrieve().bodyToMono(String.class).block();
                
        log.info("Trip availability response::{}", response);
        
        availabilityResponse = convert(response, RoomAvailabilityResponse.class);
        
        return availabilityResponse;
    }

    protected static ValidAvailabilityData getAvailability(int numNights) {

        if (availabilityMap.containsKey(numNights)) {
            return availabilityMap.get(numNights);
        } else {
            return getAvailability(numNights, customerId);
        }

    }

    protected static ValidAvailabilityData getAvailability(int numNights, String customerId) {
        return getAvailabilityTestData(numNights, customerId);
    }

    protected static String getAllScopes() {
        // combine rbs scopes, myvegas scopes and scopes specifically required
        // for tests
        return Arrays.stream(RBSTokenScopes.values()).map(RBSTokenScopes::getValue)
                .collect(Collectors.joining(ServiceConstant.WHITESPACE_STRING))
                + ServiceConstant.WHITESPACE_STRING
                + Arrays.stream(MyVegasTokenScopes.values()).map(MyVegasTokenScopes::getValue)
                        .collect(Collectors.joining(ServiceConstant.WHITESPACE_STRING))
                + ServiceConstant.WHITESPACE_STRING + "itinerary:create";
    }

    public static String getOktaAccessToken(LinkedMultiValueMap<String, String> paramsMap) {
        OktaResponse result = oktaRealClient.post()
                .uri(builder -> builder.path(TestConstant.OKTA_ACCESS_TOKEN_URL).queryParams(paramsMap).build())
                .headers(httpHeaders -> {
                    httpHeaders.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
                    httpHeaders.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_ALL);
                    httpHeaders.set(ServiceConstant.HEADER_AUTHORIZATION,
                            getSecretValue(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE));
                }).exchange().flatMap(clientResponse -> clientResponse.bodyToMono(OktaResponse.class)).retry(3).log()
                .block();

        log.info("Access Token Created {}", result.getAccessToken());
        return result.getAccessToken();
    }

    private static String getSecretValue(String key) {
        return azureKeyVaultSecrets.get(key);
    }

}
