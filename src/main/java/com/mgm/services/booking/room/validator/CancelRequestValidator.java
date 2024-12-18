package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.CancelRequest;

/**
 * Validator class for cancel reservation service
 */
public class CancelRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CancelRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "firstName", "_invalid_first_name");
        ValidationUtils.rejectIfEmpty(errors, "lastName", "_invalid_last_name");
        ValidationUtils.rejectIfEmpty(errors, "confirmationNumber", "_invalid_confirmation_number");
    }

}
