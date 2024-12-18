package com.mgm.services.booking.room.model.acrschargeandtax;

import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Data
public class TaxDetails {
    private RoomChargeItemType taxType;
    private String taxCodes;
    public List<String> getTaxCodes(){
        if(StringUtils.isNotEmpty(this.taxCodes)) {
            return Arrays.asList(this.taxCodes.split(","));
        }else {
            return Arrays.asList("");
        }
    }
}
