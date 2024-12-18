package com.mgm.services.booking.room.transformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.ComponentPrices;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.crs.searchoffers.AdditionalProductRate.PricingFrequencyEnum;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.DepositPolicy;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.util.DateUtil;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;

@UtilityClass
@Log4j2
public class RoomAndComponentChargesTranformer {
		
	/**
	 * This will convert SuccessfulPricing to RoomReservation
	 * 
	 * @param body
	 * @return
	 */
	public static RoomReservation getRoomAndComponentChargesResponse(SuccessfulPricing body, int numRooms, AcrsProperties acrsProperties) {
		RoomReservation roomReservation = new RoomReservation();
		DataRsPricing data = body.getData();
		OfferPricing offer = data.getRequestedOffer();
		String ratePlanCode = null;
		if(null != offer) {
			ratePlanCode = offer.getRatePlanCode();
			ProductUsePricing productUse = offer.getProductUses().get(0);
			String los = data.getDuration().substring(0, data.getDuration().length() - 1);
			LocalDate endDate = data.getStart().plusDays(Long.parseLong(los));
			// set room charges
			// depositPolicy
			Policies policy = offer.getPolicies();
			if (null != policy) {
				DepositPolicy depositPolicyCalc = new DepositPolicy();
				depositPolicyCalc.setCreditCardRequired(
						null != policy.getGuarantee() && policy.getGuarantee().getIsRequired());
				depositPolicyCalc
						.setDepositRequired(null != policy.getDeposit() && policy.getDeposit().getIsRequired());
				roomReservation.setDepositPolicyCalc(depositPolicyCalc);
			}
			// depositDetails
			roomReservation.setDepositCalc(createAcrsDepositCal(offer));
			List<RatePlanPricing> dataRatePlans = data.getRatePlans();
			// chargesAndTaxes
			roomReservation.setChargesAndTaxesCalc(getRoomChargesAndTaxes(offer, data.getStart(), endDate, acrsProperties
					, data.getHotel().getPropertyCode(), dataRatePlans));
			// bookings
			roomReservation.setBookings(getRoomPricesFromOfferPricing(offer, dataRatePlans, data.getStart(), endDate));
			// tripDetails
			roomReservation.setCheckInDate(DateUtil.toDate(data.getStart()));
			roomReservation.setCheckOutDate(DateUtil.toDate(endDate));
			Optional<GuestCounts> aqc10 = productUse.getGuestCounts().stream()
					.filter(guest -> ServiceConstant.NUM_ADULTS_MAP.equalsIgnoreCase(guest.getOtaCode())).findFirst();
			if (aqc10.isPresent()) {
				roomReservation.setNumAdults(aqc10.get().getCount());
			}
			Optional<GuestCounts> aqc8 = productUse.getGuestCounts().stream()
					.filter(guest -> ServiceConstant.NUM_CHILD_MAP.equalsIgnoreCase(guest.getOtaCode())).findFirst();
			if (aqc8.isPresent()) {
				roomReservation.setNumChildren(aqc8.get().getCount());
			}
			roomReservation.setNumRooms(productUse.getQuantity());
			// programId
			roomReservation.setProgramId(ratePlanCode);
			// roomTypeId
			roomReservation.setRoomTypeId(productUse.getInventoryTypeCode());
    	}
        //inc-4  promo code
		roomReservation.setPromo(getPromoCodeFromRatePlanCode(data.getRatePlans(), ratePlanCode));

        // propertyId
        roomReservation.setPropertyId(data.getHotel().getPropertyCode());
		// set component charges
		roomReservation.setAvailableComponents(getACRSComponentPrices(data,numRooms));
		return roomReservation;
	}

	private static String getPromoCodeFromRatePlanCode(List<RatePlanPricing> ratePlans, String ratePlanCode) {
		if (CollectionUtils.isEmpty(ratePlans)) {
			log.warn("Unable to find promo code from empty ratePlans collection in searchOffers response.");
			return null;
		}
		if (StringUtils.isEmpty(ratePlanCode)) {
			log.warn("RatePlanCode is null or empty when trying to find Promo code in searchOffers response.");
			return null;
		}

		Optional<String> promoCodeOptional = ratePlans.stream()
				.filter(ratePlanPricing -> ratePlanCode.equalsIgnoreCase(ratePlanPricing.getCode()))
				.map(RatePlanPricing::getPromoCode)
				.filter(Objects::nonNull)
				.findFirst();

		return promoCodeOptional.orElse(null);
	}

	private List<RoomBookingComponent> getACRSComponentPrices(DataRsPricing data, int numOfRooms) {
        List<RoomBookingComponent> roomBookingComponents = new ArrayList<>();
		final Map<String, BaseAcrsTransformer.AddonComponentRateInfo> ratePlansMap = new HashMap<>();
		ratePlansMap.putAll(BaseAcrsTransformer.getRatePlans(data.getRatePlans()));

		if (CollectionUtils.isNotEmpty(data.getAdditionalOffers())) {
            data.getAdditionalOffers().forEach(offer -> {
                RoomBookingComponent roomBookingComponent = new RoomBookingComponent();
                roomBookingComponents.add(roomBookingComponent);
                if(null != offer.getPolicies() && null != offer.getPolicies().getDeposit()) {
                    roomBookingComponent.setDepositAmount(Double.parseDouble(offer.getPolicies().getDeposit().getAmt()));
                    roomBookingComponent.setIsDespsitRequired(offer.getPolicies().getDeposit().getIsRequired());  
                }else{
                    roomBookingComponent.setDepositAmount(0);
                    roomBookingComponent.setIsDespsitRequired(false);
                }

                String inventoryTypeCode = offer.getProductUses().get(0).getInventoryTypeCode();
                final String componentId = ACRSConversionUtil.createComponentGuid(inventoryTypeCode,
                		offer.getRatePlanCode(), ServiceConstant.COMPONENT_FORMAT, data.getHotel().getPropertyCode());
                roomBookingComponent.setId(componentId);
				if (offer.getRatePlanCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1)
						|| offer.getRatePlanCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_HDN)) {
					roomBookingComponent.setCode(offer.getRatePlanCode());
				} else {
					roomBookingComponent.setCode(inventoryTypeCode);
				}
				//CBSR-1906 set shortDescription and long desc from ACRS rate plan
				BaseAcrsTransformer.AddonComponentRateInfo rateInfo = ratePlansMap.get(offer.getRatePlanCode());
				if(null != rateInfo) {
					roomBookingComponent.setShortDescription(rateInfo.getShortDesc());
					roomBookingComponent.setLongDescription(rateInfo.getLongDesc());
				}
                roomBookingComponent.setActive(true);
                AdditionalProductRate rate = offer.getProductUses().get(0).getRates();
                PricingFrequencyEnum pricingFrequency = rate.getPricingFrequency();
                if (pricingFrequency == AdditionalProductRate.PricingFrequencyEnum.PERNIGHT) {
                    roomBookingComponent.setPricingApplied(ServiceConstant.NIGHTLY_PRICING_APPLIED);
                } else if (pricingFrequency == AdditionalProductRate.PricingFrequencyEnum.PERSTAY) {
                    roomBookingComponent.setPricingApplied(ServiceConstant.CHECKIN_PRICING_APPLIED);
                } else {
                    // per use
                    if (rate.getBookingPattern() == AdditionalProductRate.BookingPatternEnum.DAYOFCHECKIN) {
                        roomBookingComponent.setPricingApplied(ServiceConstant.CHECKIN_PRICING_APPLIED);
                    } else {
                        roomBookingComponent.setPricingApplied(ServiceConstant.CHECKOUT_PRICING_APPLIED);
                    }
                }

                // set roomBookingComponent.prices
                ComponentPrices prices = new ComponentPrices();
                LocalDate currentDate = data.getStart();
                LocalDate lastNight = offer.getProductUses().get(0).getPeriod().getEnd();
                // endDate is checkout date so we don't need to create a
                // roomPrice
                // for
                // that day.
                lastNight = lastNight.plusDays(-1);
                while (!currentDate.isAfter(lastNight)) {
                    LocalDate finalCurrentDate = currentDate;
                    AdditionalDailyRates additionalDailyRates = offer.getProductUses().stream()
                            .filter(productUse -> !finalCurrentDate.isBefore(productUse.getPeriod().getStart())
                                    && finalCurrentDate.isBefore(productUse.getPeriod().getEnd()))
                            .collect(Collectors.toList()).stream()
                            .flatMap(productUse1 -> productUse1.getRates().getDailyRates().stream())
                            .filter(rateChange -> !finalCurrentDate.isBefore(rateChange.getStart())
                                    && finalCurrentDate.isBefore(rateChange.getEnd()))
                            .findFirst().orElse(null);
                    ComponentPrice price = new ComponentPrice();
                    price.setDate(DateUtil.toDate(currentDate));
                    if (null != additionalDailyRates && null != additionalDailyRates.getDailyTotalRate()) {
                        price.setAmount(Double.parseDouble(additionalDailyRates.getDailyTotalRate().getBsAmt()) * numOfRooms);
                        String totaltax = additionalDailyRates.getDailyTotalRate().getTotalTaxes();
                        price.setTax((Double.parseDouble(null != totaltax ? totaltax : "0") * numOfRooms));
						prices.add(price);
					}

                    currentDate = currentDate.plusDays(1);
                }

                roomBookingComponent.setPrices(prices);
            });
        }
        return roomBookingComponents;
    }

    public static List<RoomPrice> getRoomPricesFromOfferPricing(OfferPricing offer, List<RatePlanPricing> ratePlans,
																LocalDate startDate, LocalDate endDate) {
		List<LocalDate> datesToProcess = Stream.iterate(startDate, localDate -> localDate.plusDays(1))
				.limit(ChronoUnit.DAYS.between(startDate, endDate))
				.collect(Collectors.toList());

		return datesToProcess.stream()
				.map(currentDate -> getRoomPriceFromOfferPricingForDate(offer, ratePlans, currentDate))
				.collect(Collectors.toList());
    }

	private static RoomPrice getRoomPriceFromOfferPricingForDate(OfferPricing offer, List<RatePlanPricing> ratePlans,
																 final LocalDate currentDate){
		String ratePlanCode = offer.getRatePlanCode();
		List<ProductUsePricing> dateRelevantProductUses = offer.getProductUses().stream()
				.filter(ProductUsePricing::isIsMainProduct)
				.filter(productUse1 -> !currentDate.isBefore(productUse1.getPeriod().getStart())
						&& currentDate.isBefore(productUse1.getPeriod().getEnd()))
				.collect(Collectors.toList());

		if (dateRelevantProductUses.size() != 1) {
			// log error
			log.error("Expected single productUse for date {}, instead found {} productUses: {}",
					currentDate, dateRelevantProductUses.size(), dateRelevantProductUses);
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}
		ProductUsePricing dateRelevantProductUse = dateRelevantProductUses.get(0);

		Optional<DailyRatesPricing> dailyRateDetails = dateRelevantProductUse.getRates().getDailyRates().stream()
				.filter(rateChange -> !currentDate.isBefore(rateChange.getStart())
						&& currentDate.isBefore(rateChange.getEnd()))
				.findFirst();

		boolean isComp = BaseAcrsTransformer.checkIfCompUsingRatePlanPricing(ratePlanCode, ratePlans);
		double baseAmount = isComp ? 0.0 : -1.0;
		if (dailyRateDetails.isPresent() && !isComp) {
			baseAmount = getBaseAmount(dateRelevantProductUse, dailyRateDetails.get());
		}
		RoomPrice roomPrice = new RoomPrice();
		roomPrice.setDate(ReservationUtil.convertLocalDateToDate(currentDate));
		roomPrice.setBasePrice(baseAmount);
		roomPrice.setPrice(baseAmount);
		roomPrice.setProgramId(ratePlanCode);
		roomPrice.setComp(isComp);

		return roomPrice;
	}

	private static double getBaseAmount(ProductUsePricing dateRelevantProductUse, DailyRatesPricing dailyRateDetails) {
		String baseAmountString = dailyRateDetails.getDailyTotalRate().getBsAmt();
		double baseAmount = Double.parseDouble(baseAmountString);
		int numOfRooms = Optional.ofNullable(dateRelevantProductUse.getQuantity()).orElse(1);
		if (numOfRooms > 1) {
			BigDecimal baseAmountBigDecimal = new BigDecimal(baseAmountString);
			baseAmount = baseAmountBigDecimal.divide(new BigDecimal(numOfRooms), 2, RoundingMode.HALF_UP).doubleValue();
		}
		return baseAmount;
	}

	public static RoomChargesAndTaxes getRoomChargesAndTaxes(OfferPricing offer, LocalDate startDate,
															 LocalDate endDate, AcrsProperties acrsProperties,
															 String propertyCode, List<RatePlanPricing> ratePlans) {
		List<LocalDate> datesToProcess = Stream.iterate(startDate, localDate -> localDate.plusDays(1))
				.limit(ChronoUnit.DAYS.between(startDate, endDate))
				.collect(Collectors.toList());

		// Collect RoomChargesAndTaxes per date into list of RoomChargesAndTaxes
		List<RoomChargesAndTaxes> roomChargesAndTaxesPerDate = datesToProcess.stream()
				.map(currentDate -> getRoomChargesAndTaxesForDate(offer.getProductUses(), acrsProperties,
						propertyCode, currentDate, ratePlans, offer.getRatePlanCode()))
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

	private static RoomChargesAndTaxes getRoomChargesAndTaxesForDate(List<ProductUsePricing> productUsePricingList,
																	 AcrsProperties acrsProperties,
																	 String propertyCode, final LocalDate currentDate,
																	 List<RatePlanPricing> ratePlans,
																	 String ratePlanCode) {
		RoomChargesAndTaxes roomChargesAndTaxes = new RoomChargesAndTaxes();
		List<ProductUsePricing> dateRelevantProductUses = productUsePricingList.stream()
				.filter(ProductUsePricing::isIsMainProduct)
				.filter(productUse -> !currentDate.isBefore(productUse.getPeriod().getStart()) &&
						currentDate.isBefore(productUse.getPeriod().getEnd()))
				.collect(Collectors.toList());
		if (dateRelevantProductUses.size() != 1) {
			log.warn("Expected that there is only a single productUse for date when calculating charges and taxes. " +
					"Instead found: {}", dateRelevantProductUses);
		}
		ProductUsePricing dateRelevantProductUse = dateRelevantProductUses.get(0);
		Optional<DailyRatesPricing> dailyRateDetails = dateRelevantProductUse.getRates().getDailyRates().stream()
				.filter(rateChange -> !currentDate.isBefore(rateChange.getStart())
						&& currentDate.isBefore(rateChange.getEnd()))
				.findFirst();
		if (dailyRateDetails.isPresent()) {
			boolean isComp = BaseAcrsTransformer.checkIfCompUsingRatePlanPricing(ratePlanCode, ratePlans);
			// charge
			RoomChargeItem roomChargeItem = getRoomChargeItemForDate(acrsProperties, propertyCode,
					currentDate, dailyRateDetails.get(), isComp);
			roomChargesAndTaxes.setCharges(Collections.singletonList(roomChargeItem));

			// tax and fees
			RoomChargeItem taxNFeesItem = getTaxRoomChargeItemForDate(acrsProperties, propertyCode,
					currentDate, dailyRateDetails.get(), isComp);
			roomChargesAndTaxes.setTaxesAndFees(Collections.singletonList(taxNFeesItem));
		}
		return roomChargesAndTaxes;
	}

	private static RoomChargeItem getTaxRoomChargeItemForDate(AcrsProperties acrsProperties, String propertyCode,
															  LocalDate finalCurrentDate,
															  DailyRatesPricing dailyRatesPricing, boolean isComp) {
		return RoomReservationChargesTransformer.getTaxRoomChargeItemForDate(acrsProperties, propertyCode,
				finalCurrentDate,
				transform(dailyRatesPricing), isComp);
	}

	private static RoomChargeItem getRoomChargeItemForDate(AcrsProperties acrsProperties, String propertyCode,
											  LocalDate finalCurrentDate, DailyRatesPricing dailyRateDetails,
														   boolean isComp) {
		return RoomReservationChargesTransformer.getRoomChargeItemForDate(acrsProperties, propertyCode,
				finalCurrentDate, transform(dailyRateDetails), isComp);
	}

	private DailyRatesSingle transform(DailyRatesPricing dailyRatesPricing) {
		DailyRatesSingle dailyRatesSingle = new DailyRatesSingle();
		dailyRatesSingle.setStart(dailyRatesPricing.getStart());
		dailyRatesSingle.setEnd(dailyRatesPricing.getEnd());
		dailyRatesSingle.isPrimaryRate(dailyRatesPricing.getIsPrimaryRate());
		dailyRatesSingle.setDailyTotalRate(dailyRatesPricing.getDailyTotalRate());
		// Note: At time of writing (Search_Offer version 5.3.2) DailyRatesSingle is missing the following fields
		// from DailyRatesPricing: dailyBaseOccRate & dailyExtraOccRate. We don't currently use these fields in our
		// integration.
		return dailyRatesSingle;
	}

    public static Deposit createAcrsDepositCal(OfferPricing offer) {
        Deposit depositCalc = new Deposit();
        Policies offerPolicies = offer.getPolicies();
        if (null != offerPolicies) {
             com.mgm.services.booking.room.model.crs.searchoffers.Deposit depositPolicy = offerPolicies.getDeposit();
            if (null != depositPolicy) {
                double depositCalcAmount = Double.parseDouble(depositPolicy.getAmt());
                depositCalc.setAmount(depositCalcAmount);
                 depositCalc.setDueDate(
                        CommonUtil.getDate(depositPolicy.getDeadline().getDateTime(),
                                ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
            }
             CancellationNoShow cancellation = offerPolicies.getCancellationNoShow();
            if (null != cancellation) {
                depositCalc.setForfeitAmount(
                        Double.parseDouble(cancellation.getAmt()));
                depositCalc.setForfeitDate(CommonUtil.getDate(
                        cancellation.getDeadline().getDateTime(),
                        ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
            }
            depositCalc.setForfeitDate(
                    (depositCalc.getForfeitDate() != null) ? depositCalc.getForfeitDate() : depositCalc.getDueDate());
        }
        return depositCalc;
    }
}
