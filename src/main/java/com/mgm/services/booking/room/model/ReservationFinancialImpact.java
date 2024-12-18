package com.mgm.services.booking.room.model;

import lombok.Data;

/**
 * Model class with fields representing financial impact for the room
 * reservation
 * 
 * @author nitpande0
 *
 */
public @Data class ReservationFinancialImpact {
    private String productType;
    private String operation;
    private double totalTransactionAmount;
    private double amountCharged;
    private double balanceDue;
    private String confirmationNumber;
    private RoomBookingItemized roomBookingItemized = new RoomBookingItemized();

    /**
     * Itemized information for the booking
     * 
     * @author nitpande0
     *
     */
    public static @Data class RoomBookingItemized {
        private double roomSubtotal;
        private double taxes;
        private double resortFeeTax;
    }

}
