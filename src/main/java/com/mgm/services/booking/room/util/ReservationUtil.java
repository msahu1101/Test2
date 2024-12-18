package com.mgm.services.booking.room.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.mgm.services.booking.room.config.CustomDataDeserializer;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.*;
import com.mgm.services.booking.room.model.acrschargeandtax.ChargeDetails;
import com.mgm.services.booking.room.model.acrschargeandtax.TaxCodeExceptionDetails;
import com.mgm.services.booking.room.model.acrschargeandtax.TaxDetails;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.inventory.*;
import com.mgm.services.booking.room.model.paymentservice.*;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV3Request;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.authorization.BookingType;
import com.mgm.services.common.util.BaseCommonUtil;
import com.mgm.services.common.util.DateUtil;
import com.mgm.services.common.util.ValidationUtil;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mgm.services.booking.room.util.CommonUtil.localDateToDate;

/**
 * Final class to define common utility methods around reservations.
 * 
 */
@Log4j2
public final class ReservationUtil {
	
	private ReservationUtil() {
        // Hiding implicit constructor
    }

    public static long getTimeDiffInSec(String date1, String date2){
        LocalDateTime ldt1 = LocalDateTime.parse(date1);
        LocalDateTime ldt2 = LocalDateTime.parse(date2);
        long milli1 = ldt1.atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE)).toInstant().toEpochMilli();
        long milli2 = ldt2.atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE)).toInstant().toEpochMilli();
        long milliDiff =  milli1-milli2;
        if(0 > milliDiff ){
            milliDiff = milliDiff*(-1);
        }
        return milliDiff/1000;
    }


    /**
     * This method return the amount that the customer will forfeit if the
     * customer cancels the reservation
     * 
     * @param reservation
     *            the reservation obj
     * @param appProperties
     *            the application properties
     * @return the forfeit amount
     */
    public static double getForfeitAmount(RoomReservation reservation, ApplicationProperties appProperties) {
        double forfeitAmount = ServiceConstant.ZERO_DOUBLE_VALUE;
        // Find if the reservation is within forfeit period
        if (null != reservation.getDepositCalc() && null != reservation.getDepositCalc().getForfeitDate()) {
            ZoneId propertyZone = ZoneId.of(appProperties.getTimezone(reservation.getPropertyId()));
            LocalDateTime propertyDate = LocalDateTime.now(propertyZone);
            LocalDateTime forfeitDate = reservation.getDepositCalc().getForfeitDate().toInstant().atZone(propertyZone)
                    .toLocalDateTime();
            if (forfeitDate.isBefore(propertyDate)) {
                // Set forfeit amount when within forfeit period
                forfeitAmount = reservation.getDepositCalc().getForfeitAmount();
            }
        }
        return forfeitAmount;
    }

    /**
     * This method checks if the reservation is within forfeit window
     * 
     * @param reservation
     *            the reservation obj
     * @param appProperties
     *            the application properties
     * @return Returns true if the reservation is within forfeit window
     */
    public static boolean isForfeit(RoomReservation reservation, ApplicationProperties appProperties) {
        ZoneId propertyZone = ZoneId.of(appProperties.getTimezone(reservation.getPropertyId()));
        LocalDateTime propertyDate = LocalDateTime.now(propertyZone);
        // Find if the reservation is within forfeit period
        if(reservation.getDepositCalc()!=null && reservation.getDepositCalc().getForfeitDate() != null) {
            LocalDateTime forfeitDate = reservation.getDepositCalc().getForfeitDate().toInstant().atZone(propertyZone)
                    .toLocalDateTime();
            return forfeitDate.isBefore(propertyDate);
        }
        return false;
    }
    
    /**
     * This method checks if the property ownership is transferred based on a handover date and reservation check in date.
     * @param reservation
     * @param appProperties
     * @return true if property's local date is before handover date or if check in date is before hand over date.
     */
    
    public static boolean returnReservationForTransferredProperty(RoomReservation reservation, Date handOverDate, ApplicationProperties appProperties) {
    	ZoneId propertyZone = ZoneId.of(appProperties.getTimezone(reservation.getPropertyId()));
    	LocalDate propertyDate = LocalDate.now(propertyZone);
    	LocalDate propertyHandOverDate = handOverDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    	if(propertyDate.isBefore(propertyHandOverDate)) {
    		return true;
    	}
    	LocalDate checkInDate = reservation.getCheckInDate().toInstant().atZone(propertyZone).toLocalDate();
    	return checkInDate.isBefore(propertyHandOverDate);
    }

    /**
     * This method returns the refund amount for the customer
     * 
     * @param reservation
     *            the reservation obj
     * @param appProperties
     *            the application properties
     * @return the forfeit amount
     */
    public static double getRefundAmount(RoomReservation reservation, ApplicationProperties appProperties) {

        double refundAmount = ServiceConstant.ZERO_DOUBLE_VALUE;
        if (null != reservation.getDepositCalc()) {
            refundAmount = reservation.getAmountDue() - getForfeitAmount(reservation, appProperties);
        }

        return refundAmount;
    }

    public static double getRefundAmount(RoomReservation reservation,boolean overideForfeit, ApplicationProperties appProperties) {

        double refundAmount = ServiceConstant.ZERO_DOUBLE_VALUE;
        if (null != reservation.getDepositCalc()) {
            if(overideForfeit){
                refundAmount = reservation.getAmountDue();
            }else{
                refundAmount = reservation.getAmountDue() - getForfeitAmount(reservation, appProperties);
            }
        }
        return refundAmount;
    }
    
    /**
     * This method returns the deposit amount for the reservation
     * @param reservation
     * @return
     */
    public static double getDepositAmount(RoomReservation reservation) {

        double depositAmount = ServiceConstant.ZERO_DOUBLE_VALUE;
        if (null != reservation.getDepositCalc()) {
            depositAmount = reservation.getDepositCalc()
                    .getOverrideAmount() >= 0 ? reservation.getDepositCalc()
                            .getOverrideAmount()
                            : reservation.getDepositCalc()
                                    .getAmount();
        }

        return depositAmount;
    }

    /**
     * This method returns the amount paid against a deposit on a booked reservation
     * @param reservation
     * @return sum of payment charge amounts on reservation where `isDeposit==true`
     */
    public static double getAmountPaidAgainstDeposit(RoomReservation reservation) {
        double amountPaid = 0.0;

        if (reservation.getState() == ReservationState.Booked) {
            if (null != reservation.getPayments()){
                amountPaid = reservation.getPayments().stream()
                        .filter(p-> ServiceConstant.PAYMENT_STATUS_SETTLED.equalsIgnoreCase(p.getStatus()))
                        .mapToDouble(Payment::getChargeAmount)
                        .sum();
            }
        }

        return amountPaid;
    }

    /**
     * Prepares the cancelFinancialImpact from the reservation response.
     * 
     * @param reservation reservation object.
     * @param response reservation response.
     * @param appProperties properties store.
     * @return String - json notion of CancellationFinancialImpact.
     */
    public static String getCancelFinancialImpact(RoomReservation reservation, RoomReservationResponse response,
            ApplicationProperties appProperties) {
        CancellationFinancialImpact financialImpact = new CancellationFinancialImpact();
        financialImpact.setProductType(BookingType.ROOM.getValue());
        financialImpact.setOperation(response.getState().toString());
        financialImpact.setTotalTransactionAmount(response.getRates().getReservationTotal());
        financialImpact.setConfirmationNumber(response.getConfirmationNumber());
        financialImpact.getRoomCancellation().setAmountForfeit(CommonUtil.round(
                ReservationUtil.getForfeitAmount(reservation, appProperties), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomCancellation().setAmountRefund(CommonUtil
                .round(ReservationUtil.getRefundAmount(reservation, appProperties), ServiceConstant.DEFAULT_PRECISION));
        return CommonUtil.objectToJson(financialImpact);
    }

    /**
     * Returns the ReservationFinancialImpact, which is prepared using reservation response object.
     * 
     * @param response reservation response.
     * @return - Json notion of ReservationFinancialImpact.
     */
    public static String getReservationFinancialImpact(BaseReservationResponse response) {
        ReservationFinancialImpact financialImpact = new ReservationFinancialImpact();
        financialImpact.setProductType(BookingType.ROOM.getValue());
        financialImpact.setOperation(response.getState().toString());
        financialImpact.setTotalTransactionAmount(
                CommonUtil.round(response.getRates().getReservationTotal(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.setAmountCharged(
                CommonUtil.round(response.getPayment().getChargeAmount(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.setBalanceDue(
                CommonUtil.round(response.getRates().getBalanceUponCheckIn(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.setConfirmationNumber(response.getConfirmationNumber());
        financialImpact.getRoomBookingItemized().setResortFeeTax(
                CommonUtil.round(response.getRates().getResortFeeAndTax(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomBookingItemized().setRoomSubtotal(
                CommonUtil.round(response.getRates().getAdjustedRoomSubtotal(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomBookingItemized()
                .setTaxes(CommonUtil.round(response.getRates().getRoomChargeTax(), ServiceConstant.DEFAULT_PRECISION));
        return CommonUtil.objectToJson(financialImpact);
    }
    
    
    public static String getReservationFinancialImpact(RoomReservation reservation) {
    	
        RatesSummary rateSummary = RoomReservationTransformer.transform(reservation);
                       
        double amountPaid = ServiceConstant.ZERO_DOUBLE_VALUE;
        if (null != reservation.getPayments()) {
            for (Payment charge : reservation.getPayments()) {
                if (charge.isDeposit()) {
                    amountPaid += charge.getChargeAmount();
                }
            }
        }
        
        ReservationFinancialImpact financialImpact = new ReservationFinancialImpact();
        
        financialImpact.setProductType(BookingType.ROOM.getValue());
        financialImpact.setOperation(reservation.getState().toString());
        financialImpact.setConfirmationNumber(reservation.getConfirmationNumber());

        financialImpact.setTotalTransactionAmount(
                CommonUtil.round(rateSummary.getReservationTotal(), ServiceConstant.DEFAULT_PRECISION));

        financialImpact.setAmountCharged(
                CommonUtil.round(amountPaid, ServiceConstant.DEFAULT_PRECISION));
        financialImpact.setBalanceDue(
                CommonUtil.round(rateSummary.getBalanceUponCheckIn(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomBookingItemized().setResortFeeTax(
                CommonUtil.round(rateSummary.getResortFeeAndTax(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomBookingItemized().setRoomSubtotal(
                CommonUtil.round(rateSummary.getAdjustedRoomSubtotal(), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomBookingItemized()
                .setTaxes(CommonUtil.round(rateSummary.getRoomChargeTax(), ServiceConstant.DEFAULT_PRECISION));

        return CommonUtil.objectToJson(financialImpact);

   	
    }
    
    public static String getCancelFinancialImpact(RoomReservation reservation,
            ApplicationProperties appProperties) {
        CancellationFinancialImpact financialImpact = new CancellationFinancialImpact();
        
        RatesSummary rateSummary = RoomReservationTransformer.transform(reservation);
        
        financialImpact.setProductType(BookingType.ROOM.getValue());
        financialImpact.setOperation(reservation.getState().toString());
        financialImpact.setTotalTransactionAmount(rateSummary.getReservationTotal());
        financialImpact.setConfirmationNumber(reservation.getConfirmationNumber());
        financialImpact.getRoomCancellation().setAmountForfeit(CommonUtil.round(
                ReservationUtil.getForfeitAmount(reservation, appProperties), ServiceConstant.DEFAULT_PRECISION));
        financialImpact.getRoomCancellation().setAmountRefund(CommonUtil
                .round(ReservationUtil.getRefundAmount(reservation, appProperties), ServiceConstant.DEFAULT_PRECISION));
        return CommonUtil.objectToJson(financialImpact);
    }
    
    public static Date convertLocalDateToDate(java.time.LocalDate localDate) {
        return java.util.Date.from(localDate.atStartOfDay(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE)).toInstant());
    }

    public static LocalDate convertDateToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE))
                .toLocalDate();
    }
    
    public static LocalDate convertDateToLocalDateAtSystemDefault(Date date) {
    	return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        return convertDateToLocalDateTime(date, ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE));
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date, ZoneId zoneId) {
        return date.toInstant()
                .atZone(zoneId)
                .toLocalDateTime();
    }

    public static boolean areDatesEqualExcludingTime(Date firstDate, Date secondDate) {
        return (0 == getDateWithoutTime(firstDate).compareTo(getDateWithoutTime(secondDate)));
    }

    public static boolean isFirstDateBeforeSecondDateExcludingTime(Date firstDate, Date secondDate) {
        return getDateWithoutTime(firstDate).before(getDateWithoutTime(secondDate));
    }

    public static boolean isFirstDateAfterSecondDateExcludingTime(Date firstDate, Date secondDate) {
        return getDateWithoutTime(firstDate).after(getDateWithoutTime(secondDate));
    }

    public static boolean isOverlappingDatesOnModification(Date newCheckIn, Date newCheckOut, Date originalCheckIn, Date originalCheckOut) {
        return isFirstDateBeforeSecondDateExcludingTime(newCheckIn, originalCheckOut) && isFirstDateAfterSecondDateExcludingTime(newCheckOut, originalCheckIn);
    }
    public static RoomPrice createRoomPrice(LocalDate date, AuroraPriceResponse acrsPricingResponse) {
        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setDate(convertLocalDateToDate(date));
        roomPrice.setProgramId(acrsPricingResponse.getProgramId());
        roomPrice.setBasePrice(acrsPricingResponse.getBasePrice());
        roomPrice.setPrice(acrsPricingResponse.getDiscountedPrice());
        roomPrice.setComp(acrsPricingResponse.isComp());
        return roomPrice;
    }

    public static Date getDateWithoutTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * @param reservation
     * @return
     */
    public static RatesSummary getRateSummary(RoomReservation reservation) {
        RatesSummary rateSummary = new RatesSummary();

        double roomSubtotal = 0;
        double resortFee = 0;
        double roomDiscSubtotal = 0;
        double componentCharge = 0;
        double resortFeeTax = 0;
        double occupancyFee = 0;
        double tourismFee = 0;
        double casinoSurcharge = 0;

        // adding null check to support retrieval of 3rd party reservations
        if (null != reservation.getBookings()) {
            for (RoomPrice price : reservation.getBookings()) {
                roomSubtotal += price.getBasePrice();
                // CBSR-1567 Prioritizing Comp over override price based on PO modification observations.
                if(price.isComp()) {
                	roomDiscSubtotal += 0;
                }
                else if (price.getOverridePrice() >= 0) {
                    roomDiscSubtotal += price.getOverridePrice();
                } else {
                    roomDiscSubtotal += price.getPrice();
                }
            }
        }

        rateSummary.setRoomSubtotal(roomSubtotal);
        rateSummary.setProgramDiscount(roomSubtotal - roomDiscSubtotal);

        // adding null check to support retrieval of 3rd party reservations
        if (null != reservation.getChargesAndTaxesCalc() && reservation.getChargesAndTaxesCalc().getCharges() != null) {
            for (RoomChargeItem charges : reservation.getChargesAndTaxesCalc().getCharges()) {
                for (ItemizedChargeItem chargeItem : charges.getItemized()) {
                	final String item = chargeItem.getItem();
                    final RoomChargeItemType itemType = chargeItem.getItemType();
                    if (null != item && (item.equalsIgnoreCase(ServiceConstant.OCCUPANCY_FEE_ITEM) || ServiceConstant.ACRS_OCCUPANCY_FEE_ITEM.contains(item))) {
                        occupancyFee += chargeItem.getAmount();
                    } else if(null != item && (item.equalsIgnoreCase(ServiceConstant.TOURISM_FEE_ITEM) || ServiceConstant.ACRS_TOURISM_FEE_ITEM.contains(item))) {
                        tourismFee += chargeItem.getAmount();
                    } else if (null != item && (item.equalsIgnoreCase(ServiceConstant.CASINO_SURCHARGE_ITEM) || ServiceConstant.ACRS_CASINO_SURCHARGE_ITEM.contains(item))) {
                        casinoSurcharge += chargeItem.getAmount();
                    } else if (null != itemType && itemType.equals(RoomChargeItemType.ComponentCharge)) {
                        componentCharge += chargeItem.getAmount();
                    } else if (null != itemType && itemType.equals(RoomChargeItemType.ResortFee)) {
                        resortFee += chargeItem.getAmount();
                    }
                }
            }
        }

        rateSummary.setResortFee(resortFee);
        rateSummary.setRoomRequestsTotal(componentCharge);
        rateSummary.setAdjustedRoomSubtotal(
                rateSummary.getRoomSubtotal() - rateSummary.getProgramDiscount() + componentCharge);
        rateSummary.setOccupancyFee(occupancyFee);
        rateSummary.setTourismFee(tourismFee);
        rateSummary.setCasinoSurcharge(casinoSurcharge);

        setTaxes(reservation, rateSummary, resortFeeTax);

        if (reservation.getDepositCalc() != null) {
            rateSummary.setDepositDue(ReservationUtil.getDepositAmount(reservation));
        }
        
        // Calculate resort fee per night
        int nights = Math.toIntExact(
                ChronoUnit.DAYS.between(ReservationUtil.convertDateToLocalDate(reservation.getCheckInDate()),
                        ReservationUtil.convertDateToLocalDate(reservation.getCheckOutDate())));
        // To handle cabana booking
        nights = (nights == 0) ? 1 : nights;
        rateSummary.setResortFeePerNight(rateSummary.getResortFee() / nights);
        rateSummary.setTripSubtotal(rateSummary.getDiscountedSubtotal() + rateSummary.getResortFee());
        rateSummary.setDiscountedAveragePrice(rateSummary.getDiscountedSubtotal() / nights);
        return rateSummary;
    }

    private static void setTaxes(RoomReservation reservation, RatesSummary rateSummary, double resortFeeTax) {
        double roomChargeTax = 0;
        double tourismFeeAndTax = 0;
        double casinoSurchargeAndTax = 0;

        // Taxes
        // TODO We believe it is a bug that ACRS is responding with null, if
        // confirmed remove null check when bug is fixed
        // if not a bug then remove this note and leave nullcheck.
        // Note: adding null check to support retrieval of 3rd party reservations
        if (null != reservation.getChargesAndTaxesCalc()
                && null != reservation.getChargesAndTaxesCalc().getTaxesAndFees()) {
            for (RoomChargeItem charges : reservation.getChargesAndTaxesCalc().getTaxesAndFees()) {
                for (ItemizedChargeItem chargeItem : charges.getItemized()) {
                	final String item = chargeItem.getItem();
                    final RoomChargeItemType itemType = chargeItem.getItemType();
                    if (null != item && (item.equalsIgnoreCase(ServiceConstant.TOURISM_FEE_ITEM) || ServiceConstant.ACRS_TOURISM_FEE_ITEM.contains(item))) {
                        tourismFeeAndTax += chargeItem.getAmount();
                    } else if (null != item && (item.equalsIgnoreCase(ServiceConstant.CASINO_SURCHARGE_ITEM) || ServiceConstant.ACRS_CASINO_SURCHARGE_ITEM.contains(item))) {
                        casinoSurchargeAndTax += chargeItem.getAmount();
                    } else if (null != itemType && itemType.equals(RoomChargeItemType.ResortFeeTax)) {
                        resortFeeTax += chargeItem.getAmount();
                    } else if (null != itemType && itemType.equals(RoomChargeItemType.ExtraGuestChargeTax)) {
                        // do nothing
                        // Exclude ExtraGuestChargeTax as that is already part
                        // of the RoomChargeTax
                    } else {
                        roomChargeTax += chargeItem.getAmount();
                    }

                }
            }
        }

        rateSummary.setResortFeeAndTax(rateSummary.getResortFee() + resortFeeTax);
        rateSummary.setTourismFeeAndTax(rateSummary.getTourismFee() + tourismFeeAndTax);
        rateSummary.setCasinoSurchargeAndTax(rateSummary.getCasinoSurcharge() + casinoSurchargeAndTax);
        rateSummary.setRoomChargeTax(roomChargeTax);
    }
    
    /**
     * Adds special requests to include additional ones for occupancy tax and
     * tourism fee applicable to borgata property
     * 
     * @param reservation
     *            Room reservation object
     * @param appProps
     *            Application property configurations
     */
    public static void addSpecialRequests(RoomReservation reservation, ApplicationProperties appProps) {

        if (reservation.getPropertyId().equals(appProps.getBorgataPropertyId())) {

            List<String> specialRequests = reservation.getSpecialRequests();
            specialRequests.addAll(appProps.getBorgataSpecialRequests());
            
            // remove possible duplicates
            reservation.setSpecialRequests(specialRequests.stream().distinct().collect(Collectors.toList()));

        }
    }
    
    /**
     * Removes special requests related to occupancy tax and
     * tourism fee for borgata property from display
     * 
     * @param reservation
     *            Room reservation object
     * @param appProps
     *            Application property configurations
     */
    public static void removeSpecialRequests(RoomReservation reservation, ApplicationProperties appProps) {

        if (reservation.getPropertyId().equals(appProps.getBorgataPropertyId())) {

            List<String> specialRequests = reservation.getSpecialRequests();
            appProps.getBorgataSpecialRequests().forEach(specialRequests::remove);

        }
    }

    public static void sanitizedComments(RoomReservation reservation) {
        if (null != reservation && StringUtils.isNotEmpty(reservation.getComments())) {
            String sanitizedComments = reservation.getComments().replaceAll(ServiceConstant.OPERA_UNALLOWED_CHAR_REGEX,
                    ServiceConstant.WHITESPACE_STRING);
            sanitizedComments = EmojiParser.removeAllEmojis(sanitizedComments);
            reservation.setComments(sanitizedComments);
        }
    }

    public static boolean isCreditCardExpired(RoomReservation reservation) {
        boolean isCreditCardExpired = false;
        if (null != reservation && null != reservation.getPayments()) {
            isCreditCardExpired = reservation.getPayments()
                    .stream()
                    .anyMatch(payment -> payment.getChargeCardExpiry() != null
                            && !DateUtil.isFutureMonth(payment.getChargeCardExpiry()));
        }
        return isCreditCardExpired;
    }
    
    public static HttpMessageConverter createHttpMessageConverter() {
        SimpleModule customDataModule = new SimpleModule();
        customDataModule.addDeserializer(CustomData.class, new CustomDataDeserializer());
        // Only send non-null
        ObjectMapper objMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(getJavaTimeModuleISO())
                .registerModule(customDataModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // TODO remove this when schema is validated
        HttpMessageConverter msgConverter = new MappingJackson2HttpMessageConverter(objMapper);
        return msgConverter;
    }

    public static JavaTimeModule getJavaTimeModuleISO() {
        // LocalDate serialization
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        LocalDateDeserializer dateTimeDeserializer = new LocalDateDeserializer(formatter);
        LocalDateSerializer dateTimeSerializer = new LocalDateSerializer(formatter);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDate.class, dateTimeDeserializer);
        javaTimeModule.addSerializer(LocalDate.class, dateTimeSerializer);

        return javaTimeModule;

    }
    public static boolean hasCheckInDateElapsed(Date date, String timezone) {
    	ZoneId propertyZone = ZoneId.of(timezone);
    	LocalDate today = LocalDate.now(propertyZone);
    	LocalDate checkinDate = convertDateToLocalDate(date);
    	return checkinDate.isBefore(today);
    }

    public static Map<RoomChargeItemType, List<String>> getTaxTypeTaxCodeMapByPropertyCode(AcrsProperties acrsProperties,
            String propertyCode) {
        Map<RoomChargeItemType, List<String>> taxTypeTaxCodesMap = new HashMap<>();
        List<TaxDetails> taxTypeCodes = acrsProperties.getAcrsPropertyTaxCodeMap().get(propertyCode);
        if (CollectionUtils.isNotEmpty(taxTypeCodes)) {
            taxTypeCodes.stream().forEach(tax -> taxTypeTaxCodesMap.put(tax.getTaxType(),
                    tax.getTaxCodes()));
        }

        return taxTypeTaxCodesMap;
    }

    public static RoomChargeItemType getTaxAndChargeTypeByTaxAndChargeCode(
            Map<RoomChargeItemType, List<String>> taxAndChargeTypeTaxAndChargeCodesMap, String taxAndChargeCode) {
        RoomChargeItemType roomChargeItemType = null;
        if (!taxAndChargeTypeTaxAndChargeCodesMap.isEmpty()) {
            Optional<RoomChargeItemType> matchedTaxKey = taxAndChargeTypeTaxAndChargeCodesMap.entrySet().stream()
                    .filter(x -> x.getValue().contains(taxAndChargeCode)).findFirst().map(entry -> entry.getKey());
            if (matchedTaxKey.isPresent()) {
                roomChargeItemType = matchedTaxKey.get();
            }
        }
        return roomChargeItemType;
    }


    public static <T> T logAndReturnCrsResponseBody(ResponseEntity<T> crsResponse, String apiName, Logger log, boolean isLogInfoLevel) {
        T crsResponseBody = crsResponse.getBody();
        if (null == crsResponseBody) {
            // AFAIK there is no scenario where an empty response is valid
            log.error("Crs Response body is empty in {} API response.", apiName);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }

        if (isLogInfoLevel) {
            log.info(ServiceConstant.ACRS_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(crsResponse.getHeaders()));
            log.info(ServiceConstant.ACRS_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(crsResponseBody));
        } else if (log.isDebugEnabled()) {
            log.debug(ServiceConstant.ACRS_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(crsResponse.getHeaders()));
            log.debug(ServiceConstant.ACRS_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(crsResponseBody));
        }

        if (crsResponse.getStatusCodeValue() >= 200 && crsResponse.getStatusCodeValue() < 300) {
            return crsResponseBody;
        } else {
            // Should be unreachable as this scenario should be caught by httpErrorHandlers
            log.error(ServiceConstant.HTTP_ERROR_STATUS_CODE_LOG_PREFIX, crsResponse.getStatusCodeValue());
            log.error(ServiceConstant.HTTP_ERROR_MESSAGE_LOG_PREFIX, crsResponseBody);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, crsResponseBody.toString());
        }
    }

    public static <T> T logAndReturnEnrResponseBody(ResponseEntity<T> enrResponse, String apiName, Logger log, boolean isLogInfoEnabled) {
        T enrResponseBody = enrResponse.getBody();
        if (null == enrResponseBody) {
            log.error("Enr Response body is empty in {} API response.", apiName);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }

        if (isLogInfoEnabled){
            log.info(ServiceConstant.ENR_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(enrResponse.getHeaders()));
            log.info(ServiceConstant.ENR_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(enrResponseBody));
        } else if (log.isDebugEnabled()) {
            log.debug(ServiceConstant.ENR_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(enrResponse.getHeaders()));
            log.debug(ServiceConstant.ENR_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(enrResponseBody));
        }

        if (enrResponse.getStatusCodeValue() >= 200 && enrResponse.getStatusCodeValue() < 300) {
            return enrResponseBody;
        } else {
            log.error(ServiceConstant.HTTP_ERROR_STATUS_CODE_LOG_PREFIX, enrResponse.getStatusCodeValue());
            log.error(ServiceConstant.HTTP_ERROR_MESSAGE_LOG_PREFIX, enrResponseBody);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, enrResponseBody.toString());
        }
    }

    public static <T> T logAndReturnPetResponseBody(ResponseEntity<T> petResponse, String apiName, Logger log, boolean isLogInfoEnabled) {
        T petResponseBody = petResponse.getBody();
        if (null == petResponseBody) {
            log.error("PET Response body is empty in {} API response.", apiName);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }

        if (isLogInfoEnabled){
            log.info(ServiceConstant.PET_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(petResponse.getHeaders()));
            log.info(ServiceConstant.PET_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(petResponseBody));
        } else if (log.isDebugEnabled()) {
            log.debug(ServiceConstant.PET_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(petResponse.getHeaders()));
            log.debug(ServiceConstant.PET_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(petResponseBody));
        }

        if (petResponse.getStatusCodeValue() >= 200 && petResponse.getStatusCodeValue() < 300) {
            return petResponseBody;
        } else {
            log.error("Fatal Exception during find reservation Payment Exchange Token: ");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND,
                    CommonUtil.convertObjectToJsonString(petResponse.getBody()));
        }
    }

    public static <T> T logAndReturnRefDataResponseBody(ResponseEntity<T> refDataResponse, String apiName, Logger log, boolean isLogInfoEnabled) {
        T refDataResponseBody = refDataResponse.getBody();
        if (null == refDataResponse) {
            log.error("Ref Data response body is empty in {} API response.", apiName);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }

        if (isLogInfoEnabled){
            log.info(ServiceConstant.REF_DATA_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(refDataResponse.getHeaders()));
            log.info(ServiceConstant.REF_DATA_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(refDataResponse));
        } else if (log.isDebugEnabled()) {
            log.debug(ServiceConstant.REF_DATA_HEADER_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(refDataResponse.getHeaders()));
            log.debug(ServiceConstant.REF_DATA_RESPONSE_LOG_PREFIX, apiName,
                    CommonUtil.convertObjectToJsonString(refDataResponse));
        }

        if (refDataResponse.getStatusCodeValue() >= 200 && refDataResponse.getStatusCodeValue() < 300) {
            return refDataResponseBody;
        } else {
            log.error("Fatal Exception while invoking refData API: ");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    CommonUtil.convertObjectToJsonString(refDataResponse.getBody()));
        }
    }


    public static boolean checkExceptionTaxCode(AcrsProperties acrsProperties, String taxCode, String propertyCode) {

        List<String> taxExceptionCodes = acrsProperties.getAcrsPropertyTaxCodeExceptionMap().get(propertyCode);
        if (CollectionUtils.isNotEmpty(taxExceptionCodes)) {
            return taxExceptionCodes.contains(taxCode);
        } else {
            return false;
        }

    }
    
    public static List<String> getTaxExceptions(TaxCodeExceptionDetails codeException){
       List<String> codeList = new ArrayList<>();
        if(StringUtils.isNotEmpty(codeException.getTaxCodeExceptions())) {
            codeList = Arrays.asList(codeException.getTaxCodeExceptions().split("\\s*,\\s*"));
            
        }
        return codeList;
    }
    
    /**
     * 
     * @param acrsProperties
     * @param propertyCode
     * @return
     */
    public static Map<RoomChargeItemType, List<String>> getChargeTypeChargeCodeMapByPropertyCode(AcrsProperties acrsProperties,
            String propertyCode) {

        Map<RoomChargeItemType, List<String>> propertyChargeCodeMap = new HashMap<>();
       List<ChargeDetails> chargeTypeCodes = acrsProperties.getAcrsPropertyChargeCodeMap().get(propertyCode);
                if (CollectionUtils.isNotEmpty(chargeTypeCodes)) {
                    chargeTypeCodes.stream().forEach(charge -> propertyChargeCodeMap.put(charge.getChargeType(),
                            charge.getChargeCodes()));
                }
        return propertyChargeCodeMap;
    }

    
    public static List<ItemizedChargeItem> getOtherCharges(String propertyCode, List<TaxAmount> taxList, AcrsProperties acrsProperties) {
        Map<RoomChargeItemType, List<String>> chargeCodesMap = ReservationUtil
                .getChargeTypeChargeCodeMapByPropertyCode(acrsProperties, propertyCode);
        List<ItemizedChargeItem> itemizedChargeItemList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(taxList)) {
          for (TaxAmount tax : taxList) {
              RoomChargeItemType chargeItemType = ReservationUtil.getTaxAndChargeTypeByTaxAndChargeCode(chargeCodesMap,
                      tax.getTaxCode());
              if (null != chargeItemType) {
                  ItemizedChargeItem chrgItem = new ItemizedChargeItem();
                  chrgItem.setAmount(Double.parseDouble(tax.getAmount()));
                  chrgItem.setItemType(chargeItemType);
                  chrgItem.setItem(tax.getTaxCode());
                  itemizedChargeItemList.add(chrgItem);
              }
            }
        }
        return itemizedChargeItemList;
    }
    /**
     * 
     * @param propertyCode
     * @param taxList
     * @param acrsProperties
     */
    public static List<ItemizedChargeItem> getOtherOfferCharges(String propertyCode,
                                                                List<com.mgm.services.booking.room.model.crs.searchoffers.TaxAmount> taxList,
                                                                AcrsProperties acrsProperties) {
        Map<RoomChargeItemType, List<String>> chargeCodesMap = ReservationUtil
                .getChargeTypeChargeCodeMapByPropertyCode(acrsProperties, propertyCode);
        List<ItemizedChargeItem> itemizedChargeItemList = new ArrayList<>();
        for (com.mgm.services.booking.room.model.crs.searchoffers.TaxAmount tax : taxList) {
            RoomChargeItemType chargeItemType = ReservationUtil.getTaxAndChargeTypeByTaxAndChargeCode(chargeCodesMap,
                    tax.getCode());
            if (null != chargeItemType) {
                ItemizedChargeItem chrgItem = new ItemizedChargeItem();
                chrgItem.setAmount(Double.parseDouble(tax.getAmt()));
                chrgItem.setItemType(chargeItemType);
                chrgItem.setItem(tax.getCode());
                itemizedChargeItemList.add(chrgItem);
            }
        }
        return itemizedChargeItemList;
    }
    
    
    public static boolean isBlockCodeHdePackage(String blockCode) {
        boolean isBlockCodeHdePackage = false;
        if (StringUtils.isNotEmpty(blockCode)) {
            if (StringUtils.right(blockCode, 3).equalsIgnoreCase(ServiceConstant.HDE_PACKAGE)) {
                isBlockCodeHdePackage = true;
            }
        }
        return isBlockCodeHdePackage;
    }

    public static boolean allowedTitleList(String title,Set<String> allowedTitleList) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(allowedTitleList) && allowedTitleList.contains(title)) {
            result = true;
        }
        return result;
    }

    public static boolean checkInventoryAvailability(InventoryGetRes inventoryGetRes, int tickets) {
        boolean isF1InvAvailable = false;
        if (null != inventoryGetRes) {
            for (InventoryDetails inventory : inventoryGetRes.getInventories()) {
                isF1InvAvailable = isF1PackageInvAvailable(inventory, tickets);
            }
        }
        return isF1InvAvailable;
    }

    private static boolean isF1PackageInvAvailable(InventoryDetails inventory, int tickets) {
        boolean isF1InvAvailable = false;
        if (null != inventory.getInventory() && null != inventory.getInventory().getLevel1()) {
            for (Level level : inventory.getInventory().getLevel1()) {
                // Check F1 inventory
                if (org.apache.commons.lang.StringUtils.isNumeric(level.getTotalAvailableUnits()) && Integer.parseInt(level.getTotalAvailableUnits()) >= tickets) {
                    isF1InvAvailable = true;
                    level.setUnits(tickets);
                } else {
                    throw new BusinessException(ErrorCode.F1_INVENTORY_NOT_AVAILABLE);
                }
            }
        }
        return isF1InvAvailable;
    }

    public static String getF1DefaultCasinoComponentCode(List<String> ratePlanTags) {
        String componentCode = null;
        Optional<String> optionalComponent = ratePlanTags.stream().filter(x -> x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_CASINO_START_TAG) || x.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)).findFirst();
        if (optionalComponent.isPresent()) {
            String optionalComponentCode = optionalComponent.get();
            if (optionalComponentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
                componentCode = optionalComponentCode;
            } else {
                componentCode = optionalComponentCode.substring(2);
            }
        }
        return componentCode;
    }

    public static Float getRoomComponentPrice(RoomComponent roomComponent, Date checkInDate, Date checkOutDate) {
        Float price;
        if (StringUtils.isNotEmpty(roomComponent.getPricingApplied()) && roomComponent.getPricingApplied().equalsIgnoreCase(ServiceConstant.NIGHTLY_PRICING_APPLIED)) {
            price = roomComponent.getPrice();
        } else {
            long tripLength = TimeUnit.DAYS.convert((checkOutDate.getTime() - checkInDate.getTime()),
                    TimeUnit.MILLISECONDS);
            BigDecimal baseAmountBigDecimal = BigDecimal.valueOf(roomComponent.getPrice());
            price = baseAmountBigDecimal.divide(new BigDecimal(tripLength), 2, RoundingMode.HALF_UP).floatValue();
        }
        return price;
    }

    public static Integer f1TicketQuantity(String componentCode, ApplicationProperties applicationProperties) {
        int count = 0;
        try {
            count = Integer.parseInt(componentCode.substring(componentCode.lastIndexOf(ServiceConstant.F1_COMPONENT_TICKET_COUNT_START) + 1,
                    componentCode.lastIndexOf(ServiceConstant.F1_COMPONENT_TICKET_COUNT_START) + 2));
        } catch (Exception e) {
            count = applicationProperties.getMinF1TicketCount();
        }
        return count;
    }

    public static void updateRatesSummary (RoomReservationV2Response roomReservationV2Response, RoomComponent roomComponent, Float updatedPrice) {
        RatesSummary ratesSummary = roomReservationV2Response.getRatesSummary();
        ratesSummary.setDiscountedSubtotal(ratesSummary.getDiscountedSubtotal() + roomComponent.getPrice());
        ratesSummary.setRoomSubtotal(ratesSummary.getRoomSubtotal() + roomComponent.getPrice());
        ratesSummary.setDiscountedAveragePrice(ratesSummary.getDiscountedAveragePrice() + updatedPrice);
        ratesSummary.setTripSubtotal(ratesSummary.getTripSubtotal() + roomComponent.getPrice());
        ratesSummary.setRoomRequestsTotal(ratesSummary.getRoomRequestsTotal() - roomComponent.getPrice());
    }

    public static void purchasedComponentsF1Updates(RoomReservationV2Response roomReservationV2Response,
                                                    ApplicationProperties applicationProperties,
                                                    List<String> ratePlanTags, boolean cancelFlow) {
        if (StringUtils.isNotEmpty(roomReservationV2Response.getPropertyId())
                && StringUtils.equalsIgnoreCase(applicationProperties.getTcolvPropertyId(), roomReservationV2Response.getPropertyId())) {
            String componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(ratePlanTags));
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(componentCode)) {
                for (PurchasedComponent purchasedComponent : roomReservationV2Response.getPurchasedComponents()) {
                    if (null != purchasedComponent.getCode() && purchasedComponent.getCode().equalsIgnoreCase(componentCode)) {
                        String f1ComponentCode = ReservationUtil.getF1DefaultPublicTicketComponentCode(ratePlanTags);
                        purchasedComponent.setCode(f1ComponentCode);
                        if (!cancelFlow) {
                            purchasedComponent.setNonEditable(true);
                        }
                    }
                }
            }
        } else {
            if (!cancelFlow) {
                roomReservationV2Response.getPurchasedComponents().forEach(x -> {
                    if (null != x.getCode() && x.getCode().startsWith(ServiceConstant.F1_COMPONENT_START_F1)) {
                        x.setNonEditable(true);
                    }
                });
            }
        }
    }

    /*public static void f1ResponseComponentUpdates(List<String> ratePlanTags, String grandStandName, CalculateRoomChargesResponse response, int tickets) {
    List<RoomBookingComponent> extraF1RoomBookingComponents;
    extraF1RoomBookingComponents = response.getAvailableComponents().stream().filter(x ->
            (null != x.getCode() && x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1) &&
                    x.getCode().toUpperCase().contains(grandStandName))).collect(Collectors.toList());
    response.getAvailableComponents().removeIf(x -> null != x.getCode() &&
            (x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1) ||
                    x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_HDN)));
    List<RoomBookingComponent> finalF1RoomBookingComponents = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(extraF1RoomBookingComponents)) {
        int count = 1;
        Optional<String> optionalTransientComponent = ratePlanTags.stream().filter(x ->
                x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_TRANSIENT_START_TAG)).findFirst();
        if(optionalTransientComponent.isPresent()) {
        while (count <= tickets) {
            int finalCount = count;
            Optional<RoomBookingComponent> bookingComponentOptional = extraF1RoomBookingComponents.stream().filter(x ->
                    Integer.parseInt(org.apache.commons.lang.StringUtils.right(x.getCode(), 1)) == finalCount).findFirst();
            if (bookingComponentOptional.isPresent()) {
                RoomBookingComponent roomBookingComponent = bookingComponentOptional.get();
                if (count <= tickets) {
                    roomBookingComponent.setNonEditable(true);
                }
                finalF1RoomBookingComponents.add(roomBookingComponent);
            }
            count++;
        }
        if (CollectionUtils.isNotEmpty(response.getAvailableComponents())) {
            finalF1RoomBookingComponents.addAll(response.getAvailableComponents());
        }
        response.setAvailableComponents(finalF1RoomBookingComponents);
    }
  }
}*/

    public static void f1ResponseComponentUpdates(CalculateRoomChargesResponse response, String grandStandName,
                                                  ApplicationProperties applicationProperties, String propertyId, List<String> ratePlanTags) {
        Optional<RoomBookingComponent> f1RoomBookingComponent;
        if (StringUtils.isNotEmpty(propertyId) && StringUtils.equalsIgnoreCase(applicationProperties.getTcolvPropertyId(), propertyId)) {
            f1RoomBookingComponent = response.getAvailableComponents().stream().filter(x ->
                    (null != x.getCode() && null != applicationProperties.getTcolvF1ComponentCodeByGrandStand(grandStandName) &&
                            x.getCode().equalsIgnoreCase(applicationProperties.getTcolvF1ComponentCodeByGrandStand(grandStandName)))).findFirst();
        } else {
            f1RoomBookingComponent = response.getAvailableComponents().stream().filter(x ->
                    (null != x.getCode() && x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1) &&
                            x.getCode().toUpperCase().contains(grandStandName))).findFirst();
        }
        response.getAvailableComponents().removeIf(x -> null != x.getCode() &&
                (x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1) ||
                        x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_HDN) ||
                        applicationProperties.getTcolvF1ComponentCodes().contains(x.getCode())));
        List<RoomBookingComponent> finalRoomBookingComponents = new ArrayList<>();
        if (f1RoomBookingComponent.isPresent()) {
            RoomBookingComponent roomBookingComponent = f1RoomBookingComponent.get();
            if (StringUtils.isNotEmpty(propertyId) && StringUtils.equalsIgnoreCase(applicationProperties.getTcolvPropertyId(), propertyId)) {
                String componentCode = ReservationUtil.getF1DefaultPublicTicketComponentCode(new ArrayList<>(ratePlanTags));
                roomBookingComponent.setCode(componentCode);
            }
            roomBookingComponent.setNonEditable(true);
            finalRoomBookingComponents.add(roomBookingComponent);
        }
        if (CollectionUtils.isNotEmpty(response.getAvailableComponents())) {
            finalRoomBookingComponents.addAll(response.getAvailableComponents());
        }
        response.setAvailableComponents(finalRoomBookingComponents);
    }

    /*public static int getTicketCount(RoomProgramValidateResponse validateResponse, CreateRoomReservationResponse response) {
        int ticketCount = 0;
        String f1DefaultCasinoComponentCode = ReservationUtil.getF1DefaultCasinoComponentCode(validateResponse.getRatePlanTags());
        if (StringUtils.isNotEmpty(f1DefaultCasinoComponentCode)) {
            ticketCount += ReservationUtil.f1TicketQuantity(f1DefaultCasinoComponentCode);
        }
        long f1ComponentCount = response.getRoomReservation().getPurchasedComponents().stream().filter(x ->
                (null != x.getCode() && x.getCode().startsWith(ServiceConstant.F1_COMPONENT_START_F1))).count();
        ticketCount += f1ComponentCount;
        return ticketCount;
    }*/

    public static int getTicketCount(RoomProgramValidateResponse validateResponse, CreateRoomReservationResponse response,
                                     ApplicationProperties applicationProperties) {
        int ticketCount = 0;
        int f1PurchasedComponentTicketCount = 0;
        String f1DefaultCasinoComponentCode = ReservationUtil.getF1DefaultCasinoComponentCode(validateResponse.getRatePlanTags());
        if (StringUtils.isNotEmpty(f1DefaultCasinoComponentCode)) {
            ticketCount += f1TicketQuantity(f1DefaultCasinoComponentCode, applicationProperties);
        }
        Optional<PurchasedComponent> f1PurchasedComponent = response.getRoomReservation().getPurchasedComponents().stream().filter(x ->
                (null != x.getCode() && x.getCode().startsWith(ServiceConstant.F1_COMPONENT_START_F1))).findFirst();
        if (f1PurchasedComponent.isPresent()) {
            try {
                f1PurchasedComponentTicketCount = f1TicketQuantity(f1PurchasedComponent.get().getCode(), applicationProperties);
            } catch (Exception e) {
                f1PurchasedComponentTicketCount = applicationProperties.getMinF1TicketCount();
            }
        } else if (null != response && null != response.getRoomReservation() &&
                null != response.getRoomReservation().getPropertyId()) {
            if (StringUtils.equalsIgnoreCase(applicationProperties.getTcolvPropertyId(), response.getRoomReservation().getPropertyId())) {
                String componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(validateResponse.getRatePlanTags()));
                if (StringUtils.isNotEmpty(componentCode)) {
                    Optional<PurchasedComponent> f1TcolvPurchasedComponent = response.getRoomReservation().getPurchasedComponents().stream().filter(x ->
                            (null != x.getCode() && x.getCode().equalsIgnoreCase(componentCode))).findFirst();
                    if (f1TcolvPurchasedComponent.isPresent()) {
                        f1PurchasedComponentTicketCount = getF1TicketCountFromF1Tag(validateResponse.getRatePlanTags(), applicationProperties);
                    }
                }
            }
        }
        ticketCount += f1PurchasedComponentTicketCount;
        return ticketCount;
    }

    public static HoldInventoryReq createHoldInventoryRequest (InventoryGetRes inventoryGetRes, String holdId) {
        HoldInventoryReq request = new HoldInventoryReq();
        request.setInventories(inventoryGetRes.getInventories());
        request.setHoldId(holdId);
        request.setProductAssignment(inventoryGetRes.getProductAssignment());
        request.setProductCode(inventoryGetRes.getProductCode());
        request.setProductType(inventoryGetRes.getProductType());
        request.setVenueId(inventoryGetRes.getVenueId());
        return request;
    }

    public static ReleaseV2Request createReleaseV2Request(ReleaseV3Request releaseRequest) {
        ReleaseV2Request request = new ReleaseV2Request();
        request.setConfirmationNumber(releaseRequest.getConfirmationNumber());
        request.setPropertyId(releaseRequest.getPropertyId());
        request.setCustomerId(releaseRequest.getCustomerId());
        request.setSource(releaseRequest.getSource());
        request.setMlifeNumber(releaseRequest.getMlifeNumber());
        request.setCustomerTier(releaseRequest.getCustomerTier());
        request.setPerpetualPricing(releaseRequest.isPerpetualPricing());
        request.setChannel(releaseRequest.getChannel());
        request.setF1Package(releaseRequest.isF1Package());
        return request;
    }

    public static CommitInventoryReq createCommitInventoryRequest (CreateRoomReservationRequest createReservationRequest, String confirmationNumber, int ticketCount) {
        CommitInventoryReq request = new CommitInventoryReq();
        request.setHoldId(createReservationRequest.getRoomReservation().getHoldId());
        request.setConfirmationNumber(confirmationNumber);
        request.setOrderId(createReservationRequest.getRoomReservation().getOrderId());
        request.setOrderLineItemId(createReservationRequest.getRoomReservation().getOrderLineItemId());
        request.setUnits(ticketCount);
        return request;
    }

    public static ReleaseInventoryReq createReleaseInventoryRequest (String holdId) {
        ReleaseInventoryReq releaseInventoryReq = new ReleaseInventoryReq();
        releaseInventoryReq.setHoldId(holdId);
        return releaseInventoryReq;
    }

    public static RollbackInventoryReq createRollbackInventoryRequest (String confirmationNumber) {
        RollbackInventoryReq rollbackInventoryReq = new RollbackInventoryReq();
        rollbackInventoryReq.setConfirmationNumber(confirmationNumber);
        return rollbackInventoryReq;
    }

/*public static int getF1TicketCount(List<String> ratePlanTags, int numGuests, ApplicationProperties applicationProperties) {
    int ticketCount = applicationProperties.getMinF1TicketCount();
    Optional<String> optionalCasinoComponent = ratePlanTags.stream().filter(x -> x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_CASINO_START_TAG) || x.toUpperCase().startsWith(ServiceConstant.F1_COMP_TAG)).findFirst();
    if (optionalCasinoComponent.isPresent() || numGuests > 1) {
        ticketCount = applicationProperties.getMaxF1TicketCount();
    }
    return ticketCount;
}*/

    public static int getF1TicketCountFromF1Tag(List<String> ratePlanTags, ApplicationProperties applicationProperties) {
        int ticketCount = 0;
        try {
            Optional<String> f1ComponentTag = ratePlanTags.stream().filter(x -> x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_CASINO_START_TAG) ||
                    x.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG) || x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_TRANSIENT_START_TAG)).findFirst();
            if (f1ComponentTag.isPresent()) {
                ticketCount = f1TicketQuantity(f1ComponentTag.get(), applicationProperties);
            }
        } catch (Exception e) {
            ticketCount = applicationProperties.getMinF1TicketCount();
        }
        return ticketCount;
    }

    public static String getProductCodeForF1Program(String productCode, List<String> programTags, ApplicationProperties appProps) {
        Optional<String> optionalTransientComponent = programTags.stream().filter(x -> x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_TRANSIENT_START_TAG)).findFirst();
        if (optionalTransientComponent.isPresent()) {
            StringBuilder prefixedProductCode = new StringBuilder();
            return prefixedProductCode.append(appProps.getPublicF1ProductPrefix()).append(productCode).toString();
        }
        return productCode;
    }

    public static String getF1DefaultPublicTicketComponentCode(List<String> ratePlanTags) {
        String componentCode = null;
        Optional<String> optionalComponent = ratePlanTags.stream().filter(x -> x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_TRANSIENT_START_TAG)).findFirst();
        if (optionalComponent.isPresent()) {
            String optionalComponentCode = optionalComponent.get();
            componentCode = optionalComponentCode.substring(2);
        }
        return componentCode;
    }
    
    public static String getTCOLVF1TicketComponentCode(List<String> ratePlanTags) {
        String componentCode = null;
        Optional<String> optionalComponent = ratePlanTags.stream().filter(x -> x.toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_TCOLV_START_TAG)).findFirst();
        if (optionalComponent.isPresent()) {
            String optionalComponentCode = optionalComponent.get();
            componentCode = optionalComponentCode.substring(2);
        }
        return componentCode;
    }

    public static String getF1AdditionalPublicTicketComponentCode(List<String> ratePlanTags, ApplicationProperties applicationProperties) {
        String additionalComponentCode = null;
        if (!ratePlanTags.isEmpty() && ratePlanTags.contains(applicationProperties.getF1PackageTag())) {
            for (String validProductCode : applicationProperties.getValidF1ProductCodes()) {
                if (ratePlanTags.contains(validProductCode)) {
                    validProductCode = getProductCodeForF1Program(validProductCode, new ArrayList<>(ratePlanTags), applicationProperties);
                    String grandstandName = StringUtils.right(validProductCode, 3);
                    if (null != grandstandName) {
                        additionalComponentCode = applicationProperties.getF1N24AdditionalTicketByGrandStand(grandstandName);
                    }
                }
            }
        }
        return additionalComponentCode;
    }

    public static double componentPriceToAdd(LocalDate checkInDate, LocalDate checkOutDate, RoomComponent component, boolean isNightlyPricing, ApplicationProperties appProperties) {
        double toAdd = 0.0;
        if (null != component && null != component.getPrice()) {
            if (!isNightlyPricing) {
                return component.getPrice();
            }
            toAdd = getRoomComponentPrice(component,
                    localDateToDate(checkInDate, appProperties.getDefaultTimezone()),
                    localDateToDate(checkOutDate, appProperties.getDefaultTimezone()));
        }
        return toAdd;
    }

    public static String getPartyReservationFinancialImpact(PartyRoomReservation partyRoomReservation) {
        ReservationFinancialImpact financialImpact = new ReservationFinancialImpact();
        double totalTransactionAmount = ServiceConstant.ZERO_DOUBLE_VALUE;
        double amountCharged = ServiceConstant.ZERO_DOUBLE_VALUE;
        double balanceDue = ServiceConstant.ZERO_DOUBLE_VALUE;
        double roomSubtotal = ServiceConstant.ZERO_DOUBLE_VALUE;
        double taxes = ServiceConstant.ZERO_DOUBLE_VALUE;
        double resortFeeTax = ServiceConstant.ZERO_DOUBLE_VALUE;
        try {
            for (RoomReservation reservation : partyRoomReservation.getRoomReservations()) {
                RatesSummary rateSummary = RoomReservationTransformer.transform(reservation);
                double amountPaid = ServiceConstant.ZERO_DOUBLE_VALUE;
                if (null != reservation.getPayments()) {
                    for (Payment charge : reservation.getPayments()) {
                        if (charge.isDeposit()) {
                            amountPaid += charge.getChargeAmount();
                        }
                    }
                }
                if (StringUtils.isEmpty(financialImpact.getProductType())) {
                    financialImpact.setProductType(BookingType.ROOM.getValue());
                }
                if (StringUtils.isEmpty(financialImpact.getOperation())) {
                    financialImpact.setOperation(reservation.getState().toString());
                }
                if (StringUtils.isEmpty(financialImpact.getConfirmationNumber())) {
                    financialImpact.setConfirmationNumber(reservation.getConfirmationNumber());
                }
                totalTransactionAmount += CommonUtil.round(rateSummary.getReservationTotal(), ServiceConstant.DEFAULT_PRECISION);
                amountCharged += CommonUtil.round(amountPaid, ServiceConstant.DEFAULT_PRECISION);
                balanceDue += CommonUtil.round(rateSummary.getBalanceUponCheckIn(), ServiceConstant.DEFAULT_PRECISION);
                roomSubtotal += CommonUtil.round(rateSummary.getAdjustedRoomSubtotal(), ServiceConstant.DEFAULT_PRECISION);
                taxes += CommonUtil.round(rateSummary.getRoomChargeTax(), ServiceConstant.DEFAULT_PRECISION);
                resortFeeTax += CommonUtil.round(rateSummary.getResortFeeAndTax(), ServiceConstant.DEFAULT_PRECISION);
            }
            financialImpact.setTotalTransactionAmount(totalTransactionAmount);
            financialImpact.setAmountCharged(amountCharged);
            financialImpact.setBalanceDue(balanceDue);
            financialImpact.getRoomBookingItemized().setResortFeeTax(resortFeeTax);
            financialImpact.getRoomBookingItemized().setRoomSubtotal(roomSubtotal);
            financialImpact.getRoomBookingItemized().setTaxes(taxes);
            return CommonUtil.objectToJson(financialImpact);
        } catch (Exception ex) {
            log.error("Exception while creating the party reservation financial impact : {}", ex.getMessage());
        }
        return null;
    }
    public static RoomProgramV2Request createRoomV2Request(String programId, String checkInDate,
                                                           String checkOutDate) {
        RoomProgramV2Request acrsProgramDetailsRequest = new RoomProgramV2Request();
        acrsProgramDetailsRequest.setProgramIds(Collections.singletonList(programId));
        acrsProgramDetailsRequest.setStartDate(checkInDate);
        acrsProgramDetailsRequest.setEndDate(checkOutDate);
        return acrsProgramDetailsRequest;
    }

    public static Double calculateDailyAmtAftTax(List<com.mgm.services.booking.room.model.crs.searchoffers.TaxAmount> taxList, Double discountedPrice, Map<String, Boolean> isRatePlanCompMap, double actualAmtAftTax, String ratePlanCode) {
    double effectiveTotalAmtAftTax = actualAmtAftTax;
    //If comp rate plan then hide room price and room tax
    if(Boolean.TRUE.equals(isRatePlanCompMap.get(ratePlanCode))){
        effectiveTotalAmtAftTax = actualAmtAftTax - discountedPrice;
        List<com.mgm.services.booking.room.model.crs.searchoffers.TaxAmount> discardTaxAndFeesList = taxList.stream().filter(tax -> ServiceConstant.ACRS_OCCUPANCY_FEE_ITEM.contains(tax.getCode())).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(discardTaxAndFeesList)){
            double discardedAmt = discardTaxAndFeesList.stream().mapToDouble(tax -> Double.parseDouble(tax.getAmt())).sum();
            effectiveTotalAmtAftTax = effectiveTotalAmtAftTax - discardedAmt;
        }
    }
    return effectiveTotalAmtAftTax;
    }


	public static boolean isSuppressWebComponent(String componentId, AcrsProperties acrsProperties) {
		String componentNRPlanCode = ACRSConversionUtil.getComponentNRPlanCode(componentId);
		if (null == acrsProperties ||
				CollectionUtils.isEmpty(acrsProperties.getSuppresWebComponentPatterns()) ||
				StringUtils.isEmpty(componentNRPlanCode)) {
			return false;
		}
		return acrsProperties.getSuppresWebComponentPatterns().stream()
				.anyMatch(componentNRPlanCode::startsWith);
	}

    public static ModifyRoomReservationResponse setWebSuppresableComponentFlag(ModifyRoomReservationResponse response, AcrsProperties acrsProperties){
        if (null != response && null != response.getRoomReservation() &&
            null != response.getRoomReservation().getPurchasedComponents()) {
            response.getRoomReservation().getPurchasedComponents().stream().filter(component ->
							ReservationUtil.isSuppressWebComponent(component.getId(), acrsProperties))
                    .forEach(specialRequest -> specialRequest.setNonEditable(true));
        }
        return response;
    }

    public static boolean isBlackListed(List<String> whiteListMarketCodes, List<RoomMarket> reservationMarketCodes){
    	if(CollectionUtils.isNotEmpty(whiteListMarketCodes)) {
    		if (CollectionUtils.isNotEmpty(reservationMarketCodes)) {
    			String reservationMarketCode = reservationMarketCodes.get(0).getMarketCode();
    				boolean blacklisted = !whiteListMarketCodes.contains(reservationMarketCode);
    				if (blacklisted) {
    					log.info("Market code {} is black listed", reservationMarketCode);
    				}
    				return blacklisted;
    			} else {
    				//If there is no market code then that is black listed
    				log.info("Market code is missing, hence returning black listed as true");
    				return true;
    			}
    		}else{
    			return false;
    		}
    	}
    public static Date convertStringDateToDate(String dateInStringForm) {
    	if(StringUtils.isNotEmpty(dateInStringForm)) {
    		return convertLocalDateToDate(java.time.LocalDate.parse(dateInStringForm));
    	}
    	return null;
    }

    public static Date convertStringDateTimeToDate(String dateInStringForm) {
        if (StringUtils.isNotEmpty(dateInStringForm)) {
            if (dateInStringForm.contains("T")) {
                return convertLocalDateToDate(LocalDate.from(java.time.LocalDateTime.parse(dateInStringForm)));
            } else {
                return convertStringDateToDate(dateInStringForm);
            }
        }
        return null;
    }
    public static List<String> componentIdsToCodes(List<String> pkgComponentIds) {
        List<String> pimPkgComponents = Collections.emptyList();
        if(CollectionUtils.isNotEmpty(pkgComponentIds)) {
            pimPkgComponents = pkgComponentIds.stream()
                    .map(ACRSConversionUtil::getComponentCode)
                    .collect(Collectors.toList());
        }
        return pimPkgComponents;

    }
    
    public static boolean isPkgComponent(String id, List<String> pkgComponentCodes) {
        if (id == null || (StringUtils.isNotBlank(id) && ValidationUtil.isUuid(id))) {
            return false;
        }     
        String pkgCode = ACRSConversionUtil.getComponentCode(id);
        boolean isComponentPresentInPkgCode = false;
        if(null != pkgCode && CollectionUtils.isNotEmpty(pkgComponentCodes)){
            isComponentPresentInPkgCode= pkgComponentCodes.contains(pkgCode);
        }
        return isComponentPresentInPkgCode;
    }

    public static List<PurchasedComponent> setPkgComponentFlagForPurchasedComponents(List<PurchasedComponent> purchasedComponentsList, List<String> pkgComponentCodes) {
        if(CollectionUtils.isNotEmpty(purchasedComponentsList) && CollectionUtils.isNotEmpty(pkgComponentCodes)){
            purchasedComponentsList.forEach(component -> {
                if (ReservationUtil.isPkgComponent(component.getId(), pkgComponentCodes)) {
                    component.setNonEditable(true);
                    component.setIsPkgComponent(true);
                }
            });
        }
        return purchasedComponentsList;
    }

    public static boolean isImageInPendingState(ReservationRetrieveResReservation pendingResvRes) {
        return ImageStatus.PENDING.equals(pendingResvRes.getData().getHotelReservation().getImageStatus());
    }
	public static List<String> getMissingPkgComponentsInRequest(List<String> roomRequests,
			List<String> pkgComponentCodes, List<PurchasedComponent> existingComponents) {
		List<String> missingPkgCompIdsInRequest = Collections.emptyList();
		if(CollectionUtils.isNotEmpty(existingComponents) && CollectionUtils.isNotEmpty(pkgComponentCodes)) {
			List<String> existingPkgCompIds = existingComponents.stream().map(PurchasedComponent::getId).filter(compId -> isPkgComponent(compId, pkgComponentCodes)).collect(Collectors.toList());
			if(CollectionUtils.isNotEmpty(existingPkgCompIds)){
				missingPkgCompIdsInRequest = existingPkgCompIds.stream().filter(existingId -> !roomRequests.contains(existingId)).collect(Collectors.toList());
                return missingPkgCompIdsInRequest;
			}
		}
		return missingPkgCompIdsInRequest;
	}

    public static HotelData getHotelData(Date checkInDate, Date checkOutDate, String amount) {
        HotelData hotelData = new HotelData();
        hotelData.setRoomRate(amount);
        if (null != checkInDate) {
            hotelData.setCheckinDate(BaseCommonUtil.getDateStr(checkInDate, ServiceConstant.ISO_8601_DATE_FORMAT));
        }
        if (null != checkOutDate) {
            hotelData.setCheckoutDate(BaseCommonUtil.getDateStr(checkOutDate, ServiceConstant.ISO_8601_DATE_FORMAT));
        }
        return hotelData;
    }

    public static AuthRequest getAuthRequestTCOLV(Date checkIn, Date checkOut, CreditCardCharge charge, String merchantId) {
        final AuthRequest authReq = new AuthRequest();
        authReq.setMerchantID(merchantId);
        String amount;
        if (ServiceConstant.AMEX_TYPE.equalsIgnoreCase(charge.getType())) {
            amount = "0.01";
        } else {
            amount = "0.0";
        }
        authReq.setAmount(amount);
        final PaymentMethods paymentMethods = new PaymentMethods();
        final PaymentMethodsCard card = new PaymentMethodsCard();
        card.setPaymentToken(charge.getNumber());
        card.setNameOnCard(charge.getHolder());
        paymentMethods.setCard(card);
        authReq.setPaymentMethods(paymentMethods);
        authReq.setBillTo(new BillTo());
        authReq.setHotelData(getHotelData(checkIn, checkOut, amount));
        return authReq;
    }
    public static void createOverrideShareWithPricing(ReservationPendingReq reservationReq){
        reservationReq.getData().getHotelReservation().getSegments().get(0).getOffer().getProductUses()
                .forEach(pu-> {
                    pu.setRequestedProductRates(createRequestedProductRates(pu));
                });
    }

    private static List<RateReq> createRequestedProductRates(ProductUseReqItem productUseReqItem) {
        List<RateReq> requestedProductRates = productUseReqItem.getRequestedProductRates();
        if (null != productUseReqItem.getPeriod()) {
            Period period = productUseReqItem.getPeriod();
            LocalDate startDate = period.getStart();
            LocalDate endDate = period.getEnd();
            do {
                RateReq rateReq = new RateReq();
                rateReq.setStart(startDate);
                BaseReq baseReq = new BaseReq();
                baseReq.setAmount("0.0");
                baseReq.setOverrideInd(true);
                rateReq.base(baseReq);
                if (startDate.plusDays(1).equals(endDate)) {
                    rateReq.setEnd(endDate);
                } else {
                    rateReq.setEnd(startDate.plusDays(1));
                }
                requestedProductRates.add(rateReq);
                startDate = startDate.plusDays(1);
            } while (!startDate.equals(endDate));
        }
        return requestedProductRates;
    }
}