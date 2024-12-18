package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.ReservationAssociateRequest;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;

public class ReservationAssociateRequestValidator implements Validator {

    private ValidationHelper helper = new ValidationHelper();

    @Override
    public boolean supports(Class<?> clazz) {
        return ReservationAssociateRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        ValidationUtils.rejectIfEmpty(errors, "confirmationNumber",
                ErrorCode.ASSOCIATION_VIOLATION_NO_CONFIRMATION_NUMBER.getErrorCode());

        rejectIfNotAGuestToken(errors);
    }

    /**
     * Validate whether the token is JWT token.
     */
    private void rejectIfNotAGuestToken(Errors errors) {
        if (!helper.isTokenAGuestToken()) {
            errors.rejectValue("confirmationNumber", ErrorCode.SERVICE_TOKEN_NOT_SUPPORTED.getErrorCode());
        }
    }

}
