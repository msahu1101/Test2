package com.mgm.services.booking.room.model.request;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The Class CreateCustomerRequest.
 */
@EqualsAndHashCode(
        callSuper = false)
public @SuperBuilder @Getter @AllArgsConstructor @NoArgsConstructor class CreateCustomerRequest extends ProfileRequest {

    private String password;
    private int secretQuestionId;
    private String secretAnswer;
    @Setter
    private boolean isEnroll;
    @Setter
    private boolean activate;
    private boolean isCaslOptin;
}
