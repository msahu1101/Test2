package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public @Data class CustomerOffer {

    private String id;
    private String propertyId;
    private String promo;
    private CustomerOfferType type;
    private boolean poProgram;
    private String contentPath;
    private String contentType;
}
