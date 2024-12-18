package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.booking.room.model.response.PkgDateDTOV1;
import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

import java.util.List;

@Data
public class PackageComponentsRequestDTOV1 extends BaseRequest {
    private List<PkgDateDTOV1> dates;
    private int numAdults;
    private List<PkgDataDTO> data;
}
