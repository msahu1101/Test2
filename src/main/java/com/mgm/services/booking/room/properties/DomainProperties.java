package com.mgm.services.booking.room.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration class to read domain related properties from
 * application.properties file with "domain" prefix
 */
@Component
@ConfigurationProperties(
        prefix = "domain")
public @Data class DomainProperties {

    private String phoenix;
    private String aem;
    private String contentapi;
    private String orms;
    private String okta;
    private String accertify;
    private String tokenize;
    private String crs;
    private String idms;
    private String itinerary;
    private String eventGrid;
    private String playstudio;
    private String refData;
    private String ns;
    private String myVegas;
    private String enrSearch;
    private String crsUcpRetrieveResv;
    private String loyalty;
    private String paymentOrchestration;
    private String ocrsSearchReservation;
    private String ocrsPartialReservationUpdate;
    private String cvsIdentity;
    private String cvs;
    private String operaRetrieve;
    private String paymentPPSOrchestration;
    private String inventoryService;
    private String partnerAccountBasic;
    private String partnerAccountSearch;
    private String packageComponentsSearch;
    private String profileService;
}
