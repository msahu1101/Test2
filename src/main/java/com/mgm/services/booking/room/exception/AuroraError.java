package com.mgm.services.booking.room.exception;

public enum AuroraError {

    NOERROR("NoError", AuroraError.SYSTEM_ERROR), SYSTEMERROR("SystemError", AuroraError.SYSTEM_ERROR),
    UNKNOWNERROR("UnknownError", AuroraError.SYSTEM_ERROR), BACKENDERROR("BackendError", AuroraError.SYSTEM_ERROR),
    BACKENDSTATUSERROR("BackendStatusError", AuroraError.SYSTEM_ERROR),
    BACKENDTECHNICALERROR("BackendTechnicalError", AuroraError.SYSTEM_ERROR),
    BACKENDSYSTEMERROR("BackendSystemError", AuroraError.SYSTEM_ERROR),
    BACKENDRESPONSEPARSEERROR("BackendResponseParseError", AuroraError.SYSTEM_ERROR),
    BACKENDUNKNOWNERROR("BackendUnknownError", AuroraError.SYSTEM_ERROR),
    INTERNALERROR("InternalError", AuroraError.SYSTEM_ERROR),
    ACCOUNTNOTFOUND("AccountNotFound", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCREDENTIALS("InvalidCredentials", AuroraError.FUNCTIONAL_ERROR),
    ACCOUNTNOTACTIVE("AccountNotActive", AuroraError.FUNCTIONAL_ERROR),
    ACCOUNTCREATIONFAILED("AccountCreationFailed", AuroraError.FUNCTIONAL_ERROR),
    INVALIDSECRETANSWER("InvalidSecretAnswer", AuroraError.FUNCTIONAL_ERROR),
    INVALIDSECRETANSWERATTEMPTSEXCEEDED("InvalidSecretAnswerAttemptsExceeded", AuroraError.FUNCTIONAL_ERROR),
    ACCOUNTALREADYEXISTS("AccountAlreadyExists", AuroraError.FUNCTIONAL_ERROR),
    INVALIDMLIFENUMBER("InvalidMlifeNumber", AuroraError.FUNCTIONAL_ERROR),
    SEARCHTOOBROAD("SearchTooBroad", AuroraError.FUNCTIONAL_ERROR),
    CUSTOMERNOTMLIFEMEMBER("CustomerNotMlifeMember", AuroraError.FUNCTIONAL_ERROR),
    CUSTOMERDOESNOTHAVESECRETQUESTIONS("CustomerDoesNotHaveSecretQuestions", AuroraError.FUNCTIONAL_ERROR),
    SECRETQUESTIONUPDATEFAILED("SecretQuestionUpdateFailed", AuroraError.FUNCTIONAL_ERROR),
    WEBEMAILUPDATEFAILED("WebEmailUpdateFailed", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCUSTOMERID("InvalidCustomerId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDROOMTYPEID("InvalidRoomTypeId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDPROGRAMID("InvalidProgramId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCOMPONENTID("InvalidComponentId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDREQUESTMESSAGESTRUCTURE("InvalidRequestMessageStructure", AuroraError.FUNCTIONAL_ERROR),
    INVALIDREQUESTMESSAGEVALUES("InvalidRequestMessageValues", AuroraError.FUNCTIONAL_ERROR),
    INVALIDPROPERTYID("InvalidPropertyId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDITINERARYID("InvalidItineraryId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDRESERVATIONID("InvalidReservationId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDRESERVATIONSTATE("InvalidReservationState", AuroraError.FUNCTIONAL_ERROR),
    INVALIDRATETABLENAME("InvalidRateTableName", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCREDITCARD("InvalidCreditCard", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCREDITCARDEXPIRATION("InvalidCreditCardExpiration", AuroraError.FUNCTIONAL_ERROR),
    CUSTOMERNOTTRANSIENT("CustomerNotTransient", AuroraError.FUNCTIONAL_ERROR),
    CUSTOMERNOTPATRON("CustomerNotPatron", AuroraError.FUNCTIONAL_ERROR),
    CHARGEAMOUNTMISMATCH("ChargeAmountMismatch", AuroraError.FUNCTIONAL_ERROR),
    BOOKINGFAILED("BookingFailed", AuroraError.FUNCTIONAL_ERROR),
    BOOKINGPARTIALLYFAILED("BookingPartiallyFailed", AuroraError.FUNCTIONAL_ERROR),
    NOPATRONASSOCIATEDWITHWEBACCOUNT("NoPatronAssociatedWithWebAccount", AuroraError.FUNCTIONAL_ERROR),
    BACKENDREJECTEDPRODUCTCODE("BackendRejectedProductCode", AuroraError.FUNCTIONAL_ERROR),
    BACKENDREJECTEDROOMCATEGORY("BackendRejectedRoomCategory", AuroraError.FUNCTIONAL_ERROR),
    BACKENDREJECTEDRATECODE("BackendRejectedRateCode", AuroraError.FUNCTIONAL_ERROR),
    BOOKINGNOTFOUND("BookingNotFound", AuroraError.FUNCTIONAL_ERROR),
    BADCHARACTERSINNEWCUSTOMERPIN("BadCharactersInNewCustomerPIN", AuroraError.FUNCTIONAL_ERROR),
    BADNEWCUSTOMEREMAILADDRESS("BadNewCustomerEmailAddress", AuroraError.FUNCTIONAL_ERROR),
    INVALIDAUTHMODE("InvalidAuthMode", AuroraError.FUNCTIONAL_ERROR),
    BACKENDNOTACCEPTINGPAYMENTS("BackendNotAcceptingPayments", AuroraError.FUNCTIONAL_ERROR),
    RESERVATIONNOTSUPPORTED("ReservationNotSupported", AuroraError.FUNCTIONAL_ERROR),
    INVALIDROUTINGAUTHORIZERID("InvalidRoutingAuthorizerId", AuroraError.FUNCTIONAL_ERROR),
    UNSUPPORTEDROUTINGLIMITTYPE("UnsupportedRoutingLimitType", AuroraError.FUNCTIONAL_ERROR),
    INVALIDGUARANTEECODE("InvalidGuaranteeCode", AuroraError.FUNCTIONAL_ERROR),
    INVALIDROOMPRICINGRULEID("InvalidRoomPricingRuleId", AuroraError.FUNCTIONAL_ERROR),
    CREDITCARDREQUIRED("CreditCardRequired", AuroraError.FUNCTIONAL_ERROR),
    INVALIDBLOCKCODE("InvalidBlockCode", AuroraError.FUNCTIONAL_ERROR),
    PAYMENTAUTHORIZATIONFAILED("PaymentAuthorizationFailed", AuroraError.FUNCTIONAL_ERROR),
    CREDITCARDCURRENCYCHECKFAILED("CreditCardCurrencyCheckFailed", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCREDITCARDCURRENCY("InvalidCreditCardCurrency", AuroraError.FUNCTIONAL_ERROR),
    INVALIDPATRONPROMOID("InvalidPatronPromoId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDTRAVELAGENTID("InvalidTravelAgentId", AuroraError.FUNCTIONAL_ERROR),
    INVALIDOPERAPROMOCODE("InvalidOperaPromoCode", AuroraError.FUNCTIONAL_ERROR),
    BLACKLISTRESERVATION("BlacklistReservation", AuroraError.FUNCTIONAL_ERROR),
    BOOKINGALREADYEXISTS("BookingAlreadyExists", AuroraError.FUNCTIONAL_ERROR),
    NOROOMINVENTORY("NoRoomInventory", AuroraError.FUNCTIONAL_ERROR),
    INVALIDHGPNUMBER("InvalidHgpNumber", AuroraError.FUNCTIONAL_ERROR),
    INVALIDBRIDGEPROPERTYMAPPING("InvalidBridgePropertyMapping", AuroraError.FUNCTIONAL_ERROR),
    NOTENOUGHPOINTS("NotEnoughPoints", AuroraError.FUNCTIONAL_ERROR),
    BUSINESSRULEVIOLATION("BusinessRuleViolation", AuroraError.FUNCTIONAL_ERROR),
    NRGRESERVATION("NRGReservation", AuroraError.FUNCTIONAL_ERROR),
    UNABLETOPRICETRIP("UnableToPriceTrip", AuroraError.FUNCTIONAL_ERROR),
    NODATAFOUND("NoDataFound", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCONTENTTYPE("InvalidContentType", AuroraError.FUNCTIONAL_ERROR),
    REQUESTTIMEDOUT("RequestTimedOut", AuroraError.SYSTEM_ERROR),
    UNSUPPORTEDOPERATION("UnsupportedOperation", AuroraError.FUNCTIONAL_ERROR),
    INVALIDSESSION("InvalidSession", AuroraError.SYSTEM_ERROR),
    COMMUNICATIONERROR("CommunicationError", AuroraError.SYSTEM_ERROR),
    NOTCONNECTED("NotConnected", AuroraError.SYSTEM_ERROR),
    CONCURRENTMODIFICATION("ConcurrentModification", AuroraError.SYSTEM_ERROR),
    INVALIDPARTITION("InvalidPartition", AuroraError.SYSTEM_ERROR),
    MALFORMEDREFERENCEDATA("MalformedReferenceData", AuroraError.SYSTEM_ERROR),
    MALFORMEDDTO("MalformedDTO", AuroraError.FUNCTIONAL_ERROR),
    MALFORMEDBACKENDDTO("MalformedBackendDTO", AuroraError.SYSTEM_ERROR),
    TYPECONSTRAINTVIOLATION("TypeConstraintViolation", AuroraError.SYSTEM_ERROR),
    CUSTOMERCACHEREPOSITORYMISMATCH("CustomerCacheRepositoryMismatch", AuroraError.SYSTEM_ERROR),
    INVALIDORIGIN("InvalidOrigin", AuroraError.FUNCTIONAL_ERROR),
    INVALIDCHANNELID("InvalidChannelId", AuroraError.FUNCTIONAL_ERROR),
    SERVERUNAVAILABLE("ServerUnavailable", AuroraError.SYSTEM_ERROR),
    SOURCESYSTEMUNAVAILABLE("SourceSystemUnavailable", AuroraError.SYSTEM_ERROR),
    UNAUTHORIZED("UnAuthorized", AuroraError.SYSTEM_ERROR),
    REQUESTEXPIREDINCACHE("RequestExpiredInCache", AuroraError.SYSTEM_ERROR);

    private String code;
    private String type;

    public static final String SYSTEM_ERROR = "System";
    public static final String FUNCTIONAL_ERROR = "Functional";

    AuroraError(String code, String type) {
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return this.code;
    }

    public String getType() {
        return this.type;
    }

    public static String getErrorType(String code) {
        for (AuroraError e : values()) {
            if (code.equals(e.code)) {
                return e.type;
            }
        }
        return code;
    }
}
