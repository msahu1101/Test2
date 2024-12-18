package com.mgm.services.booking.room.validator;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.validator.util.ReservationValidatorUtil;
import com.mgm.services.booking.room.validator.util.ReservationValidatorUtil.BookingType;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Validator class for room reservation charges request payload.
 * 
 * @author jayveera
 *
 */
public class RoomReservationChargesRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RoomReservationChargesRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RoomReservationChargesRequest request = (RoomReservationChargesRequest) target;

        if (!ValidationUtil.isUuid(request.getPropertyId())) {
            errors.rejectValue("propertyId", ErrorCode.INVALID_PROPERTY.getErrorCode());
        }
        final String roomTypeId = request.getRoomTypeId();
        if (!ValidationUtil.isUuid(roomTypeId) && !ACRSConversionUtil.isAcrsRoomCodeGuid(roomTypeId)) {
            errors.rejectValue("roomTypeId", ErrorCode.INVALID_ROOMTYPE.getErrorCode());
        }
        ValidationUtils.rejectIfEmpty(errors, "bookings", ErrorCode.INVALID_BOOKINGS.getErrorCode());

        UserProfileRequest profile = request.getProfile();
        if (null != profile) {
            ReservationValidatorUtil.validateProfile("profile", profile, errors);
        }

        TripDetailsRequest tripDetails = request.getTripDetails();
        if (null != tripDetails) {
            ReservationValidatorUtil.validateTripDetails("tripDetails", tripDetails, errors, BookingType.PREBOOK);
        }

    }

}
