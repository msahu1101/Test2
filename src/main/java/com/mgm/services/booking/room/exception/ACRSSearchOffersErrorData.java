package com.mgm.services.booking.room.exception;

import com.mgm.services.booking.room.model.crs.searchoffers.Denial;
import lombok.Data;

import java.util.List;

@Data
public class ACRSSearchOffersErrorData {
    private List<Denial> denials;
}
