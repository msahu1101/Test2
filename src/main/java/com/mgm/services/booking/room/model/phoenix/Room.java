package com.mgm.services.booking.room.model.phoenix;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(
        callSuper = true)
@ToString(
        callSuper = true)
public @Data class Room extends BasePhoenixEntity {

    private List<RoomComponent> components = new ArrayList<>();
    private String operaRoomCode;
}
