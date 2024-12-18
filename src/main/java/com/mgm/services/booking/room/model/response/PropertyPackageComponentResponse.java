package com.mgm.services.booking.room.model.response;


import com.mgm.services.booking.room.model.RoomComponent;
import lombok.Data;

import java.util.List;

@Data
public class PropertyPackageComponentResponse {
    //PropertyPackageComponent
    private String propertyId;
    private List<RoomComponent> pkgComponents;
}
