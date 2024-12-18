package com.mgm.services.booking.room.model.phoenix;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(
        callSuper = true)
@ToString(
        callSuper = true)
public @Data class Property extends BasePhoenixEntity {

    private HotelSettings hotelSettings;

}
