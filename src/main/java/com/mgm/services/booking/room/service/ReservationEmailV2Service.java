package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;

/**
 * Services providing functionalities for sending room confirmation and
 * cancellation emails for V2 APIs.
 */
public interface ReservationEmailV2Service {

	/**
	 * Send room reservation confirmation email
	 * 
	 * @param reservation room reservation
	 * @param response - response post reservation
	 */
    void sendConfirmationEmail(RoomReservation reservation, RoomReservationV2Response response, boolean isHDEPackageReservation);

    /**
     * Send room reservation cancellation email.
     * 
     * @param reservation room reservation
	 * @param response - response post reservation
     */
    void sendCancellationEmail(RoomReservation reservation, RoomReservationV2Response response);
}
