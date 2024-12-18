package com.mgm.services.booking.room.model.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for Customer Offer Search Request.
 * 
 * @author jayveera
 *
 */
public @SuperBuilder @Getter @AllArgsConstructor @NoArgsConstructor class CustomerOffersRequestDTO {

    private String propertyId;
    private boolean notRolledToSegments;
    private boolean notSorted;
    private boolean wantCommentary;
    private String source;
    private long customerId;
    private String mlifeNumber;

}
