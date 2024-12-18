package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class Deposit implements Serializable {

    private static final long serialVersionUID = -4335643372505667465L;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date dueDate;
    private String amountPolicy;
    private double amount;
    
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date forfeitDate;
    private double forfeitAmount;
    private double overrideAmount;
    private String depositRuleCode;
    private String cancellationRuleCode;
    private Double refundAmount;
    private Double calculatedForefeitAmount;
    private List<ItemizedChargeItem> itemized;
}
