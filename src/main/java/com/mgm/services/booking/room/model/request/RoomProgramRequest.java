package com.mgm.services.booking.room.model.request;

import java.util.ArrayList;
import java.util.List;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=true)
public @Data class RoomProgramRequest extends BaseRequest {

    private List<String> propertyIds = new ArrayList<>();

}
