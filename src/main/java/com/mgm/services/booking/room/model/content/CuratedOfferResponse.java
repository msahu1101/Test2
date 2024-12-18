package com.mgm.services.booking.room.model.content;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

public @Data class CuratedOfferResponse {

    private String productType;
    private List<CuratedOfferCategory> curatedOffersList = new ArrayList<>();
}
