package com.mgm.services.booking.room.model;

import java.io.Serializable;
import java.util.List;

import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.model.CartItem;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class RoomCartItem extends CartItem implements Serializable {

    private static final long serialVersionUID = 4534171339215276877L;
    
    private RoomReservation reservation;
    private List<RoomRequest> availableComponents;
    private String auroraItineraryId;
    private RoomCartRequest cartItemRequest;
    private boolean promotedMlifePrice;
}
