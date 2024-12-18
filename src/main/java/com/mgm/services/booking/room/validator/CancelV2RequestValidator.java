/**
 * 
 */
package com.mgm.services.booking.room.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator class for CancelV2Request.
 * 
 * @author laknaray
 *
 */
public class CancelV2RequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CancelV2Request.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CancelV2Request cancelV2Request = (CancelV2Request)target;
        if (cancelV2Request.getCustomerId() <= 0) {
            errors.rejectValue("customerId", ErrorCode.INVALID_CUSTOMER_ID.getErrorCode());
        }
    }

}
