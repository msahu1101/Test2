package com.mgm.services.booking.room;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.platform.commons.PreconditionViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.OktaRequest;
import com.mgm.services.booking.room.model.OktaResponse;
import com.mgm.services.booking.room.model.ProgramEligibility;
import com.mgm.services.booking.room.model.TestData;
import com.mgm.services.booking.room.model.ValidAvailabilityData;
import com.mgm.services.booking.room.model.request.ReservationRequest;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.response.ConsolidatedRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RatePlanResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.common.aws.AWSSecretsManagerUtil;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class BaseRoomBookingIntegrationTest {

    public static final String YES = "yes";

    protected static WebTestClient client;

    protected static WebClient realClient;

    protected static WebClient oktaRealClient;

    protected static TestData defaultTestData;

    protected static String testDataFileName = "/test-data.json";

    protected static final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

    protected static ObjectMapper mapper = null;

    protected static int DATE_INCREMENT_COUNTER = 2;
    protected static int MAX_DAYS = 100;
    private static int MYVEGAS_COUNTER = 0;

    protected static String STATE_TOKEN;
    private static String V2_ACCESS_TOKEN;
    private static String V2_OKTA_SESSION_ID;

    String RESP_RESERVATION_ID;

    public static String baseUrl;
    public static String apiKey;
    public static String passThroughFlag;
    public static String envPrefix;
    public static String oktaClientId;
    public static String oktaClientSecret;
    public static String awsProfile;
    public static boolean oktaTransientFlag;
    public static Properties defaultProperties;
    public static Properties envSpecificProperties;
    public static String deploymentEnv;
    private static Map<String, String> azureKeyVaultSecrets = new HashMap<>();

    public static AWSSecretsManagerUtil awsSecretsManagerUtil;

    @BeforeClass
    public static void setup() throws Exception {
        baseUrl = System.getenv("baseUrlV1");
        apiKey = System.getenv("apiKey");
        envPrefix = System.getenv("envPrefix");
        passThroughFlag = System.getenv("ApiGtwy_Passthru");
        deploymentEnv = System.getenv(TestConstant.DEPLOYMENT_ENV);

        awsProfile = TestConstant.APP_AWS_PROFILE;

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = "http://localhost:8080";
            awsProfile = "";
        }

        initializeSecretsManager();
        initializeOktaTokenCredentials();

        client = WebTestClient.bindToServer().baseUrl(baseUrl).responseTimeout(Duration.ofMillis(500000)).build();
        realClient = WebClient.builder().baseUrl(baseUrl).build();
        oktaRealClient = WebClient.builder().filter(logRequest()).filter(logResponseStatus())
                .baseUrl(TestConstant.OKTA_BASE_URL).build();
        mapper = new ObjectMapper();
        defaultTestData = getDefaultTestData();
        populateValidTestData();

    }

    protected static void initializeSecretsManager() throws IOException {
        if (StringUtils.isNotBlank(deploymentEnv) && deploymentEnv.equalsIgnoreCase(TestConstant.ENV_AZURE)) {
            azureKeyVaultSecrets.put(ServiceConstant.OKTA_CLIENT_ID,
                    System.getProperty(ServiceConstant.OKTA_CLIENT_ID));
            azureKeyVaultSecrets.put(ServiceConstant.OKTA_CLIENT_SECRET,
                    System.getProperty(ServiceConstant.OKTA_CLIENT_SECRET));
            azureKeyVaultSecrets.put(ServiceConstant.OKTA_TRANSIENT_FLAG,
                    System.getProperty(ServiceConstant.OKTA_TRANSIENT_FLAG));
            azureKeyVaultSecrets.put(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE,
                    System.getProperty(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE));
            azureKeyVaultSecrets.put(TestConstant.OKTA_TEST_USERNAME,
                    System.getProperty(TestConstant.OKTA_TEST_USERNAME));
            azureKeyVaultSecrets.put(TestConstant.OKTA_TEST_USERPASSWORD,
                    System.getProperty(TestConstant.OKTA_TEST_USERPASSWORD));
            azureKeyVaultSecrets.put(TestConstant.SECRET_VALUE_X_SIGN,
                    System.getProperty(TestConstant.SECRET_VALUE_X_SIGN));
        } else {
            awsSecretsManagerUtil = new AWSSecretsManagerUtil(
                    getPropertiesKeyValue(TestConstant.APP_AWS_SECRETS_REGION),
                    getPropertiesKeyValue(TestConstant.APP_AWS_SECRET_NAME), awsProfile);
        }
    }

    private static String getSecretValue(String key) {
        if (StringUtils.isNotBlank(deploymentEnv) && deploymentEnv.equalsIgnoreCase(TestConstant.ENV_AZURE)) {
            return azureKeyVaultSecrets.get(key);
        } else {
            return awsSecretsManagerUtil.getSecretValue(key);
        }
    }

    /**
     * Resetting token before every test to avoid any kind of conflicts
     */
    @Before
    public void reset() {

        STATE_TOKEN = null;

    }

    /**
     * This method fetches the required okta credentials from secret manager
     * 
     * @throws IOException
     */
    protected static void initializeOktaTokenCredentials() throws IOException {

        log.info("REGION:{}, SECRET_NAME:{}, Profile:{}", getPropertiesKeyValue(TestConstant.APP_AWS_SECRETS_REGION),
                getPropertiesKeyValue(TestConstant.APP_AWS_SECRET_NAME), awsProfile);

        oktaClientId = getSecretValue(ServiceConstant.OKTA_CLIENT_ID);
        oktaClientSecret = getSecretValue(ServiceConstant.OKTA_CLIENT_SECRET);
        oktaTransientFlag = Boolean.getBoolean(getSecretValue(ServiceConstant.OKTA_TRANSIENT_FLAG));

    }

    /**
     * This method loads the application.properties and
     * application-<env>.properties It searches for a key in the
     * application-<env>.properties and if not found, it loads it from
     * application.properties
     * 
     * @param key
     *            the key to search for
     * @return the value
     * @throws IOException
     */
    public static String getPropertiesKeyValue(String key) throws IOException {
        if (defaultProperties == null) {
            defaultProperties = loadProperties(TestConstant.APP_PROPERTIES_FILE);
        }
        if (envSpecificProperties == null) {
            try {
                envSpecificProperties = loadProperties(
                        getEnvSpecificPropertiesFilePath(TestConstant.APP_PROPERTIES_FILE));
            } catch (Exception ex) {
                envSpecificProperties = defaultProperties;
            }
        }
        String keyValue = envSpecificProperties.getProperty(key);
        if (keyValue == null) {
            keyValue = defaultProperties.getProperty(key);
        }

        return keyValue;
    }

    public static Properties loadProperties(String resourceFileName) throws IOException {
        Properties configuration = new Properties();
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceFileName);
        configuration.load(inputStream);
        inputStream.close();
        return configuration;
    }

    /**
     * This method checks if the property specified in the default test data is
     * available for the given check-in and checkout date. If it is not
     * available, then it will keep incrementing the check-in date by 3 days
     * till we get a availability. Also, it will set only the available room
     * type in the test data
     */
    protected static ValidAvailabilityData getAvailabilityTestData(ProgramEligibility request, boolean enableJwbHeader,
            boolean enableJwbCookie) {
        ValidAvailabilityData data = null;
        List<RatePlanResponse> availabilityList = null;
        int counter = 0;
        createTokenWithCustomerDetails(request.getMlifeNumber(), -1);
        while (counter < MAX_DAYS) {
            counter += DATE_INCREMENT_COUNTER;

            String baseUrl = TestConstant.URL_ROOM_AVAILABILITY + "?checkInDate=" + getFutureDate(counter)
                    + "&checkOutDate=" + getFutureDate(counter + DATE_INCREMENT_COUNTER) + "&propertyId="
                    + request.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults();
            baseUrl = appendValidAttribute(baseUrl, "programId", request.getProgramId());
            baseUrl = appendValidAttribute(baseUrl, "promoCode", request.getPromoCode());
            final String url = baseUrl;

            availabilityList = realClient.get().uri(baseUrl).headers(headers -> {
                addAllHeaders(headers);
                if (enableJwbHeader) {
                    headers.add(TestConstant.ENABLE_JWB, "true");
                }
                if (enableJwbCookie) {
                    headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=true");
                }
            }).exchange().doOnError(clientError -> {
                throw new TestExecutionException(
                        "Error occurred invoking url :" + url + ", error message:" + clientError.getMessage(),
                        clientError);
            }).doOnSuccess(response -> validateSuccessResponse(response, url))
                    .flatMapMany(response -> response.bodyToFlux(RatePlanResponse.class).collectList()).blockLast();

            if (!availabilityList.isEmpty()) {

                for (RatePlanResponse availability : availabilityList) {
                    if (!availability.getRooms().isEmpty() && availability.getRooms().size() > 2) {
                        data = new ValidAvailabilityData();
                        data.setCheckInDate(getFutureDate(counter));
                        data.setCheckOutDate(getFutureDate(counter + DATE_INCREMENT_COUNTER));
                        data.setRoomTypeId(availability.getRooms().stream().skip(1).findFirst().get().getRoomTypeId());
                        data.setProgramId(availability.getProgramId());
                        data.setPropertyId(request.getPropertyId());
                        data.setSecondRoomTypeId(
                                availability.getRooms().stream().skip(2).findFirst().get().getRoomTypeId());
                        return data;
                    }
                }
            }

        }

        throw new PreconditionViolationException(
                "No room found to be available for next few weeks. Request: " + request);

    }

    protected static String appendValidAttribute(String baseUrl, String attributeName, String attributeValue) {
        if (StringUtils.isNotEmpty(attributeValue)) {
            return baseUrl.concat(String.format("&%s=%s", attributeName, attributeValue));
        }
        return baseUrl;
    }

    protected static void populateValidTestData() {
        ProgramEligibility cartRequest = new ProgramEligibility();
        cartRequest.setPropertyId(defaultTestData.getPropertyId());
        ValidAvailabilityData defaultData = getAvailabilityTestData(cartRequest, false, false);
        defaultTestData.setCheckInDate(defaultData.getCheckInDate());
        defaultTestData.setCheckOutDate(defaultData.getCheckOutDate());
        defaultTestData.setRoomTypeId(defaultData.getRoomTypeId());
        defaultTestData.setProgramId(defaultData.getProgramId());
    }

    protected static String getRotatingMyVegasCode() {
        String[] tokens = defaultTestData.getRotatingMyVegasRedemptionCode().split(",");
        MYVEGAS_COUNTER = (MYVEGAS_COUNTER + 1) % tokens.length;
        log.info("Using redemption code at index={} with value {}", MYVEGAS_COUNTER, tokens[MYVEGAS_COUNTER]);
        return tokens[MYVEGAS_COUNTER].trim();
    }

    protected static TestData getDefaultTestData() {
        String fileName = getEnvSpecificFileName(testDataFileName);
        return getObjectFromJSON(fileName, TestData.class);
    }

    /**
     * This method returns the environment specific test data file. It tries to
     * load the file with the format <envPrefix>-<fileName> like
     * qa-test-data.json for qa environment If the file is not found, then it
     * loads the default file with the name <fileName> like test-data.json
     * 
     * @param fileName
     *            the <fileName>
     * @param fileFormat
     *            default is </><envPrefix><fileName>
     * @return environment specific file name
     */
    public static String getEnvSpecificFileName(String fileName) {
        String envFileName = String.format(TestConstant.TEST_DATA_FILE_FORMAT, "/", envPrefix,
                fileName.replaceFirst("/", ""));
        File file = new File(envFileName);
        if (!file.exists()) {
            envFileName = fileName;
        }
        log.info("Loading Data from File::{}", envFileName);
        return envFileName;
    }

    public static String getEnvSpecificPropertiesFilePath(String fileName) {

        if (StringUtils.isEmpty(envPrefix)) {
            return fileName;
        }

        String envFileName = String.format(TestConstant.PROPERTIES_FILE_FORMAT,
                fileName.replaceAll(TestConstant.PROPERTIES_FILE_SUFFIX, ""), envPrefix,
                TestConstant.PROPERTIES_FILE_SUFFIX);
        log.info("Loading Data from File::{}", envFileName);
        return envFileName;
    }

    public BaseRoomBookingIntegrationTest() {
        super();
    }

    protected static String getFutureDate(int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, days);

        Date dateObj = c.getTime();
        return format.format(dateObj);
    }

    protected static String getPastDate() {
        // Using -3 days from today as check-in date
        return getFutureDate(-3);
    }

    protected static String getCheckInDate() {
        // Using 6 days from today as check-in date
        return getFutureDate(15);
    }

    protected static String getCheckOutDate() {
        // Using 8 days from today check-out date
        return getFutureDate(18);
    }

    public static <T> T convert(File file, Class<T> target) {

        try {
            ArrayList<Module> modules = new ArrayList<>();
            modules.add(new JavaTimeModule());
            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build();
            return mapper.readValue(new FileInputStream(file), target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public static <T> T convert(String json, Class<T> target) {

        try {
            ArrayList<Module> modules = new ArrayList<>();
            modules.add(new JavaTimeModule());
            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build();
            return mapper.readValue(json, target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public <T> T convert(File file, CollectionType type) {

        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public void addDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Exception trying to put the thread to sleep: ", e);
        }
    }

    public static void createToken() {
        if (STATE_TOKEN == null) {
            createToken(null);
        }
    }

    public static void createTokenWithCustomerDetails(String mlifeNumber, long customerId) {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClientId(oktaClientId);
        tokenRequest.setClientSecret(oktaClientSecret);
        tokenRequest.setTransientFlag(oktaTransientFlag);
        tokenRequest.setMlifeNumber(mlifeNumber);
        tokenRequest.setCustomerId(customerId);
        createToken(tokenRequest);
    }

    /**
     * Create session in redis by calling token API if token is not already
     * available
     */
    public static void createToken(TokenRequest tokenRequest) {
        if (tokenRequest == null) {
            tokenRequest = new TokenRequest();
            tokenRequest.setClientId(oktaClientId);
            tokenRequest.setClientSecret(oktaClientSecret);
            tokenRequest.setTransientFlag(oktaTransientFlag);
        }

        realClient.post().uri(builder -> builder.path("/v1/authorize").build()).body(BodyInserters.fromValue(tokenRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, true, false, true)).exchange()
                .doOnError(clientError -> {
                    throw new TestExecutionException(
                            "Error on getting token , Error message:" + clientError.getMessage(), clientError);
                }).doOnSuccess(clientResponse -> {
                    STATE_TOKEN = validateAndGetHeaderValue(clientResponse, "x-state-token",
                            "On invoking enpoint /v1/token, ");
                }).flatMap(clientResponse -> clientResponse.bodyToMono(TokenResponse.class)).block();

        log.info("Token Created {}", STATE_TOKEN);
    }

    public static String getOktaAccessToken() {
        return getOktaAccessToken(getAccessTokenParamsMap());
    }

    public static String getOktaAccessToken(LinkedMultiValueMap<String, String> paramsMap) {
        if (StringUtils.isEmpty(V2_ACCESS_TOKEN)) {
            OktaResponse result = oktaRealClient.post()
                    .uri(builder -> builder.path(TestConstant.OKTA_ACCESS_TOKEN_URL)
                            .queryParams(paramsMap).build())
                    .headers(httpHeaders -> createAccessTokenAuthorizationHeader(httpHeaders)).exchange()
                    .flatMap(clientResponse -> clientResponse.bodyToMono(OktaResponse.class)).log().block();

            V2_ACCESS_TOKEN = result.getAccessToken();
            log.info("Access Token Created {}", V2_ACCESS_TOKEN);
        }
        return V2_ACCESS_TOKEN;
    }

    public static String getOktaSessionId() {
        if (StringUtils.isEmpty(V2_OKTA_SESSION_ID)) {

            OktaRequest request = new OktaRequest();
            request.setUsername(getSecretValue(TestConstant.OKTA_TEST_USERNAME));
            request.setPassword(getSecretValue(TestConstant.OKTA_TEST_USERPASSWORD));

            OktaResponse result = oktaRealClient.post()
                    .uri(builder -> builder.path(TestConstant.OKTA_SESSION_ID_URL).build())
                    .body(BodyInserters.fromValue(request))
                    .headers(httpHeaders -> createOktaSessionAuthorizationHeader(httpHeaders)).exchange()
                    .flatMap(clientResponse -> clientResponse.bodyToMono(OktaResponse.class)).log().block();

            V2_OKTA_SESSION_ID = Optional.ofNullable(StringUtils.trimToNull(result.getOktaSessionId()))
                    .orElseThrow(() -> new TestExecutionException("No Okta session id received."));
            log.info("Session Id Created {}", V2_OKTA_SESSION_ID);
        }
        return V2_OKTA_SESSION_ID;
    }

    public static LinkedMultiValueMap<String, String> getAccessTokenParamsMap() {
        LinkedMultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
        paramsMap.add("grant_type", "password");
        paramsMap.add("username", getSecretValue(TestConstant.OKTA_TEST_USERNAME));
        paramsMap.add("password", getSecretValue(TestConstant.OKTA_TEST_USERPASSWORD));
        paramsMap.add("scope", "openid offline_access profile");
        return paramsMap;

    }

    public static HttpHeaders createAccessTokenAuthorizationHeader(HttpHeaders headers) {
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_ALL);
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, getSecretValue(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE));
        return headers;
    }

    public static HttpHeaders createOktaSessionAuthorizationHeader(HttpHeaders headers) {
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
        return headers;
    }

    // This method returns filter function which will log request data
    protected static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            log.info("Request Body: {}", clientRequest.body());
            log.info("Count of Headers::{}, Count of Attributes::{}", clientRequest.headers().size(),
                    clientRequest.attributes().keySet().size());
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> log.info("Header {}={}", name, value)));
            clientRequest.attributes().keySet()
                    .forEach((key) -> log.info("Key {}={}", key, clientRequest.attributes().get(key)));
            return Mono.just(clientRequest);
        });
    }

    protected static ExchangeFilterFunction logResponseStatus() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }

    public RoomReservationResponse makeSuccessTestReservation() {
        makeTestPreReservation(true, null);
        return makeTestReservationWithAccertify().getBooked().get(0);
    }

    public ConsolidatedRoomReservationResponse makeTestReservationWithAccertify() {
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);

        Mono<ConsolidatedRoomReservationResponse> resultPost = realClient.post()
                .uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException(
                            "Error on reserving room for endpoint /v1/reserve/room, Error message : "
                                    + error.getMessage(),
                            error);
                }).flatMap(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateSuccessResponse(clientResponse, "/v1/reserve/room");
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("Reservation Response: {}", entity.getBody());
                        return convert(entity.getBody(), ConsolidatedRoomReservationResponse.class);
                    });

                });

        ConsolidatedRoomReservationResponse response = resultPost.block();
        log.info("Room reservation response::{}", response);

        if (null == response || CollectionUtils.isEmpty(response.getBooked())) {
            throw new PreconditionViolationException("Attempt to complete reservation failed. Response: " + response);
        }

        return response;
    }

    public String makeTestPreReservation(boolean withProgram, String mlifeNumber) {

        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());
        // request.setMlifeNumber(defaultTestData.getMlifeNumber());
        request.setMlifeNumber(mlifeNumber);
        ValidAvailabilityData data = getAvailabilityTestData(request, false, false);

        RoomCartRequest preReserveRequest = new RoomCartRequest();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        final LocalDate checkInLDate = LocalDate.parse(data.getCheckInDate(), formatter);
        final LocalDate checkOutLDate = LocalDate.parse(data.getCheckOutDate(), formatter);

        preReserveRequest.setCheckInDate(checkInLDate);
        preReserveRequest.setCheckOutDate(checkOutLDate);
        preReserveRequest.setNumGuests(defaultTestData.getNumAdults());
        preReserveRequest.setPropertyId(defaultTestData.getPropertyId());
        preReserveRequest.setRoomTypeId(data.getRoomTypeId());
        if (withProgram) {
            preReserveRequest.setProgramId(data.getProgramId());
        }
        log.info("Adding item to cart: {}", preReserveRequest);

        Mono<RoomReservationResponse> result = realClient.post().uri(builder -> builder.path("/v1/cart/room").build())
                .body(BodyInserters.fromValue(preReserveRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().doOnError(clientError -> {
                    throw new TestExecutionException(
                            "Error on adding room to cart, endpoint:/v1/reserve/room, Error message: "
                                    + clientError.getMessage(),
                            clientError);
                }).doOnSuccess(response -> validateSuccessResponse(response, "/v1/cart/room"))
                .flatMap(clientResponse -> clientResponse.bodyToMono(RoomReservationResponse.class));

        RoomReservationResponse response = result.block();
        log.info(ServiceConstant.X_STATE_TOKEN + ": {}", STATE_TOKEN);

        if (null == response || StringUtils.isEmpty(response.getItemId())) {
            throw new PreconditionViolationException("Attempt to add room to cart failed. Response: " + response);
        }

        return response.getItemId();

    }

    protected void addSecondRoomToCart() {
        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());

        ValidAvailabilityData data = getAvailabilityTestData(request, false, false);
        RoomCartRequest preReserveRequest = new RoomCartRequest();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        final LocalDate checkInLDate = LocalDate.parse(data.getCheckInDate(), formatter);
        final LocalDate checkOutLDate = LocalDate.parse(data.getCheckOutDate(), formatter);

        preReserveRequest.setCheckInDate(checkInLDate);
        preReserveRequest.setCheckOutDate(checkOutLDate);
        preReserveRequest.setPropertyId(defaultTestData.getPropertyId());
        preReserveRequest.setRoomTypeId(data.getSecondRoomTypeId());
        preReserveRequest.setProgramId(data.getProgramId());

        log.info("Adding item to cart: {}", preReserveRequest);

        Mono<RoomReservationResponse> result = realClient.post().uri(builder -> builder.path("/v1/cart/room").build())
                .body(BodyInserters.fromValue(preReserveRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().doOnError(clientError -> {
                    throw new TestExecutionException(
                            "Error on adding room to cart, endpoint:/v1/reserve/room, Error message: "
                                    + clientError.getMessage(),
                            clientError);
                }).doOnSuccess(response -> validateSuccessResponse(response, "/v1/cart/room"))
                .flatMap(clientResponse -> clientResponse.bodyToMono(RoomReservationResponse.class));

        RoomReservationResponse response = result.block();
        log.info(ServiceConstant.X_STATE_TOKEN + ": {}", STATE_TOKEN);

        if (null == response || StringUtils.isEmpty(response.getItemId())) {
            throw new PreconditionViolationException("Attempt to add room to cart failed. Response: " + response);
        }
    }

    public String makeTestPreReservationForJwb(boolean enableJwbHeader, boolean enableJwbCookie) {

        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());
        ValidAvailabilityData data = getAvailabilityTestData(request, enableJwbHeader, enableJwbCookie);

        RoomCartRequest preReserveRequest = new RoomCartRequest();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        final LocalDate checkInLDate = LocalDate.parse(data.getCheckInDate(), formatter);
        final LocalDate checkOutLDate = LocalDate.parse(data.getCheckOutDate(), formatter);

        preReserveRequest.setCheckInDate(checkInLDate);
        preReserveRequest.setCheckOutDate(checkOutLDate);
        preReserveRequest.setNumGuests(defaultTestData.getNumAdults());
        preReserveRequest.setPropertyId(defaultTestData.getPropertyId());
        preReserveRequest.setRoomTypeId(data.getRoomTypeId());
        preReserveRequest.setProgramId(data.getProgramId());

        log.info("Adding item to cart: {}", preReserveRequest);

        Mono<RoomReservationResponse> result = realClient.post().uri(builder -> builder.path("/v1/cart/room").build())
                .body(BodyInserters.fromValue(preReserveRequest)).headers(httpHeaders -> {
                    addAllHeaders(httpHeaders);
                    httpHeaders.add(TestConstant.ENABLE_JWB, "true");
                }).exchange().doOnError(clientError -> {
                    throw new TestExecutionException(
                            "Error on adding room to cart, endpoint:/v1/reserve/room, Error message: "
                                    + clientError.getMessage(),
                            clientError);
                }).doOnSuccess(response -> validateSuccessResponse(response, "/v1/cart/room"))
                .flatMap(clientResponse -> clientResponse.bodyToMono(RoomReservationResponse.class));

        RoomReservationResponse response = result.block();
        log.info(ServiceConstant.X_STATE_TOKEN + ": {}", STATE_TOKEN);

        if (null == response || StringUtils.isEmpty(response.getItemId())) {
            throw new PreconditionViolationException("Attempt to add room to cart failed. Response: " + response);
        }
        return response.getItemId();

    }

    public String makeTestPreReservationForJwbStagedUser(String customerEmailId, boolean enableJwbHeader,
            boolean enableJwbCookie) {
        client.post().uri("/v1/profile/deactivate/" + customerEmailId)
                .headers(httpHeaders -> addAllHeaders(httpHeaders)).exchange().expectStatus().isOk();

        client.delete().uri("/v1/profile/delete/" + customerEmailId).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk();

        return makeTestPreReservationForJwb(enableJwbHeader, enableJwbCookie);
    }

    protected RoomCartRequest prereserveRequestBuilder(ValidAvailabilityData data) {
        RoomCartRequest preReserveRequest = new RoomCartRequest();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        final LocalDate checkInLDate = LocalDate.parse(data.getCheckInDate(), formatter);
        final LocalDate checkOutLDate = LocalDate.parse(data.getCheckOutDate(), formatter);

        preReserveRequest.setCheckInDate(checkInLDate);
        preReserveRequest.setCheckOutDate(checkOutLDate);
        preReserveRequest.setPropertyId(data.getPropertyId());
        preReserveRequest.setRoomTypeId(data.getRoomTypeId());
        preReserveRequest.setProgramId(data.getProgramId());
        preReserveRequest.setNumGuests(defaultTestData.getNumAdults());
        return preReserveRequest;
    }

    public static <T> T getObjectFromJSON(String filePath, Class<T> target) {

        File requestFile = new File(BaseRoomBookingIntegrationTest.class.getResource(filePath).getPath());
        T obj = convert(requestFile, target);

        return obj;

    }

    public void addApiGtwyHeaders(HttpHeaders headers) {
        if (YES.equalsIgnoreCase(passThroughFlag)) {
            headers.add(TestConstant.X_API_KEY, apiKey);
            headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEB);
        }
    }

    public static void addAllHeaders(HttpHeaders headers) {
        addAllHeaders(headers, true, true, true);
    }

    public static void addAllHeaders(HttpHeaders headers, boolean addSignatureHeader) {
        addAllHeaders(headers, true, true, true);
        if (addSignatureHeader) {
            headers.add(TestConstant.X_SIGNATURE_HDR, getSecretValue(TestConstant.SECRET_VALUE_X_SIGN));
        }
    }

    public static void addAllHeaders(HttpHeaders headers, boolean addSignatureHeader, boolean addJwbHeader) {
        addAllHeaders(headers, addSignatureHeader);
        if (addJwbHeader) {
            headers.add(TestConstant.ENABLE_JWB, "true");
        }
    }

    public static void addAllHeaders(HttpHeaders headers, boolean addSourceHeader, boolean addTokenHeader,
            boolean addChannelHeader) {
        if (YES.equalsIgnoreCase(passThroughFlag)) {
            headers.add(TestConstant.X_API_KEY, apiKey);
        }
        if (addSourceHeader) {
            headers.add(TestConstant.HEADER_SOURCE_V1, "mgmresorts");
        }

        if (addChannelHeader) {
            headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_TESTCLIENT);
        }

        if (addTokenHeader) {
            createToken();
            headers.add(ServiceConstant.X_STATE_TOKEN, STATE_TOKEN);
        }
        headers.add(ServiceConstant.HEADER_SKIP_MYVEGAS_CONFIRMATION, ServiceConstant.TRUE);
    }

    public static void addAllHeaders(HttpHeaders headers, boolean addSourceHeader, boolean addTokenHeader,
            boolean addChannelHeader, boolean jwbHeaderValue) {
        if (YES.equalsIgnoreCase(passThroughFlag)) {
            headers.add(TestConstant.X_API_KEY, apiKey);
        }
        if (addSourceHeader) {
            headers.add(TestConstant.HEADER_SOURCE_V1, "mgmresorts");
        }

        if (addChannelHeader) {
            headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEB);
        }

        if (addTokenHeader) {
            createToken();
            headers.add(ServiceConstant.X_STATE_TOKEN, STATE_TOKEN);
        }

        headers.add(TestConstant.ENABLE_JWB, BooleanUtils.toStringTrueFalse(jwbHeaderValue));
        headers.add(ServiceConstant.HEADER_SKIP_MYVEGAS_CONFIRMATION, ServiceConstant.TRUE);
    }

    public HttpHeaders addApiHeaders(HttpHeaders headers, Map<String, String> otherHeadersMap) {
        addApiGtwyHeaders(headers);

        if (otherHeadersMap != null) {
            for (Map.Entry<String, String> entry : otherHeadersMap.entrySet()) {
                headers.add(entry.getKey(), entry.getValue());
            }
        }
        return headers;
    }

    protected void validateGetRequestErrorDetails(String uri, String errorCode, String errorMsg) {
        validateGetRequestErrorDetails(uri, errorCode, errorMsg, true, true, true);
    }

    protected void validateMissingParametersErrorDetails(String uri, String errorCode, String errorMsg) {
        validateMissingParametersErrorDetails(uri, errorCode, errorMsg, true, true, true);
    }

    protected void validateGetRequestAPIGtwyErrorDetails(String uri, String errorAttribute) {
        validateGetRequestAPIGtwyErrorDetails(uri, errorAttribute, true, true, true);
    }

    protected void validateMissingParametersErrorDetails(String uri, String errorCode, String errorMsg,
            boolean addSourceHeader, boolean addTokenHeader, boolean addChannelHeader) {
        BodyContentSpec body = client.get().uri(uri)
                .headers(httpHeaders -> addAllHeaders(httpHeaders, addSourceHeader, addTokenHeader, addChannelHeader))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            if (StringUtils.isNotBlank(deploymentEnv) && deploymentEnv.equalsIgnoreCase(TestConstant.ENV_AZURE)) {
                body.jsonPath("$.error.code").isEqualTo(TestConstant.APIM_ERROR_CODE).jsonPath("$.error.message")
                        .isEqualTo(TestConstant.APIM_ERROR_DESC);
            } else {
                body.jsonPath("$.code").isEqualTo(errorCode).jsonPath("$.msg").isEqualTo(errorMsg);
            }
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()));
        }

    }

    protected void validateGetRequestErrorDetails(String uri, String errorCode, String errorMsg,
            boolean addSourceHeader, boolean addTokenHeader, boolean addChannelHeader) {
        BodyContentSpec body = client.get().uri(uri)
                .headers(httpHeaders -> addAllHeaders(httpHeaders, addSourceHeader, addTokenHeader, addChannelHeader))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.code").isEqualTo(errorCode).jsonPath("$.msg").isEqualTo(errorMsg);
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()));
        }

    }

    protected void validateGetRequestAPIGtwyErrorDetails(String uri, String errorAttribute, boolean addSourceHeader,
            boolean addTokenHeader, boolean addChannelHeader) {
        BodyContentSpec body = client.get().uri(uri)
                .headers(httpHeaders -> addAllHeaders(httpHeaders, addSourceHeader, addTokenHeader, addChannelHeader))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.message").isEqualTo(String.format(TestConstant.API_GTWY_ERROR_MSG, errorAttribute));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    protected void validateShapeGetRequestIdAndTypeExists(String uri) {
        BodyContentSpec body = client.get().uri(uri).headers(headers -> {
            addAllHeaders(headers, true);
        }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].id").exists().jsonPath("$.[0].type").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    protected void validateGetRequestIdAndTypeExists(String uri) {
        validateGetRequestIdAndTypeExists(uri, true, true);
    }

    protected void validateGetRequestIdAndTypeExists(String uri, boolean addSourceHeader, boolean addTokenHeader) {
        BodyContentSpec body = client.get().uri(uri).headers(headers -> {
            addAllHeaders(headers);
        }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].id").exists().jsonPath("$.[0].type").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    protected void validateGetRequestNoHeaderTest(String uri) {
        validateGetRequestErrorDetails(uri, ErrorCode.RUNTIME_MISSING_SOURCE_HEADER.getErrorCode(),
                ErrorCode.RUNTIME_MISSING_SOURCE_HEADER.getDescription(), false, true, true);
        validateGetRequestErrorDetails(uri, ErrorCode.NO_TOKEN_IN_HEADER.getErrorCode(),
                ErrorCode.NO_TOKEN_IN_HEADER.getDescription(), true, false, true);
        validateGetRequestErrorDetails(uri, ErrorCode.INVALID_CHANNEL_HEADER.getErrorCode(),
                ErrorCode.INVALID_CHANNEL_HEADER.getDescription(), true, true, false);
    }

    protected void validatePostRequestErrorDetails(String uri, Object request, String errorCode, String errorMsg) {
        validatePostRequestErrorDetails(uri, request, errorCode, errorMsg, true, true, true);

    }

    protected void validatePostRequestErrorDetails(String uri, Object request, String errorCode, String errorMsg,
            boolean addSourceHeader, boolean addTokenHeader, boolean addChannelHeader) {
        BodyContentSpec body = client.post().uri(uri).body(BodyInserters.fromValue(request))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, addSourceHeader, addTokenHeader, addChannelHeader))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.code").isEqualTo(errorCode).jsonPath("$.msg").isEqualTo(errorMsg);
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()));
        }
    }

    protected void validatePostRequestNoHeaderTest(String uri, Object request) {
        validatePostRequestErrorDetails(uri, request, ErrorCode.RUNTIME_MISSING_SOURCE_HEADER.getErrorCode(),
                ErrorCode.RUNTIME_MISSING_SOURCE_HEADER.getDescription(), false, true, true);
        validatePostRequestErrorDetails(uri, request, ErrorCode.NO_TOKEN_IN_HEADER.getErrorCode(),
                ErrorCode.NO_TOKEN_IN_HEADER.getDescription(), true, false, true);
        validatePostRequestErrorDetails(uri, request, ErrorCode.INVALID_CHANNEL_HEADER.getErrorCode(),
                ErrorCode.INVALID_CHANNEL_HEADER.getDescription(), true, true, false);
    }

    protected BodyContentSpec validatePostRequestAttributesExists(String uri, Object request, String parentAttribute,
            String... childAttributes) {
        return validatePostRequestAttributesExists(uri, request, true, true, true, parentAttribute, childAttributes);
    }

    protected BodyContentSpec validatePostRequestAttributesExists(String uri, Object request, boolean addSourceHeader,
            boolean addTokenHeader, boolean addChannelHeader, String parentAttribute, String... childAttributes) {
        BodyContentSpec bodySpec = client.post().uri(uri).body(BodyInserters.fromValue(request)).headers(headers -> {
            addAllHeaders(headers, addSourceHeader, addTokenHeader, addChannelHeader);
        }).exchange().expectStatus().isOk().expectBody().consumeWith(body -> {
            log.error(body);
        });
        try {
            // validate parent attribute exists
            bodySpec.jsonPath(parentAttribute).exists();
            // validate that all the child attribute exists
            for (String attr : childAttributes) {
                bodySpec.jsonPath(parentAttribute + "." + attr).exists();
            }
            // Return the bodyspec for any further validation needed in the
            // calling
            // method
        } catch (Exception e) {
            throw new TestExecutionException("Json validation error occruured. Error message: " + e.getMessage()
                    + ", json response: " + new String(bodySpec.returnResult().getResponseBodyContent()));
        }
        return bodySpec;
    }

    protected BodyContentSpec validatePostRequestAttributesExists(String uri, Object request, boolean jwbHeaderValue,
            boolean jwbCookieValue, String parentAttribute, String... childAttributes) {
        boolean[] addHeaders = { true, true, true, jwbHeaderValue };
        return validatePostRequestAttributesExists(uri, request, addHeaders, jwbCookieValue, parentAttribute,
                childAttributes);
    }

    protected BodyContentSpec validatePostRequestAttributesExists(String uri, Object request, boolean[] addHeaders,
            boolean jwbCookieValue, String parentAttribute, String... childAttributes) {
        BodyContentSpec bodySpec = client.post().uri(uri).body(BodyInserters.fromValue(request)).headers(headers -> {
            addAllHeaders(headers, addHeaders[0], addHeaders[1], addHeaders[2], addHeaders[3]);
            headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=" + jwbCookieValue);
        }).exchange().expectStatus().isOk().expectBody().consumeWith(body -> {
            log.error(body);
        });
        // validate parent attribute exists
        bodySpec.jsonPath(parentAttribute).exists();
        // validate that all the child attribute exists
        for (String attr : childAttributes) {
            bodySpec.jsonPath(parentAttribute + "." + attr).exists();
        }
        // Return the bodyspec for any further validation needed in the calling
        // method
        return bodySpec;
    }

    protected BodyContentSpec validateGetRequestAttributesExists(String uri, String parentAttribute,
            String... childAttributes) {
        return validateGetRequestAttributesExists(uri, true, true, true, parentAttribute, childAttributes);
    }

    protected BodyContentSpec validateGetRequestAttributesExists(String uri, boolean addSourceHeader,
            boolean addTokenHeader, boolean addChannelHeader, String parentAttribute, String... childAttributes) {
        BodyContentSpec bodySpec = client.get().uri(uri).headers(headers -> {
            addAllHeaders(headers, addSourceHeader, addTokenHeader, addChannelHeader);
        }).exchange().expectStatus().is2xxSuccessful().expectBody();
        // validate parent attribute exists
        bodySpec.jsonPath(parentAttribute).exists();
        // validate that all the child attribute exists
        for (String attr : childAttributes) {
            bodySpec.jsonPath(parentAttribute + "." + attr).exists();
        }
        // Return the bodyspec for any further validation needed in the calling
        // method
        return bodySpec;
    }

    protected static void validateSuccessResponse(ClientResponse clientResponse, String baseURL) {
        if (!clientResponse.statusCode().is2xxSuccessful()) {
            throw new TestExecutionException("The request is unsuccessfull for url " + baseURL + ", Response code: "
                    + clientResponse.statusCode() + ", Transaction ID: " + clientResponse.headers().header("x-mgm-transaction-id"));
        }
    }

    protected static void validate4XXFailureResponse(ClientResponse clientResponse, String baseURL) {
        if (!clientResponse.statusCode().is4xxClientError()) {
            throw new TestExecutionException("The response should be 4xx message for " + baseURL + ", Response code: "
                    + clientResponse.statusCode());
        }
    }

    protected static String validateAndGetHeaderValue(ClientResponse clientResponse, String headerName, String step) {
        if (clientResponse.headers().header(headerName).isEmpty()) {
            throw new TestExecutionException(
                    step + headerName + " not returned as header, Response Status: " + clientResponse.statusCode());
        }
        return Optional.ofNullable(StringUtils.trimToNull(clientResponse.headers().header(headerName).get(0)))
                .orElseThrow(() -> new TestExecutionException(step + " empty value returned as header " + headerName
                        + ", Response Status: " + clientResponse.statusCode()));

    }

    /**
     * Takes an email and append it with a random number to make the email id as
     * unique.
     *
     * @param emailId
     *            test email id
     * @return random email id as a string
     */
    public static String getRandomEmailId(String emailId) {
        String randomEmailId = emailId;
        if (StringUtils.isNoneEmpty(emailId) && emailId.indexOf('@') != -1) {
            long random = Calendar.getInstance().getTimeInMillis();
            randomEmailId = emailId.replace("@", random + "@");
        }
        return randomEmailId;
    }

    public static <T> T convertToJson(File inputFile, Class<T> target) {
        try {
            ArrayList<Module> modules = new ArrayList<>();
            modules.add(new JavaTimeModule());
            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build();
            return mapper.readValue(getContent(new FileInputStream(inputFile)), target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    private static String getContent(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[10];
        StringBuilder sb = new StringBuilder();
        while (inputStream.read(buffer) != -1) {
            sb.append(new String(buffer));
            buffer = new byte[10];
        }
        inputStream.close();
        return sb.toString().replaceAll("currentdate",
                new SimpleDateFormat(ServiceConstant.ISO_8601_DATE_FORMAT, Locale.ENGLISH).format(new Date()));
    }

}
