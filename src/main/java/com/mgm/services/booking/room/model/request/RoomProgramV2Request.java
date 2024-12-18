package com.mgm.services.booking.room.model.request;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.annotations.ValidateDateFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=true)
public @Data class RoomProgramV2Request extends BaseRequest {

    @NotNull(message = "_invalid_program_id")
    private List<String> programIds;

    @ValidateDateFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT, errorMessage = "_invalid_start_date_")
    private String startDate;

    @ValidateDateFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT, errorMessage = "_invalid_end_date_")
    private String endDate;

    private boolean promoSearch;
}
