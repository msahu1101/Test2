package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang.StringUtils;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class RoomProgramValidateRequest extends BaseRequest {

    private String programId;
    private String promoCode;
    private String promo;
    private String propertyId;
    private String redemptionCode;
    private boolean isModifyFlow;

    @AssertTrue(
            message = "_invalid_program")
    public boolean isProgramAvailable() {
        return StringUtils.isNotEmpty(programId) || StringUtils.isNotEmpty(promoCode) || StringUtils.isNotEmpty(promo);
    }

    @AssertTrue(
            message = "_invalid_promocode")
    public boolean isPropertyAvailable() {
        return (StringUtils.isEmpty(promoCode) && StringUtils.isEmpty(promo)) || StringUtils.isNotEmpty(propertyId);
    }
}
