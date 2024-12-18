package com.mgm.services.booking.room.model.refdata;

import java.util.List;

import lombok.Data;
@Data
public class RefDataEntityRes {
    String type;
    String propertyId;
    List<RefDataEntity> elements;
}
