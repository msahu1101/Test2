package com.mgm.services.booking.room.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

/**
 * Utility class to mask JSON data based on keys marked as sensitive
 *
 */
@UtilityClass
public class LogMask {
    private static final String JSON_REPLACEMENT_REGEX = "\"$1\":\"--REDACTED--\"";
    private static final String QUERY_REPLACEMENT_REGEX = "$1=--REDACTED--&";
    private static final String REGEX_DELIMITER = "|";

    private String[] jsonKeysForACRS = {
            "age", "identificationInfo", "personName", "addressLines", "unstructuredAddress", "cityName",
            "county", "stateProv", "countryName", "countryCode", "companyName", "telephones", "emails", "loyaltyId",
            "membershipId", "cardCode", "expireDate", "issuanceCountry", "multiFactorInfo", "comments"
            // commented these ACRS specific keywords because these are already masked in the base masking
            // "token", "postalCode", "cardHolderName"
     };

    private static final String PATTERN_REGEX_ACRS = "\"(%s)\":((?=\\[)\\[[^]]*\\]|(?=\\{)\\{[^\\}]*\\}|\\\"[^\"]*\\\"|\\d+)";
    private static final Pattern jsonPattern_ACRS = Pattern
            .compile(String.format(PATTERN_REGEX_ACRS, String.join(REGEX_DELIMITER, jsonKeysForACRS)));

    // Json keys which are expected to contain sensitive data
    private String[] jsonKeys = { "firstName", "lastName", "cardNumber", "number", "cvv", "expiry", "chargeCardExpiry",
            "cardHolder", "chargeCardHolder", "userName", "password", "phoneNumbers", "emailAddress1", "street1",
            "street2", "emailAddress", "emailAddress2", "address1", "address2", "city", "state", "country", "holder",
            "cardHolderName", "billingAddress1", "billingAddress2", "creditCardNumber", "postalCode", "chargeCardNumber",
            "creditCardExpireMonth", "creditCardExpireYear", "encryptedccToken", "phone", "paymentToken", "name",
            "token", "ccToken", "chargeCardMaskedNumber", "dateOfBirth", "nameOnCard", "phone", "paymentToken",
             "value", "givenName", "surname", "line1", "stateProvinceCode",
            "address", "phones", "Authorization",
            // ACRS additions:
            "issuanceLocation", "issuanceCountry", "namePrefix", "givenName", "surname", "middleName", "nameSuffix", "addressLines",
            "cityName", "stateProv", "countryName", "countryCode", "address", "age", "cardCode", "expireDate",
            "cardNumber", "cvvNumber", "userLastName", "userFirstName", "city", "state", "postalCode", "country"};


    private static final String PATTERN_REGEX = "\"(%s)\":\\s?\\[?\"([^\"]+)\"\\]?";
    private static final String QUERY_PATTERN_REGEX = "(%s)=([^&]+)&?";
    private static final Pattern jsonPattern = Pattern
            .compile(String.format(PATTERN_REGEX, String.join(REGEX_DELIMITER, jsonKeys)));
    private static final Pattern queryPattern = Pattern
            .compile(String.format(QUERY_PATTERN_REGEX, String.join(REGEX_DELIMITER, jsonKeys)));

    /**
     * Redact the sensitive information from attributes marked to sensitive in
     * nature
     * 
     * @param message
     *            Message to be masked
     * @return Returns masked message
     */
    public String mask(String message) {
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = jsonPattern.matcher(message);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, JSON_REPLACEMENT_REGEX);

        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public String maskACRSMessage(String message) {
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = jsonPattern_ACRS.matcher(message);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, JSON_REPLACEMENT_REGEX);

        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Redact the sensitive information from map values
     * 
     * @param params
     *            Map of params
     * @return Returns update map with redacted values
     */
    public Map<String, String> mask(Map<String, String> params) {
        Map<String, String> redactedParams = new HashMap<>();
        redactedParams.putAll(params);
        redactedParams.keySet().forEach(key -> redactedParams.put(key, "--REDACTED--"));

        return redactedParams;
    }
    
    /**
     * Redact the sensitive information from map values
     * 
     * @param params
     *            Map of params
     * @return Returns update map with redacted values
     */
    public String maskQueryString(String message) {
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = queryPattern.matcher(message);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, QUERY_REPLACEMENT_REGEX);

        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
