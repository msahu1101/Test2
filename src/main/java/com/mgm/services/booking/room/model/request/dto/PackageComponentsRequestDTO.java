package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

import java.util.List;

@Data
public class PackageComponentsRequestDTO extends BaseRequest {

    private int numAdults;
    private List<PkgRequestDTO> data;
}
