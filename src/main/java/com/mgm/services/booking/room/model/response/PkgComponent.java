package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.RoomComponent;
import lombok.Data;

import java.time.LocalDate;


@Data
public class PkgComponent extends RoomComponent {
    private String code;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate start;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate end;
    public PkgComponent(String code,LocalDate start,LocalDate end,String id, boolean nightlyCharge, double price, String description, String pricingApplied,
                        Float taxRate, String shortDescription, String longDescription, String ratePlanName, String ratePlanCode, double amtAftTax){
        super(id,nightlyCharge,price,description,pricingApplied,taxRate,shortDescription,longDescription,ratePlanName,ratePlanCode,amtAftTax);
        this.code = code;
        this.start = start;
        this.end = end;
    }
}
