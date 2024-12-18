package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

public  @Data
class  EcidbyEmrRequest extends BaseRequest {
    private String emr;
}
