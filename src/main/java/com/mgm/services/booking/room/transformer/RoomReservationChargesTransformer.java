package com.mgm.services.booking.room.transformer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.crs.searchoffers.CancellationNoShow;
import com.mgm.services.booking.room.model.crs.searchoffers.DailyRateDetailsSingle;
import com.mgm.services.booking.room.model.crs.searchoffers.DailyRatesSingle;
import com.mgm.services.booking.room.model.crs.searchoffers.OfferSingle;
import com.mgm.services.booking.room.model.crs.searchoffers.Period;
import com.mgm.services.booking.room.model.crs.searchoffers.Policies;
import com.mgm.services.booking.room.model.crs.searchoffers.ProductUseSingle;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulSingleAvailability;
import com.mgm.services.booking.room.model.crs.searchoffers.TaxAmount;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.util.DateUtil;

import lombok.experimental.UtilityClass;

@UtilityClass
@Log4j2
public class RoomReservationChargesTransformer {

    // transformer for singleAvailabilityTo room charges
    public static RoomReservation singleAvailabilityToResvTransform(RoomReservation roomReservation,
																	SuccessfulSingleAvailability crsResponse, AcrsProperties acrsProperties) {
		DataRsSingle crsResponseData = crsResponse.getData();
		if (CollectionUtils.isNotEmpty(crsResponseData.getOffers())) {
            // chargesAndTaxesCalc
            // Note there are 4 types of charges that GSE supports so we need to
            // map these 4 for now and may extend later; each has a corresponding tax as
            // well.(RoomCharge, ExtraGuestCharge, ResortFee, ComponentCharge)

			// I'm not sure why we are only looking at only the first offer. I refactored to extract the variable but
			// do not know the reasoning behind why we only look at the .get(0)
			OfferSingle firstOfferSingle = crsResponseData.getOffers().get(0);
			roomReservation.setChargesAndTaxesCalc(
                    convertAcrsTotalReservationToCharge(firstOfferSingle, acrsProperties,
							crsResponseData.getHotel().getPropertyCode(), crsResponseData.getRatePlans()));
			// AmountDue should be equal to the sum of the amounts in creditCardCharge for an unbooked Reservation.
			if (CollectionUtils.isNotEmpty(roomReservation.getCreditCardCharges())) {
				roomReservation.setAmountDue(roomReservation.getCreditCardCharges().stream().mapToDouble(CreditCardCharge::getAmount).sum());
			}

			// DepositCalc & DepositPolicyCalc
			Policies offerPolicies = firstOfferSingle.getPolicies();
			if (null != offerPolicies) {
				roomReservation.setDepositCalc(getDepositFromAcrsPolicies(roomReservation, offerPolicies));
				roomReservation.setDepositPolicyCalc(getDepositPolicyFromAcrsPolicies(offerPolicies));
			}

			// GuaranteeCode
            if (CollectionUtils.isNotEmpty(crsResponseData.getRatePlans())) {
                roomReservation.setGuaranteeCode(crsResponseData.getRatePlans().get(0).getPolicyTypeCode());
                roomReservation.setPromo(crsResponseData.getRatePlans().get(0).getPromoCode());
            }

			//Program ID
			roomReservation.setProgramId(firstOfferSingle.getRatePlanCode());
			//TODO: Implement this for ICE later after validation
			//Implementing this only for Web as required for Bruno Mars Premium Package Launch
			if(null != roomReservation.getSource() && !ServiceConstant.ICE.equalsIgnoreCase(roomReservation.getSource())) {
				//If Group Code is requested set the Group Code as Program Id
				if(roomReservation.getIsGroupCode()) {
					roomReservation.setProgramId(firstOfferSingle.getGroupCode());
				}
			}
			// This is added for hold reservation with group Code.
			// In hold request we will not have bookings object.
			if (CollectionUtils.isEmpty(roomReservation.getBookings())) {
				roomReservation.setBookings(acrsSingleToRoomPrice(crsResponse, roomReservation.getCheckInDate(),
						roomReservation.getCheckOutDate()));
			}
	}

	return roomReservation;
    }

	private static Deposit getDepositFromAcrsPolicies(RoomReservation roomReservation, Policies offerPolicies) {
		Deposit deposit = new Deposit();
		if (null != offerPolicies.getDeposit()) {
			deposit.setDueDate(CommonUtil.getDate(offerPolicies
					.getDeposit().getDeadline().getDateTime(), ServiceConstant.ISO_8601_DATE_FORMAT));
		}

		deposit.setAmount(getAcrsDepositAmount(offerPolicies));
		CancellationNoShow cancellationNoShow = offerPolicies.getCancellationNoShow();
		if (null != cancellationNoShow) {
			deposit.setForfeitAmount(Double.parseDouble(cancellationNoShow.getAmt()));
			deposit.setForfeitDate(CommonUtil.getDate(cancellationNoShow.getDeadline().getDateTime(), ServiceConstant.ISO_8601_DATE_FORMAT));
		}

		Optional<Deposit> depositCalc = Optional.ofNullable(roomReservation.getDepositCalc());
		deposit.setOverrideAmount(depositCalc.map(Deposit::getOverrideAmount).orElse(-1.0));
		// TODO itemized data not available?
		return deposit;
	}

	private static DepositPolicy getDepositPolicyFromAcrsPolicies(Policies offerPolicies) {
		if (null == offerPolicies) {
			return null;
		}
		DepositPolicy depositPolicy = new DepositPolicy();

		Guarantee guarantee = offerPolicies.getGuarantee();
		com.mgm.services.booking.room.model.crs.searchoffers.Deposit deposit = offerPolicies.getDeposit();
		// Setting CreditCardRequired/DepositRequired to false if not present in Policies.
		depositPolicy.setCreditCardRequired((null != guarantee) && guarantee.getIsRequired());
		depositPolicy.setDepositRequired((null != deposit) && deposit.getIsRequired());

		return depositPolicy;
	}

	private double getAcrsDepositAmount(Policies policies) {
        if (null != policies.getDeposit()) {
            return Double.parseDouble(policies.getDeposit().getAmt());
        } else {
            return 0;
        }
    }

    public static RoomChargesAndTaxes convertAcrsTotalReservationToCharge(OfferSingle offer, AcrsProperties acrsProperties,
																		  String propertyCode,
																		  List<RatePlanSingle> ratePlanResList) {
		Comparator<Period> periodStartComparator = Comparator.comparing(Period::getStart);
		Comparator<Period> periodEndComparator = Comparator.comparing(Period::getEnd);
		List<ProductUseSingle> offerProductUses = offer.getProductUses();

		Optional<Period> startDateOptional = offerProductUses.stream()
				.map(ProductUseSingle::getPeriod)
				.min(periodStartComparator);
		Optional<Period> endDateOptional = offerProductUses.stream()
				.map(ProductUseSingle::getPeriod)
				.max(periodEndComparator);

		if (!startDateOptional.isPresent() || !endDateOptional.isPresent()) {
			log.error("Unable to parse minimum start or end date from OfferSingle ProductUses.");
			log.debug("ProductUses unable to find minimum start or end date from: {}", offerProductUses);
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}

		LocalDate startDate = startDateOptional.get().getStart();
		LocalDate endDate = endDateOptional.get().getEnd();

		List<LocalDate> datesToProcess = Stream.iterate(startDate, localDate -> localDate.plusDays(1))
				.limit(ChronoUnit.DAYS.between(startDate, endDate))
				.collect(Collectors.toList());

		// Collect RoomChargesAndTaxes per date into list of RoomChargesAndTaxes
		List<RoomChargesAndTaxes> roomChargesAndTaxesPerDate = datesToProcess.stream()
				.map(currentDate -> getRoomChargesAndTaxesForDate(offer.getProductUses(), acrsProperties,
						propertyCode, currentDate, ratePlanResList))
				.collect(Collectors.toList());

		// Combine output of multiple dates into single RoomChargesAndTaxes object
		RoomChargesAndTaxes roomChargesAndTaxes = new RoomChargesAndTaxes();
		List<RoomChargeItem> roomChargeItems = roomChargesAndTaxesPerDate.stream()
				.flatMap(roomChargesAndTaxes1 -> roomChargesAndTaxes1.getCharges().stream())
				.collect(Collectors.toList());
		roomChargesAndTaxes.setCharges(roomChargeItems);

		List<RoomChargeItem> taxesAndFeesChargeItems = roomChargesAndTaxesPerDate.stream()
				.flatMap(roomChargesAndTaxes1 -> roomChargesAndTaxes1.getTaxesAndFees().stream())
				.collect(Collectors.toList());
		roomChargesAndTaxes.setTaxesAndFees(taxesAndFeesChargeItems);
		return roomChargesAndTaxes;
    }

	private static RoomChargesAndTaxes getRoomChargesAndTaxesForDate(List<ProductUseSingle> productUseSingleList,
																	 AcrsProperties acrsProperties,
																	 String propertyCode, final LocalDate currentDate,
																	 List<RatePlanSingle> ratePlans){
		RoomChargesAndTaxes roomChargesAndTaxes = new RoomChargesAndTaxes();

		List<ProductUseSingle> dateRelevantProductUses = productUseSingleList.stream()
				.filter(productUse -> !currentDate.isBefore(productUse.getPeriod().getStart()) &&
						currentDate.isBefore(productUse.getPeriod().getEnd()))
				.collect(Collectors.toList());
		if (dateRelevantProductUses.size() != 1) {
			log.warn("Expected that there is only a single productUse for date when calculating charges and taxes. " +
					"Instead found: {}", dateRelevantProductUses);
		}
		ProductUseSingle dateRelevantProductUse = dateRelevantProductUses.get(0);
		Optional<DailyRatesSingle> dailyRatesSingle = dateRelevantProductUse.getRates().getDailyRates().stream()
				.filter(ratePlanSingle -> !currentDate.isBefore(ratePlanSingle.getStart()) &&
						currentDate.isBefore(ratePlanSingle.getEnd()))
				.findFirst();
		if (dailyRatesSingle.isPresent()) {
			String ratePlanCode = dateRelevantProductUse.getRatePlanCode();
			boolean isComp = BaseAcrsTransformer.checkIfCompUsingRatePlanSingle(ratePlanCode, ratePlans);
			// charge
			RoomChargeItem roomChargeItem = getRoomChargeItemForDate(acrsProperties, propertyCode, currentDate,
					dailyRatesSingle.get(), isComp);
			roomChargesAndTaxes.setCharges(Collections.singletonList(roomChargeItem));

			// tax and fees
			RoomChargeItem taxesAndFeesChargeItem = getTaxRoomChargeItemForDate(acrsProperties, propertyCode,
					currentDate, dailyRatesSingle.get(), isComp);
			roomChargesAndTaxes.setTaxesAndFees(Collections.singletonList(taxesAndFeesChargeItem));
		}
		return roomChargesAndTaxes;
	}

	static RoomChargeItem getRoomChargeItemForDate(AcrsProperties acrsProperties, String propertyCode,
												   LocalDate finalCurrentDate,
												   DailyRatesSingle dailyRatesSingle, boolean isComp) {
		RoomChargeItem roomChargeItem = new RoomChargeItem();
		roomChargeItem.setDate(DateUtil.toDate(finalCurrentDate));
		// break down charge included roomCharge and ResortFee
		List<ItemizedChargeItem> itemizedChargeItemList = new ArrayList<>();
		ItemizedChargeItem roomCharge = new ItemizedChargeItem();
		roomCharge.setAmount(0.0);
		if (!isComp) {
			roomCharge.setAmount(Double.parseDouble(dailyRatesSingle.getDailyTotalRate().getBsAmt()));
		}
		roomCharge.setItemType(RoomChargeItemType.RoomCharge);
		itemizedChargeItemList.add(roomCharge);
		List<TaxAmount> taxList = dailyRatesSingle.getDailyTotalRate().getTaxList();
		// add other charges
		itemizedChargeItemList
				.addAll(ReservationUtil.getOtherOfferCharges(propertyCode, taxList, acrsProperties));

		roomChargeItem.setItemized(itemizedChargeItemList);
		// set Total charge
		roomChargeItem.setAmount(
				itemizedChargeItemList.stream().mapToDouble(ItemizedChargeItem::getAmount).sum());
		return roomChargeItem;
	}

	static RoomChargeItem getTaxRoomChargeItemForDate(AcrsProperties acrsProperties, String propertyCode,
															  LocalDate finalCurrentDate,
															  DailyRatesSingle dailyRatesSingle, boolean isComp) {
		RoomChargeItem taxNFeesItem = new RoomChargeItem();
		taxNFeesItem.setDate(DateUtil.toDate(finalCurrentDate));
		// break down taxes is not available in CRS single avail
		// response.
		List<ItemizedChargeItem> itemizedTaxNfeesItemList = new ArrayList<>();
		Map<RoomChargeItemType, List<String>> taxTypeTaxCodesMap = ReservationUtil.getTaxTypeTaxCodeMapByPropertyCode(acrsProperties, propertyCode);
		List<TaxAmount> taxList = dailyRatesSingle.getDailyTotalRate().getTaxList();

		taxList.forEach(tax -> {
			RoomChargeItemType roomChargeItemType = ReservationUtil.getTaxAndChargeTypeByTaxAndChargeCode(taxTypeTaxCodesMap, tax.getCode());
			if (null != roomChargeItemType) {
				ItemizedChargeItem existingTaxType = getFeesTaxType(roomChargeItemType,
						itemizedTaxNfeesItemList);
				double taxAmount = 0.0;
				if(RoomChargeItemType.RoomChargeTax != roomChargeItemType || !isComp) {
					// If Comp then the RoomChargeTax should be 0.0, in all other cases take tax.getAmt()
					taxAmount = Double.parseDouble(tax.getAmt());
				}
				if (null != existingTaxType && !ReservationUtil.checkExceptionTaxCode(acrsProperties, tax.getCode(), propertyCode)) {
					existingTaxType
							.setAmount(existingTaxType.getAmount() + taxAmount);
				} else {
					ItemizedChargeItem feeTaxCharge = new ItemizedChargeItem();
					feeTaxCharge.setAmount(taxAmount);
					feeTaxCharge.setItemType(roomChargeItemType);
					feeTaxCharge.setItem(tax.getCode());
					itemizedTaxNfeesItemList.add(feeTaxCharge);
				}
			}
		});
		taxNFeesItem.setAmount(
				itemizedTaxNfeesItemList.stream().mapToDouble(ItemizedChargeItem::getAmount).sum());
		taxNFeesItem.setItemized(itemizedTaxNfeesItemList);
		return taxNFeesItem;
	}

    /**
     * 
     * @param chargeItemType
     * @param itemizedTaxNfeesItemList
     * @return
     */
    private ItemizedChargeItem getFeesTaxType(RoomChargeItemType chargeItemType,
            List<ItemizedChargeItem> itemizedTaxNfeesItemList) {
        if (CollectionUtils.isNotEmpty(itemizedTaxNfeesItemList)) {
            return itemizedTaxNfeesItemList.stream()
                    .filter(taxItem -> chargeItemType.equals(taxItem.getItemType()))
					.findFirst()
					.orElse(null);
        } else {
            return null;
        }
    }

	public static List<RoomPrice> acrsSingleToRoomPrice(SuccessfulSingleAvailability crsResponse, Date checkInDate,
														Date checkOutDate) {
		LocalDate startDate = ReservationUtil.convertDateToLocalDate(checkInDate);
		LocalDate endDate = ReservationUtil.convertDateToLocalDate(checkOutDate);
		OfferSingle offer = crsResponse.getData().getOffers().get(0);

		return Stream.iterate(startDate, date -> date.plusDays(1))
				.limit(startDate.until(endDate, ChronoUnit.DAYS))
				.map(localDate -> getRoomPriceFromOfferForDate(localDate, offer))
				.collect(Collectors.toList());
	}

	private static RoomPrice getRoomPriceFromOfferForDate(final LocalDate currentDate, OfferSingle offer) {
		Optional<ProductUseSingle> productUses = offer.getProductUses().stream()
				.filter(productUse -> Objects.nonNull(productUse.getRates()))
				.filter(productUse -> !currentDate.isBefore(productUse.getPeriod().getStart()))
				.filter(productUse -> currentDate.isBefore(productUse.getPeriod().getEnd()))
				.findFirst();
		if (!productUses.isPresent()) {
			log.warn("Unable to price trip from single avail for date: {} with productUses: {}", currentDate,
					offer.getProductUses());
			throw new BusinessException(ErrorCode.UNABLE_TO_PRICE);
		}
		return createRoomPriceFromProductUseForDate(productUses.get(), currentDate,
				offer.getGroupCode());
	}

	private static RoomPrice createRoomPriceFromProductUseForDate(ProductUseSingle productUse,
            LocalDate roomPriceDate, String groupCode) {
        Optional<DailyRateDetailsSingle> dailyRateDetails = productUse.getRates().getDailyRates().stream()
                .filter(rateChange -> !roomPriceDate.isBefore(rateChange.getStart()))
                .filter(rateChange -> roomPriceDate.isBefore(rateChange.getEnd()))
				.findFirst()
                .map(DailyRatesSingle::getDailyTotalRate);

        double amountToPay = -1.0;
        double resortFee = -1.0;
        if (dailyRateDetails.isPresent()) {
			DailyRateDetailsSingle dailyRateDetailsSingle = dailyRateDetails.get();
			amountToPay = Double.parseDouble(dailyRateDetailsSingle.getBsAmt());
            resortFee = getResortFeeFromDailyRateDetail(dailyRateDetailsSingle);
        }

        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setDate(ReservationUtil.convertLocalDateToDate(roomPriceDate));
        roomPrice.setPrice(amountToPay);
        roomPrice.setResortFee(resortFee);
		roomPrice.setProgramId(productUse.getRatePlanCode());
		// if group code there set it as programId instead
		if (null != groupCode) {
			roomPrice.setProgramId(groupCode);
		}

        return roomPrice;
    }

    private double getResortFeeFromDailyRateDetail(DailyRateDetailsSingle dailyRateDetailsSingle) {
        return dailyRateDetailsSingle.getTaxList().stream().filter(tax -> tax.getCode().equals("RSFEE"))
                .map(TaxAmount::getAmt).map(Double::parseDouble).findFirst().orElse(ServiceConstant.ZERO_DOUBLE_VALUE);
    }
}
