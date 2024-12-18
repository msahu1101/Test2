package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.RoomPriceDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.AuroraPriceV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
public class RoomPriceDAOImpl extends BaseStrategyDAO implements RoomPriceDAO {
    private RoomPriceDAOStrategyACRSImpl acrsStrategy;

    private RoomPriceDAOStrategyGSEImpl gseStrategy;

    private SecretsProperties secretProperties;

    @Autowired
    public RoomPriceDAOImpl(RoomPriceDAOStrategyACRSImpl acrsStrategy, RoomPriceDAOStrategyGSEImpl gseStrategy,
                            SecretsProperties secretProperties, AcrsProperties acrsProperties,
                            ReferenceDataDAOHelper referenceDataDAOHelper) {
        this.acrsStrategy = acrsStrategy;
        this.gseStrategy = gseStrategy;
        this.secretProperties = secretProperties;
        // super class dependencies
        this.acrsProperties = acrsProperties;
        this.referenceDataDAOHelper = referenceDataDAOHelper;
    }

    @Override public List<AuroraPriceResponse> getRoomPrices(AuroraPriceRequest pricingRequest) {
        List<AuroraPriceResponse> auroraPriceResponseList;
        if ( null != pricingRequest.getPropertyId() ){
            // PropertyId supplied apply strategy
            RoomPriceDAOStrategy strategy = gseStrategy;
            if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
                strategy = acrsStrategy;
            }
          auroraPriceResponseList = getRoomPrices(pricingRequest, strategy);
        } else {
            // No PropertyId supplied need to fetch for all properties

            // Get pricing from GSE for all properties
            auroraPriceResponseList = getRoomPrices(pricingRequest, gseStrategy);

            // Audit response for prices managed by ACRS to drop and re-add from ACRS
            List<AuroraPriceResponse> trimmedAuroraPriceResponseList = auroraPriceResponseList.stream()
                                                                        .filter(auroraPriceResponse -> !isPropertyManagedByAcrs(auroraPriceResponse.getPropertyId()))
                                                                        .collect(Collectors.toList());

            // Add all room prices from ACRS managed properties
            trimmedAuroraPriceResponseList.addAll(getAllAcrsRoomPrices(pricingRequest));

            // final List ready for return
            auroraPriceResponseList = trimmedAuroraPriceResponseList;
        }
        return auroraPriceResponseList;
    }

    private List<AuroraPriceResponse> getAllAcrsRoomPrices(AuroraPriceRequest pricingRequest) {
        
        List<String> aCRSPropertyIds;
        // PropertyIds in pricing request is optional for filtering
        if (CollectionUtils.isEmpty(pricingRequest.getPropertyIds())) {
            aCRSPropertyIds = aCrsPropertyList();
        } else {
            aCRSPropertyIds = pricingRequest.getPropertyIds().stream().filter(this::isPropertyManagedByAcrs)
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(aCRSPropertyIds)) {
            return Collections.emptyList();
        } else {
            String promo = (StringUtils.isNotEmpty(pricingRequest.getPromo())) ? pricingRequest.getPromo() : null;
            AuroraPriceRequest auroraPriceRequest = AuroraPriceRequest.builder()
                    .checkInDate(pricingRequest.getCheckInDate()).checkOutDate(pricingRequest.getCheckOutDate())
                    .numGuests(pricingRequest.getNumGuests()).programRate(pricingRequest.isProgramRate())
                    .roomTypeIds(pricingRequest.getRoomTypeIds())
                    .auroraItineraryIds(pricingRequest.getAuroraItineraryIds())
                    .customerId(pricingRequest.getCustomerId()).programId(pricingRequest.getProgramId())
                    .enableMrd(pricingRequest.isEnableMrd()).source(pricingRequest.getSource())
                    .promo(promo)
                    .propertyIds(aCRSPropertyIds).mlifeNumber(pricingRequest.getMlifeNumber()).build();

            return acrsStrategy.getResortPrices(auroraPriceRequest);
        }
    }

    private List<AuroraPriceResponse> getRoomPrices(AuroraPriceRequest pricingRequest, RoomPriceDAOStrategy strategy){
        log.debug(createStrategyLogEntry("getRoomPrices", pricingRequest.getCustomerId(), pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(),
                                         strategy));
        return strategy.getRoomPrices(pricingRequest);
    }

    @Override public List<AuroraPriceResponse> getCalendarPrices(AuroraPriceRequest pricingRequest) {
        // Program Id is required to be non-Null in calendar price requests
        RoomPriceDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return getCalendarPrices(pricingRequest, strategy);
    }

    private List<AuroraPriceResponse> getCalendarPrices(AuroraPriceRequest pricingRequest, RoomPriceDAOStrategy strategy){
        log.debug(createStrategyLogEntry("getCalendarPrices", pricingRequest.getCustomerId(), pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(),
                                         strategy));
        return strategy.getCalendarPrices(pricingRequest);
    }

    @Override public List<AuroraPriceResponse> getIterableCalendarPrices(AuroraPriceRequest pricingRequest) {
        // Program Id is required to be non-Null in calendar price requests
        RoomPriceDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return getIterableCalendarPrices(pricingRequest, strategy);
    }

    private List<AuroraPriceResponse> getIterableCalendarPrices(AuroraPriceRequest pricingRequest, RoomPriceDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("getIterableCalendarPrices", pricingRequest.getCustomerId(), pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(),
                                         strategy));
        return strategy.getIterableCalendarPrices(pricingRequest);
    }

    @Override
    public AuroraPricesResponse getRoomPricesV2(AuroraPriceRequest pricingRequest) {
        AuroraPricesResponse auroraPriceResponseList = new AuroraPricesResponse();
        if ( null != pricingRequest.getPropertyId() ){
            // PropertyId supplied apply strategy
            RoomPriceDAOStrategy strategy = gseStrategy;
            if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
                strategy = acrsStrategy;
            }// TODO else if GSE is disabled throw exception
            auroraPriceResponseList = getRoomPricesV2(pricingRequest, strategy);
        } else {
            List<AuroraPriceResponse> trimmedAuroraPriceResponseList = new ArrayList<>();
            // No PropertyId supplied need to fetch for all properties
            try {
                // Get pricing from GSE for all properties
                auroraPriceResponseList = getRoomPricesV2(pricingRequest, gseStrategy);

                // Audit response for prices managed by ACRS to drop and re-add from ACRS
                trimmedAuroraPriceResponseList = auroraPriceResponseList.getAuroraPrices().stream()
                        .filter(auroraPriceResponse -> !isPropertyManagedByAcrs(auroraPriceResponse.getPropertyId()))
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                log.info("Error occured while invoking GSE for room prices", ex.getMessage());
            }
            // Add all room prices from ACRS managed properties
            trimmedAuroraPriceResponseList.addAll(getAllAcrsRoomPrices(pricingRequest));

            // final List ready for return
            auroraPriceResponseList.setAuroraPrices(trimmedAuroraPriceResponseList);
            auroraPriceResponseList.setMrdPricing(pricingRequest.isEnableMrd());
        }
        return auroraPriceResponseList;
    }
    
    private AuroraPricesResponse getRoomPricesV2(AuroraPriceRequest pricingRequest, RoomPriceDAOStrategy strategy){
        log.debug(createStrategyLogEntry("getRoomPricesV2", pricingRequest.getCustomerId(), pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(),
                                         strategy));
        return strategy.getRoomPricesV2(pricingRequest);
    }

    @Override
    public List<AuroraPriceResponse> getCalendarPricesV2(AuroraPriceRequest pricingRequest) {
        // Program Id is required to be non-Null in calendar price requests
        RoomPriceDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return getCalendarPricesV2(pricingRequest, strategy);
    }
    
    private List<AuroraPriceResponse> getCalendarPricesV2(AuroraPriceRequest pricingRequest, RoomPriceDAOStrategy strategy){
        log.debug(createStrategyLogEntry("getCalendarPricesV2", pricingRequest.getCustomerId(), pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(),
                                         strategy));
        return strategy.getCalendarPricesV2(pricingRequest);
    }

    @Override
    public List<AuroraPriceResponse> getIterableCalendarPricesV2(AuroraPriceRequest pricingRequest) {
        // Program Id is required to be non-Null in calendar price requests
        RoomPriceDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return getIterableCalendarPricesV2(pricingRequest, strategy);
    }
    
    private List<AuroraPriceResponse> getIterableCalendarPricesV2(AuroraPriceRequest pricingRequest, RoomPriceDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("getIterableCalendarPricesV2", pricingRequest.getCustomerId(), pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(),
                                         strategy));
        return strategy.getIterableCalendarPricesV2(pricingRequest);
    }

    private String createStrategyLogEntry(String method, long customerId, LocalDate checkInDate, LocalDate checkOutDate, RoomPriceDAOStrategy strategy) {
        String strategyString = (strategy instanceof RoomPriceDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "RoomPriceDAOImpl > "
                + method
                + " | CustID: "
                + customerId
                + " | arrival: "
                + checkInDate
                + " | departure: "
                + checkOutDate
                + " | "
                + strategyString;
    }

    @Override
    public List<AuroraPriceV3Response> getLOSBasedCalendarPrices(AuroraPriceV3Request pricingRequest) {
    	RoomPriceDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(pricingRequest.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return getLOSBasedCalendarPrice(pricingRequest, strategy);
    }

    @Override
    public List<AuroraPriceResponse> getGridAvailabilityForSoldOut(AuroraPriceRequest auroraPriceRequest) {
        return acrsStrategy.getGridAvailabilityForSoldOut(auroraPriceRequest);
    }

    private List<AuroraPriceV3Response> getLOSBasedCalendarPrice(AuroraPriceV3Request pricingRequest, RoomPriceDAOStrategy strategy) {
        return strategy.getLOSBasedCalendarPrices(pricingRequest);
    }

    private List<String> aCrsPropertyList() {
        List<String> aCrsPropertyList = new ArrayList<>();
        String acrsEnabledProperties = secretProperties.getSecretValue(acrsProperties.getAcrsPropertyListSecretKey());
        if (StringUtils.isNotBlank(acrsEnabledProperties)) {
            aCrsPropertyList = Arrays.asList(acrsEnabledProperties.trim().split(","));
        }
        return aCrsPropertyList;
    }
}
