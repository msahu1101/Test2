package com.mgm.services.booking.room.model;

import lombok.Data;

/**
 * Model class with fields representing financial impact for the room
 * cancellation
 * 
 * @author nitpande0
 *
 */
public @Data class CancellationFinancialImpact {
    private String productType;
    private String operation;
    private double totalTransactionAmount;
    private String confirmationNumber;
    private RoomCancellation roomCancellation = new RoomCancellation();

    /**
     * Model class for the cancellation charges
     * @author nitpande0
     *
     */
    public static @Data class RoomCancellation {
        private double amountForfeit;
        private double amountRefund;
    }

}
