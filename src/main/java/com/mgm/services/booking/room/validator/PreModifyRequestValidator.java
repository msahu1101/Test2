package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator class for pre-modify reservation end point.
 *
 */
public class PreModifyRequestValidator implements Validator {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return PreModifyRequest.class.equals(clazz);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#validate(java.lang.Object,
     * org.springframework.validation.Errors)
     */
    @Override
    public void validate(Object target, Errors errors) {

        ValidationUtils.rejectIfEmpty(errors, "confirmationNumber", "_invalid_confirmation_number");
        ValidationUtils.rejectIfEmpty(errors, "firstName", "_invalid_first_name");
        ValidationUtils.rejectIfEmpty(errors, "lastName", "_invalid_last_name");

        final String invalidDatesCode = ErrorCode.INVALID_DATES.getErrorCode();

        ValidationUtils.rejectIfEmpty(errors, "tripDetails", invalidDatesCode);
        ValidationUtils.rejectIfEmpty(errors, "tripDetails.checkInDate", invalidDatesCode);
        ValidationUtils.rejectIfEmpty(errors, "tripDetails.checkOutDate", invalidDatesCode);
    }

}
