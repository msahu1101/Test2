package com.mgm.services.booking.room.model.request;

import com.mgm.services.booking.room.model.request.dto.PackageComponentsRequestDTO;
import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

@Data
public class PackageComponentRequest extends BaseRequest {

    private PackageComponentsRequestDTO request;

}
