package com.mgm.services.booking.room.constant;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface to define constants used across the application.
 *
 */
public final class ServiceConstant {

	public static final int DEFAULT_GUESTS = 2;
	public static final String MESSAGE_TYPE_ERROR = "error";
	public static final String MESSAGE_TYPE_WARN = "warn";
	public static final String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";
	public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd";
	public static final String MMYY_DATE_FORMAT = "MMyy";
	public static final String DEFAULT_TIME_ZONE = "America/Los_Angeles";
	public static final String UTF_8 = "UTF-8";
	public static final String HEADER_SOURCE = "source";
	public static final String EMPTY_STRING = "";
	public static final String WHITESPACE_STRING = " ";
	public static final String HEADER_TRANSACTION_ID = "x-mgm-transaction-id";
	public static final String EMAIL_HOSTURL = "hostUrl";
	public static final String EMAIL_URI_SCHEME = "uriScheme";
	public static final String EMAIL_CUSTOMERFNAME = "firstName";
	public static final String EMAIL_CUSTOMERCARDTYPE = "cardType";
	public static final String EMAIL_CARDBILLING = "cardBilling";
	public static final String EMAIL_BILLING_START = "billingLineStart";
	public static final String EMAIL_BILLING_END = "billingLineEnd";
	public static final String EMAIL_DISPLAYNONE = "display:none !important; max-height: 0px; font-size: 0px; overflow: hidden; mso-hide: all";
	public static final String EMAIL_BILLINGLINE_START = "<span style='display:none !important; max-height: 0px; font-size: 0px; overflow: hidden; mso-hide: all'>";
	public static final String EMAIL_BILLINGLINE_END = "</span>";
	public static final String EMAIL_BLANKCARD = "----";
	public static final String EMAIL_CUSTOMERCARDNUM = "cardNumber";
	public static final String EMAIL_FROMCUSTOMERMAIL = "fromCustMail";
	public static final String EMAIL_TOCUSTOMERMAIL = "toCustMail";
	public static final String EMAIL_CONFIRMATIONNUM = "confirmationNumber";
	public static final String EMAIL_CONFNUM = "confirmationNum";
	public static final String EMAIL_DEFAULT_IMAGE = "image";
	public static final String EMAIL_PROPID = "propertyId";
	public static final String EMAIL_ROOMNAME = "roomName";
	public static final String EMAIL_PROPNAME = "propertyName";
	public static final String EMAIL_PHONENUM = "phoneNum";
	public static final String EMAIL_STAY_DURATION = "stayDuration";
	public static final String EMAIL_CHECKIN_DATE = "checkInDate";
	public static final String EMAIL_CHECKOUT_DATE = "checkOutDate";
	public static final String EMAIL_ROOM_RATE_TAX = "roomRateWithTax";
	public static final String EMAIL_RESORT_FEE = "resortFee";
	public static final String EMAIL_RES_TOTAL = "reservationTotal";
	public static final String EMAIL_AMT_PAID = "amountPaid";
	public static final String EMAIL_BALANCE_DUE = "balanceDue";
	public static final String EMAIL_ADA_COMPATIBLE = "adaCompatible";
	public static final String EMAIL_OFFER_NAME = "offerName";
	public static final String EMAIL_OFFER_DESC = "offerDescription";
	public static final String EMAIL_OFFER_CODE = "offerCode";
	public static final String EMAIL_PREPROMO_COPY = "prepromotionalCopy";
	public static final String EMAIL_ROOM_CONFIRMATION = "roomConfirmationString";
	public static final String EMAIL_ROOM_RESERVATIONS = "roomReservations";
	public static final String EMAIL_CANCEL_CONFIRMATION = "cancelConfirmation";
	public static final String EMAIL_CANCEL_DATE = "cancelDate";
	public static final String EMAIL_AMT_REFUND = "amountRefunded";
	public static final String EMAIL_ROOM_REQUESTS = "roomRequests";
	public static final String EMAIL_ROOM_REQUESTS_PRICE = "roomRequestsPrice";
	public static final String EMAIL_FREE_CANCEL_DATE = "refundDate";
	public static final String EMAIL_DEPOSIT_FORFEIT = "depositForfeit";
	public static final String EMAIL_ITINERARY_LINK = "viewItineraryLink";
	public static final String EMAIL_RESERVE_PHONE = "reservationPhoneNum";
	public static final String EMAIL_RESORT_FEE_AVG_PER_NIGHT = "resortFeeAvgPerNight";
	public static final String EMAIL_OCCUPANCY_FEE_TOTAL = "occupancyFeeTotal";
	public static final String EMAIL_OCCUPANCY_FEE_AVG_PER_NIGHT = "occupancyFeeAvgPerNight";
	public static final String EMAIL_TOURISM_FEE_TAX_TOTAL = "tourismFeeTaxTotal";
	public static final String EMAIL_CASINO_SURCHARGE_TAX_TOTAL = "casinoHotelSurchargeFeeTotal";
	public static final String EMAIL_TOURISM_FEE_AVG_PER_NIGHT = "tourismFeeAvgPerNight";
	public static final String EMAIL_CASINO_SURCHARGE_AVG_PER_NIGHT = "casinoHotelSurchargeFeeAvgPerNight";
	public static final String X_STATE_TOKEN = "x-state-token";
	public static final String X_STATE_TOKEN_EXPIRES = "x-state-token-expires";
	public static final String EXPIRES_COOKIE = "expires-cookie";
	public static final String HTTPS = "https";
	public static final String SET_COOKIE = "Set-Cookie";
	public static final String COOKIE = "Cookie";
	public static final String X_API_KEY = "x-api-key";
	public static final String HEADER_CHANNEL = "channel";
	public static final String TOKEN_URL = "/v1/token";
	public static final String TOKEN_V2_URL = "/v2/token";
	public static final int SESSION_EXPIRY = 3600;
	public static final String SESSON_NAMESPACE = "booking";
	public static final String PAYMENT_TYPE_CREDIT_CARD = "Credit Card";
	public static final String CURRENCY_USD = "USD";
	public static final String CONFIRMATION_PENDING = "PENDING";
	public static final String TRANSACTION_TYPE_ONLINE = "Online";
	public static final String TIME_ZONE_FORMAT_WITH_LOCALE = "yyyy-MM-dd HH:mm:ssZ";
	public static final String INVALID_TX_EMAIL = "reject@mgmgrand.com";
	public static final String RTC_RATES_FORMAT = "0.00#";
	/** The date format with time. */
	public static final String DATE_FORMAT_WITH_TIME = "yyyy-MM-dd'T'HH:mm:ssZZZ";
	public static final String DATE_FORMAT_WITH_TIME_SECONDS = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String TRUE = "true";
	public static final String HEADER_SKIP_MYVEGAS_CONFIRMATION = "skipMyVegasConfirm";
	public static final String HEADER_TRACE_ID = "trace-id";
	public static final String HEADER_KEY_AUTHORIZATION = "Authorization";
	public static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_KEY_API_KEY = "x-api-key";
	public static final String KEYVAULT_RBS_KEY = "rbs-secrets";

	public static final String MASKING_FIELDS_ARRAY = "maskingFieldsArray";

	public static final String SERVER_USERNAME = "myvegas-jms-serverUserName";
	public static final String SERVER_USERPWD = "myvegas-jms-serverUserPassword";
	public static final String JMS_USERNAME = "myvegas-jms-jmsUserName";
	public static final String JMS_USERPWD = "myvegas-jms-jmsUserPassword";
	public static final String ACCERTIFY_APIKEY = "apigateway-accertify-key";
	public static final String OKTA_API_TOKEN = "okta-api-token";
	public static final String AURORA_USER = "aurora-user";
	public static final String AURORA_PWD = "aurora-password";
	public static final String OKTA_CLIENT_ID = "okta-client-id";
	public static final String HEADER_OKTA_SESSION_ID = "osid";
	public static final String HEADER_OKTA_ACCESS_TOKEN = "access-token";
	public static final String OKTA_CLIENT_SECRET = "okta-client-secret";
	public static final String OKTA_TRANSIENT_FLAG = "okta-transient-flag";
	public static final String COUNTRY_US = "US";
	public static final String COUNTRY_CANADA = "CA";
	public static final int DEFAULT_PRECISION = 2;
	public static final String HEADER_AUTHORIZATION = "Authorization";
	public static final String DESTINATION_HEADER = "destHeaders";
	public static final String HEADER_X_AUTHORIZATION = "x-authorization";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_ACCEPT = "Accept";
	public static final String HEADER_CLIENT_REF = "Ama-Client-Ref";
	public static final String PO_FLAG = "perpetualOffer";
	public static final String CONTENT_TYPE_URLENCODED = "application/x-www-form-urlencoded";
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_TYPE_ALL = "*/*";
	public static final String HEADER_AUTH_BASIC = "Basic ";
	public static final String HEADER_AUTH_BEARER = "Bearer ";
	public static final String HEADER_AUTH_OKTA_CUSTOM = "SSWS ";
	public static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
	public static final String STRICT_TRANSPORT_SECURITY_DEFAULT = "max-age=15600000";
	public static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
	public static final String CONTENT_SECURITY_POLICY_DEFAULT = "default-src 'self';";
	public static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
	public static final String X_CONTENT_TYPE_OPTIONS_DEFAULT = "nosniff";
	public static final String HEADER_X_XSS_PROTECTION = "X-XSS-Protection";
	public static final String X_XSS_PROTECTION_DEFAULT = "1; mode=block";
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	public static final String ITINERARY_FOR_PARTYRESERVATION = "Itinerary-For-PartyReservation";
	public static final String OCCUPANCY_FEE_ITEM = "OCC";
	public static final String TOURISM_FEE_ITEM = "TOR";
	public static final String CASINO_SURCHARGE_ITEM = "COS";
	public static final List<String> ACRS_OCCUPANCY_FEE_ITEM = Arrays.asList("OCFEE", "OCTAX");
	public static final List<String> ACRS_TOURISM_FEE_ITEM = Arrays.asList("TPFEE", "TPFTX");
	//Todo: add the casino charge and tax
	public static final List<String> ACRS_CASINO_SURCHARGE_ITEM = Arrays.asList("COSCH","COSTX");
	public static final String[] MY_VEGAS_KEYWORDS = new String[]{"MYVEGAS", "MY VEGAS"};
	public static final String CVS_CLIENT_ID = "cvs-client-id";
	public static final String CVS_CLIENT_SECRET = "cvs-client-secret";
	public static final String CVS_MLIFENO_PARAM = "mlifeNo";

	// CRS API Headers
	public static final String HEADER_CRS_AMA_API_VERSION = "Ama-Api-Version";
	public static final String HEADER_CRS_AMA_POS = "Ama-Pos";
	public static final String HEADER_CRS_AMA_OWNER = "Ama-Reservation-Owner";
	public static final String IATA_AGENT_DETAILS = "{\"agencyDetails\": {\"identifiers\": [{\"type\":\"IATA\", \"id\":\"%s\"}]}}";
	public static final String RRUPSELL_DETAILS = "{\"hotelSource\":\"%s\",\"callCenterInfo\":[{\"id\":\"%s\",\"type\":\"VDN\"}]}";
	public static final String HOTEL_CODE = "{\"hotelSource\":\"";
	public static final String HEADER_CRS_AMA_CHANNEL_IDENTIFIERS = "Ama-Channel-Identifiers";
	public static final String HEADER_CRS_AMA_CHANNEL_IDENTIFIERS_VALUE = "{\"vendorCode\":\"%s\",\"reportingData\":{\"channelClass\":\"%s\",\"channelType\":\"%s\",\"channel\":\"%s\",\"subChannel\":\"%s\"}}";
	public static final String VENDOR_CODE = "{\"vendorCode\":\"";

	/* CRS REQUEST PARAMS */
	public static final String CRS_RESERVATION_CFNUMBER = "cfNumber";
	public static final String CRS_PROPERTY_CODE = "property_code";
	public static final String CRS_START_DATE = "start_date";
	public static final String CRS_END_DATE = "end_date";
	public static final String CRS_DURATION = "duration";
	public static final String CRS_CHAIN_CODE = "chain_code";
	public static final String CRS_RESERVATION_LINKID = "linkId";
	public static final String HEADER_OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
	public static final String ACRS_ENVIRONMENT = "acrsEnvironment";
	public static final String ENR_ENVIRONMENT = "enrEnvironment";
	public static final String APIGEE_ENVIRONMENT = "apigeeEnvironment";
	public static final String ACRS_VERSION = "acrsVersion";
	public static final String ACRS_CHAINCODE = "acrsChainCode";
	public static final String ACRS_GAMING_ROUTING = "routingCode";
	public static final String ACRS_PO_ROUTING = "mgm-ga";
	public static final String ACRS_NONPO_ROUTING = "mgm";
	public static final String ACRS_MLIFE_PROGRAM_NAME = "PC";

	/* CRS MAPPING Values */
	public static final String NUM_ADULTS_MAP = "AQC10";
	public static final String NUM_CHILD_MAP = "AQC8";
	public static final String REFUND = "REFUND";
	public static final String DATE_FORMAT_WITH_12_HOUR_NOTATION = "MMM d, yyyy hh:mm a";
    public static final String DATE_FORMAT = "MMM d, yyyy";
	public static final String PERUSE_CHECKOUT_PRICING_FREQ = "DayOfCheckOut";
	public static final String NIGHTLY_PRICING_APPLIED = "NIGHTLY";
	public static final String CHECKIN_PRICING_APPLIED = "CHECKIN";
	public static final String CHECKOUT_PRICING_APPLIED = "CHECKOUT";
	//Function app environment value for credit card tokenize
	public static final String CRSUCPRETRIEVE_ENVIRONMENT= "crsUcpRetrieveResvEnvironment";
	public static final String HEADER_CLIENT_ID = "client_id";
	public static final String HEADER_CLIENT_SECRET = "client_secret";
	public static final String HEADER_GRANT_TYPE = "grant_type";
	public static final String HEADER_SCOPE = "scope";
	public static final String ROOM_BOOKING_SERVICE_DOMAIN_CODE = "632";
	public static final String SESSION_AGE = "session-age";
	public static final String ACRS_CATEGORY_CASINO = "Casino";
	public static final String ACRS_CATEGORY_TRANSIENT = "Transient";

	public static final String PRODUCT_CODE_SR = "SR";

	public static final int NUM_1000 = 1000;
	public static final int NUM_16 = 16;
	public static final int NUM_65536 = 65536;
	public static final int NUM_128 = 128;
	public static final int NUM_3PM = 15;
	public static final String X_MGM_CORRELATION_ID = "x-mgm-correlation-id";
	public static final String X_PARTNER_CORRELATION_ID = "x-correlation-id";
	public static final String X_MGM_TRANSACTION_ID = "x-mgm-transaction-id";
	public static final String X_MGM_TRANSACTIONID = "x-mgm-transactionID";
	public static final String X_MGM_SOURCE = "x-mgm-source";
	public static final String X_MGM_CHANNEL = "x-mgm-channel";
	public static final String APPLICABLE_PHONE_TYPE = "Home,Business,Fax,Pager,Mobile,Other";
	public static final String APPLICABLE_ADDRESS_TYPE = "Home,Business,Alternate,Other";
	public static final String APPROVED = "APPROVED";
	public static final String IDMS_ENVIRONMENT = "idmsEnvironment";
	public static final String PAYMENT_ENVIRONMENT = "paymentEnvironment";
	public static final String TOKEN_PATH = "tokenPath";
	public static final String RESERVATION_ID = "reservationId";
	public static final String CARD_EXP_PATH = "cardExpireDate";
	public static final String GRANT_TYPE_VALUE = "client_credentials";

	public static final int ACRS_DAY_LIMIT = 62;

	public static final String MLIFE_NUM = "mlifeNumber";
	public static final String CUSTOMER_ID = "customerId";
	public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	public static final String KEY_TRACKE = "tracke";

	public static final String IDMS_CLIENT_ID = "idms-client-id";
	public static final String IDMS_CLIENT_SECRET = "idms-client-secret";
	public static final String PAYMENT_TOKENIZE_API_KEY = "payment-tokenize-api-key";
	public static final String PAYMENT_ENCRYPTION_PUBLIC_KEY = "freedompay-public-key";
	public static final char EQUAL = '=';
	public static final char COLON = ':';
	public static final char SEMICOLON = ';';

	public static final String CVS_VALUES_DELIMITER = "|";
	public static final String INVALID = "invalid";
	public static final String UNRANKED_CVS_VALUE = "unranked";

	public static final String IDMS_TOKEN_SCOPE_CLAIM = "scp";
	public static final String IDMS_TOKEN_GROUP_CLAIM = "groups";
	public static final String PARTY_CONFIRMATION_NUMBER_PREFIX = "PRTY_";

	// Place holders constants for Urls
	public static final String ENVIRONMENT_PH = "environment";
	public static final String CONFIRMATION_NUMBER_PH = "confirmationNumber";
	public static final String ITINERARY_ENVIRONMENT_PH = "itineraryEnvironment";
	public static final String NS_ENVIRONMENT_PH = "nsEnvironment";
	public static final String ITINERARY_ID_PH = "itineraryId";
	public static final String REDEMPTION_CODE_PH = "redemptionCode";
	public static final String RESERVATION_DATE_PH = "reservationDate";
	public static final String MYVEGAS_ENVIRONMENT_PH = "myVegasEnvironment";
	public static final String ITINERARY_RETRIEVE_PH = "roomConfirmationNumber";

	public static final String GUEST_TOKEN_TYPE = "mgm_guest";
	public static final String SERVICE_TOKEN_TYPE = "mgm_service";
	public static final String IDMS_TOKEN_MLIFE_CLAIM = "mlife";
	public static final String IDMS_TOKEN_GIVEN_NAME_CLAIM = "given_name";
	public static final String IDMS_TOKEN_FAMILY_NAME_CLAIM = "family_name";
	public static final String IDMS_TOKEN_EMAIL_CLAIM = "email";
	public static final String IDMS_TOKEN_CUSTOMER_ID_CLAIM = "com.mgm.gse.id";
	public static final String IDMS_TOKEN_CUSTOMER_TIER_CLAIM = "com.mgm.loyalty.tier";
	public static final String IDMS_TOKEN_PERPETUAL_CLAIM = "com.mgm.loyalty.perpetual_eligible";
	public static final String IDMS_TOKEN_MGM_ID_CLAIM = "com.mgm.id";
	public static final String WEB = "Web";
	public static final String WEBBE = "WEBBE";
	public static final String BRANDED = "Branded";
	public static final String RATEPLN_CRSPROP_ID = "R:%s|CP:%s";
	public static final String COMP = "COMP";
	public static final String CASH = "CASH";
	public static final String AMEX_TYPE = "American Express";
	public static final String CRS_VIEW = "view";
	// Move this to ENUM if we are populating more scenarios
	public static final String SOLDOUT_STRING = "SO";
	public static final String COMP_STRING = "CO";
	public static final String CASH_STRING = "CA";
	public static final String CASH_CH = "CH";
	public static final String CASH_POWER_RANK_STRING = "CHS";	
	public static final String DD_CASH_TYPE_STRING = "DD";
	public static final String RESERVED_STRING = "RESERVED";
	public static final String ROUTING_INSTRUCTIONS_PROGRAM = "Program";

	public static final String ROOM_BOOKING_SERVICE = "ROOM_BOOKING_SERVICE";

	public static final String COMPONENT_FORMAT = "COMPONENT";
	public static final String ROOMFEATURE_FORMAT = "ROOM-FEATURE";
	public static final String SPECIALREQUEST_FORMAT = "SPECIAL-REQUEST";
	public static final String ROOM_FEATURE_SPECIAL_REQUEST  = "ROOM_FEATURE_SPECIAL_REQUEST";
	public static final String ROOMFEATURE_ACRS_TYPE = "FEA";
	public static final String SPECIALREQ_ACRS_TYPE = "SPE";
	public static final String SEGMENT_CANCEL = "CL";
	public static final String SEGMENT_DELETED_ON_MODIFY = "DEL_ON_MODIFY";
	public static final String GROUP_PROGRAM_CATEGORY = "Group";
	public static final String FLAT_LIMITTYPE_ICE = "Value";
	public static final Double ZERO_DOUBLE_VALUE = 0.0;
	public static final int THREE_DECIMAL_PLACES = 3;
	public static final String OPERA_UNALLOWED_CHAR_REGEX = "[!@#$%^&*()=+\\\\[\\\\]{}\\\\|?><\\\",.?`~*\\\\+]";
	public static final int TWO_DECIMAL_PLACES = 2;
	public static final String ROOM_CHARGE_ITEM = "RoomCharge";
	public static final String DEFAULT_RESV_CONTEXT = "42326";
    public static final String MGM_STR = "MGM";
    public static final long CANCEL_MAIN_ADDON_SEC_DIFF = 6;
    public static final String ICECC = "ICECC";
	public static final String ICE = "ICE";
	public static final String INVALID_SOURCE ="Invalid Source Header Is provided.";
    public static final String  EN ="en";
    public static final String ALERT_AREA_CODE = "ALERT_AREA_CODE";
    public static final String ALERT_CODE = "ALERT_CODE";
    public static final String TRACE_DEPARTMENT = "TRACE_DEPARTMENT";
    public static final String REFDATA_SPECIAL_REQUEST_TYPE = "SPECIAL_REQUEST";
    public static final String REFDATA_ROOM_FEATURE_TYPE = "ROOM_FEATURE";
	public static final String ACRS_AUTH_CLIENT_ID_ICE = "acrs-auth-client-id-ice";
	public static final String ACRS_AUTH_CLIENT_SECRET_ICE = "acrs-auth-client-secret-ice";
	public static final String ACRS_AUTH_CLIENT_ID_WEB = "acrs-auth-client-id-web";
	public static final String ACRS_AUTH_CLIENT_SECRET_WEB = "acrs-auth-client-secret-web";
	public static final String OPERA_ACRS_RESV_REF_TYPE="AMADEUSCRS";
	public static final String OPERA_GSE_RESV_REF_TYPE="AURORA";
	public static final String OCRS_TIMEZONE = "UTC";
	public static final String ACRS_RESORT_FEE = "RSFEE";
	public static final String SLOT = "SLOT";
	public static final String SLOTS = "Slots";
	public static final String SLOT_RES = "Slot";
	public static final String HTTP_METHOD_PATCH = "PATCH";
	public static final String HTTP_METHOD_GET = "GET";
	public static final String DEPENDENT_SYSTEM_TYPE = "Dependent_System";
	public static final String ACRS_DEPENDENT_SYSTEM = "ACRS";
	public static final String PAYMENT_DEPENDENT_SYSTEM = "Payment";
	public static final String PAYMENT_STATUS_SETTLED = "Settled";
	public static final String ITINERARY_DEPENDENT_SYSTEM = "Itinerary";
	public static final String API_NAME_TYPE = "API_Name";
	public static final String API_URL_TYPE = "API_URL";
	public static final String TIME_TYPE = "Time";
	public static final String DURATION_TYPE = "Duration";
	public static final String HTTP_STATUS_CODE = "HTTP_Status";
	public static final String OXI_ERROR_MESSAGE = "OXI Error<The Http Server replied with a 5XX status code>";
    public static final Pattern CUST_ID_NOT_FOUND_ERROR_MSG_PATTERN = Pattern
            .compile("\\<MalformedRequestDTO\\>\\[customer id '[0-9]*' not found\\<-1\\>\\]");
    public static final String WEB_CANCELLATION_REASON = "WEB-CANCEL";
    public static final String HDE_PACKAGE = "HDE";
	public static final String CANCELED = "CANCELED";
	public static final String INHOUSE = "INHOUSE";
	public static final String NOSHOW = "NOSHOW";
	public static final String CHECKEDOUT = "CHECKEDOUT";
	public static final String WAITLISTED = "WAITLISTED";
	public static final String F1_COMPONENT_CASINO_START_TAG = "C:";
	public static final String F1_COMP_TAG = "F124Q2COMP";
	public static final String F1_COMPONENT_START_F1 = "F1";
	public static final String F1_COMPONENT_START_HDN = "HDN";
	public static final char F1_COMPONENT_TICKET_COUNT_START = 'Q';
	public static final String F1_COMPONENT_TRANSIENT_START_TAG = "D:";
	public static final String F1_COMPONENT_TCOLV_START_TAG = "T:";
	public static final String INVENTORY_SERVICE_INVALID_HOLDID_ERROR = "640-2-400";
	public static final String RBS_CHANNEL_HEADER = "RBS";
	public static final String OCRS_AURORA_RESV_REF_TYPE = "AURORA";
	public static final String MIRAGE_OXI_DEACTIVATED_MESSAGE = "PROPERTY_160_DEACTIVATED_IN_OXI";
	public static final String GOLDSTRIKE_OXI_DEACTIVATED_MESSAGE = "PROPERTY_345_DEACTIVATED_IN_OXI";
	public static final String IDMS_TOKEN_PERPETUAL_ELIGIBLE_PROPERTY_IDS_CLAIM = "com.mgm.loyalty.perpetual_eligible_properties";
	public static final String AGENT_TEXT_NEWLINE = "\n";
    public static final String REDIS_KEY_DELIMITER = ":";
    public static final String ACRS_MYVEGAS_RATEPLAN = "TMYVG";
    public static final String PC_PROGRAM_CODE = "PC";
    public static final String HTTP_CALL_TIME_TAKEN = "HTTP {} request to {} , time taken : {} ms";

    private ServiceConstant() {
		// Hiding implicit constructor
	}
	//Redis read
	public static final String ROOM_STR = "ROOM";
	public static final String ROOMCODE_STR = "ROOMCODE";
	public static final String PROGRAM_STR = "PROGRAM";
	public static final String SEGMENT_STR = "SEGMENT";
	public static final String COMPONENT_STR = "COMPONENT";
	public static final String PROMO_STR = "PROMO";
	public static final String PTRN_PROMO_STR = "PTRNPROMO";
	public static final String GROUP_STR = "GROUP";
	public static final String PROPERTY_RANK_STR = "PROPERTY_RANK";
	public static final String EXTERNALCODE_STR = "EXTERNALCODE";
	public static final String PROPERTY_STR = "PROPERTY";
	public static final String REGION_STR = "REGION";
	public static final String ACRS_PROPERTY_TAXFEES_STR = "ACRSTAXNFEES";
	
	//partner account
	public static final String PARTNER_CLIENT_ID = "partner-client-id";
	public static final String PARTNER_CLIENT_SECRET = "partner-client-secret";
	public static final String PARTNER_VERSION_PARAM = "partnerVersion";
	public static final String PARTNER_GRANTTYPE_VALUE= "client_credentials";
	public static final String PARTNER_SCOPE_VALUE="openid+admin:WRITE_CONSUMERS+admin:READ_CONSUMERS";
	public static final String ACRS_HEADER_LOG_PREFIX = "Received headers from acrs {} API {} : {}";
	public static final String ACRS_RESPONSE_LOG_PREFIX = "Received response from acrs {} API {} : {}";
	public static final String ENR_HEADER_LOG_PREFIX = "Received headers from enr ratePlan search API {} : {}";
	public static final String ENR_RESPONSE_LOG_PREFIX = "Received response from enr ratePlan search API {} : {}";
	public static final String HTTP_ERROR_STATUS_CODE_LOG_PREFIX = "Unhandled HTTP Error: Status Code {}";
	public static final String HTTP_ERROR_MESSAGE_LOG_PREFIX = "Unhandled HTTP Error: Message {}";
	public static final String PET_HEADER_LOG_PREFIX = "Received headers from find reservation  Payment Token exchange {} : {}";
	public static final String PET_RESPONSE_LOG_PREFIX = "Received response from find reservation  Payment Token exchange {} : {}";
	public static final String REF_DATA_HEADER_LOG_PREFIX = "Received headers from refData API {} : {}";
	public static final String REF_DATA_RESPONSE_LOG_PREFIX = "Received response from refData API {} : {}";
	
	public static final String HEADER_FRAUD_AGENT_TOKEN = "Fraud-Agent-Token";
	public static final String HEADER_USER_AGENT= "User-Agent";
	public static final String CANCELLED_BY_MERCHANT = "CANCELED_BY_MERCHANT";
	public static final String COMPLETED = "COMPLETED";
	public static final String CANCELLED_BY_CUSTOMER = "CANCELED_BY_CUSTOMER";
	public static final String FAILED_TEXT = "FAILED";
	public static final String IN_AUTH_TRANSACTION_ID= "inAuthTransactionId";
	public static final String EXTERNAL_SESSION_ID= "sessionId";
	public static final String IP_ADDRESS = "ipAddress";
	public static final String X_USER_ID = "x-user-id";


	public static final String MSG_OPERA_CNF_NUMBER_MISSING =  "Opera confirmation number is missing";

	public static final String PARTNER_CUSTBASICINFO_REQ_PARAM="MGMRES";

	public static final String OXI_DEACTIVATED_MESSAGE = "DEACTIVATED_IN_OXI";

	public static final String MOVABLEINK_SOURCE = "movableink";

	public static final String  PKGCOMPONENT_STR = "PKGCOMPONENT";
	public static final String YEAR_PREFIX = "20";
	public static final String ACRS_PROPERTY_CODE = "acrsPropertyCode";
	public static final String NON_ROOM_INVENTORY_TYPE = "nonRoomInventoryType";
	public static final String API_VERSION = "apiVersion";
	public static final String CORE_VERSION= "coreVersion";
	public static final String SHOW_PKG_TYPE = "shows";
	public static final String PO_DEEPLINK_ENC_ALGORITHM= "AES";
	public static final String ENR_RATE_PLAN_BY_PROPERTY = "ENRRATEPLANBYPROPERTY";
	public static final String ENR_RATE_PLAN_BY_ID = "ENRRATEPLANBYID";

	public static final String ENR_RATE_PLAN_BY_CODE = "ENRRATEPLANBYCODE";

	public static final String ENR_RATE_PLAN_BY_CHANNEL = "ENRRATEPLANBYCHANNEL";
	public static final String ENR_RATE_PLAN_BY_PROPERTY_CHANNEL = "ENRCHANNELRATEPLANBYPROPERTYCHANNEL";
	public static final String ENR_PROMO_BY_PROPERTY = "ENRPROMOBYPROPERTY";
	public static final String ENR_REDIS_ENABLED = "true";
	public static final String ENR_PROMO_BY_CODE = "ENRPROMOBYCODE";

}
