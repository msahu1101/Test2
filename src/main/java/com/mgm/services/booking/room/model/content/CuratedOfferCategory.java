package com.mgm.services.booking.room.model.content;

import java.util.List;

import lombok.Data;

public @Data class CuratedOfferCategory {

    private String title;
    private String category;
    private List<CuratedOffer> offers;
}
