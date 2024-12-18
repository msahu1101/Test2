package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.response.*;

/**
 * Service interface that exposes services for room offer related
 * functionalities.
 * 
 */
public interface RoomProgramService {

    /**
     * Get room offers applicable for the customer.
     * 
     * @param offersRequest Offers request
     * @return Room offers applicable
     */
    List<RoomProgram> getRoomOffers(RoomProgramRequest offersRequest);

    /**
     * Returns true if the program is applicable for the customer.
     * 
     * @param validateRequest Room program validate request
     * @return Returns true if the program is applicable for the customer.
     */
    boolean isProgramApplicable(RoomProgramValidateRequest validateRequest);

    /**
     * Returns program validity and eligibility status for the customer.
     * 
     * @param validateRequest Room program validate request
     * @return Returns program validity and eligibility status
     */
    RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest validateRequest);

    /**
     * Finds program Id based on promo code. Property Id is required to look up
     * promo code.
     * 
     * @param promoCode  Promo Code
     * @param propertyId Property GUID
     * @return Returns program Id for the promo code
     */
    String getProgramByPromoCode(String promoCode, String propertyId);

    /**
     * Returns default perpetual programs available for the user across different
     * properties.
     * 
     * @param request Perpetual program request
     * @return Default perpetual programs
     */
    List<PerpetaulProgram> getDefaultPerpetualPrograms(PerpetualProgramRequest request);

    /**
     * Get the segment id for the given program id
     * 
     * @param programId the program id
     * @return the segment id
     */
    RoomProgramSegmentResponse getProgramSegment(String programId);

    /**
     * Returns the room program for the given request.
     * 
     * @param request - to get the room programs from GSE
     * @return room program response
     */
    ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramsRequest request);

    /**
     * Returns the room offers for the customer.
     * 
     * @param customerOffersSearchRequest - to get the customer offers.
     * @return room offers for the customer.
     */
    CustomerOfferResponse getCustomerOffers(CustomerOffersRequest customerOffersSearchRequest);
    
    /**
     * Returns the room offers for the customer.
     * 
     * @param customerOffersSearchRequest - to get the customer offers.
     * @return room offers for the customer.
     */
    CustomerOfferV3Response getCustomerOffers(CustomerOffersV3Request customerOffersSearchRequest);
    
    /**
     * Returns default perpetual programs available for the user across different
     * properties.
     * 
     * @param request Perpetual program request
     * @return Default perpetual programs
     */
    List<PerpetaulProgram> getDefaultPerpetualProgramsV2(PerpetualProgramRequest request);
    
    /**
     * Returns program validity and eligibility status for the customer.
     * 
     * @param validateRequest Room program validate request
     * @return Returns program validity and eligibility status
     */
    RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest validateRequest);

    /**
     * Get the segment id for the given program id
     *
     * @param programRequest the program request
     * @return the segment id
     */
    List<RoomOfferDetails> getProgram(RoomProgramV2Request programRequest);
    
    /**
     * Gets participating program ids for the given segment id/code.
     * 
     * @param segment
     *            Segment code or GUID
     * @param source
     *            Client source
     * @return
     */
    RoomSegmentResponse getRoomSegment(String segment, String source);
    
    /**
     * Gets participating program ids for a given segment or by resolving
     * segment from given program id.
     * 
     * @param segment
     *            Segment code or GUID
     * @param programId
     *            Program Id
     * @param source
     *            Client source
     * @return
     */
    RoomSegmentResponse getRoomSegment(String segment, String programId, String source);
    
}
