package com.mgm.services.booking.room.model.response;

import lombok.Data;

import java.util.List;

@Data
public class PackageComponentResponse {  //PropertyPackageComponent
    private String propertyId;
    private String programId;
    private String roomTypeId;
    private List<PkgComponent> showComponents;
    private List<PkgComponent> inclusionComponents;
}
