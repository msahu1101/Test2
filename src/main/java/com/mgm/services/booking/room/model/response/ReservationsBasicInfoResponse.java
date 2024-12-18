package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;
/**
 * Response object for party and share with reservations API.
 * 
 * @author jayveera
 *
 */
public @Data class ReservationsBasicInfoResponse {

    private List<ReservationBasicInfo> reservationAdditionalInfo;

    public ReservationsBasicInfoResponse() {}
    
    public ReservationsBasicInfoResponse(List<ReservationBasicInfo> reservationAdditionalInfo) {
        this.reservationAdditionalInfo = reservationAdditionalInfo;
    }

}
