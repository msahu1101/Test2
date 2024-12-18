package com.mgm.services.booking.room.model.request;

import java.util.List;

import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class RoomCartUpdateRequest extends BaseRequest {

    private List<RoomRequest> roomRequests;
    private String specialRequests;

}
