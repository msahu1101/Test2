package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class RoomPrice implements Serializable {

    private static final long serialVersionUID = 5984039442463549246L;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date date;
    private double basePrice;
    private double customerPrice;
    private double price;
    @JsonProperty("isDiscounted")
    private Boolean discounted;
    private boolean programIdIsRateTable;
    private double overridePrice = -1.0;
    private boolean overrideProgramIdIsRateTable;
    @JsonProperty("isComp")
    private boolean comp;
    private boolean resortFeeIsSpecified;
    private double resortFee;
    private String programId;
    private String pricingRuleId;
    
    //Attributes for V2 APIs
    private String overrideProgramId;
    private String overridePricingRuleId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RoomPrice roomPrice = (RoomPrice) o;
        return Objects.equals(date, roomPrice.date) && basePrice == roomPrice.basePrice
                && customerPrice == roomPrice.customerPrice && price == roomPrice.price
                && Objects.equals(discounted, roomPrice.discounted) && programIdIsRateTable == roomPrice.programIdIsRateTable
                && overridePrice == roomPrice.overridePrice
                && overrideProgramIdIsRateTable == roomPrice.overrideProgramIdIsRateTable && comp == roomPrice.comp
                && resortFeeIsSpecified == roomPrice.resortFeeIsSpecified && resortFee == roomPrice.resortFee
                && Objects.equals(programId, roomPrice.programId) && Objects.equals(pricingRuleId, roomPrice.pricingRuleId)
                && Objects.equals(overrideProgramId, roomPrice.overrideProgramId)
                && Objects.equals(overridePricingRuleId, roomPrice.overridePricingRuleId);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(date, basePrice, customerPrice, price, discounted, programIdIsRateTable, overridePrice,
                overrideProgramIdIsRateTable, comp, resortFeeIsSpecified, resortFee, programId, pricingRuleId,
                overrideProgramId, overridePricingRuleId);
    }
}
