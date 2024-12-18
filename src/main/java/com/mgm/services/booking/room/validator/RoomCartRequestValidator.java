package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Validator class for pre-reserve end point request
 */
public class RoomCartRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RoomCartRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final String invalidDatesCode = ErrorCode.INVALID_DATES.getErrorCode();

        ValidationUtils.rejectIfEmpty(errors, "checkInDate", invalidDatesCode);
        ValidationUtils.rejectIfEmpty(errors, "checkOutDate", invalidDatesCode);
        ValidationUtils.rejectIfEmpty(errors, "propertyId", "_invalid_property");
        ValidationUtils.rejectIfEmpty(errors, "roomTypeId", "_invalid_roomtype");

        RoomCartRequest preReserveRequest = (RoomCartRequest) target;

        if (!ValidationUtil.isTripDatesValid(preReserveRequest.getCheckInDate(), preReserveRequest.getCheckOutDate())) {
            errors.rejectValue("checkInDate", "_invalid_dates");
        }

        if (!ValidationUtil.isUuid(preReserveRequest.getPropertyId())) {
            errors.rejectValue("propertyId", "_invalid_property");
        }

        if (!ValidationUtil.isUuid(preReserveRequest.getRoomTypeId())) {
            errors.rejectValue("roomTypeId", "_invalid_roomtype");
        }

    }

}
