
/**
 * 
 */
package com.mgm.services.booking.room.dao.impl;

import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.exception.AuroraError;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.cache.PhoenixComponentsCacheService;
import com.mgm.services.booking.room.util.CCTokenDecryptionClient;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.messages.Credentials;
import com.mgmresorts.aurora.service.Client;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Abstract class to consolidate all the common functionalities with respect to
 * integration with Aurora services.
 *
 */
@Log4j2
public class AuroraBaseDAO extends BaseStrategyDAO {

    @Autowired
    private AuroraProperties properties;

    @Autowired
    private ApplicationProperties applicationProperties;
    
    @Autowired(required = false)
    private CCTokenDecryptionClient decryptionClient;

    @Autowired
    private PhoenixComponentsCacheService phoenixComponentsCacheService;

    private static Map<String, Client> auroraClients = new HashMap<String, Client>();

    @PostConstruct
    private void initializeAuroraConnections() {
        synchronized (this) {
            if (auroraClients.isEmpty()) {
                // Setting system properties, just bcos thats how client jar
                // works
                System.setProperty("aurora.app.name", properties.getAppName());
                System.setProperty("aurora.app.numpartitions", properties.getAppPartitions());
                System.setProperty("aurora.security.enabled", properties.getSecurityEnabled());
                System.setProperty("aurora.publickey", properties.getPublicKey());

                log.info(System.getProperty("aurora.app.name"));
                log.info(System.getProperty("aurora.app.numpartitions"));
                log.info(properties.getUrl());
                log.info(System.getProperty("aurora.security.enabled"));
                log.info(System.getProperty("aurora.publickey"));

                if (!applicationProperties.isGseDisabled()) {
                    for (AuroraProperties.AuroraCredential credential : properties.getChannelCredentials()) {
                        Credentials credentialsObj = new Credentials();
                        credentialsObj.setUsername(credential.getName());
                        credentialsObj.setPassword(credential.getCode());
                        Client auroraClient = new Client(credential.getName());
                        auroraClient.setResponseTimeout(properties.getResponseTimeout());
                        auroraClient = auroraClient.open(properties.getUrl(), credentialsObj);
                        if (null == auroraClient) {
                            log.error("Unable to open Aurora client connetion for : {}", credential.getName());
                        } else {
                            log.info("Opened Aurora client connetion for : {}", credential.getName());
                        }

                        auroraClients.put(credential.getKey().toLowerCase(), auroraClient);
                    }
                }
            }
        }

    }

    public void reinitializeAuroraConnections() {
        
        synchronized (this) {
            log.info(System.getProperty("aurora.app.name"));
            log.info(System.getProperty("aurora.app.numpartitions"));
            log.info(properties.getUrl());
            log.info(System.getProperty("aurora.security.enabled"));
            log.info(System.getProperty("aurora.publickey"));

            if (!applicationProperties.isGseDisabled()) {
                for (AuroraProperties.AuroraCredential credential : properties.getChannelCredentials()) {
                    Credentials credentialsObj = new Credentials();
                    credentialsObj.setUsername(credential.getName());
                    credentialsObj.setPassword(credential.getCode());
                    Client auroraClient = new Client(credential.getName());
                    auroraClient.setResponseTimeout(properties.getResponseTimeout());
                    auroraClient = auroraClient.open(properties.getUrl(), credentialsObj);
                    if (null == auroraClient) {
                        log.error("Unable to open Aurora client connetion for : {}", credential.getName());
                    } else {
                        log.info("Opened Aurora client connetion for : {}", credential.getName());
                    }

                    // close existing connection
                    auroraClients.get(credential.getKey().toLowerCase()).close();
                    // Replace it with new connection
                    auroraClients.put(credential.getKey().toLowerCase(), auroraClient);
                }
            }
        }
    }

    @PreDestroy
    private void closeAuroraConnecion() {
        if (null != auroraClients && !auroraClients.isEmpty()) {
            log.info("Invoked Pre Destroy to Close Aurora Client Connection.");
            for (Client client : auroraClients.values()) {
                client.close();
                log.debug("Closed the Aurora client connection.");
            }
            auroraClients.clear();
        }
    }

    /**
     * Returns aurora client for the given property id.
     * 
     * @param propertyId Property Id
     * @return Aurora client instance
     */
    public Client getAuroraClient(String propertyId) {
        if (applicationProperties.isGseDisabled()){
            log.warn("Request received for GSE Client when GSE is disabled by configuration.");
            throw new SystemException(ErrorCode.SYSTEM_ERROR, new RuntimeException("GSE is currently disabled by configuration."));
        }else if (auroraClients.isEmpty()) {
            log.info("Creating Aurora Client Connection Map as it is Empty!!");
            initializeAuroraConnections();
        }

        return Optional.ofNullable(auroraClients.get(propertyId.toLowerCase(Locale.ENGLISH)))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PROPERTY, "Incoming request has invalid property id as x-mgm-source header"));
    }

    /**
     * Returns default aurora client for channel independent operations.
     * 
     * @return Default Aurora Client instance
     */
    public Client getDefaultAuroraClient() {
        if (auroraClients.isEmpty()) {
            log.info("Creating Aurora Client Connection Map as it is Empty!!");
            initializeAuroraConnections();
        }

        return auroraClients.get("mgmri");
    }

    protected RoomReservation getDesiredRoomReservation(com.mgmresorts.aurora.common.RoomReservation[] reservations,
            String reservationId) {
        com.mgm.services.booking.room.model.reservation.RoomReservation desiredReservation = null;
        // Retrieving the roomReservation object matching the reservationId from the
        // request
        for (com.mgmresorts.aurora.common.RoomReservation roomReservation : reservations) {
            if (reservationId.equals(roomReservation.getId())) {
                desiredReservation = CommonUtil.copyProperties(roomReservation, RoomReservation.class);
                break;
            }
        }
        return desiredReservation;
    }

    protected RoomReservation getDesiredRoomReservationByOperaConfirmationNumber(com.mgmresorts.aurora.common.RoomReservation[] reservations,
                                                                                 String operaConfirmationNumber){
        com.mgm.services.booking.room.model.reservation.RoomReservation desiredReservation = null;
        // Retrieving the roomReservation object matching the operaConfirmationNumber
        for (com.mgmresorts.aurora.common.RoomReservation roomReservation : reservations) {
            if(StringUtils.isNotBlank(operaConfirmationNumber)) {
            	if (operaConfirmationNumber.equals(roomReservation.getOperaConfirmationNumber())) {
                    desiredReservation = CommonUtil.copyProperties(roomReservation, RoomReservation.class);
                    break;
                }
            }else {
            	throw new BusinessException(ErrorCode.NO_CONFIRMATION_NUMBER, ServiceConstant.MSG_OPERA_CNF_NUMBER_MISSING);
            }
        }
        return desiredReservation;
    }

    protected void handleAuroraError(EAuroraException ex) {
        String errorType = AuroraError.getErrorType(ex.getErrorCode().name());
        if(AuroraError.PAYMENTAUTHORIZATIONFAILED.name().equalsIgnoreCase(ex.getErrorCode().name())) {
        	throw new BusinessException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED, ex.getMessage());
        }
        if (AuroraError.FUNCTIONAL_ERROR.equals(errorType)) {
            throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getMessage());
        } else {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, ex);
        }

    }
    
    protected void replaceCCToken(RoomReservation roomReservation) {
        if (decryptionClient != null && CollectionUtils.isNotEmpty(roomReservation.getCreditCardCharges())) {
            roomReservation.getCreditCardCharges().stream().forEach(ccCharges -> {
                try {
                    String decryptedToken = decryptionClient.decrypt(ccCharges.getNumber());
                    ccCharges.setDecryptedNumber(decryptedToken);
                } catch (Exception e) {
                    log.error("Exception occured while decrypting the ccToken: ", e);
                }
            });
        }
    }

    protected void populateChargesAndTaxesCalcWithSpecialRequests(RoomReservation roomReservation) {
        if (null != roomReservation.getSpecialRequests()) {
            final Map<String, RoomComponent> specialRequestCache = new HashMap<>();
            roomReservation.getSpecialRequests().forEach(id -> {
                RoomComponent specialRequestObj = phoenixComponentsCacheService.getComponent(id);
                if (null == specialRequestObj) {
                    log.info("Component:: {} not found in the 'components' cache", id);
                    return;
                }
                specialRequestCache.put(specialRequestObj.getName(), specialRequestObj);
            });

            List<RoomChargeItem> charges = roomReservation.getChargesAndTaxesCalc().getCharges();
            List<RoomChargeItem> taxesAndFees = roomReservation.getChargesAndTaxesCalc().getTaxesAndFees();

            for (int i = 0; i < charges.size(); i++) {
                List<ItemizedChargeItem> chargeItems = charges.get(i).getItemized();
                for (int j = 0; j < chargeItems.size(); j++) {
                    if (null != specialRequestCache.get(chargeItems.get(j).getItem())) {
                        ItemizedChargeItem chargeItem = chargeItems.get(j);
                        log.info("Updating chargesAndTaxes.charges[{}].itemized[{}] with item {}", i, j,
                                chargeItem.getItem());
                        RoomComponent specialRequestObj = specialRequestCache.get(chargeItem.getItem());

                        chargeItem.setId(specialRequestObj.getId());
                        chargeItem.setShortDescription(specialRequestObj.getShortDescription());
                        chargeItem.setActive(specialRequestObj.isActiveFlag());
                        chargeItem.setPricingApplied(specialRequestObj.getPricingApplied());

                        ItemizedChargeItem taxesAndFeesItem = taxesAndFees.get(i).getItemized().get(j);
                        log.info("Updating chargesAndTaxes.taxesAndFees[{}].itemized[{}] with item {}", i, j,
                                taxesAndFeesItem.getItem());
                        taxesAndFeesItem.setId(specialRequestObj.getId());
                        taxesAndFeesItem.setShortDescription(specialRequestObj.getShortDescription());
                        taxesAndFeesItem.setActive(specialRequestObj.isActiveFlag());
                        taxesAndFeesItem.setPricingApplied(specialRequestObj.getPricingApplied());
                    }
                }
            }

        }
    }
}
