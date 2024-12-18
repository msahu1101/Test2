package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.response.ProfileServiceResponse;

public interface ProfileServiceDAO{

    /**
     * Get cvs records for a mlife number
     *
     * @param mlifeId
     *            emr is the encrypted mlife number
     * @return Returns encryted mlife number and customer id
     */
    ProfileServiceResponse getCustomerIdByMlifeId(String mlifeId);
}
