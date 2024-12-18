package com.mgm.services.booking.room.model.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mgm.services.booking.room.model.FailureReason;
import com.mgm.services.booking.room.model.reservation.RoomRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)

public @Data class RoomReservationResponse extends BaseReservationResponse {

    private String roomTypeId;
    private String programId;
    private List<RoomRequest> roomRequests;
    private boolean cancellationPenaltyApplies;

    @Getter(onMethod = @__(@JsonIgnore))
    @Setter
    private FailureReason failureReason;

}
