package com.mgm.services.booking.room.model.loyalty;

import com.mgm.services.booking.room.util.CommonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class UpdatedPromotion {

    private int patronId;
    private int promoId;
    private int siteId;
    private String propertyId;
    private int statusNo;
    private String sourceReservationNo;

}
