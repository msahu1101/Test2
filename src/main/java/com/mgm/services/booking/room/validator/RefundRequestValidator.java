package com.mgm.services.booking.room.validator;

import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.common.exception.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class RefundRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return RefundRequestValidator.class.equals(clazz);
    }
    @Override
    public void validate(Object target, Errors errors) {
        PaymentRoomReservationRequest paymentRoomReservationRequest = (PaymentRoomReservationRequest)target;
        if(StringUtils.isEmpty(paymentRoomReservationRequest.getConfirmationNumber())){
            {
                errors.rejectValue("confirmationNumber", ErrorCode.NO_CONFIRMATION_NUMBER.getErrorCode());
            }
        }

    }

}
