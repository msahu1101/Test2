package com.mgm.services.booking.room.model.request;

import java.util.ArrayList;
import java.util.List;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class PreModifyV2Request extends BaseRequest {

    private String confirmationNumber;
    private TripDetailsRequest tripDetails;
    private String propertyId;
    private List<String> roomRequests = new ArrayList<>();
    private String firstName;
    private String lastName;
    private RoomReservation findResvResponse;
}
