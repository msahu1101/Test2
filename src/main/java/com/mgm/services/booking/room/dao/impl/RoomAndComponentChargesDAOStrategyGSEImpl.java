package com.mgm.services.booking.room.dao.impl;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.service.impl.CommonServiceImpl;
import com.mgmresorts.aurora.service.EAuroraException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomAndComponentChargesDAOStrategy;
import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.ComponentPrices;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

/**
 * DAO implementation class to provide methods for calculating room and component charges
 *
 * @author uttam
 */
@Component
public class RoomAndComponentChargesDAOStrategyGSEImpl implements RoomAndComponentChargesDAOStrategy {

    @Autowired
    private ComponentDAOStrategyGSEImpl componentDAOStrategyGSE;

    @Autowired
    private ReservationDAOStrategyGSEImpl reservationDAOStrategyGSEImpl;

    @Autowired
    private RoomPriceDAOStrategyGSEImpl roomPriceDAOStrategyGSEImpl;

    @Autowired
    private RoomCacheService roomCacheService;

    @Autowired
    private RoomProgramCacheService roomProgramCacheService;
    
    @Autowired
    private ReservationServiceHelper reservationServiceHelper;

    @Autowired
    CommonServiceImpl commonService;

    @Override
    public RoomReservation calculateRoomAndComponentCharges(RoomReservation roomReservation) {
        List<RoomRequest> availableRoomComponents = new ArrayList<>();
        List<AuroraPriceResponse> auroraPriceResponses = new ArrayList<>();
        RoomReservation holdReservationResponse = null;
        try {
            availableRoomComponents = componentDAOStrategyGSE.getRoomComponentAvailability(getRoomComponentRequest(roomReservation));
            final List<String> applicableComponentIds = availableRoomComponents.stream().map(RoomRequest::getId).collect(Collectors.toList());
            auroraPriceResponses = roomPriceDAOStrategyGSEImpl.getRoomPrices(getAuroraPriceRequest(roomReservation));
            roomReservation.setBookings(getAvailableRoomBookings(auroraPriceResponses));
            roomReservation.setSpecialRequests(applicableComponentIds);
            //Check if booking limit is already applied
            commonService.checkBookingLimitApplied(roomReservation);
            holdReservationResponse = reservationDAOStrategyGSEImpl.updateRoomReservation(roomReservation);
            roomReservation.setSpecialRequests(null);
            holdReservationResponse.setPerpetualPricing(checkPerpetualPricing(auroraPriceResponses));
        } catch (EAuroraException ex) {
            if (com.mgmresorts.aurora.common.ErrorCode.MalformedDTO == ex.getErrorCode() && ex.getMessage().contains("not valid")) {
                throw new BusinessException(com.mgm.services.common.exception.ErrorCode.INVALID_REQUEST_PARAMS, ex.getMessage());
            } else if (com.mgmresorts.aurora.common.ErrorCode.BlacklistReservation == ex.getErrorCode()) {
                throw new BusinessException(com.mgm.services.common.exception.ErrorCode.RESERVATION_BLACKLISTED, ex.getMessage());
            } else if (com.mgmresorts.aurora.common.ErrorCode.InvalidRoomPricingRuleId == ex.getErrorCode()) {
                throw new BusinessException(ErrorCode.INVALID_ROOM_PRICING_RULE_ID, ex.getMessage());
            }
            else {
                throw ex;
            }
        }
        if (holdReservationResponse.isPerpetualPricing()) {
            RoomReservation saveRoomReservationResponse = null;
            try {
                saveRoomReservationResponse = reservationDAOStrategyGSEImpl.saveRoomReservation(roomReservation);
                holdReservationResponse.setId(saveRoomReservationResponse.getId());
                holdReservationResponse.setItineraryId(saveRoomReservationResponse.getItineraryId());
            } catch (EAuroraException ex) {
                if (com.mgmresorts.aurora.common.ErrorCode.MalformedDTO == ex.getErrorCode() && ex.getMessage().contains("not valid")) {
                    throw new BusinessException(com.mgm.services.common.exception.ErrorCode.INVALID_REQUEST_PARAMS, ex.getMessage());
                } else if (com.mgmresorts.aurora.common.ErrorCode.BlacklistReservation == ex.getErrorCode()) {
                    throw new BusinessException(com.mgm.services.common.exception.ErrorCode.RESERVATION_BLACKLISTED, ex.getMessage());
                } else if (com.mgmresorts.aurora.common.ErrorCode.InvalidRoomPricingRuleId == ex.getErrorCode()) {
                    throw new BusinessException(ErrorCode.INVALID_ROOM_PRICING_RULE_ID, ex.getMessage());
                }
                else {
                    throw ex;
                }
            }
        }
        holdReservationResponse.setAvailableComponents(
                getAvailableComponents(roomReservation.getRoomTypeId(), availableRoomComponents, holdReservationResponse));
        
        // adjust deposit if required
        adjustDeposit(holdReservationResponse.getDepositCalc());
        // Using copy properties here to apply price rounding since price        
        // rounding was skipped in DAO layer to perform adjust deposit before        
        // rounding is applied.        
        // If this rounding is not applied, we will have scenarios where some        
        // elements will be off by a cent after reservation completion.        
        return CommonUtil.copyProperties(holdReservationResponse, RoomReservation.class);
    }

    private boolean checkPerpetualPricing(List<AuroraPriceResponse> auroraPriceResponses) {
        for (AuroraPriceResponse auroraPriceResponse : auroraPriceResponses) {
            if (roomProgramCacheService.isProgramPO(auroraPriceResponse.getProgramId())) {
                return true;
            }
        }
        return false;
    }

    private List<RoomBookingComponent> getAvailableComponents(String roomTypeId, List<RoomRequest> availableRoomComponents, RoomReservation holdReservationResponse) {
        Map<String, RoomComponent> componentMap = new HashMap<>();
        List<RoomBookingComponent> componentList = new ArrayList<>();
        if (!availableRoomComponents.isEmpty()) {
            Room room = roomCacheService.getRoom(roomTypeId);
            if (null != room) {
                List<RoomComponent> components = room.getComponents();
                components.forEach(component -> componentMap.put(component.getId(), component));
                for (RoomRequest availableComponent : availableRoomComponents) {
                    final RoomBookingComponent roomBookingComponent = new RoomBookingComponent();
                    roomBookingComponent.setId(availableComponent.getId());
                    roomBookingComponent.setCode(componentMap.get(availableComponent.getId()).getName());
                    roomBookingComponent.setShortDescription(availableComponent.getShortDescription());
                    roomBookingComponent.setLongDescription(availableComponent.getLongDescription());
                    roomBookingComponent.setActive(true);
                    roomBookingComponent.setPricingApplied(availableComponent.getPricingApplied());
                    roomBookingComponent.setDepositAmount(getComponentDeposit(roomBookingComponent.getCode(), holdReservationResponse.getDepositCalc()));
                    roomBookingComponent.setPrices(getComponentPrices(holdReservationResponse.getChargesAndTaxesCalc(), roomBookingComponent.getCode()));
                    componentList.add(roomBookingComponent);
                }
            }
        }
        return componentList;
    }
    
    private double getComponentDeposit(String componentCode, Deposit depositCalc) {

        // Set deposit amount at component level if it's included in the deposit calculation.
        // This will indicate if component prices should be included in deposit when guests selects it.
        if (null != depositCalc && !CollectionUtils.isEmpty(depositCalc.getItemized())) {
            return depositCalc.getItemized()
                    .stream()
                    .filter(i -> i.getItem()
                            .equalsIgnoreCase(componentCode))
                    .collect(Collectors.summingDouble(ItemizedChargeItem::getAmount));
        }
        return 0;
    }

    private ComponentPrices getComponentPrices(RoomChargesAndTaxes chargesAndTaxesCalc, String roomBookingComponentCode) {
        ComponentPrices componentPrices = new ComponentPrices();
        for (RoomChargeItem roomChargeItem : chargesAndTaxesCalc.getCharges()) {
            Optional<ItemizedChargeItem> itemizedChargeItem = roomChargeItem.getItemized().stream()
                    .filter(x -> roomBookingComponentCode.equals(x.getItem()))
                    .findFirst();
            if (itemizedChargeItem.isPresent()) {
                double amount = itemizedChargeItem.get().getAmount();
                roomChargeItem.getItemized().remove(itemizedChargeItem.get());
                List<RoomChargeItem> taxesFees = chargesAndTaxesCalc.getTaxesAndFees().stream()
                        .filter(x -> DateUtils.isSameDay(roomChargeItem.getDate(), x.getDate())).collect(Collectors.toList());
                double taxAmt = 0;
                for (RoomChargeItem taxChargeItem : taxesFees) {
                    Optional<ItemizedChargeItem> itemizedTaxItem = taxChargeItem.getItemized().stream()
                            .filter(x -> roomBookingComponentCode.equals(x.getItem())).findFirst();
                    if (itemizedTaxItem.isPresent()) {
                        taxAmt = itemizedTaxItem.get().getAmount();
                        taxChargeItem.getItemized().remove(itemizedTaxItem.get());
                    }
                }
                ComponentPrice componentPrice = new ComponentPrice();
                componentPrice.setDate(roomChargeItem.getDate());
                componentPrice.setAmount(amount);
                componentPrice.setTax(taxAmt);
                componentPrices.add(componentPrice);
            }
        }
        return componentPrices;
    }
    
    private void adjustDeposit(Deposit depositCalc) {

        // Component prices should be excluded from deposit from hold API
        // response
        if (null != depositCalc && !CollectionUtils.isEmpty(depositCalc.getItemized())) {

            double componentPrice = depositCalc.getItemized()
                    .stream()
                    .filter(i -> i.getItemType()
                            .equals(RoomChargeItemType.ComponentCharge)
                            || i.getItemType()
                                    .equals(RoomChargeItemType.ComponentChargeTax))
                    .collect(Collectors.summingDouble(ItemizedChargeItem::getAmount));

            // adjust deposit amount
            depositCalc.setAmount(depositCalc.getAmount() - componentPrice);

            // remove component items from itemized
            depositCalc.getItemized()
                    .removeIf(i -> i.getItemType()
                            .equals(RoomChargeItemType.ComponentCharge)
                            || i.getItemType()
                                    .equals(RoomChargeItemType.ComponentChargeTax));
        }
    }

    private List<RoomPrice> getAvailableRoomBookings(List<AuroraPriceResponse> auroraPriceResponses) {
        List<RoomPrice> roomBookings = new ArrayList<>();
        auroraPriceResponses.stream().findAny().orElseThrow(() -> new BusinessException(ErrorCode.UNABLE_TO_PRICE, "No price found for the input dates"));
        for (AuroraPriceResponse priceResponse : auroraPriceResponses) {
            if (priceResponse.getDiscountedPrice() < 0) {
                throw new BusinessException(ErrorCode.UNABLE_TO_PRICE, "No price found for the given date: " + priceResponse.getDate());
            }
            final RoomPrice booking = new RoomPrice();
            booking.setDate(priceResponse.getDate());
            booking.setProgramId(priceResponse.getProgramId());
            booking.setPricingRuleId(priceResponse.getPricingRuleId());
            booking.setPrice(priceResponse.getDiscountedPrice());
            booking.setComp(priceResponse.isComp());
            booking.setOverridePrice(-1);
            booking.setResortFee(priceResponse.getResortFee());
            booking.setBasePrice(priceResponse.getBasePrice());
            roomBookings.add(booking);
        }
        return roomBookings;
    }

    private AuroraPriceRequest getAuroraPriceRequest(RoomReservation roomReservation) {
        deriveAndSetCustomerId(roomReservation);
        
        // programRate should be true only when programId is available and not a
        // PO program. PO program can allow mixed or fallback pricing
        boolean programRate = StringUtils.isNotEmpty(roomReservation.getProgramId())
                && !roomProgramCacheService.isProgramPO(roomReservation.getProgramId());
        return AuroraPriceRequest.builder()
                .checkInDate(roomReservation.getCheckInDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .checkOutDate(roomReservation.getCheckOutDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .roomTypeIds(Collections.singletonList(roomReservation.getRoomTypeId()))
                .programId(roomReservation.getProgramId())
                .numGuests(roomReservation.getNumAdults())
                .numChildren(roomReservation.getNumChildren())
                .customerId(roomReservation.getCustomerId())
                .numRooms(roomReservation.getNumRooms())
                .propertyId(roomReservation.getPropertyId())
                .programRate(programRate)
                .source(roomReservation.getSource())
                .auroraItineraryIds(roomReservation.getShoppedItineraryIds())
                .build();
    }
    
    private void deriveAndSetCustomerId(RoomReservation roomReservation) {
        // derive customer id
        long customerId = roomReservation.getCustomerId();
        if (null != roomReservation.getProfile() && roomReservation.getProfile()
                .getId() > 0) {
            customerId = roomReservation.getProfile()
                    .getId();

        }

        // update all customer id references
        roomReservation.setCustomerId(customerId);
        if (null == roomReservation.getProfile()) {
            ReservationProfile profile = new ReservationProfile();
            profile.setId(customerId);
        } else {
            roomReservation.getProfile()
                    .setId(customerId);
        }
    }

    private RoomComponentRequest getRoomComponentRequest(RoomReservation roomReservation) {
        final RoomComponentRequest roomComponentRequest = new RoomComponentRequest();
        roomComponentRequest.setTravelEndDate(roomReservation.getCheckOutDate());
        roomComponentRequest.setPropertyId(roomReservation.getPropertyId());
        roomComponentRequest.setRoomTypeId(roomReservation.getRoomTypeId());
        roomComponentRequest.setTravelStartDate(roomReservation.getCheckInDate());
        roomComponentRequest.setSource(roomReservation.getSource());
        return roomComponentRequest;
    }
}
