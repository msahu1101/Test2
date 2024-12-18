package com.mgm.services.booking.room.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator class for pre-modify reservation end point.
 *
 */
public class PreModifyRequestV2Validator implements Validator {

    private ValidationHelper helper = new ValidationHelper();

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return PreModifyV2Request.class.equals(clazz);
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
        rejectIfFirstOrLastNamesEmpty(target, errors);
    }

    /**
     * Validate whether the token is JWT token otherwise request payload should have
     * firstName and lastName.
     */
    private void rejectIfFirstOrLastNamesEmpty(Object target, Errors errors) {
        PreModifyV2Request preModifyV2Request = (PreModifyV2Request) target;

        // Allow only service token to access without first and last name for ICE use case. Hence only
        // checking requests with non-service JWT token must have firstName and lastName
        // in the request, otherwise reject.
        if (!helper.hasServiceRoleAccess() && (StringUtils.isEmpty(preModifyV2Request.getFirstName())
                || StringUtils.isEmpty(preModifyV2Request.getLastName()))) {
            errors.rejectValue("firstName", ErrorCode.INVALID_NAME.getErrorCode());
        }
    }
}
