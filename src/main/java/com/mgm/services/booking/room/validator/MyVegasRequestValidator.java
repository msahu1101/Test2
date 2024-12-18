package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator class for myvegas request
 */
public class MyVegasRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return MyVegasRequestValidator.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "redemptionCode", ErrorCode.NO_REDEMPTION_CODE.getErrorCode());
    }

}
