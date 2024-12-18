package com.mgm.services.booking.room.model.response;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;

public @Data class CustomerOfferV3Response {

    private List<CustomerOffer> offers = new LinkedList<>();
    private String userCvsValues;
}
