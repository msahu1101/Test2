package com.mgm.services.booking.room.transformer;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.TripDetailsV3;
import com.mgm.services.booking.room.model.crs.calendarsearches.*;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Utility class providing functions for reservation object transformations
 * returns auroraPriceResponse; required for API outputs.
 */
@UtilityClass
public class CalendarAvailabilityTransformer {

	public static List<AuroraPriceResponse> transformCalendarAvailability(
			List<SuccessfulCalendarAvailability> crsResponseList, int requestedNumRooms, boolean isPerpetualOffer) {
		final List<AuroraPriceResponse> calendarPricesResponseList = new ArrayList<>();

		crsResponseList.forEach(crsResponse -> {
			final Set<LocalDate> reqDateList = createDateList(crsResponse);

			crsResponse.getData().getHotels().forEach(hotels -> {
				if (crsResponse.getWarnings() != null) {
					handleWarnings(crsResponse.getWarnings());
				}				
				if (hotels.getCalendar() != null) {
					hotels.getCalendar().forEach(calendar -> {

						final Double totalAmount = Double.parseDouble(calendar.getLowestRate().getTtlAmt());
						final ArrayList<Integer> refIds = calendar.getLowestRate().getRefIds();
						calendar.getOffers().forEach(offers -> {
							if (refIds.contains(offers.getId())) {
								LocalDate start = calendar.getStart();
								LocalDate end = calendar.getEnd();

								// for v2 duration is always 1,so there will be always 1 rateplan in
								// DailyApplicableRatePlan
								final String ratePlanCode = !isPerpetualOffer ? offers.getRpCode()
										: offers.getDailyApplicableRatePlan().get(0);
								boolean isAvailable = false;
								for (ProductAvail pdtAvl : offers.getPdtAvl()) {
									final int numberOfAvailableProducts = pdtAvl.getNbAvlPdts() != null
											? pdtAvl.getNbAvlPdts()
											: 0;
									if (numberOfAvailableProducts >= requestedNumRooms) {
										isAvailable = true;
										break;
									}
								}
								while (isAvailable && (start.isBefore(end) || start.isEqual(end))) {
									if (reqDateList.contains(start)) {
										calendarPricesResponseList.add(createAuroraPrice(
												ReservationUtil.convertLocalDateToDate(start), totalAmount,
												ratePlanCode, hotels.getHotel().getPropertyCode(), isPerpetualOffer));
										reqDateList.remove(start);
									}
									start = start.plusDays(1);
								}

							}

						});
					});
					reqDateList.forEach(localDate -> {
						calendarPricesResponseList
								.add(createSoldOutAuroraPriceResponse(ReservationUtil.convertLocalDateToDate(localDate)));
					});
				}

			});
		});
		return calendarPricesResponseList;
	}

	public static List<AuroraPriceResponse> transformCalendarAvailabilityForSoldOut(
			SuccessfulCalendarAvailability crsResponse, int requestedNumRooms, boolean isPerpetualOffer) {
		final List<AuroraPriceResponse> calendarPricesResponseList = new ArrayList<>();
		crsResponse.getData().getHotels().forEach(hotels -> {
				if (crsResponse.getWarnings() != null) {
					handleWarnings(crsResponse.getWarnings());
				}
				if (hotels.getCalendar() != null) {
					hotels.getCalendar().forEach(calendar -> {
						calendar.getOffers().forEach(offers -> {
								LocalDate start = calendar.getStart();
								LocalDate end = calendar.getEnd();

								for (ProductAvail pdtAvl : offers.getPdtAvl()) {
									final int numberOfAvailableProducts = pdtAvl.getNbAvlPdts() != null
											? pdtAvl.getNbAvlPdts()
											: 0;
									if (numberOfAvailableProducts >= requestedNumRooms) {
										int counter = 0;
										while (start.isBefore(end) || start.isEqual(end)) {
											final String ratePlanCode = !isPerpetualOffer ? offers.getRpCode()
													: offers.getDailyApplicableRatePlan().get(counter++);
												AuroraPriceResponse price = createAuroraPrice(
														ReservationUtil.convertLocalDateToDate(start), Double.parseDouble(offers.getTtlAmt()),
														ratePlanCode, hotels.getHotel().getPropertyCode(), isPerpetualOffer);
												price.setRoomTypeId(pdtAvl.getPdtCode());
												calendarPricesResponseList.add(price);
											start = start.plusDays(1);
										}
									}
								}
						});
					});
				}

			});

		return calendarPricesResponseList;
	}

	public static List<AuroraPriceV3Response> transformCalendarAvailabilityV3(
			List<SuccessfulCalendarAvailability> crsResponseList, Map<LocalDate, Double> basePrices,
			int requestedNumRooms, int lengthOfStay, boolean isPerpetualOffer, String baseRatePlan, String requestedRatePlan) {
		return crsResponseList.stream()
				.flatMap(crsResponse ->
			getAuroraPriceV3ResponsesFromSuccessfulCalendarAvailability(basePrices, requestedNumRooms, lengthOfStay, isPerpetualOffer,
					baseRatePlan, requestedRatePlan, crsResponse).stream())
				.collect(Collectors.toList());
	}

	private static List<AuroraPriceV3Response> getAuroraPriceV3ResponsesFromSuccessfulCalendarAvailability(Map<LocalDate, Double> basePrices,
																										   int requestedNumRooms,
																										   int lengthOfStay, boolean isPerpetualOffer,
																										   String baseRatePlan, String requestedRatePlan,
																										   SuccessfulCalendarAvailability crsResponse) {
		final Set<LocalDate> soldOutDates = createDateList(crsResponse);
		List<AuroraPriceV3Response> priceList = crsResponse.getData().getHotels().stream()
				.flatMap(hotels -> getAuroraPriceV3ResponseFromHotelCalendar(basePrices, requestedNumRooms,
						lengthOfStay, isPerpetualOffer, baseRatePlan, requestedRatePlan, crsResponse, soldOutDates,
						hotels).stream())
				.collect(Collectors.toList());

		// Add `SoldOut` AuroraPriceResponse for dates not present in the response
		// the set soldOutDates contains dates in the crsResponse date range that had no price represented
		priceList.addAll(soldOutDates.stream()
				.map(localDate -> createAuroraPriceSoldOutV3(ReservationUtil.convertLocalDateToDate(localDate)))
				.collect(Collectors.toList()));

		return priceList;
	}

	private static List<AuroraPriceV3Response> getAuroraPriceV3ResponseFromHotelCalendar(Map<LocalDate, Double> basePrices,
																int requestedNumRooms,
																  int lengthOfStay, boolean isPerpetualOffer, String baseRatePlan,
																  String requestedRatePlan, SuccessfulCalendarAvailability crsResponse,
																  Set<LocalDate> soldOutDatesMutable, HotelCalendar hotels) {
		List<AuroraPriceV3Response> response = new ArrayList<>();
		if (crsResponse.getWarnings() != null) {
			handleWarnings(crsResponse.getWarnings());
		}

		final List<RatePlanCalendar> ratePlans = hotels.getRatePlans();
		final Set<String> compRatePlans = getCompRatePlans(ratePlans);
		final String promoCode = CollectionUtils.isNotEmpty(ratePlans) ? ratePlans.get(0).getPromoCode() :
				null;
		final String promoRatePlanCode = StringUtils.isNotEmpty(promoCode) ? ratePlans.get(0).getCode() : null;
		if(null != hotels.getCalendar()) {
			response = hotels.getCalendar().stream()
					.flatMap(calendar ->
							getAuroraPriceV3ResponsesFromOfferPlanning(basePrices, requestedNumRooms, lengthOfStay, isPerpetualOffer,
									baseRatePlan, requestedRatePlan, soldOutDatesMutable, promoCode, promoRatePlanCode,
									calendar, compRatePlans).stream())
					.collect(Collectors.toList());
		}
		return response;
	}

	private static List<AuroraPriceV3Response> getAuroraPriceV3ResponsesFromOfferPlanning(Map<LocalDate, Double> basePrices,
																						  int requestedNumRooms,
																						  int lengthOfStay,
																						  boolean isPerpetualOffer,
																						  String baseRatePlan,
																						  String requestedRatePlan,
																						  Set<LocalDate> soldOutDatesMutable,
																						  String promoCode,
																						  String promoRatePlanCode,
																						  OfferPlanning calendar,
																						  Set<String> compRatePlans) {
		List<AuroraPriceV3Response> priceList = new ArrayList<>();
		final OfferCalendar minOffer = getMinOffer(requestedNumRooms, promoCode, promoRatePlanCode, requestedRatePlan
				, calendar, compRatePlans);
		if (null != minOffer) {
			LocalDate start = calendar.getStart();
			final LocalDate end = calendar.getEnd();
			while (!start.isAfter(end)) {
				priceList.add(createAuroraPriceV3(minOffer, basePrices, start, promoRatePlanCode,
						getRoomType(requestedNumRooms, minOffer), lengthOfStay, isPerpetualOffer, baseRatePlan, promoCode,
						requestedRatePlan, compRatePlans));
				soldOutDatesMutable.remove(start);
				start = start.plusDays(1);
			}
		}
		return priceList;
	}

	private Set<String> getCompRatePlans(List<RatePlanCalendar> calendarRatePlans) {
		if(null == calendarRatePlans) {
			return new HashSet<>();
		}
		return calendarRatePlans.stream()
				.filter(ratePlanCalendar -> Boolean.TRUE.equals(ratePlanCalendar.isAreAmtsConfidential()))
				.map(RatePlanCalendar::getCode)
				.collect(Collectors.toSet());
	}

	private static String getRoomType(int requestedNumRooms, OfferCalendar minOffer) {
		for (ProductAvail pdtAvl : minOffer.getPdtAvl()) {
			final int numberOfAvailableProducts = pdtAvl.getNbAvlPdts() != null
					? pdtAvl.getNbAvlPdts()
					: 0;
			if (numberOfAvailableProducts >= requestedNumRooms) {
				return pdtAvl.getPdtCode();
			}
		}
		return null;
	}

	private static OfferCalendar getMinOffer(int requestedNumRooms, String promoCode, String promoRatePlanCode,
											 String selectedRatePlan, OfferPlanning calendar, Set<String> compRatePlans) {
		OfferCalendar minOffer = null;
		final List<Integer> refIds = calendar.getLowestRate().getRefIds();
		final List<OfferCalendar> offers = calendar.getOffers();

		// Find min Promo Offer
		if (StringUtils.isNotEmpty(promoCode)) {
			minOffer = getMinPromoOffer(requestedNumRooms, promoRatePlanCode, offers, compRatePlans);
		}

		// if selectedRatePlan is provided
        if (null == minOffer && StringUtils.isNotEmpty(selectedRatePlan)) {
			minOffer = getMinOfferFromSelectedRatePlan(requestedNumRooms, selectedRatePlan, offers, compRatePlans);
		}

		// Find min general offer
		if (null == minOffer) {
			minOffer = getMinGeneralOffer(requestedNumRooms, compRatePlans, refIds, offers);
		}

		return minOffer;
	}

	private static OfferCalendar getMinGeneralOffer(int requestedNumRooms, Set<String> compRatePlans,
													List<Integer> lowestRateRefIds, List<OfferCalendar> offers) {
		// First check for comp rate plan offers
		if ( CollectionUtils.isNotEmpty(compRatePlans)) {
			Optional<OfferCalendar> compOfferPresent = offers.stream()
					.filter(offer -> compRatePlans.contains(offer.getRpCode()))
					.filter(offer -> isInventoryAvailable(requestedNumRooms, offer))
					.findFirst();
			if (compOfferPresent.isPresent()) {
				return compOfferPresent.get();
			}
		}

		// No comp found with inventory, search all remaining offers in the lowest priced lowestRateRefIds for available
		// inventory
		Optional<OfferCalendar> offerPresent = offers.stream()
				.filter(offer -> lowestRateRefIds.contains(offer.getId()))
				.filter(offer -> isInventoryAvailable(requestedNumRooms, offer))
				.findFirst();
		return offerPresent.orElse(null);
	}

	private static OfferCalendar getMinOfferFromSelectedRatePlan(int requestedNumRooms, String selectedRatePlan,
																 List<OfferCalendar> offers,
																 Set<String> compRatePlans) {
		OfferCalendar minOffer = null;
		List<OfferCalendar> selectedOffers = offers.stream()
				.filter(offer -> StringUtils.equalsIgnoreCase(selectedRatePlan, offer.getRpCode())
						&& isInventoryAvailable(requestedNumRooms, offer))
				.collect(Collectors.toList());

		// If selectedRatePlan is considered comp then just return the first available offer with inventory.
		if(compRatePlans.contains(selectedRatePlan) && !selectedOffers.isEmpty()) {
			return selectedOffers.get(0);
		}

		for (OfferCalendar selectedOffer : selectedOffers) {
			double currentTtlAmt = Double.parseDouble(selectedOffer.getTtlAmt());
			if (minOffer == null || currentTtlAmt < Double.parseDouble(minOffer.getTtlAmt())) {
				minOffer = selectedOffer;
			}
		}
		return minOffer;
	}

	private static OfferCalendar getMinPromoOffer(int requestedNumRooms, String promoRatePlanCode,
												  List<OfferCalendar> offers, Set<String> compRatePlans) {
		OfferCalendar minOffer = null;
		boolean isPromoRatePlanCodeComp = compRatePlans.contains(promoRatePlanCode);
		double minOfferTotalAmount = -1.0;
		for (OfferCalendar offer : offers) {
			String rpCode = StringUtils.isNotEmpty(offer.getRpCode()) ? offer.getRpCode() : null;
			if (CollectionUtils.isNotEmpty(offer.getDailyApplicableRatePlan())) {
				rpCode = offer.getDailyApplicableRatePlan().get(0);
			}
			if (StringUtils.equalsIgnoreCase(rpCode, promoRatePlanCode)) {
				double currentTtlAmt = Double.parseDouble(offer.getTtlAmt());

				if (isPromoRatePlanCodeComp && isInventoryAvailable(requestedNumRooms, offer)) {
					// if comp and has enough inventory short circuit search
					minOffer = offer;
					break;
				} else if ((minOffer == null || currentTtlAmt < minOfferTotalAmount)
							&& isInventoryAvailable(requestedNumRooms, offer)) {
						minOffer = offer;
						minOfferTotalAmount = currentTtlAmt;
				}
			}
		}
		return minOffer;
	}

	private static boolean isInventoryAvailable(int requestedNumRooms, OfferCalendar offer) {
		if (null != offer && CollectionUtils.isNotEmpty(offer.getPdtAvl())) {
			for (ProductAvail pdtAvl : offer.getPdtAvl()) {
				final Integer nbAvlPdts = pdtAvl.getNbAvlPdts();
				final int numberOfAvailableProducts = nbAvlPdts != null ? nbAvlPdts : 0;
				if (numberOfAvailableProducts >= requestedNumRooms) {
					return true;
				}
			}
		}
		return false;
	}

	public static Map<LocalDate, Double> transformCalendarBasePrice(
			List<SuccessfulCalendarAvailability> crsResponseList, String baseRatePlanCode) {

		final Map<LocalDate, Double> basePrices = new HashMap<>();

		crsResponseList.forEach(crsResponse -> {
			crsResponse.getData().getHotels().forEach(hotels -> {
				if (crsResponse.getWarnings() != null) {
					handleWarnings(crsResponse.getWarnings());
				}
				if (hotels.getCalendar() != null) {

					hotels.getCalendar().forEach(calendar -> {
						final LocalDate start = calendar.getStart();
						final LocalDate end = calendar.getEnd();

						calendar.getOffers().forEach(offer -> {
							if (StringUtils.equalsIgnoreCase(baseRatePlanCode, offer.getRpCode())) {
								final Double lastPrice = basePrices.get(start);
								final Double currentPrice = Double.valueOf(offer.getTtlAmt());
								if (null == lastPrice || lastPrice > currentPrice) {
									basePrices.put(start, currentPrice);
								}
							}
						});

						final Double lastPrice = basePrices.get(start);
						LocalDate currentStartDate = start.plusDays(1);
						while (!currentStartDate.isAfter(end)) {
							basePrices.put(currentStartDate, lastPrice);
							currentStartDate = currentStartDate.plusDays(1);
						}
					});
				}
			});
		});

		return basePrices;

	}

	private static void handleWarnings(List<Warning> warnings) {
		warnings.forEach(warning -> {
			if (warning.getCode() == 50028) {
				throw new BusinessException(ErrorCode.SYSTEM_ERROR);
			} else if (warning.getCode() == 50038) {
				throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
			}
		});
	}

	private Set<LocalDate> createDateList(SuccessfulCalendarAvailability crsResponse) {
		Set<LocalDate> dateList = new HashSet<>();
		LocalDate requestStartDate = crsResponse.getData().getStart();
		LocalDate requestEndDate = crsResponse.getData().getEnd();

		while (!requestStartDate.isAfter(requestEndDate)) {
			dateList.add(requestStartDate);
			requestStartDate = requestStartDate.plusDays(1);
		}

		return dateList;
	}

	private static AuroraPriceResponse createSoldOutAuroraPriceResponse(Date from) {
		final AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
		auroraPriceResponse.setStatus(AvailabilityStatus.SOLDOUT);
		auroraPriceResponse.setDate(from);
		auroraPriceResponse.setDiscountedPrice(-1);

		return auroraPriceResponse;
	}

	private static AuroraPriceResponse createAuroraPrice(Date datetoSet, Double totalAmount, String ratePlanCode,
			String propertyCode, boolean isPerpetualOffer) {
		final AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
		auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
		auroraPriceResponse.setDate(datetoSet);
		auroraPriceResponse.setDiscountedPrice(totalAmount);
		auroraPriceResponse.setProgramId(ratePlanCode);
		auroraPriceResponse.setPropertyId(propertyCode);
		auroraPriceResponse.setComp(BaseAcrsTransformer.checkIfCompByString(ratePlanCode));
		auroraPriceResponse.setPOApplicable(isPerpetualOffer);
		return auroraPriceResponse;
	}


	private static AuroraPriceV3Response createAuroraPriceSoldOutV3(Date from) {
		final AuroraPriceV3Response auroraPriceResponse = new AuroraPriceV3Response();
		auroraPriceResponse.setStatus(AvailabilityStatus.SOLDOUT);
		auroraPriceResponse.setDate(from);
		auroraPriceResponse.setTotalNightlyTripPrice(-1.0);
		auroraPriceResponse.setTotalNightlyTripBasePrice(-1.0);
		return auroraPriceResponse;
	}

	private static AuroraPriceV3Response createAuroraPriceV3(OfferCalendar offer, Map<LocalDate, Double> basePrices,
															 LocalDate start, String promoRatePlanCode,
															 String roomType, int lengthOfStay,
															 boolean isPerpetualOffer, String baseRatePlan,
															 String promoCode, String requestedRatePlan,
															 Set<String> compRatePlans) {
		final AuroraPriceV3Response auroraPriceResponse = new AuroraPriceV3Response();
		auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
		auroraPriceResponse.setDate(ReservationUtil.convertLocalDateToDate(start));
		auroraPriceResponse.setRoomTypeId(roomType);
		boolean isComp = compRatePlans.contains(offer.getRpCode());

		List<TripDetailsV3> listOfTripDetails = new ArrayList<>();
		for (int i = 0; i < lengthOfStay; i++) {
			TripDetailsV3 tripDetails = new TripDetailsV3();
			tripDetails.setDate(ReservationUtil.convertLocalDateToDate(start.plusDays(i)));
			if (isPerpetualOffer) {
				tripDetails.setProgramId(offer.getDailyApplicableRatePlan().get(i));
				isComp = BaseAcrsTransformer.checkIfCompByString(offer.getDailyApplicableRatePlan().get(i));
			} else {
				tripDetails.setProgramId(offer.getRpCode());
			}
			tripDetails.setComp(isComp);
			listOfTripDetails.add(tripDetails);
		}
		boolean isPerpetualPricing = isPerpetualPricing(offer.getDailyApplicableRatePlan());
		if (isPerpetualPricing) {
			auroraPriceResponse.setPOApplicable(true);
			auroraPriceResponse.setPricingMode(PricingModes.PERPETUAL);
		} else if (baseRatePlan.equals(offer.getRpCode())
				|| (StringUtils.isNotBlank(requestedRatePlan)
				&& !offer.getRpCode().equals(requestedRatePlan))) {
			auroraPriceResponse.setPricingMode(PricingModes.BEST_AVAILABLE);
		}else {
			auroraPriceResponse.setPricingMode(PricingModes.PROGRAM);
		}

		auroraPriceResponse.setTripDetails(listOfTripDetails);
		double totalNightlyTripPrice = 0.0d;
		if(!isComp) {
			totalNightlyTripPrice = Double.parseDouble(offer.getTtlAmt());
		}
		auroraPriceResponse.setTotalNightlyTripPrice(totalNightlyTripPrice);
		final Double totalBasePrice = basePrices.get(start);
		auroraPriceResponse.setTotalNightlyTripBasePrice(null != totalBasePrice ? totalBasePrice : 0);

		String rpCode = StringUtils.isNotEmpty(offer.getRpCode()) ? offer.getRpCode() : null;
		if (CollectionUtils.isNotEmpty(offer.getDailyApplicableRatePlan())) {
			rpCode = offer.getDailyApplicableRatePlan().get(0);
		}
		if (StringUtils.equalsIgnoreCase(rpCode, promoRatePlanCode)) {
			auroraPriceResponse.setPromo(promoCode);
		}
		
		return auroraPriceResponse;
	}

	private static boolean isPerpetualPricing(List<String> getDailyApplicableRatePlans) {
		return getDailyApplicableRatePlans != null && getDailyApplicableRatePlans.stream()
				.anyMatch(ratePlans -> ratePlans.startsWith(ServiceConstant.COMP_STRING)
						|| ratePlans.startsWith(ServiceConstant.CASH_STRING)
						|| ratePlans.startsWith(ServiceConstant.CASH_POWER_RANK_STRING));
	}
}