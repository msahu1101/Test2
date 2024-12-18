package com.mgm.services.booking.room.model.request;

import java.util.Date;
import java.util.List;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public @Data class RoomComponentRequest extends BaseRequest {

    private String propertyId;
    private String roomTypeId;
    private String programId;
    private List<String> componentIds;
    private Date travelStartDate;
    private Date travelEndDate;
}
