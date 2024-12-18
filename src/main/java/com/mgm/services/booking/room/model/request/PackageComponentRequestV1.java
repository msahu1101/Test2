package com.mgm.services.booking.room.model.request;

import com.mgm.services.booking.room.model.request.dto.PackageComponentsRequestDTOV1;
import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

@Data
public class PackageComponentRequestV1 extends BaseRequest {
    private PackageComponentsRequestDTOV1 request;
}
