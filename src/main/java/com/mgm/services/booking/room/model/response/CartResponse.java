package com.mgm.services.booking.room.model.response;

import java.util.List;

import com.mgm.services.booking.room.model.CartSummary;

import lombok.Data;

/**
 * Model object for holding the cart response 
 * @author nitpande0
 *
 */
public @Data class CartResponse {
    private List<RoomReservationResponse> items;
    private CartSummary summary;

}
