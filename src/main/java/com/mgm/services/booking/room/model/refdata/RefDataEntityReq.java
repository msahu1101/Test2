package com.mgm.services.booking.room.model.refdata;

import java.util.List;

import lombok.Data;
@Data
public class RefDataEntityReq {
String type;
List<String> ids;
List<String> codes;
String propertyId;

}
