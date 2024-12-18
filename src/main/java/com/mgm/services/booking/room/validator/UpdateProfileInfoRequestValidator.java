package com.mgm.services.booking.room.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.validator.util.ReservationValidatorUtil;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Validator for modify checkout flow request.
 * 
 * @author jayveera
 *
 */
public class UpdateProfileInfoRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UpdateProfileInfoRequestValidator.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        
        UpdateProfileInfoRequest updateProfileInfoRequest = (UpdateProfileInfoRequest)target;
        if (StringUtils.isBlank(updateProfileInfoRequest.getItineraryId())) {
            errors.rejectValue("itineraryId", ErrorCode.INVALID_ITINERARY_ID.getErrorCode());
        }
        
        UserProfileRequest profile = updateProfileInfoRequest.getUserProfile();
        if (null != profile) {
            ReservationValidatorUtil.validateProfile("userProfile", profile, errors);
        }
    }
}
