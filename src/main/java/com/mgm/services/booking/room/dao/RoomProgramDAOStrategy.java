package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.request.RoomProgramPromoAssociationRequest;
import com.mgm.services.booking.room.model.request.RoomProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DAO interface to expose services to fetch available programs for a room
 *
 */
public interface RoomProgramDAOStrategy {

    String IDS_ATTRIBUTE = "ids";
    String ACTIVE_ATTRIBUTE = "active";
    String CODE_ATTRIBUTE = "code";
    String PROMOS_ATTRIBUTE = "promos";
    String PROMOS_SEARCH_TYPE = "searchType";
    String SEARCH_TYPE_PROMO_VALUE = "PROMO";
    String PROPERTY_CODE_ATTRIBUTE = "propertyCode";
    String ROOM_TYPE_ATTRIBUTE = "roomType";
    String TRAVEL_START_DATE_ATTRIBUTE = "travelStartDate";
    String TRAVEL_END_DATE_ATTRIBUTE = "travelEndDate";
    String BOOK_DATE_ATTRIBUTE = "bookDate";

    /**
     * This method fetches the applicable Programs for a customer
     * 
     * @param request
     * @return
     */
    ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramRequestDTO request);

    /**
     * This method returns the applicable offers/rateplans for the customer
     * 
     * @param request
     * @return
     */
    CustomerOfferResponse getCustomerOffers(CustomerOffersRequestDTO request);

    /**
     * This method validates a Program/RatePlan for its eligibility
     * 
     * @param validateRequest
     * @return
     */
    RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest validateRequest);

    /**
     * This method validates a Program/RatePlan for its eligibility
     * 
     * @param validateRequest
     * @return
     */
    RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest validateRequest);

    /**
     * This method returns ProgramId by opera PromoCode search
     * 
     * @param propertyId
     * @param promoCode
     * @return
     */
    String getProgramByPromoCode(String propertyId, String promoCode);

    /**
     * This method returns the eligible room offers
     * 
     * @param offersRequest
     * @return
     */
    List<RoomProgram> getRoomOffers(RoomProgramRequest offersRequest);

    /**
     * Finds and returns programs sharing the same rate plan code
     * 
     * @param ratePlanCode
     *            Rate plan code
     * @param source
     *            client source
     * @return
     */
    List<RoomProgramBasic> findProgramsByRatePlanCode(String ratePlanCode, String source);

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
     * Returns opera promo code by program id.
     * 
     * @param programId
     *            Program GUID
     * @return Returns opera promo code by program id.
     */
    String getRatePlanCodeByProgramId(String programId);
    
    /**
     * Returns room offers applicable for the the given customer which includes patron promos,
     * perpetual programs and ICE only programs.
     * 
     * @param offersRequest
     *            offers request
     * @param patronOffers
     *            input patron offers
     * @return Returns list of room offers
     */
    RoomProgramsResponseDTO getRoomPrograms(RoomProgramsRequestDTO offersRequest, List<CustomerPromotion> patronOffers,CVSResponse customerValues);
    
    /**
     * Returns true is the supplied GUID is a legacy GSE segment GUID.
     * 
     * @param programId
     *            Program GUID
     * @return
     */
    boolean isSegmentGUID(String programId);

    /**
     * This method returns ratePlan details from ACRS searched by the Id
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
     * This method returns the programId to promo association
     * @param request
     * @return
     */
    public Map<String, String> getProgramPromoAssociation(RoomProgramPromoAssociationRequest request);

    /**
     * Finds and returns programs sharing the same group code by looking up
     *
     * @param groupCode
     *            group code
     * @param source
     *            client source
     * @return
     */
    List<RoomProgramBasic> findProgramsByGroupCode(String groupCode, LocalDate checkInDate, LocalDate checkOutDate, String source);
}
