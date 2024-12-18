package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Validator class for token request
 */
public class TokenRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TokenRequestValidator.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "clientId", "_no_client_id");
        ValidationUtils.rejectIfEmpty(errors, "clientSecret", "_no_client_secret");
    }

}
