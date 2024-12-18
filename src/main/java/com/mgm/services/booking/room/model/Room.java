package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class Room {
    
    public Room(String id) {
        this.id = id;
    }
    
    private String id;
}
