package com.mgm.services.booking.room.model.response;

import lombok.Data;

import java.util.List;

@Data
public class PackageComponentResponseV1 {
    private PkgDateDTOV1 date;
    private List<PropertyPackageComponentResponse> pkgComponent;
}
