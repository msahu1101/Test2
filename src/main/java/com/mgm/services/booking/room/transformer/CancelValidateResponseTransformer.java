package com.mgm.services.booking.room.transformer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CancelValidateResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.ReservationUtil;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create and return CancelValidateResponse object from
 * RoomReservation object.
 */
@UtilityClass
public final class CancelValidateResponseTransformer {

    /**
     * Construct the validate response to cancel room reservation. Reservation
     * cannot be cancelled if check-in day is current day or already passed or
     * the reservation is already cancelled. If the reservation is in forfeit
     * window, get the forfeit amount along with totals.
     * 
     * @param reservation
     *            Room Reservation object
     * @param appProps
     *            Application properties
     * @return Returns cancel validate response
     */
    public static CancelValidateResponse getCancelValidateResponse(RoomReservation reservation, ApplicationProperties appProps) {

        RoomReservationResponse roomReservation = RoomReservationTransformer.transform(reservation, appProps);
        final String timezone = appProps.getTimezone(reservation.getPropertyId());
        ZoneId propertyZone = ZoneId.of(timezone);
        LocalDate propertyDate = LocalDate.now(propertyZone);
        LocalDate checkInDate = reservation.getCheckInDate().toInstant().atZone(propertyZone).toLocalDate();

        CancelValidateResponse validateResponse = new CancelValidateResponse();
        // Reservation cannot be cancelled if check-in date is same day or
        // already passed or already cancelled or num rooms is greater than 1
        validateResponse.setCancelable(true);
        if (checkInDate.isEqual(propertyDate) || checkInDate.isBefore(propertyDate)
                || reservation.getState().equals(ReservationState.Cancelled) || reservation.getNumRooms() > 1) {
            validateResponse.setCancelable(false);
        }

        // Find if the reservation is within forfeit period        
        LocalDateTime forfeitDate = reservation.getDepositCalc().getForfeitDate().toInstant().atZone(propertyZone)
                .toLocalDateTime();
        if (forfeitDate.isBefore(LocalDateTime.now(propertyZone))) {
            validateResponse.setForfeit(true);
            // Set forfeit amount when within forfeit period
            validateResponse.setForfeitAmount(reservation.getDepositCalc().getForfeitAmount());
        }
        // Set refund and total amounts
        validateResponse.setReservationTotal(roomReservation.getRates().getReservationTotal());
        validateResponse
                .setRefundAmount(ReservationUtil.getAmountPaidAgainstDeposit(reservation) - validateResponse.getForfeitAmount());
        // I'm not sure if validateResponse.setDepositAmount should be equal to the calculated deposit amount or the amount previously paid
        // against the deposit. Most of the time these 2 numbers will be the same.
        validateResponse.setDepositAmount(ReservationUtil.getAmountPaidAgainstDeposit(reservation));

        return validateResponse;
    }
}
