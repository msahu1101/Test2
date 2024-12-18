package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.request.dto.MultiDateDTO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MultiDateResortPrice {
    private MultiDateDTO date;
    private List<ResortPriceResponse> resortPrices;

}
