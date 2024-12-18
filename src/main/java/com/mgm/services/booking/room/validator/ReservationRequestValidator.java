package com.mgm.services.booking.room.validator;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.google.common.base.Strings;
import com.mgm.services.booking.room.model.PaymentInfo;
import com.mgm.services.booking.room.model.UserAddress;
import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.booking.room.model.request.ReservationRequest;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Validator class for reservation/checkout payload
 */
public class ReservationRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ReservationRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        ValidationUtils.rejectIfEmpty(errors, "profile", "_invalid_profile");
        ValidationUtils.rejectIfEmpty(errors, "billing", "_invalid_billing");

        ValidationUtils.rejectIfEmpty(errors, "billing.payment", "_invalid_payment");
        ValidationUtils.rejectIfEmpty(errors, "billing.payment.cardNumber", "_invalid_cardNumber");
        ValidationUtils.rejectIfEmpty(errors, "billing.payment.cardHolder", "_invalid_cardHolder");
        ValidationUtils.rejectIfEmpty(errors, "billing.payment.type", "_invalid_cardType");
        ValidationUtils.rejectIfEmpty(errors, "billing.payment.cvv", "_invalid_cvv");
        ValidationUtils.rejectIfEmpty(errors, "billing.payment.expiry", "_invalid_expiry");

        ValidationUtils.rejectIfEmpty(errors, "billing.address", "_invalid_address");
        ValidationUtils.rejectIfEmpty(errors, "billing.address.street1", ErrorCode.INVALID_STREET.getErrorCode());
        ValidationUtils.rejectIfEmpty(errors, "billing.address.city", "_invalid_city");
        ValidationUtils.rejectIfEmpty(errors, "billing.address.country", "_invalid_country");

        ReservationRequest reservationRequest = (ReservationRequest) target;

        boolean eligibleForAccountCreation = reservationRequest.isEligibleForAccountCreation();

        UserProfile profile = reservationRequest.getProfile();
        validateProfile(profile, errors, eligibleForAccountCreation);

        if (null != reservationRequest.getBilling()) {
            UserAddress address = reservationRequest.getBilling().getAddress();
            PaymentInfo payment = reservationRequest.getBilling().getPayment();
            validatePayment(payment, errors);
            validateBillingAddress(address, errors);
        }

    }

    private void validateProfile(UserProfile profile, Errors errors, boolean eligibleForAccountCreation) {
        if (null != profile) {
            validateEmptyProfile(profile, errors, eligibleForAccountCreation);

            if (StringUtils.isNotEmpty(profile.getEmail()) && !ValidationUtil.isValidEmailFormat(profile.getEmail())) {
                errors.rejectValue("profile.email", "_invalid_email");
            }

            if (StringUtils.isNotEmpty(profile.getFirstName()) && !ValidationUtil.isValidName(profile.getFirstName())) {
                errors.rejectValue("profile.firstName", "_invalid_name");
            }

            if (StringUtils.isNotEmpty(profile.getLastName()) && !ValidationUtil.isValidName(profile.getLastName())) {
                errors.rejectValue("profile.lastName", "_invalid_name");
            }

            if (StringUtils.isNotEmpty(profile.getPhone()) && !ValidationUtil.isValidPhone(profile.getPhone())) {
                errors.rejectValue("profile.phone", "_invalid_phone");

            }

            if (eligibleForAccountCreation) {
                validateAdditionalProfileFields(profile, errors);
            }

        }
    }

    /**
     * This methods will validate the additional profile fields required for
     * account creation in JWB flow.
     *
     * @param profile
     *            user profile object
     * @param errors
     *            errors object
     */
    private void validateAdditionalProfileFields(UserProfile profile, Errors errors) {
        if (StringUtils.isNotEmpty(profile.getPassword()) && !ValidationUtil.isValidPassword(profile.getPassword())) {
            errors.rejectValue("profile.password", "_invalid_password");
        }

        if (profile.getDateOfBirth() != null && !ValidationUtil.isValidDOB(profile.getDateOfBirth())) {
            errors.rejectValue("profile.dateOfBirth", ErrorCode.INVALID_DOB.getErrorCode());
        }

        if (StringUtils.isNotEmpty(profile.getSecurityAnswer())
                && !ValidationUtil.isValidSecretAnswer(profile.getSecurityAnswer())) {
            errors.rejectValue("profile.securityAnswer", "_invalid_securityAnswer");
        }
    }

    private void validateEmptyProfile(UserProfile profile, Errors errors, boolean eligibleForAccountCreation) {
        if (StringUtils.isEmpty(profile.getMlifeNumber()) && profile.getCustomerId() <= 0
                && isEmptyGuest(profile, eligibleForAccountCreation)) {
            errors.rejectValue("profile", "_invalid_profile");
        }
    }

    private boolean isEmptyGuest(UserProfile profile, boolean eligibleForAccountCreation) {
        boolean isEmptyGuest;
        String[] guestFields = { profile.getEmail(), profile.getFirstName(), profile.getLastName(),
                profile.getPhone() };

        if (eligibleForAccountCreation) {
            guestFields = new String[] { profile.getEmail(), profile.getFirstName(), profile.getLastName(),
                    profile.getPhone(), profile.getPassword(), profile.getSecurityQuestionId(),
                    profile.getSecurityAnswer() };
            isEmptyGuest = (Arrays.asList(guestFields).stream().anyMatch(Strings::isNullOrEmpty)
                    || profile.getDateOfBirth() == null);
        } else {
            isEmptyGuest = Arrays.asList(guestFields).stream().anyMatch(Strings::isNullOrEmpty);
        }
        return isEmptyGuest;
    }

    private void validatePayment(PaymentInfo payment, Errors errors) {

        if (null != payment) {
            if (StringUtils.isNotEmpty(payment.getCardHolder())
                    && !ValidationUtil.isValidCardHolder(payment.getCardHolder())) {
                errors.rejectValue("billing.payment.cardHolder", "_invalid_cardHolder");
            }

            if (StringUtils.isNotEmpty(payment.getCvv()) && !StringUtils.isNumeric(payment.getCvv())) {
                errors.rejectValue("billing.payment.cvv", "_invalid_cvv");
            }

            if (!ValidationUtil.isValidCardNumber(payment.getCardNumber())) {
                errors.rejectValue("billing.payment.cardNumber", "_invalid_cardNumber");
            }
        }

    }

    private void validateBillingAddress(UserAddress address, Errors errors) {

        if (null != address) {
            if (StringUtils.isNotEmpty(address.getStreet1()) && !ValidationUtil.isValidAddress1(address.getStreet1())) {
                errors.rejectValue("billing.address.street1", ErrorCode.INVALID_STREET.getErrorCode());
            }

            if (StringUtils.isNotEmpty(address.getStreet2()) && !ValidationUtil.isValidAddress2(address.getStreet2())) {
                errors.rejectValue("billing.address.street2", ErrorCode.INVALID_STREET.getErrorCode());
            }

            if (StringUtils.isNotEmpty(address.getCity()) && !ValidationUtil.isValidCity(address.getCity())) {
                errors.rejectValue("billing.address.city", "_invalid_city");
            }

            if (!ValidationUtil.isValidState(address.getState(), address.getCountry())) {
                errors.rejectValue("billing.address.state", "_invalid_state");
            }

            if (!ValidationUtil.isValidPostalCode(address.getPostalCode(), address.getCountry())) {
                errors.rejectValue("billing.address.postalCode", "_invalid_postalCode");
            }
        }
    }

}
