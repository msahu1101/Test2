/**
 * 
 */
package com.mgm.services.booking.room.model.request;

import java.io.Serializable;
import java.util.Locale;

import lombok.Data;

public abstract @Data class AbstractBaseRequest implements Serializable {

    private static final long serialVersionUID = -594063531042268184L;

    private String propertyId;
    private String customerEmail;
    private long customerId = -1;
    private Integer mlifeNo;
    private Locale locale;
    private String jsessionId;
    private String clientIp;
    private String hostUrl;
    private boolean isTransientUser = Boolean.FALSE;
    private boolean notSearchUserByMlifeNo;
    private String hgpNo;
    private String offerType;
    private String selectedPropertyId;
    private String cookieToken;
    private String oktaSessionId;
    private String oktaUserId;
    private String expiresAt;
    private boolean replaceTickets;
    private boolean caslOptin;
    private String overridePropertyId;
}
