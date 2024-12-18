package com.mgm.services.booking.room.model.content;

import lombok.Data;

public @Data class CuratedOffer {

    private String subCategoryLabel;
    private String id;
    private boolean bookableOnline;
    private boolean memberOffer;
    private boolean multiOffer;
    private String propertyId;
    private String path;
    private String contentType;
}
