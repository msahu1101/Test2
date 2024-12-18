package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class Payment implements Serializable {

    private static final long serialVersionUID = -1401787398019198611L;
    private int reservationInstance;
    private double chargeAmount;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date chargeCardExpiry;
    private boolean dccChecked;
    private boolean dccEligible;
    private Date dccTransDate;
    private double dccAmount;
    private double dccRate;
    private double dccSettleAmount;
    private String status;
    private boolean isDeposit;
    private boolean isExternal;
    private String chargeCardHolder;
    private String chargeCurrencyCode;
    private String chargeCardType;
    private String chargeCardMaskedNumber;
    private String chargeCardNumber;
    private String dccCurrencyCode;
    private String dccAcceptMessage;
    private String dccAuthApprovalCode;
    private String dccSettleReference;
    private String transactionId;
    private String paymentTxnId;
    @JsonGetter("fxChecked")
    public boolean isDccChecked() {
        return dccChecked;
    }

    @JsonSetter("dccChecked")
    public void setDccChecked(boolean dccChecked) {
        this.dccChecked = dccChecked;
    }

    @JsonGetter("fxEligible")
    public boolean isDccEligible() {
        return dccEligible;
    }

    @JsonSetter("dccEligible")
    public void setDccEligible(boolean dccEligible) {
        this.dccEligible = dccEligible;
    }

    @JsonGetter("fxTransDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    public Date getDccTransDate() {
        if (dccTransDate != null) {
            return (Date) dccTransDate.clone();
        }
        return null;
    }

    @JsonSetter("dccTransDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    public void setDccTransDate(Date dccTransDate) {
        if(null != dccTransDate) {
            this.dccTransDate = (Date) dccTransDate.clone();
        }
    }

    @JsonGetter("fxAmount")
    public double getDccAmount() {
        return dccAmount;
    }

    @JsonSetter("dccAmount")
    public void setDccAmount(double dccAmount) {
        this.dccAmount = dccAmount;
    }

    @JsonGetter("fxRate")
    public double getDccRate() {
        return dccRate;
    }

    @JsonSetter("dccRate")
    public void setDccRate(double dccRate) {
        this.dccRate = dccRate;
    }

    @JsonGetter("fxSettleAmount")
    public double getDccSettleAmount() {
        return dccSettleAmount;
    }

    @JsonSetter("dccSettleAmount")
    public void setDccSettleAmount(double dccSettleAmount) {
        this.dccSettleAmount = dccSettleAmount;
    }

    @JsonGetter("isDeposit")
    public boolean isDeposit() {
        return isDeposit;
    }

    @JsonSetter("isDeposit")
    public void setDeposit(boolean isDeposit) {
        this.isDeposit = isDeposit;
    }

    @JsonGetter("isExternal")
    public boolean isExternal() {
        return isExternal;
    }

    @JsonSetter("isExternal")
    public void setExternal(boolean isExternal) {
        this.isExternal = isExternal;
    }

    @JsonGetter("fxCurrencyCode")
    public String getDccCurrencyCode() {
        return this.dccCurrencyCode;
    }

    @JsonSetter("dccCurrencyCode")
    public void setDccCurrencyCode(String dccCurrencyCode) {
        this.dccCurrencyCode = dccCurrencyCode;
    }

    @JsonGetter("fxAcceptMessage")
    public String getDccAcceptMessage() {
        return dccAcceptMessage;
    }

    @JsonSetter("dccAcceptMessage")
    public void setDccAcceptMessage(String dccAcceptMessage) {
        this.dccAcceptMessage = dccAcceptMessage;
    }

    @JsonGetter("fxAuthApprovalCode")
    public String getDccAuthApprovalCode() {
        return dccAuthApprovalCode;
    }

    @JsonSetter("dccAuthApprovalCode")
    public void setDccAuthApprovalCode(String dccAuthApprovalCode) {
        this.dccAuthApprovalCode = dccAuthApprovalCode;
    }
    
    @JsonGetter("fxSettleReference")
    public String getDccSettleReference() {
        return dccSettleReference;
    }

    @JsonSetter("dcSettleReference")
    public void setDccSettleReference(String dccSettleReference) {
        this.dccSettleReference = dccSettleReference;
    }

}