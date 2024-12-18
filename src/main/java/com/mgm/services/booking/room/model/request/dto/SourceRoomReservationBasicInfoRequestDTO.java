package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

public @SuperBuilder @Getter @AllArgsConstructor @NoArgsConstructor class SourceRoomReservationBasicInfoRequestDTO {

    private String confirmationNumber;
    private String operaPartyCode;
    private String source;
    private long customerId;
    private String mlifeNumber;
    private RoomReservation fetchedRoomReservation;

    public Optional<RoomReservation> getFetchedRoomReservation() {
        return Optional.ofNullable(fetchedRoomReservation);
    }
}
