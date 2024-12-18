package com.mgm.services.booking.room.validator;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Validator class for reservation/checkout payload for v2 services
 */
public class RoomProgramValidateRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PerpetualProgramRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RoomProgramValidateRequest roomProgramValidateRequest = (RoomProgramValidateRequest) target;

        final String programId = roomProgramValidateRequest.getProgramId();
        if (StringUtils.isNotEmpty(programId)
                && !ACRSConversionUtil.isAcrsRatePlanGuid(programId)
                && !ACRSConversionUtil.isAcrsGroupCodeGuid(programId)
                && !ValidationUtil.isUuid(programId)) {
            errors.rejectValue("programId", ErrorCode.INVALID_PROGRAM_ID.getErrorCode());
        }
        if (StringUtils.isNotEmpty(roomProgramValidateRequest.getPropertyId())
                && !ValidationUtil.isUuid(roomProgramValidateRequest.getPropertyId())) {
            errors.rejectValue("propertyId", ErrorCode.INVALID_PROPERTY_ID.getErrorCode());
        }

    }
}
