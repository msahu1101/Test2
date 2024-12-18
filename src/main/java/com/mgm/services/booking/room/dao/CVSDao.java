package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.response.CVSResponse;

/**
 * Interface exposing services for CVS related functionality
 *
 */
public interface CVSDao {

    /**
     * Get cvs records for a mlife number
     * 
     * @param mLifeNumber
     *            Mlife number for which cvs record should be returned
     * @return Returns cvs record
     */
    CVSResponse getCustomerValues(String mLifeNumber);
}
