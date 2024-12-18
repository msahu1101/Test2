/**
 * 
 */
package com.mgm.services.booking.room.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.CancelV3Request;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator class for CancelVRequest.
 * 
 * @author laknaray
 *
 */
public class CancelV3RequestValidator implements Validator {

    private ValidationHelper helper = new ValidationHelper();

    @Override
    public boolean supports(Class<?> clazz) {
        return CancelV3Request.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CancelV3Request request = (CancelV3Request) target;
        if (StringUtils.isEmpty(request.getConfirmationNumber())) {
            errors.rejectValue("confirmationNumber", ErrorCode.NO_CONFIRMATION_NUMBER.getErrorCode());
        }
        // First and last names are mandatory for requests which neither has
        // elevated access nor the token is a guest token
        //if (!helper.hasElevatedAccessToUpdate() && !helper.isTokenAGuestToken()) {
        if(!helper.validateTokenBasedOrServiceBasedRole() && !helper.isTokenAGuestToken()) {
            if (StringUtils.isBlank(request.getFirstName())) {
                errors.rejectValue("firstName", ErrorCode.NO_FIRST_NAME.getErrorCode());
            }
            if (StringUtils.isBlank(request.getLastName())) {
                errors.rejectValue("lastName", ErrorCode.NO_LAST_NAME.getErrorCode());
            }
        }
    }

}
