package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;

@RunWith(MockitoJUnitRunner.class)
public class IDUtilityServiceImplTest extends BaseRoomBookingTest {

    @Test
    public void whenFuzzyNameMatchDisabled_expectStringNameMatchWithNonAlphaCharsStripped() {

        IDUtilityServiceImpl utilityService = new IDUtilityServiceImpl("/", 10, 10, false);

        ReservationProfile profile = new ReservationProfile();
        profile.setFirstName("Shaikh");
        profile.setLastName("Sha-ikh");

        assertTrue(utilityService.isFirstNameLastNameMatching(profile, "Sha’ikh", "Shaik-h"));

        assertTrue(utilityService.isFirstNameLastNameMatching(profile, "Sha’ikh", "Shaikh"));

        assertFalse(utilityService.isFirstNameLastNameMatching(profile, "Sha’ikh", "Shaik"));

        profile.setFirstName("joseph hewitt");
        profile.setLastName("Sha-ikh");

        assertTrue(utilityService.isFirstNameLastNameMatching(profile, "joseph-hewitt", "Shaik-h"));
    }
}
