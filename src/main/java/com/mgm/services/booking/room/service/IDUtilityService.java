package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.reservation.ReservationProfile;

public interface IDUtilityService {

    /**
     * Compare the actual/original firstName and lastName from ReservationProfile
     * object against the firstName and lastName.
     * 
     * @param profile
     *            ReservationProfile object, to read actual/original
     *            firstName and lastNames
     * @param firstName
     *            first name string to be compared
     * @param lastName
     *            last name string to be compared
     * @return true, if first and last names are matching (fuzzy match).
     */
    public boolean isFirstNameLastNameMatching(ReservationProfile profile, String firstName, String lastName);

}
