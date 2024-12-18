package com.mgm.services.booking.room.model.phoenix;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(
        callSuper = true)
@ToString(
        callSuper = true)
public @Data class RoomSegment extends BasePhoenixEntity {

    private List<String> programs;

}
