package com.mgm.services.booking.room.validator;

import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.common.exception.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator class for reservation/checkout payload for v2 services
 */
public class RoomProgramRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RoomProgramV2Request.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final RoomProgramV2Request request = (RoomProgramV2Request) target;

        if (CollectionUtils.isEmpty(request.getProgramIds())) {
            errors.rejectValue("programIds", ErrorCode.INVALID_PROGRAM_ID.getErrorCode());
        }

        if (null == request.getStartDate()) {
            errors.rejectValue("startDate", ErrorCode.INVALID_TRAVEL_DATE.getErrorCode());
        }

        if (null == request.getEndDate()) {
            errors.rejectValue("endDate", ErrorCode.INVALID_TRAVEL_DATE.getErrorCode());
        }

    }
}
