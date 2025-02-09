package com.mgm.services.booking.room.constant;

public class TestConstant {

    public static final String URL_ROOM_AVAILABILITY = "/v1/room/rate-plans";
    public static final String URL_ADD_TO_CART = "/v1/cart/room";
    public static final String X_API_KEY = "x-api-key";
    public static final String OKTA_OSID = "osid";
    public static final String OKTA_ACCESS_TOKEN = "access-token";
    public static final String X_STATE_TOKEN  = "x-state-token";
    public static final String HEADER_CHANNEL_V1 = "channel";
    public static final String HEADER_SOURCE_V1 = "source";
    public static final String HEADER_CHANNEL_V2 = "x-mgm-channel";
    public static final String HEADER_SOURCE_V2 = "x-mgm-source";
    public static final String HEADER_STATE_TOKEN = "x-state-token";
    public static final String PROPERTIES_FILE_FORMAT = "%s-%s%s";
    public static final String PROPERTIES_FILE_SUFFIX = ".properties";
    public static final String TEST_DATA_FILE_FORMAT = "%s%s-%s";
    public static final String APP_PROPERTIES_FILE = "application.properties";
    public static final String APP_AWS_SECRETS_REGION = "application.awsSecretsRegion";
    public static final String APP_AWS_SECRET_NAME = "application.secretName";
    public static final String APP_AWS_PROFILE = "eb-roombooking-profle";
    public static final String CHANNEL_WEB = "web";
    public static final String CHANNEL_WEBCLIENT = "webclient";
    public static final String CHANNEL_TESTCLIENT = "testclient";
    public static final String API_GTWY_ERROR_MSG = "Missing required request parameters: [%s]";
    public static final String OKTA_BASE_URL = "https://mgmdmp.oktapreview.com";
    public static final String OKTA_SESSION_ID_URL = "/api/v1/sessions?additionalFields=cookieToken";
    public static final String OKTA_ACCESS_TOKEN_URL = "/oauth2/ausph7ezp3Gkkk8WN0h7/v1/token";
    public static final String OKTA_AUTHORIZATION_HEADER_VALUE = "okta-authorization-header";
    public static final String OKTA_TEST_USERNAME = "okta-test-userName";
    public static final String OKTA_TEST_USERPASSWORD = "okta-test-userPassword";
    public static final String X_SIGNATURE_HDR = "x-skey";
    public static final String SECRET_VALUE_X_SIGN = "shape-key";
    public static final String ENABLE_JWB = "enableJwb";
    public static final String JWB_CHECKOUT_REQUEST_FILENAME = "/room-checkout-requestbody-jwb.json";
    public static final String ICE = "ice";
    public static final String DUMMY_TRANSACTION_ID = "rbs-v2-it-test";
    public static final String EMPTY_RESPONSE = "Empty response object";
    public static final String EMPTY_ROOM_RESERVATION = "Empty room reservation object";
    public static final String EMPTY_BOOKING_OBJECT = "Empty booking object";
    public static final String EMPTY_CONFIRMATION_NUMBER = "Empty Confirmation number";
    public static final String EMPTY_SHARESWITH = "Empty shareWiths object";
    public static final String INCORRECT_SHARESWITH = "Incorrect number of shareWiths items";
    public static final String EMPTY_GUARANTEECODE = "Empty guaranteeCode";
    public static final String INCORRECT_BOOKINGS = "Incorrect number of bookings";
    public static final String INCORRECT_MARKETS = "Incorrect number of markets";
    public static final String INCORRECT_BILLINGS = "Incorrect number of billing objects";
    public static final String INCORRECT_PHONENUMBERS = "Incorrect number of phoneNumbers";
    public static final String INCORRECT_ADDRESSES = "Incorrect number of addresses";
    public static final String INCORRECT_SPECIAL_REQUESTS = "Incorrect specialRequests";
    public static final String INCORRECT_RESERVATIONS = "Incorrect number of total reservation (success/failures)";
    public static final String EMPTY_BILLING_OBJECT = "Empty billing object";
    public static final String EMPTY_PAYMENTS_OBJECT = "Empty payments object";
    public static final String INCORRECT_CURRENCYCODE = "Incorrect currency code";
    public static final String EMPTY_ACCEPTMESSAGE = "Empty acceptMessage for DCC transaction";
    public static final String EMPTY_AUTHAPPROVALCODE = "Empty authApprovalCode for DCC transaction";
    public static final String INCORRECT_CUSTOMERDOMINANTPLAY = "Incorrect customerDominantPlay";
    public static final String INCORRECT_SORT = "Incorrect room availability sorting";
    public static final String EMPTY_ALERTS_OBJECT = "Empty alerts object";
    public static final String EMPTY_TRACES_OBJECT = "Empty traces object";
    public static final String EMPTY_ROUTING_INSTRUCTIONS_OBJECT = "Empty routingInstructions object";
    public static final String INCORRECT_ALERT_ITEMS = "Incorrect number of alert items";
    public static final String INCORRECT_TRACE_ITEMS = "Incorrect number of trace items";
    public static final String INCORRECT_ROUTING_INSTRUCTION_ITEMS = "Incorrect number of routingInstruction items";
    public static final String[] headers = { ServiceConstant.X_MGM_CORRELATION_ID, ServiceConstant.X_MGM_TRANSACTION_ID,
            ServiceConstant.X_MGM_SOURCE, ServiceConstant.X_MGM_CHANNEL };
    public static final int ONE_NIGHT = 1;
    public static final int TWO_NIGHTS = 2;
    public static final int THREE_NIGHTS = 3;
    public static final String WEB = "web";
    public static final String MGM_RESORTS = "mgmresorts";
    public static final String MGMRI = "mgmri";
    public static final String CUSTOMER_ID = "customerId";
    public static final String DEPLOYMENT_ENV = "deploymentEnvironment";
    public static final String ENV_AZURE = "azure";
    public static final String APIM_ERROR_CODE = "632-1-104";
    public static final String APIM_ERROR_DESC = "Required source attribute is empty or invalid.";
    public static final String PROMO_CODE = "promoCode";
    public static final String PROGRAM_ID = "programId";
    public static final String PROPERTY_ID = "propertyId";
    public static final String CHECKIN_DATE = "checkInDate";
    public static final String CHECKOUT_DATE = "checkOutDate";
    public static final String NUM_ADULTS = "numAdults";
    public static final String PARTIAL_PROGRAM_ID = "partialProgramId";
    public static final String CASINO_PROMO_CODE = "casinoPromoCode";
    public static final String TRANSIENT_PROGRAM_ID = "transientProgramId";
    public static final String CASINO_PROPERTY_ID = "casinoPropertyId";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String STATUS_MUST_BE_AVAILABLE = "Status must be AVAILABLE";
    public static final String OFFER = "OFFER";
    public static final String STATUS_MUST_BE_OFFER = "Status must be OFFER";
    public static final String HEADER_KEY_USERNAME = "username";
    public static final String HEADER_KEY_PASSWORD = "password";
    public static final String HEADER_SKIP_MYVEGAS_CONFIRM = "skipMyVegasConfirm";
    public static final String INCORRECT_PROGRAM_ID = "Program id in the response should be %s";
    public static final String TEST_CANCEL_RESERVATION = "Test cancel reservation";
    public static final String JSON_VALIDATION_ERROR_MESSAGE = "Json validation error occurred. Error message: %1$s , json response: %2$s";
    public static final String PERPETUAL_PRICING = "perpetualPricing";
    public static final String JSONPATH_AVAILABILITY = "$.availability";
    public static final String JSONPATH_RATEPLANS = "$.ratePlans";
    public static final String TRUE_STRING = "true";
    public static final String EMPTY_PROFILE_ADDRESS = "Empty profile address";
    public static final String CONFIRMATION_NUMBER = "confirmationNumber";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ERROR_MESSAGE = "Error occurred on executing url : {}, Error Message : {}";
    public static final String ERROR_CODE_SHOULD_MATCH_MSG = "Error Code should match: ";
    public static final String CACHE_ONLY_KEY = "cacheOnly";

}
