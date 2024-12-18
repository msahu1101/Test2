package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.BaseReservationResponse;

/**
 * Services providing functionalities for sending room confirmation and
 * cancellation emails.
 */
public interface ReservationEmailService {

    /**
     * Send room reservation confirmation email
     * 
     * @param reservation
     *            Room reservation
     * @param response
     *            Room reservation response
     * @param emailPropertyId
     *            Property location for emails
     */
    void sendConfirmationEmail(RoomReservation reservation, BaseReservationResponse response,
            String emailPropertyId);

    /**
     * Send room reservation cancellation email
     * 
     * @param reservation
     *            Room reservation
     * @param response
     *            Room reservation response
     * @param emailPropertyId
     *            Property location for emails
     */
    void sendCancellationEmail(RoomReservation reservation, BaseReservationResponse response, String emailPropertyId);
}
