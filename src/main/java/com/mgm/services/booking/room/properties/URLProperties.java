package com.mgm.services.booking.room.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class to read url/endpoint related properties from
 * application.properties file with "url" prefix
 */
@Component
@ConfigurationProperties(
        prefix = "url")
public @Data class URLProperties {

    private String phoenixRoomPrograms;
    private String phoenixRoomSegments;
    private String phoenixRoomByProperty;
    private String phoenixProperty;
    private String phoenixRoomComponents;
    private String aemRoomConfirmationTemplate;
    private String aemRoomCancellationTemplate;
    private String aemRatePlanPath;
    private String roomContentApi;
    private String propertyRoomContentApi;
    private String programContentApi;
    private String propertyContentApi;
    private String curatedOffersContentApi;
    private String packageConfigApi;
    private String roomReservationModify;
    private String oktaToken;
    private String iataValidation;
    private String accertifyAuthorization;
    private String accertifyConfirmation;
    private String oktaSessionValidation;
    private String oktaUserDetails;
    private String oktaAccessTokenValidation;
    private String itineraryDeepLink;
    private String tokenize;
    private String playerPromos;
    private String patronPromos;

    // ACRS additions
    private String acrsReservationsCreatePending;
    private String acrsReservationsConfCommit;
    private String acrsReservationsConfPending;
    private String acrsRetrieveReservation;
    private String acrsSearchReservations;
    private String acrsRetrievePendingReservation;
    private String acrsAvailabilityReservation;
    private String acrsMultiAvailabilityReservation;
    private String acrsCancelPendingReservation;
    private String acrsOrganizationSearch;
    private String acrsGroupSearch;
    private String acrsGroupRetrieve;
    private String acrsCalendarAvailabilitySearch;
    private String acrsSearchReservation;
    private String acrsSearchOffers;
    private String acrsAuthToken;
    private String acrsCreateReservationLink;
    private String acrsReservationLink;
    private String acrsModifyReservationLink;
    private String enrChannelRatePlanSearch;
    private String enrPromoChannelRatePlanSearch;

    private String aemSignupCompletionTemplate;
        
    private String itineraryService;
    private String itineraryCreate;
    private String itineraryRetrieve;
    private String playstudioValidate;
    private String playstudioConfirm;
    private String refDataRoutingAuthByPhoenixId; 
    private String refDataRoutingAuthByAppUserId;
    private String refDataRoutingAuthByAuthorizer;
    private String notificationService;
    private String myVegasValidate;
    private String myVegasConfirm;
    private String ocrsSearchReservation;
    private String refDataRoutingInfoSearch;
    private String ocrsPartialReservationUpdate;
    //INC-4
    private String refDataAlertAndTraceSearch;
    private String paymentService;
    private String crsUcpRetrieveResvUrl;
    private String modifyPendingPaymentExchangeToken; 
    private String findReservationPaymentExchangeToken;
    
    //Added as a part of CBSR-1 for HDE Packages
    private String aemHDEPackageRoomConfirmationTemplate;
    private String operaDetailsRetrieve;
    private String operaDetailsRetrieveProperty;

    //F1 integration
    private String getInventory;
    private String holdInventory;
    private String releaseInventory;
    private String commitInventory;
    private String rollbackInventory;
    private String statusInventory;
    private String statusInventoryHoldId;
    
    //partnerAccoun
    private String partnerAccountCustomerSearch;
    private String partnerAccountCustomerBasicInfo;
    private String partnerAccountAuth;

    //package components
    private String packageComponents;

}
