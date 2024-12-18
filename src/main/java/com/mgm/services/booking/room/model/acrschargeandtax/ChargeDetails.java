package com.mgm.services.booking.room.model.acrschargeandtax;

import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Data
public class ChargeDetails {
    private RoomChargeItemType chargeType;
    private String chargeCodes;
    public List<String> getChargeCodes(){
        if(StringUtils.isNotEmpty(this.chargeCodes)) {
            return Arrays.asList(this.chargeCodes.split(","));
        }else {
            return Arrays.asList("");
        }
    }
}
