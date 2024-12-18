package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mgm.services.booking.room.util.PartnerProgramConfig;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public  @Data class PartnerConfigResponse {

    private List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues;


}
