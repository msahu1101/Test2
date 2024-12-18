/**
 * 
 */
package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.common.exception.ErrorCode;

/**
 * @author laknaray
 *
 */
public class RoomComponentV2RequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RoomComponentV2Request.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RoomComponentV2Request roomComponentV2Request = (RoomComponentV2Request) target;

        ValidationUtils.rejectIfEmpty(errors, "propertyId", ErrorCode.INVALID_PROPERTY.getErrorCode());
        ValidationUtils.rejectIfEmpty(errors, "roomTypeId", ErrorCode.INVALID_ROOMTYPE.getErrorCode());
        ValidationUtils.rejectIfEmpty(errors, "checkInDate", ErrorCode.INVALID_DATES.getErrorCode());
        ValidationUtils.rejectIfEmpty(errors, "checkOutDate", ErrorCode.INVALID_DATES.getErrorCode());
        if (roomComponentV2Request.getCheckInDate() != null && roomComponentV2Request.getCheckOutDate() != null
                && roomComponentV2Request.getCheckInDate().isAfter(roomComponentV2Request.getCheckOutDate())) {
            errors.rejectValue("checkOutDate", ErrorCode.INVALID_DATES.getErrorCode());
        }

    }

}
