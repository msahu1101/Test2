package com.mgm.services.booking.room.model.request;

import lombok.Data;
import lombok.NonNull;

public @Data class RoomRequest {

    @NonNull private String id;
    private boolean selected;
}
