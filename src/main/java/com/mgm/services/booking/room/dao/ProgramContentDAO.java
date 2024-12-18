package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.content.CuratedOfferResponse;
import com.mgm.services.booking.room.model.content.Program;

/**
 * DAO interface to expose services for retrieving program marketing content.
 *
 */
public interface ProgramContentDAO {

    /**
     * Gets and returns program marketing content for the requested program id.
     * Service relies on content api end points to retrieve program information.
     * 
     * @param propertyId
     *            Property Id to which program belongs
     * @param programId
     *            Program Id
     * @return Basic program marketing content
     */
    Program getProgramContent(String propertyId, String programId);

    /**
     * Gets and returns curated hotel offers for a property from Content APIs.
     * If propertyId is empty, curated hotel offers for mgmresorts will be
     * returned.
     * 
     * @param propertyId
     *            Property GUID
     * @return
     */
    CuratedOfferResponse getCuratedHotelOffers(String propertyId);
}
