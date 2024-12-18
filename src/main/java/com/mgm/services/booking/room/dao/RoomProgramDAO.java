package com.mgm.services.booking.room.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.*;

/**
 * DAO interface to expose services for room programs related functionalities.
 *
 */
public interface RoomProgramDAO {

    /**
     * Returns room programs for transient of logged-in customer.
     * 
     * @param offersRequest Offers request
     * @return Room offers
     */
    List<RoomProgram> getRoomOffers(RoomProgramRequest offersRequest);

    /**
     * Find program id based on promo code for a specific property.
     * 
     * @param propertyId Property Identifier
     * @param promoCode  Promo code for a property
     * @return Program Id
     */
    String getProgramByPromoCode(String propertyId, String promoCode);

    /**
     * Returns default perpetual programs available for the user across different
     * properties.
     * 
     * @param request Perpetual program request
     * @return Default perpetual programs
     */
    List<PerpetaulProgram> getDefaultPerpetualPrograms(PerpetualProgramRequest request);

    /**
     * Returns the room programs for the given request.
     * 
     * @param request - to get the room programs from GSE
     * @return the applicable program response
     */
    ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramRequestDTO request);


    /**
     * Returns room offers for the the given customer request.
     * 
     * @param searchRequestDTO the request
     * @return the list of offers
     */
    CustomerOfferResponse getCustomerOffers(CustomerOffersRequestDTO searchRequestDTO);

    /**
     * This method validates the passed on Program for its different eligibility criteria
     * @param request
     * @return
     */
    RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest request);

    /**
     * This method validates the passed on Program for its different eligibility criteria
     * @param request
     * @return
     */
    RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest request);
    
    /**
     * Finds and returns programs sharing the same rate plan code
     * 
     * @param ratePlanCode
     *            Rate plan code
     * @param source
     *            Client source
     * @return
     */
    List<RoomProgramBasic> findProgramsByRatePlanCode(String ratePlanCode, String source, boolean isPromoRatePlan);

    /**
     * Finds and returns programs sharing the same rate plan code by looking up
     * rate plan code of the supplied program
     * 
     * @param programId
     *            program id
     * @param source
     *            client source
     * @return
     */
    List<RoomProgramBasic> findProgramsIfSegment(String programId, String source);
    
    /**
     * Finds and returns programs linked as part of segment
     * 
     * @param segmentId
     *            segment id
     * @param source
     *            client source
     * @return
     */
    List<RoomProgramBasic> findProgramsBySegment(String segmentId, String source);
    
    /**
     * Returns room offers applicable for the the given customer which includes patron promos,
     * perpetual programs and ICE only programs.
     * 
     * @param offersRequest
     *            offers request
     * @return Returns list of room offers
     */
    RoomProgramsResponseDTO getRoomPrograms(RoomProgramsRequestDTO offersRequest);

    /**
     * This method retrieves the ratePlan data by ids from ACRS
     * @param request
     * @return
     */
    List<RoomOfferDetails> getRatePlanById(RoomProgramV2Request request);
    
    /**
     * Returns true if the program is a PO program
     * 
     * @param programId
     *            Program GUID
     * @return
     */
    boolean isProgramPO(String programId);
    
    /**
     * Gets participating program ids for the given segment id/code.
     * 
     * @param segment
     *            Segment code or GUID
     * @param source
     *            Client source
     * @return
     */
    RoomSegmentResponse getRoomSegment(String segment, String source, boolean isPromoRatePlan);
    
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

    /**
     * This method updates the validateResponse's isHDEPackage and valid fields
     * depending on whether the program is associated to HDE package or not.
     * 
     * @param request
     *            room program validate request
     * @param validateResponse
     *            room program validate response
     */
    void updateValidateResponseForPackagePrograms(RoomProgramValidateRequest request,
            RoomProgramValidateResponse validateResponse);

    /**
     * Get rate plan code by program id.
     * 
     * @param programId
     *            Program identifier
     * @param propertyId
     *            Property identifier
     * @return Returns rate plan code
     */
    String getRatePlanByProgramId(String programId, String propertyId);

    /**
     * This method returns programId to promo association
     * @param request
     * @return
     */
    public Map<String, String> getProgramPromoAssociation(RoomProgramPromoAssociationRequest request);

    /**
     * Finds and returns programs linked as part of segment
     *
     * @param groupCode
     *            segment id
     * @param source
     *            client source
     * @return
     */
    List<RoomProgramBasic> findProgramsByGroupCode(String groupCode, LocalDate checkInDate, LocalDate checkOutDate, String source);
}
