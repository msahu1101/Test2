package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(
        callSuper = false)
@ToString(callSuper = true)
public @Data class RoomCartRequest extends BasePriceRequest {

    private static final long serialVersionUID = -2392216540107276710L;

    @DateTimeFormat(
            pattern = "M/d/yyyy")
    @JsonFormat(
            pattern = "M/d/yyyy")
    private LocalDate checkInDate;

    @DateTimeFormat(
            pattern = "M/d/yyyy")
    @JsonFormat(
            pattern = "M/d/yyyy")
    private LocalDate checkOutDate;

    private int numGuests = ServiceConstant.DEFAULT_GUESTS;
    private String roomTypeId;
    private List<String> auroraItineraryIds;

}
