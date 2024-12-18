package com.mgm.services.booking.room.model.request;

import java.util.ArrayList;
import java.util.List;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class PreModifyRequest extends BaseRequest {

    private String confirmationNumber;
    private String firstName;
    private String lastName;
    private TripDetail tripDetails;
    private List<RoomRequest> roomRequests = new ArrayList<RoomRequest>();
}
