package com.mgm.services.booking.room.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator class to validate FindReservationV2Request object used in find
 * reservation v2 flow.
 * 
 * @author laknaray
 *
 */
public class FindReservationV2RequestValidator implements Validator {

    private ValidationHelper helper = new ValidationHelper();

    @Override
    public boolean supports(Class<?> clazz) {
        return FindReservationV2Request.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        FindReservationV2Request findReservationV2Request = (FindReservationV2Request) target;

        if (StringUtils.isBlank(findReservationV2Request.getConfirmationNumber())) {
            errors.rejectValue("confirmationNumber", ErrorCode.NO_CONFIRMATION_NUMBER.getErrorCode());
        }
        // First and last names are mandatory only for requests neither has elevated
        // access nor the token is a guest token
        //if (!helper.hasElevatedAccess() && !helper.isTokenAGuestToken()) {
        if(!helper.hasServiceRoleAccess() && !helper.isTokenAGuestToken()) {
            if (StringUtils.isBlank(findReservationV2Request.getFirstName())) {
                errors.rejectValue("firstName", ErrorCode.NO_FIRST_NAME.getErrorCode());
            }
            if (StringUtils.isBlank(findReservationV2Request.getLastName())) {
                errors.rejectValue("lastName", ErrorCode.NO_LAST_NAME.getErrorCode());
            }
        }
    }

}
