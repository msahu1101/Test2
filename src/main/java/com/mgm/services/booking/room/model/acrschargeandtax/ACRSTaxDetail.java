package com.mgm.services.booking.room.model.acrschargeandtax;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ACRSTaxDetail {
    private String propertyCode;
    private List<TaxDetails> taxes;

    public List<TaxDetails> getTaxes() {
        if (taxes == null) {
            taxes = new ArrayList<TaxDetails>();
        }
        return taxes;
    }
}
