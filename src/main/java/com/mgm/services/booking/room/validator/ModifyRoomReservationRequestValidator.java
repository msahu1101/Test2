package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.ModifyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.validator.util.ReservationValidatorUtil;
import com.mgm.services.booking.room.validator.util.ReservationValidatorUtil.BookingType;

/**
 * Validator class for reservation/checkout payload for v2 services
 */
public class ModifyRoomReservationRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ModifyRoomReservationRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        ModifyRoomReservationRequest modifyPartyRoomReservationRequest = (ModifyRoomReservationRequest) target;

        RoomReservationRequest roomReservationRequest = modifyPartyRoomReservationRequest.getRoomReservation();

        ReservationValidatorUtil.validateMandatoryFields(roomReservationRequest, errors, BookingType.MODIFY);
        ReservationValidatorUtil.validateCustomerId(roomReservationRequest.getProfile(),
                roomReservationRequest.getCustomerId(), errors);

        UserProfileRequest profile = roomReservationRequest.getProfile();
        if (null != profile) {
            ReservationValidatorUtil.validateProfile("roomReservation.profile", profile, errors);
        }
        ReservationValidatorUtil.validateBilling(roomReservationRequest.getBilling(), errors, BookingType.MODIFY);
        TripDetailsRequest tripDetails = roomReservationRequest.getTripDetails();
        if (null != tripDetails) {
            ReservationValidatorUtil.validateTripDetails("roomReservation.tripDetails", tripDetails, errors,
                    BookingType.MODIFY);
        }
    }
}
