package com.mgm.services.booking.room.transformer;

import java.time.ZoneId;
import java.util.Collections;

import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.util.ReservationUtil;
import org.apache.commons.lang3.StringUtils;

import com.mgm.services.booking.room.model.reservation.RoomReservation;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create AuroraPriceRequest from RoomAvailabilityRequest
 * object.
 */
@UtilityClass
public class AuroraPriceRequestTransformer {

    /**
     * Creates and returns AuroraPriceRequest from RoomAvailabilityRequest
     * object.
     * 
     * @param request
     *            RoomAvailabilityRequest object
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceRequest(RoomAvailabilityRequest request) {

        return AuroraPriceRequest.builder().propertyId(request.getPropertyId()).customerId(request.getCustomerId())
                .checkInDate(request.getCheckInDate()).checkOutDate(request.getCheckOutDate())
                .promoCode(request.getPromoCode()).promo(request.getPromo()).numGuests(request.getNumGuests()).programId(request.getProgramId())
                .source(request.getSource()).auroraItineraryIds(request.getAuroraItineraryIds()).build();
    }

    /**
     * Creates and returns AuroraPriceRequest from RoomAvailabilityRequest
     * object with an option to set enableMrd flag to drive multi-rate plan
     * retrieval.
     * 
     * @param request
     *            RoomAvailabilityRequest object
     * @param enableMrd
     *            Flag to enable or disable multi-rate plan prices
     * @param programRate
     *            Flag to enable or disable program rate
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceRequest(RoomAvailabilityRequest request, boolean enableMrd,
            boolean programRate) {

        return AuroraPriceRequest.builder().propertyId(request.getPropertyId()).customerId(request.getCustomerId())
                .checkInDate(request.getCheckInDate()).checkOutDate(request.getCheckOutDate())
                .numGuests(request.getNumGuests()).programId(request.getProgramId()).programRate(programRate)
                .promoCode(request.getPromoCode()).promo(request.getPromo()).source(request.getSource())
                .auroraItineraryIds(request.getAuroraItineraryIds()).enableMrd(enableMrd).build();
    }

    /**
     * Creates and returns AuroraPriceRequest from ResortPriceRequest object.
     * 
     * @param request
     *            ResortPriceRequest object
     * @param enableMrd
     *            Flag to enable or disable multi-rate plan prices
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceRequest(ResortPriceRequest request, boolean enableMrd) {

        return AuroraPriceRequest.builder().customerId(request.getCustomerId()).checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate()).numGuests(request.getNumGuests())
                .programId(request.getProgramId()).propertyId(request.getPropertyId())
                .propertyIds(request.getPropertyIds()).source(request.getSource())
                .auroraItineraryIds(request.getAuroraItineraryIds()).enableMrd(enableMrd).build();
    }

    /**
     * Creates and returns AuroraPriceRequest from ResortPriceRequest object.
     * 
     * @param request
     *            ResortPriceV2Request object
     * @param enableMrd
     *            Flag to enable or disable multi-rate plan prices
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceRequest(ResortPriceV2Request request, boolean enableMrd) {

        return AuroraPriceRequest.builder().customerId(request.getCustomerId()).checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate()).numGuests(request.getNumAdults())
                .programId(request.getProgramId()).propertyId(request.getPropertyId())
                .propertyIds(request.getPropertyIds()).source(request.getSource())
                .promoCode(request.getPromo()).auroraItineraryIds(request.getAuroraItineraryIds()).enableMrd(enableMrd)
                .mlifeNumber(request.getMlifeNumber()).isPerpetualOffer(request.isPerpetualPricing()).build();
    }

    /**
     * Creates and returns AuroraPriceRequest from CalendarPriceRequest object.
     * 
     * @param calendarRequest
     *            CalendarPriceRequest object
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraRequest(CalendarPriceRequest calendarRequest) {
        return AuroraPriceRequest.builder().propertyId(calendarRequest.getPropertyId())
                .customerId(calendarRequest.getCustomerId()).checkInDate(calendarRequest.getStartDate())
                .checkOutDate(calendarRequest.getEndDate()).numGuests(calendarRequest.getNumGuests())
                .programId(calendarRequest.getProgramId()).source(calendarRequest.getSource())
                .auroraItineraryIds(calendarRequest.getAuroraItineraryIds()).promoCode(calendarRequest.getPromoCode())
                .promo(calendarRequest.getPromo()).programRate(StringUtils.isNotEmpty(calendarRequest.getProgramId())).build();
    }

    /**
     * Creates and returns AuroraPriceRequest from CalendarPriceRequest object.
     * 
     * @param calendarRequest
     *            CalendarPriceRequest object
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraRequest(CalendarPriceV2Request calendarRequest) {
        return AuroraPriceRequest.builder().propertyId(calendarRequest.getPropertyId())
                .roomTypeIds(calendarRequest.getRoomTypeIds()).customerId(calendarRequest.getCustomerId())
                .checkInDate(calendarRequest.getStartDate()).checkOutDate(calendarRequest.getEndDate())
                .numGuests(calendarRequest.getNumAdults()).programId(calendarRequest.getProgramId())
                .source(calendarRequest.getSource()).auroraItineraryIds(calendarRequest.getAuroraItineraryIds())
                .programRate(StringUtils.isNotEmpty(calendarRequest.getProgramId()))
                .promoCode(calendarRequest.getPromoCode()).promo(calendarRequest.getPromo()).numChildren(calendarRequest.getNumChildren())
                .ignoreChannelMargins(calendarRequest.isIgnoreChannelMargins()).numRooms(calendarRequest.getNumRooms())
                .operaConfirmationNumber(calendarRequest.getOperaConfirmationNumber())
                .customerDominantPlay(calendarRequest.getCustomerDominantPlay())
                .customerRank(calendarRequest.getCustomerRank()).isGroupCode(calendarRequest.getIsGroupCode())
                .mlifeNumber(calendarRequest.getMlifeNumber()).isPerpetualOffer(calendarRequest.getPerpetualPricing())
                .build();
    }

    /**
     * Creates and returns AuroraPriceRequest from CalendarPriceV3Request
     * object.
     * 
     * @param calendarRequest
     *            CalendarPriceRequest object
     * @return Returns AuroraPriceV3Request
     */
    public static AuroraPriceV3Request getAuroraRequest(CalendarPriceV3Request calendarRequest) {
        return AuroraPriceV3Request.builder().propertyId(calendarRequest.getPropertyId())
                .roomTypeIds(calendarRequest.getRoomTypeIds()).customerId(calendarRequest.getCustomerId())
                .checkInDate(calendarRequest.getStartDate()).checkOutDate(calendarRequest.getEndDate())
                .numGuests(calendarRequest.getNumAdults()).programId(calendarRequest.getProgramId())
                .promoCode(calendarRequest.getPromoCode()).promo(calendarRequest.getPromo()).source(calendarRequest.getSource())
                .auroraItineraryIds(calendarRequest.getAuroraItineraryIds())
                .numChildren(calendarRequest.getNumChildren())
                .ignoreChannelMargins(calendarRequest.isIgnoreChannelMargins()).numRooms(calendarRequest.getNumRooms())
                .operaConfirmationNumber(calendarRequest.getOperaConfirmationNumber())
                .customerDominantPlay(calendarRequest.getCustomerDominantPlay())
                .customerRank(calendarRequest.getCustomerRank()).isGroupCode(calendarRequest.getIsGroupCode())
                .isPerpetualOffer(calendarRequest.isPerpetualPricing()).mlifeNumber(calendarRequest.getMlifeNumber())
                .tripLength(calendarRequest.getTotalNights()).build();
    }

    /**
     * Creates AuroraPriceV3Request from ModificationChangesRequest
     *
     * @param modificationChangesRequest
     * @return AuroraPriceV3Request from parameter's fields
     */
    public static AuroraPriceV3Request getAuroraRequest(ModificationChangesRequest modificationChangesRequest) {
        return AuroraPriceV3Request.builder().propertyId(modificationChangesRequest.getPropertyId())
                .roomTypeIds(Collections.singletonList(modificationChangesRequest.getRoomTypeId()))
                .customerId(modificationChangesRequest.getCustomerId())
                .checkInDate(ReservationUtil.convertDateToLocalDate(modificationChangesRequest.getTripDetails().getCheckInDate()))
                .checkOutDate(ReservationUtil.convertDateToLocalDate(modificationChangesRequest.getTripDetails().getCheckOutDate()))
                .numGuests(modificationChangesRequest.getTripDetails().getNumAdults())
                .numChildren(modificationChangesRequest.getTripDetails().getNumChildren())
                .numRooms(1)
                .programId(modificationChangesRequest.getProgramId())
                .promoCode(modificationChangesRequest.getPromoCode())
                .promo(modificationChangesRequest.getPromo())
                .propertyId(modificationChangesRequest.getPropertyId())
                .source(modificationChangesRequest.getSource())
                //.ignoreChannelMargins(modificationChangesRequest.isIgnoreChannelMargins())
                .customerDominantPlay(modificationChangesRequest.getDominantPlay())
                .customerRank(modificationChangesRequest.getRankOrSegment())
                .isGroupCode(modificationChangesRequest.isGroupCode())
                .isPerpetualOffer(modificationChangesRequest.isPerpetualPricing())
                .mlifeNumber(modificationChangesRequest.getMlifeNumber())
                .confirmationNumber(modificationChangesRequest.getConfirmationNumber())
                .build();
    }

    /**
     * Creates and returns AuroraPriceRequest suitable for V2 services from
     * RoomAvailabilityRequest object.
     *
     * @param request
     *            RoomAvailabilityRequest object
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceV2Request(RoomAvailabilityV2Request request) {

        return AuroraPriceRequest.builder().propertyId(request.getPropertyId()).customerId(request.getCustomerId())
                .checkInDate(request.getCheckInDate()).checkOutDate(request.getCheckOutDate())
                .numGuests(request.getNumAdults()).programId(request.getProgramId()).source(request.getSource())
                .auroraItineraryIds(request.getAuroraItineraryIds()).enableMrd(request.isEnableMrd())
                .ignoreChannelMargins(request.isIgnoreChannelMargins()).numRooms(request.getNumRooms())
                .operaConfirmationNumber(request.getOperaConfirmationNumber()).promoCode(request.getPromoCode()).promo(request.getPromo())
                .customerDominantPlay(request.getCustomerDominantPlay()).customerRank(request.getCustomerRank())
                .isGroupCode(request.getIsGroupCode()).mlifeNumber(request.getMlifeNumber())
                .isPerpetualOffer(request.getPerpetualPricing()).includeSoldOutRooms(request.isIncludeSoldOutRooms())
                .numChildren(request.getNumChildren()).build();

    }

    /**
     * Creates and returns AuroraPriceRequest suitable for V2 services from
     * RoomAvailabilityRequest object.
     *
     * @param request
     *            RoomAvailabilityRequest object
     * @param programRate
     *            Flag to enable or disable program rate
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceV2Request(RoomAvailabilityV2Request request, boolean programRate) {

        return AuroraPriceRequest.builder().propertyId(request.getPropertyId()).customerId(request.getCustomerId())
                .checkInDate(request.getCheckInDate()).checkOutDate(request.getCheckOutDate())
                .numGuests(request.getNumAdults()).programId(request.getProgramId()).programRate(programRate)
                .source(request.getSource()).auroraItineraryIds(request.getAuroraItineraryIds())
                .enableMrd(request.isEnableMrd()).ignoreChannelMargins(request.isIgnoreChannelMargins())
                .promoCode(request.getPromoCode()).promo(request.getPromo()).numRooms(request.getNumRooms())
                .operaConfirmationNumber(request.getOperaConfirmationNumber())
                .customerDominantPlay(request.getCustomerDominantPlay()).customerRank(request.getCustomerRank())
                .isGroupCode(request.getIsGroupCode()).mlifeNumber(request.getMlifeNumber())
                .includeSoldOutRooms(request.isIncludeSoldOutRooms()).numChildren(request.getNumChildren())
                .isPerpetualOffer(request.getPerpetualPricing()).build();

    }

    /**
     * Creates and returns AuroraPriceRequest suitable for V3 services from
     * RoomAvailabilityRequest object.
     *
     * @param request
     *            RoomAvailabilityRequest object
     * @param enableMrd
     *            request is for multi-rate display or not
     * @param programRate
     *            Boolean to indicate if mixed programs can be used for pricing
     * @return Returns AuroraPriceRequest
     */
    public static AuroraPriceRequest getAuroraPriceV3Request(RoomAvailabilityV3Request request, boolean enableMrd,
            String replacementProgramId, String promo) {
        String programId = (StringUtils.isNotEmpty(replacementProgramId)) ? replacementProgramId :
                request.getProgramId();
        boolean includeDefaultRatePlans = request.isIncludeDefaultRatePlans();

        return AuroraPriceRequest.builder().propertyId(request.getPropertyId())
                .customerId(request.getCustomerId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numGuests(request.getNumAdults())
                .programId(programId)
                .programRate(StringUtils.isNotEmpty(replacementProgramId))
                .source(request.getSource())
                .auroraItineraryIds(request.getAuroraItineraryIds())
                .enableMrd(enableMrd)
                .ignoreChannelMargins(request.isIgnoreChannelMargins())
                .numRooms(request.getNumRooms())
                .operaConfirmationNumber(request.getOperaConfirmationNumber())
                .confirmationNumber(request.getConfirmationNumber())
                .promoCode(request.getPromoCode())
                .promo(promo)
                .customerDominantPlay(request.getCustomerDominantPlay())
                .customerRank(request.getCustomerRank())
                .isGroupCode(request.getIsGroupCode())
                .mlifeNumber(request.getMlifeNumber())
                .isPerpetualOffer(request.isPerpetualPricing())
                .includeSoldOutRooms(request.isIncludeSoldOutRooms())
                .numChildren(request.getNumChildren())
                .includeDefaultRatePlans(includeDefaultRatePlans)
                .build();
    }

    public static AuroraPriceRequest getAuroraPriceV2RequestForCharges(RoomReservation roomReservation) {
        return getAuroraPriceV2RequestForCharges(roomReservation, roomReservation.getProgramId());
    }

    /**
     * It will convert RoomReservation request to AuroraPriceRequest
     * 
     * @param roomReservation
     * @return
     */
    public static AuroraPriceRequest getAuroraPriceV2RequestForCharges(RoomReservation roomReservation,
                                                                       String specificProgramId) {
        return AuroraPriceRequest.builder().propertyId(roomReservation.getPropertyId())
                .roomTypeIds(Collections.singletonList(roomReservation.getRoomTypeId()))
                .customerId(roomReservation.getCustomerId())
                .checkInDate(roomReservation.getCheckInDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .checkOutDate(
                        roomReservation.getCheckOutDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .numGuests(roomReservation.getNumAdults())
                .programId(specificProgramId)
                .source(roomReservation.getSource())
                .programRate(StringUtils.isNotEmpty(roomReservation.getProgramId()))
                .numChildren(roomReservation.getNumChildren())
                .numRooms(roomReservation.getNumRooms())
                .operaConfirmationNumber(roomReservation.getOperaConfirmationNumber())
                .customerDominantPlay(roomReservation.getCustomerDominantPlay())
                .customerRank(roomReservation.getCustomerRank())
                .isGroupCode(roomReservation.getIsGroupCode())
                .isPerpetualOffer(roomReservation.isPerpetualPricing())
                .promoCode(roomReservation.getPromo())
                .mlifeNumber(roomReservation.getProfile() != null && roomReservation.getProfile().getMlifeNo() > 0
                        ? String.valueOf(roomReservation.getProfile().getMlifeNo())
                        : null)
                .build();
    }

    /**
     * This method creates the promo association request
     * @param request
     * @return
     */
    public static RoomProgramPromoAssociationRequest getPromoAssociationRequest(RoomAvailabilityV3Request request) {
        final RoomProgramPromoAssociationRequest promoAssociationRequest = new RoomProgramPromoAssociationRequest();
        promoAssociationRequest.setPropertyId(request.getPropertyId());
        promoAssociationRequest.setPromo(request.getPromo());
        promoAssociationRequest.setProgramIds(request.getProgramIds());
        promoAssociationRequest.setSource(request.getSource());
        return promoAssociationRequest;
    }

}
