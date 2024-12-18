package com.mgm.services.booking.room.model.acrschargeandtax;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ACRSChargeDetail {
    private String propertyCode;
    private List<ChargeDetails> charges;

    public List<ChargeDetails> getCharges() {
        if (charges == null) {
            charges = new ArrayList<ChargeDetails>();
        }
        return charges;
    }
}
