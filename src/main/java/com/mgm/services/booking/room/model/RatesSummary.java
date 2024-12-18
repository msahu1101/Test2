package com.mgm.services.booking.room.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class RatesSummary {

    private List<PriceItemized> itemized;
    private double roomSubtotal;
    private double programDiscount;
    private double discountedSubtotal;
    private Double discountedAveragePrice;
    private double roomRequestsTotal;
    private double adjustedRoomSubtotal;
    private double resortFee;
    private double resortFeePerNight;
    private Double tripSubtotal;
    private double resortFeeAndTax;
    private double roomChargeTax;
    private double occupancyFee;
    private double tourismFee;
    private double tourismFeeAndTax;
    private double casinoSurcharge;
    private double casinoSurchargeAndTax;
    private double reservationTotal;
    private double depositDue;
    private double balanceUponCheckIn;
    private Double previousDeposit;
    private Double changeInDeposit;

    public Double getChangeInDeposit() {
        return null != previousDeposit ? BigDecimal.valueOf(depositDue - previousDeposit).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() : null;
    }
    
    public double getDiscountedSubtotal() {
        discountedSubtotal = roomSubtotal - programDiscount;
        return BigDecimal.valueOf(discountedSubtotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public double getAdjustedRoomSubtotal() {
        adjustedRoomSubtotal = roomSubtotal - programDiscount + roomRequestsTotal;
        return BigDecimal.valueOf(adjustedRoomSubtotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public double getReservationTotal() {
        reservationTotal = adjustedRoomSubtotal + resortFeeAndTax + roomChargeTax + occupancyFee + tourismFeeAndTax + casinoSurchargeAndTax;
        return BigDecimal.valueOf(reservationTotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public double getBalanceUponCheckIn() {
        balanceUponCheckIn = BigDecimal.valueOf(reservationTotal - depositDue).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        return balanceUponCheckIn;
    }

    /**
     * @return the itemized
     */
    public List<PriceItemized> getItemized() {
        return itemized;
    }

    /**
     * @param itemized the itemized to set
     */
    public void setItemized(List<PriceItemized> itemized) {
        this.itemized = itemized;
    }

    /**
     * @return the roomSubtotal
     */
    public double getRoomSubtotal() {
        return roomSubtotal;
    }

    /**
     * @param roomSubtotal the roomSubtotal to set
     */
    public void setRoomSubtotal(double roomSubtotal) {
        this.roomSubtotal = BigDecimal.valueOf(roomSubtotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the programDiscount
     */
    public double getProgramDiscount() {
        return programDiscount;
    }

    /**
     * @param programDiscount the programDiscount to set
     */
    public void setProgramDiscount(double programDiscount) {
        this.programDiscount = BigDecimal.valueOf(programDiscount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the roomRequestsTotal
     */
    public double getRoomRequestsTotal() {
        return roomRequestsTotal;
    }

    /**
     * @param roomRequestsTotal the roomRequestsTotal to set
     */
    public void setRoomRequestsTotal(double roomRequestsTotal) {
        this.roomRequestsTotal = BigDecimal.valueOf(roomRequestsTotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the resortFee
     */
    public double getResortFee() {
        return resortFee;
    }

    /**
     * @param resortFee the resortFee to set
     */
    public void setResortFee(double resortFee) {
        this.resortFee = BigDecimal.valueOf(resortFee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the resortFeePerNight
     */
    public double getResortFeePerNight() {
        return resortFeePerNight;
    }

    /**
     * @param resortFeePerNight the resortFeePerNight to set
     */
    public void setResortFeePerNight(double resortFeePerNight) {
        this.resortFeePerNight = BigDecimal.valueOf(resortFeePerNight).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the resortFeeAndTax
     */
    public double getResortFeeAndTax() {
        return resortFeeAndTax;
    }

    /**
     * @param resortFeeAndTax the resortFeeAndTax to set
     */
    public void setResortFeeAndTax(double resortFeeAndTax) {
        this.resortFeeAndTax = BigDecimal.valueOf(resortFeeAndTax).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the roomChargeTax
     */
    public double getRoomChargeTax() {
        return roomChargeTax;
    }

    /**
     * @param roomChargeTax the roomChargeTax to set
     */
    public void setRoomChargeTax(double roomChargeTax) {
        this.roomChargeTax = BigDecimal.valueOf(roomChargeTax).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the occupancyFee
     */
    public double getOccupancyFee() {
        return occupancyFee;
    }

    /**
     * @param occupancyFee the occupancyFee to set
     */
    public void setOccupancyFee(double occupancyFee) {
        this.occupancyFee = BigDecimal.valueOf(occupancyFee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the tourismFee
     */
    public double getTourismFee() {
        return tourismFee;
    }

    /**
     * @param tourismFee the tourismFee to set
     */
    public void setTourismFee(double tourismFee) {
        this.tourismFee = BigDecimal.valueOf(tourismFee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     *
     * @return the CasinoSurcharge
     */
    public double getCasinoSurcharge() { return casinoSurcharge; }

    /**
     * @param casinoSurcharge the CasinoSurcharge to set
     */
    public void setCasinoSurcharge(double casinoSurcharge) {
        this.casinoSurcharge = BigDecimal.valueOf(casinoSurcharge).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the tourismFeeAndTax
     */
    public double getTourismFeeAndTax() {
        return tourismFeeAndTax;
    }

    /**
     * @param tourismFeeAndTax the tourismFeeAndTax to set
     */
    public void setTourismFeeAndTax(double tourismFeeAndTax) {
        this.tourismFeeAndTax = BigDecimal.valueOf(tourismFeeAndTax).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the casinoSurchargeAndTax
     */
    public double getCasinoSurchargeAndTax() {
        return casinoSurchargeAndTax;
    }

    /**
     * @param casinoSurchargeAndTax the casinoSurchargeAndTax to set
     */
    public void setCasinoSurchargeAndTax(double casinoSurchargeAndTax) {
        this.casinoSurchargeAndTax = BigDecimal.valueOf(casinoSurchargeAndTax).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the depositDue
     */
    public double getDepositDue() {
        return BigDecimal.valueOf(depositDue).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param depositDue the depositDue to set
     */
    public void setDepositDue(double depositDue) {
        this.depositDue = BigDecimal.valueOf(depositDue).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return the previousDeposit
     */
    public Double getPreviousDeposit() {
        return previousDeposit;
    }

    /**
     * @return the tripSubtotal
     */
    public Double getTripSubtotal() {
        return tripSubtotal;
    }

    /**
     * @return the discountedAveragePrice
     */
    public Double getDiscountedAveragePrice() {
        return discountedAveragePrice;
    }

    /**
     * @param previousDeposit the previousDeposit to set
     */
    public void setPreviousDeposit(Double previousDeposit) {
        this.previousDeposit = BigDecimal.valueOf(previousDeposit).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param discountedSubtotal the discountedSubtotal to set
     */
    public void setDiscountedSubtotal(double discountedSubtotal) {
        this.discountedSubtotal = BigDecimal.valueOf(discountedSubtotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param adjustedRoomSubtotal the adjustedRoomSubtotal to set
     */
    public void setAdjustedRoomSubtotal(double adjustedRoomSubtotal) {
        this.adjustedRoomSubtotal = BigDecimal.valueOf(adjustedRoomSubtotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param reservationTotal the reservationTotal to set
     */
    public void setReservationTotal(double reservationTotal) {
        this.reservationTotal = BigDecimal.valueOf(reservationTotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param balanceUponCheckIn the balanceUponCheckIn to set
     */
    public void setBalanceUponCheckIn(double balanceUponCheckIn) {
        this.balanceUponCheckIn = BigDecimal.valueOf(balanceUponCheckIn).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param changeInDeposit the changeInDeposit to set
     */
    public void setChangeInDeposit(Double changeInDeposit) {
        this.changeInDeposit = BigDecimal.valueOf(changeInDeposit).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param tripSubtotal the tripSubtotal to set
     */
    public void setTripSubtotal(Double tripSubtotal) {
        this.tripSubtotal = BigDecimal.valueOf(tripSubtotal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param discountedAveragePrice the discountedAveragePrice to set
     */
    public void setDiscountedAveragePrice(Double discountedAveragePrice) {
        this.discountedAveragePrice = BigDecimal.valueOf(discountedAveragePrice).setScale(2, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }
}
