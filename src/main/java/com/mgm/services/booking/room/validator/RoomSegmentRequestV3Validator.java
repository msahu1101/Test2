package com.mgm.services.booking.room.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.RoomSegmentRequest;
import com.mgm.services.common.exception.ErrorCode;

public class RoomSegmentRequestV3Validator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RoomSegmentRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        final RoomSegmentRequest request = (RoomSegmentRequest) target;

        if (StringUtils.isBlank(request.getSegment()) && StringUtils.isBlank(request.getProgramId())) {
            errors.rejectValue("segment", ErrorCode.INVALID_REQUEST_PARAMS.getErrorCode());
        }

    }

}