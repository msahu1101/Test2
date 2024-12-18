package com.mgm.services.booking.room.service.cache;

import java.util.List;

import com.mgm.services.booking.room.model.phoenix.RoomProgram;

/**
 * Service interface exposing services for room program cache services.
 *
 */
public interface RoomProgramCacheService {

    /**
     * Gets the room program.
     *
     * @param programId
     *            the program id
     * @return the room program
     */
    RoomProgram getRoomProgram(String programId);

    /**
     * Returns true if the program is in cache.
     * 
     * @param programId
     *            Program Id
     * @return Returns true if program is in cache.
     */
    boolean isProgramInCache(String programId);

    /**
     * Returns true if the supplied id is a segment id.
     * 
     * @param programId
     *            Program Identifier
     * @return Returns true if the supplied id is a segment id.
     */
    boolean isSegment(String programId);

    /**
     * Get programs under the supplied segment if available.
     * 
     * @param segmentId
     *            Segment Id
     * @return Room programs under the segment
     */
    List<RoomProgram> getProgramsBySegmentId(String segmentId);

    /**
     * Get programs under the supplied group code if available.
     *
     * @param groupCode
     *            Group Code
     * @return Room programs under the segment
     */
    List<RoomProgram> getProgramsByGroupCode(String groupCode);

    /**
     * Get programs under the supplied promo code if available.
     * 
     * @param promoCode
     *            Promo code
     * @return Room programs under the segment promo code
     */
    List<RoomProgram> getProgramsByPromoCode(String promoCode);

    /**
     * Returns true if the program is a perpetual program
     * 
     * @param programId
     *            Program Id
     * @return Returns true if the program is a perpetual program
     */
    boolean isProgramPO(String programId);

    /**
     * Returns opera promo code by program id.
     * 
     * @param programId
     *            Program GUID
     * @return Returns opera promo code by program id.
     */
    String getPromoCodeByProgramId(String programId);
    
    /**
     * Get programs by patron promo ids.
     * 
     * @param promoIds
     *            List of patron promo ids.
     * @return Returns list of room programs
     */
    List<RoomProgram> getProgramsByPatronPromoIds(List<String> promoIds);
    
    /**
     * Find PO program based on customer rank and property.
     * 
     * @param customerRank
     *            Customer rank for the property
     * @param propertyId
     *            Property GUID
     * @return Return PO program if available
     */
    RoomProgram getProgramByCustomerRank(int customerRank, String propertyId);
    
}
