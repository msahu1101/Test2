package com.mgm.services.booking.room.validator;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Validator class for reservation/checkout payload for v2 services
 */
public class PerpetualProgramsRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PerpetualProgramRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PerpetualProgramRequest perpetualProgramRequest = (PerpetualProgramRequest) target;
        if (perpetualProgramRequest.getCustomerId() <= 0) {
            errors.rejectValue("customerId", ErrorCode.INVALID_CUSTOMER.getErrorCode());
        }

        if (CollectionUtils.isNotEmpty(perpetualProgramRequest.getPropertyIds())) {
            perpetualProgramRequest.getPropertyIds().stream().forEach(propertyId -> {
                if (!ValidationUtil.isUuid(propertyId)) {
                    errors.rejectValue("propertyIds", ErrorCode.INVALID_PROPERTY_ID.getErrorCode());
                }
            });
        }
    }
}
