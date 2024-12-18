package com.mgm.services.booking.room.model;

import lombok.Data;

/**
 * Billing info class
 * @author nitpande0
 *
 */
public @Data class BillingInfo {

    private PaymentInfo payment;
    private UserAddress address;
}
