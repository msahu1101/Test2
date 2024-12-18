package com.mgm.services.booking.room.filter;

/**
 * Request headers enum representing headers to be scanned.
 * 
 */
public enum RequestHeadersEnum {

    X_AMZN_TRACE_ID("X-Amzn-Trace-Id", "API_GTWYTRACEID"), X_AMZN_API_GTWY_ID("x-amzn-apigateway-api-id",
            "API_GTWYAPIID"), X_FRWD_FOR("X-Forwarded-For", "IP_ADDR"), X_MGM_TRANSACTION_ID("x-mgm-transaction-Id", "CLIENT_TRANSACTIONID"),
    		X_MGM_CORRELATION_ID("x-mgm-correlation-Id", "API_CORRELATIONID"), X_MGM_CHANNEL("x-mgm-channel", "REQ_CHANNEL"),
    		X_MGM_SOURCE("x-mgm-source", "REQ_SOURCE");

    private String header;
    private String logVal;

    RequestHeadersEnum(String header1, String logVal1) {
        this.header = header1;
        this.logVal = logVal1;
    }

    public String getHeader() {
        return header;
    }

    public String getLogVal() {
        return logVal;
    }

}
