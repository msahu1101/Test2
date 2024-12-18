package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;
/**
 * Customer offer search response.
 * 
 * @author jayveera
 *
 */
public @Data class CustomerOfferResponse {
    private List<CustomerOfferDetail> offers;
}
