package com.mgm.services.booking.room.dao;

import org.springframework.http.HttpEntity;

import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.model.request.UpdateProfileRequest;

public interface OCRSDAO {
    public UserProfile getOCRSResvPrimaryProfile(HttpEntity<?> request, String cnfNumber);

    public OcrsReservation updateProfile(UpdateProfileRequest request);

    public OcrsReservation getOCRSReservation(String cnfNumber);
}
