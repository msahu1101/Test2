/**
 * This will be used in the CreatePartyRoomReservationResponse.
 * It contains the details about the failed reservation with in the party reservation.
 */
package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class FailedReservation {

    @JsonProperty("code")
    private String errorCode;
    private String description;
    private String extInfo;
}
