package com.mgm.services.booking.room.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.mgm.services.booking.room.model.request.PreviewCommitRequest;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Validator class for reservation commit end point.
 *
 */
public class PreviewCommitRequestValidator implements Validator {

    private ValidationHelper helper = new ValidationHelper();

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return PreviewCommitRequest.class.equals(clazz);
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

        final String noTotalsCode = ErrorCode.MODIFY_VIOLATION_NO_TOTALS.getErrorCode();

        ValidationUtils.rejectIfEmpty(errors, "previewReservationTotal", noTotalsCode);
        ValidationUtils.rejectIfEmpty(errors, "previewReservationDeposit", noTotalsCode);
        rejectIfFirstOrLastNamesEmpty(target, errors);
        rejectIfCvvIsInInvalidFormat(target, errors);
    }

    /**
     * Validate whether the token is JWT token otherwise request payload should have
     * firstName and lastName.
     */
    private void rejectIfFirstOrLastNamesEmpty(Object target, Errors errors) {
        PreviewCommitRequest commitRequest = (PreviewCommitRequest) target;

        // Allow only service token to access without first and last name for ICE use case. Hence only
        // checking requests with non-service JWT token must have firstName and lastName
        // in the request, otherwise reject.
        if (!helper.hasServiceRoleAccess() && (StringUtils.isEmpty(commitRequest.getFirstName())
                || StringUtils.isEmpty(commitRequest.getLastName()))) {
            errors.rejectValue("firstName", ErrorCode.INVALID_NAME.getErrorCode());
        }
    }

    /**
     * Validate whether cvv is present in the request, if so validate the format.
     */
    private void rejectIfCvvIsInInvalidFormat(Object target, Errors errors) {
        PreviewCommitRequest commitRequest = (PreviewCommitRequest) target;
        String cvv = commitRequest.getCvv();
        if (StringUtils.isNotEmpty(cvv) && !ValidationUtil.isValidCVV(cvv)) {
            errors.rejectValue("cvv", ErrorCode.INVALID_CVV.getErrorCode());
        }
    }
}
