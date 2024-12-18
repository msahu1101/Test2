package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
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
public class RoomAndComponentChargesRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CalculateRoomChargesRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CalculateRoomChargesRequest request = (CalculateRoomChargesRequest) target;

        if (!ValidationUtil.isUuid(request.getPropertyId())) {
            errors.rejectValue("propertyId", ErrorCode.INVALID_PROPERTY.getErrorCode());
        }
        if (!ValidationUtil.isUuid(request.getRoomTypeId()) && !ACRSConversionUtil.isAcrsRoomCodeGuid(request.getRoomTypeId())) {
            errors.rejectValue("roomTypeId", ErrorCode.INVALID_ROOMTYPE.getErrorCode());
        }
        
        TripDetailsRequest tripDetails = request.getTripDetails();
        if (null != tripDetails) {
            ReservationValidatorUtil.validateTripDetails("tripDetails", tripDetails, errors, BookingType.PREBOOK);
        }

    }

}
