package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import lombok.Data;

import java.util.List;
@Data
public class ResortPriceWithTaxDTO extends ResortPriceV2Request {
    List<RateOrGroupDTO> rates;
    List<RateOrGroupDTO> groups;
    List<MultiDateDTO> dates;
}
