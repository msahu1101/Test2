package com.mgm.services.booking.room.transformer;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.crs.groupretrieve.GroupProductUseResGroupBookingReservation;
import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.util.DateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.TripDetailsV3;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.util.ReservationUtil;

import lombok.experimental.UtilityClass;

/**
 * Utility class providing functions for reservation object transformations
 * required for API outputs.
 *
 */
@UtilityClass
public class RoomAvailabilityTransformer {
	/**
	 * Transforms ACRS SingleAvailability object into AuroraPriceResponse
	 * 
	 * @param crsResponse ACRS SingleAvailability object
	 * @return List of AuroraPriceResponse
	 */
	public static List<AuroraPriceResponse> transform(SuccessfulSingleAvailability crsResponse, AuroraPriceRequest priceRequest,
			String basePriceRatePlan, boolean isPerpetualOffer, SuccessfulSingleAvailability basePriceCrsResponse) {
		List<AuroraPriceResponse> auroraPriceResponseList = new ArrayList<>();

		// for group response if AvailStatus= NOTAVAILABLE, return empty response
		if (crsResponse.getData().getGroupContract() != null
				&& crsResponse.getData().getGroupContract().getAvailStatus() != null
				&& crsResponse.getData().getGroupContract().getAvailStatus().equals(AvailStatus.NOTAVAILABLE)) {
			return auroraPriceResponseList;
		}

		// get PropertyId from crsResponse
		final String propertyId = crsResponse.getData().getHotel().getPropertyCode();
		
		final Map<String, Double> acrsBasePriceRoomTypeMapForComp = getAcrsBasePriceRoomTypeMapForNonPOFlow(basePriceCrsResponse,false);
		
		Map<String, Double> acrsBasePriceRoomTypeMap = getAcrsBasePriceRoomTypeMap(crsResponse, basePriceCrsResponse, basePriceRatePlan, isPerpetualOffer);
		

        // Creating maps to avoid repeated streaming later
        Map<String, String> ratePlanPromoMap = new HashMap<>();
        Map<String, Boolean> isRatePlanCompMap = new HashMap<>();
        Map<String, Boolean> isRatePlanPOMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(crsResponse.getData().getRatePlans())) {
            for (RatePlanSingle ratePlan : crsResponse.getData().getRatePlans()) {
                String ratePlanCode = ratePlan.getCode();
                boolean gamingBucketCompFlag = false;
                boolean gamingBucketPOFlag = false;
                boolean isPerpetualOfferFlag = Boolean.TRUE.equals(priceRequest.isPerpetualOffer());
				boolean areAmtsConfidential = Boolean.TRUE.equals(ratePlan.isAreAmtsConfidential());
                
                if(ratePlan.getGamingBucket() != null) {
                    gamingBucketCompFlag = ServiceConstant.COMP.equalsIgnoreCase(ratePlan.getGamingBucket());
                    gamingBucketPOFlag = ServiceConstant.COMP.equalsIgnoreCase(ratePlan.getGamingBucket()) || ServiceConstant.CASH.equalsIgnoreCase(ratePlan.getGamingBucket());
                }

                // RatePlan -> Promo mapping
                ratePlanPromoMap.put(ratePlanCode, ratePlan.getPromoCode());
                // RatePlan -> isComp mapping
                isRatePlanCompMap.put(ratePlanCode, gamingBucketCompFlag || areAmtsConfidential);
                // RatePlan -> isPo mapping
                isRatePlanPOMap.put(ratePlanCode, gamingBucketPOFlag );
            }
        }

		// Get AuroraPriceResponse objects from crsResponse
		crsResponse.getData().getOffers().forEach(offer -> {
			offer.getProductUses().forEach(productUse -> {
				if (null != productUse.getRates()) {
					productUse.getRates().getDailyRates().forEach(rate -> {

						final String offerRatePlanCode = offer.getRatePlanCode() != null ? offer.getRatePlanCode() : offer.getGroupCode();
                        final String ratePlanCode = productUse.getRatePlanCode() != null ? productUse.getRatePlanCode() : offerRatePlanCode;

						if (priceRequest.isPerpetualOffer() || isMatchingInputProgram(ratePlanCode, priceRequest.getProgramId())) {
                            LocalDate start = rate.getStart();
                            final LocalDate end = rate.getEnd();
                            final Double discountedPrice = Double.parseDouble(rate.getDailyTotalRate().getBsAmt());
                            Double resortFee = 0.0;
                            final List<TaxAmount> taxList = rate.getDailyTotalRate().getTaxList();
                            if(CollectionUtils.isNotEmpty(taxList)) {
                                final Optional<TaxAmount> txAmt = taxList.stream().filter(tax -> ServiceConstant.ACRS_RESORT_FEE.equals(tax.getCode())).findAny();
                                if(txAmt.isPresent()) {
                                    resortFee = Double.parseDouble(txAmt.get().getAmt());
                                }

                            }
							Double dailyAmtAftTax = 0.0;
                            String dailyAmtAftTaxStr = rate.getDailyTotalRate().getAmtAfTx();
                            if(StringUtils.isNotBlank(dailyAmtAftTaxStr)){
								//TODO re-check for COMP
								dailyAmtAftTax = ReservationUtil.calculateDailyAmtAftTax(taxList,discountedPrice,isRatePlanCompMap,Double.parseDouble(rate.getDailyTotalRate().getAmtAfTx()),ratePlanCode);

							}


                            final boolean isComp = Boolean.TRUE.equals(isRatePlanCompMap.get(ratePlanCode));
                            final String promoCode = ratePlanPromoMap.get(ratePlanCode);
                            final boolean isPerpetualPricing = Boolean.TRUE.equals(isRatePlanPOMap.get(ratePlanCode));

                            boolean availStatusOffer = offer.getAvailStatus() == null || AvailStatusOffer.AVAILABLE.equals(offer.getAvailStatus());
                            while (start.isBefore(end)) {
                                Date date = ReservationUtil.convertLocalDateToDate(start);
                                auroraPriceResponseList.add(createAuroraPriceForPerpetualOffer(productUse, date,
                                        discountedPrice, resortFee, priceRequest.getNumRooms(), propertyId, basePriceRatePlan, availStatusOffer,
                                        acrsBasePriceRoomTypeMap, isPerpetualPricing, isComp, ratePlanCode,
                                        acrsBasePriceRoomTypeMapForComp, promoCode, dailyAmtAftTax));
                                start = start.plusDays(1);
                            }
                        }
					});
				}
			});
		});

		if(!ServiceConstant.ICE.equalsIgnoreCase(priceRequest.getSource())
				&& CollectionUtils.isNotEmpty(auroraPriceResponseList)
				&& StringUtils.isEmpty(priceRequest.getProgramId())
				&& StringUtils.isEmpty(priceRequest.getPromo())){
			//ACRS will be returning all the programs for the room-date combination .
			// for lowest price we need to filter out the price.
			if(priceRequest.isNeedLowestPrice()){
				Map<String, Map<Date,List<AuroraPriceResponse>>> roomMap = auroraPriceResponseList.stream()
						.collect(Collectors.groupingBy(AuroraPriceResponse::getRoomTypeId,Collectors.groupingBy(AuroraPriceResponse::getDate)));
				Map<String,List<AuroraPriceResponse>> roomPricesMap = new HashMap<>();
				roomMap.forEach((room,dates)->{
					List<AuroraPriceResponse> minPrices = new ArrayList<>();
					 dates.forEach((date,prices)->{
						 AuroraPriceResponse minPrice = prices.stream().sorted(Comparator.comparingDouble(AuroraPriceResponse::getDiscountedPrice))
								 .collect(Collectors.toList()).get(0);
						 minPrices.add(minPrice);
					 });
					roomPricesMap.put(room,minPrices);
				});
				List<AuroraPriceResponse> minPriceList = roomPricesMap.values().stream()
						.min(Comparator.comparingDouble(prices -> prices.stream().mapToDouble(AuroraPriceResponse::getDiscountedPrice).sum()))
						.orElse(Collections.emptyList());
				return minPriceList;
			}
			//Sort based on min price if there is no programId in the request
			return auroraPriceResponseList.stream().sorted(Comparator.comparingDouble(AuroraPriceResponse::getDiscountedPrice))
					.collect(Collectors.toList());
		}else{
			return auroraPriceResponseList;
		}

	}
	/**
	 * Return Base Price Map based on basePrice response and perpetualPricing flag.
	 * @param crsResponse
	 * @param basePriceCrsResponse
	 * @param basePriceRatePlan
	 * @param isPerpetualOffer
	 * @return
	 */
    private static Map<String, Double> getAcrsBasePriceRoomTypeMap(SuccessfulSingleAvailability crsResponse,
			SuccessfulSingleAvailability basePriceCrsResponse, String basePriceRatePlan, boolean isPerpetualOffer) {
    	Map<String, Double> acrsBasePriceRoomTypeMap = new HashMap<>();
    	if(basePriceCrsResponse != null) {
			acrsBasePriceRoomTypeMap = getAcrsBasePriceRoomTypeMap(basePriceCrsResponse, basePriceRatePlan,
					false);
		}
		else {
			acrsBasePriceRoomTypeMap = getAcrsBasePriceRoomTypeMap(crsResponse, basePriceRatePlan,
					isPerpetualOffer);
		}
    	return acrsBasePriceRoomTypeMap;
	}

	/**
     * This method compares the input programId with response ratePlan codes
     * @param ratePlanCode
     * @param programId
     * @return
     */
    private static boolean isMatchingInputProgram(String ratePlanCode, String programId) {
        // Find and match the input programId with response ratePlan codes, ignoring Group codes for now
        boolean matchInputProgram = true;
        if (StringUtils.isNotEmpty(programId) && ACRSConversionUtil.isAcrsRatePlanGuid(programId)) {
            if (!StringUtils.equalsIgnoreCase(ratePlanCode, ACRSConversionUtil.getRatePlanCode(programId))) {
                matchInputProgram = false;
            }
        }
        return matchInputProgram;
    }

    public static List<AuroraPriceResponse> transform(SuccessfulSingleAvailability crsResponse, int requestedNumRooms) {
    	List<AuroraPriceResponse> auroraPriceResponseList = new ArrayList<>();

        //for group response if AvailStatus= NOTAVAILABLE, return empty response
        if(crsResponse.getData().getGroupContract() != null && crsResponse.getData().getGroupContract().getAvailStatus() != null 
        		&& crsResponse.getData().getGroupContract().getAvailStatus().equals(AvailStatus.NOTAVAILABLE)) {
        		return auroraPriceResponseList;
        	}
        
        // get PropertyId from crsResponse
        final String propertyId = crsResponse.getData().getHotel().getPropertyCode();

        // Get AuroraPriceResponse objects from crsResponse
        crsResponse.getData().getOffers().forEach(offer -> {
            offer.getProductUses().get(0).getRates().getDailyRates().forEach(rate -> {
                LocalDate start = rate.getStart();
                LocalDate end = rate.getEnd();
                Double basePrice = Double.parseDouble(rate.getDailyTotalRate().getBsAmt());
                while (start.isBefore(end)) {                    
                    Date date = ReservationUtil.convertLocalDateToDate(start);
                    auroraPriceResponseList.add(createAuroraPrice(offer, date, basePrice, requestedNumRooms, propertyId));
                    start = start.plusDays(1);
                }
            });
        });        

        return auroraPriceResponseList;
    }
    
    public static List<AuroraPriceV3Response> transformV3(SuccessfulSingleAvailability crsResponse, int requestedNumRooms) {
        //for group response if AvailStatus= NOTAVAILABLE, return empty response
        if(crsResponse.getData().getGroupContract() != null &&
				AvailStatus.NOTAVAILABLE.equals(crsResponse.getData().getGroupContract().getAvailStatus())) {
			return new ArrayList<>();
		}

        // Get AuroraPriceResponse objects from crsResponse
        return crsResponse.getData().getOffers().stream()
				.flatMap(offer -> createAuroraPriceV3ResponseFromOfferSingle(offer, requestedNumRooms).stream())
				.collect(Collectors.toList());
    }

	private static List<AuroraPriceV3Response> createAuroraPriceV3ResponseFromOfferSingle(OfferSingle offerSingle,
																						  int requestedNumRooms) {
		List<AuroraPriceV3Response> auroraPriceResponseList = new ArrayList<>();
		final String programId = offerSingle.getRatePlanCode() != null ? offerSingle.getRatePlanCode() : offerSingle.getGroupCode();
		// TODO is ProductUses guaranteed to only have a single object in the list?
		ProductUseSingle productUseSingle = offerSingle.getProductUses().get(0);
		final int numberOfAvailableProducts =
				Optional.ofNullable(productUseSingle.getNumberOfAvailableProducts()).orElse(0);
		final String roomTypeId = productUseSingle.getInventoryTypeCode();
		final AvailabilityStatus availabilityStatus = getAvailabilityStatus(requestedNumRooms,
				offerSingle.getAvailStatus(),
				numberOfAvailableProducts);
		productUseSingle.getRates().getDailyRates().forEach(rate -> {
			LocalDate start = rate.getStart();
			LocalDate end = rate.getEnd();
			Double basePrice = Double.parseDouble(rate.getDailyTotalRate().getBsAmt());
			while (start.isBefore(end)) {
				Date date = ReservationUtil.convertLocalDateToDate(start);
				auroraPriceResponseList.add(createAuroraPriceV3(programId, roomTypeId, date, basePrice,
						availabilityStatus));
				start = start.plusDays(1);
			}
		});
		return auroraPriceResponseList;
	}

	private static AvailabilityStatus getAvailabilityStatus(int requestedNumRooms, AvailStatusOffer offerAvailStatus,
															int numberOfAvailableProducts) {
		AvailabilityStatus availabilityStatus = AvailabilityStatus.SOLDOUT;
		if (offerAvailStatus.equals(AvailStatusOffer.AVAILABLE)
				&& numberOfAvailableProducts >= requestedNumRooms) {
			availabilityStatus = AvailabilityStatus.AVAILABLE;
		}
		return availabilityStatus;
	}

	/**
	 * Transforms ACRS SuccessfulMultiAvailability object into
	 * List<AuroraPriceResponse>
	 * 
	 * @param crsResponse ACRS SuccessfulMultiAvailability object
	 * @return List of AuroraPriceResponse
	 */
	public static List<AuroraPriceResponse> transformMultiAvailability(SuccessfulMultiAvailability crsResponse,
			AuroraPriceRequest pricingRequest) {
		List<AuroraPriceResponse> resortPriceResponseList = new ArrayList<>();

		List<HotelsMulti> hotelMultiList = crsResponse.getData().getHotels();
		for (HotelsMulti hotel : hotelMultiList) {
			if (hotel.getProducts() != null) {
				resortPriceResponseList.addAll(acrsHotelPriceToAuroraPriceList(hotel, pricingRequest));
			} else {
			    String promo = (CollectionUtils.isNotEmpty(hotel.getRatePlans()) && StringUtils.isNotEmpty(hotel.getRatePlans().get(0).getPromoCode())) ? hotel.getRatePlans().get(0).getPromoCode() : null;
				resortPriceResponseList.add(acrsHotelToResortPrice(hotel.getHotel().getPropertyCode(), null, null, null, promo , ServiceConstant.ZERO_DOUBLE_VALUE, ServiceConstant.ZERO_DOUBLE_VALUE, ServiceConstant.ZERO_DOUBLE_VALUE, false));
			}
		}
		return resortPriceResponseList;
	}

    private AuroraPriceResponse acrsHotelToResortPrice(String propertyCode, Date date, String programId,
													   String roomTypeId, String promo, double price, double resortFee,double amtAftTax,
													   boolean isAvailable) {
        AuroraPriceResponse resortPriceResponse = new AuroraPriceResponse();
        resortPriceResponse.setPropertyId(propertyCode);
        if (isAvailable) {
            resortPriceResponse.setBasePrice(price);
            resortPriceResponse.setDiscountedPrice(price);
            resortPriceResponse.setDiscountedMemberPrice(price);
            resortPriceResponse.setResortFee(resortFee);
            resortPriceResponse.setDate(date);
            resortPriceResponse.setProgramId(programId);
            resortPriceResponse.setRoomTypeId(roomTypeId);
            resortPriceResponse.setPromo(promo);
			resortPriceResponse.setAmtAftTax(amtAftTax);
        }
        resortPriceResponse.setStatus(isAvailable ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.NOARRIVAL);
        return resortPriceResponse;
    }
    
    
    private List<AuroraPriceResponse> acrsHotelPriceToAuroraPriceList(HotelsMulti hotel,
            AuroraPriceRequest pricingRequest) {
        List<AuroraPriceResponse> resortPriceResponseList = new ArrayList<>();
        LocalDate start = pricingRequest.getCheckInDate();
        LocalDate end = pricingRequest.getCheckOutDate();
        String propertyCode = hotel.getHotel().getPropertyCode();
		String programId = pricingRequest.getProgramId();
		String roomTypeId = null;

		// Setting the default values for the field
		double avgPrice = ServiceConstant.ZERO_DOUBLE_VALUE;
		double resortFee = ServiceConstant.ZERO_DOUBLE_VALUE;
		double amtAfTax = ServiceConstant.ZERO_DOUBLE_VALUE;
        String promo = null;

        // Finding the min of the all the offers available
        final RateDetails rateDetails = hotel.getRateDetails();
		Set<String> compRatePlans = getCompRatePlans(hotel.getRatePlans());
		Optional<OfferMulti> minOfferOptional = getMinOfferFromMultiRateDetails(rateDetails, compRatePlans);

		if(minOfferOptional.isPresent()) {
			OfferMulti minOffer = minOfferOptional.get();
			String offerRatePlanCode = minOffer.getRatePlanCode();
			programId = offerRatePlanCode;
			if (CollectionUtils.isNotEmpty(minOffer.getProductUses())) {
				roomTypeId = minOffer.getProductUses().stream()
						.map(ProductUseMulti::getInventoryTypeCode)
						.filter(StringUtils::isNotEmpty)
						.findFirst()
						.orElse(null);
			}
			promo = getPromoCodeFromOfferMulti(offerRatePlanCode, hotel.getRatePlans());
			// check if ratePlan is not a comp && has a value in BsAmt before parsing string to double
			DailyTypeAmountMultiWithAverage minOfferAvg = minOffer.getAvg();
			if (null != minOfferAvg) {
				if ((CollectionUtils.isEmpty(compRatePlans) || !compRatePlans.contains(offerRatePlanCode))
						&& StringUtils.isNotEmpty(minOfferAvg.getBsAmt())) {
					avgPrice = Double.parseDouble(minOfferAvg.getBsAmt());
				}
				if (CollectionUtils.isNotEmpty(minOfferAvg.getTaxGroups())) {
					Optional<TaxAmount> txAmt = minOfferAvg.getTaxGroups().stream()
							.filter(tax -> ServiceConstant.ACRS_RESORT_FEE.equals(tax.getCode()))
							.findAny();
					if (txAmt.isPresent()) {
						resortFee = Double.parseDouble(txAmt.get().getAmt());
					}
				}
				amtAfTax = Double.parseDouble(minOfferAvg.getAmtAfTx());
			}
		}

        while (start.isBefore(end)) {
            Date date = ReservationUtil.convertLocalDateToDate(start);
            resortPriceResponseList
                    .add(acrsHotelToResortPrice(propertyCode, date, programId, roomTypeId, promo, avgPrice, resortFee,amtAfTax, true));
            start = start.plusDays(1);
        }
        return resortPriceResponseList;
    }

	private static String getPromoCodeFromOfferMulti(String offerRatePlanCode, List<RatePlanMulti> ratePlans) {
		if (CollectionUtils.isEmpty(ratePlans) || StringUtils.isEmpty(offerRatePlanCode)){
			return null;
		}
		return ratePlans.stream()
				.filter(ratePlan -> offerRatePlanCode.equalsIgnoreCase(ratePlan.getCode()))
				.map(RatePlanMulti::getPromoCode)
				.filter(StringUtils::isNotEmpty)
				.findFirst()
				.orElse(null);
	}

	private static Set<String> getCompRatePlans(List<RatePlanMulti> ratePlans) {
		return ratePlans.stream()
				.filter(ratePlanMulti -> Boolean.TRUE.equals(ratePlanMulti.isAreAmtsConfidential()))
				.map(RatePlanMulti::getCode)
				.collect(Collectors.toSet());
	}

	private static Optional<OfferMulti> getMinOfferFromMultiRateDetails(RateDetails rateDetails,
																		Set<String> compRatePlans) {
		if(null == rateDetails || CollectionUtils.isEmpty(rateDetails.getOffers())) {
			return Optional.empty();
		}

		// Check to see if offer's ratePlanCode is a compRatePlan
		List<OfferMulti> offers = rateDetails.getOffers();
		if (CollectionUtils.isNotEmpty(compRatePlans)) {
			Optional<OfferMulti> compOfferOptional = offers.stream()
					.filter(offer -> compRatePlans.contains(offer.getRatePlanCode()))
					.findAny();
			if(compOfferOptional.isPresent()) {
				return compOfferOptional;
			}
		}
		// No comp offer so find the lowest Amount value
		return offers.stream()
				.min(Comparator.comparingDouble(x -> Double.parseDouble(x.getAvg().getBsAmt())));
	}

	private AuroraPriceResponse createAuroraPrice(OfferSingle offer, Date datetoSet, Double discountedPrice, int requestedNumRooms, String propertyId) {
        ProductUseSingle productUseSingle = offer.getProductUses().get(0);
        AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
        auroraPriceResponse.setRoomTypeId(productUseSingle.getInventoryTypeCode());
        final int numberOfAvailableProducts = productUseSingle.getNumberOfAvailableProducts() != null ? productUseSingle.getNumberOfAvailableProducts() : 0;
        auroraPriceResponse.setBasePrice(ServiceConstant.ZERO_DOUBLE_VALUE);
        // Setting basePrice in discounted price as ACRS already sends discounted price
        auroraPriceResponse.setDiscountedPrice(discountedPrice);
        // setting to false, planned in later increments
        auroraPriceResponse.setComp(false);
        auroraPriceResponse.setDate(datetoSet);
        auroraPriceResponse.setProgramId(offer.getRatePlanCode() != null ? offer.getRatePlanCode() : offer.getGroupCode());
        auroraPriceResponse.setPropertyId(propertyId);
        if (offer.getAvailStatus().equals(AvailStatusOffer.AVAILABLE) 
        		&& numberOfAvailableProducts >= requestedNumRooms) {
            auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
        } else{
            auroraPriceResponse.setStatus(AvailabilityStatus.SOLDOUT);
        }
        return auroraPriceResponse;
    }
    
    private AuroraPriceV3Response createAuroraPriceV3(String programId, String roomTypeId, Date dateToSet,
													  Double basePrice, AvailabilityStatus availabilityStatus) {
        AuroraPriceV3Response auroraPriceResponse = new AuroraPriceV3Response();
        auroraPriceResponse.setRoomTypeId(roomTypeId);
        auroraPriceResponse.setTotalNightlyTripPrice(basePrice);
		auroraPriceResponse.setPOApplicable(true); // TODO what determines this?
		auroraPriceResponse.setDate(dateToSet);
        TripDetailsV3 tripDetails = new TripDetailsV3();
        tripDetails.setDate(dateToSet);
        tripDetails.setProgramId(programId);
        tripDetails.setComp(true); // TODO WHAT??? why are we hardcoding comp to true
        auroraPriceResponse.setTripDetails(Collections.singletonList(tripDetails));
        auroraPriceResponse.setStatus(availabilityStatus);
        return auroraPriceResponse;
    }

	private AuroraPriceResponse createAuroraPriceForPerpetualOffer(ProductUseSingle productUseSingle, Date dateToSet,
																   Double discountedPrice, Double resortFee, int requestedNumRooms,
																   String propertyId, String barRatePlan,
																   Boolean isAvailable, Map<String, Double> auroraBasePriceRoomTypeMap,
																   boolean isPerpetualOffer, boolean isComp, String ratePlanCode,
																   Map<String, Double> acrsBasePriceRoomTypeMapForComp, String promoCode, Double dailyAmtAftTax) {
		final int numberOfAvailableProducts = productUseSingle.getNumberOfAvailableProducts() != null ? productUseSingle.getNumberOfAvailableProducts() : 0;
		boolean modifiedIsAvailable = isAvailable && numberOfAvailableProducts >= requestedNumRooms;
		String roomTypeId = productUseSingle.getInventoryTypeCode();

		double modifiedDiscountedPrice = (!isComp) ? discountedPrice : ServiceConstant.ZERO_DOUBLE_VALUE;
		int modifiedNumRooms = requestedNumRooms ==  0 ? 1 : requestedNumRooms;
		BigDecimal modifiedDiscountedPriceBigDecimal = BigDecimal.valueOf(modifiedDiscountedPrice);
		modifiedDiscountedPrice = modifiedDiscountedPriceBigDecimal.divide(BigDecimal.valueOf(modifiedNumRooms), 2,
				RoundingMode.HALF_UP).doubleValue();

		double modifiedBasePrice = modifiedDiscountedPrice;
		double baseAmtAftTax = ServiceConstant.ZERO_DOUBLE_VALUE;

		if (!barRatePlan.equalsIgnoreCase(ratePlanCode) || isComp) {
			Map<String, Double> basePriceMap = (isComp) ? acrsBasePriceRoomTypeMapForComp : auroraBasePriceRoomTypeMap;
			modifiedBasePrice = getBasePrice(roomTypeId, dateToSet, basePriceMap, modifiedNumRooms);
			baseAmtAftTax = getBasePrice(roomTypeId, dateToSet, basePriceMap, modifiedNumRooms);
		}

		return createAuroraPrice(roomTypeId, dateToSet, modifiedDiscountedPrice, resortFee, modifiedBasePrice,
				propertyId, modifiedIsAvailable, isPerpetualOffer, isComp,
				ratePlanCode, promoCode,dailyAmtAftTax,baseAmtAftTax);
	}

	private static double getBasePrice(String roomTypeId, Date date, Map<String, Double> basePriceMap, int numRooms) {
		String roomDateKey = roomTypeId.concat(date.toString());
		if (basePriceMap.containsKey(roomDateKey)) {
			BigDecimal basePriceBigDecimal = BigDecimal.valueOf(basePriceMap.get(roomDateKey));
			return basePriceBigDecimal.divide(BigDecimal.valueOf(numRooms), 2, RoundingMode.HALF_UP).doubleValue();
		}
		return 0;
	}

	private AuroraPriceResponse createAuroraPrice(String roomTypeId, Date dateToSet, Double discountedPrice,
												  Double resortFee, double basePrice, String propertyId,
												  Boolean isAvailable, boolean isPerpetualOffer, boolean isComp,
												  String ratePlanCode, String promoCode, Double dailyAmtAftTax, Double baseAmtAftTax) {
        AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
        auroraPriceResponse.setRoomTypeId(roomTypeId);
		auroraPriceResponse.setBasePrice(basePrice);
		auroraPriceResponse.setDiscountedPrice(discountedPrice);
		auroraPriceResponse.setResortFee(resortFee);
		auroraPriceResponse.setAmtAftTax(dailyAmtAftTax);
		auroraPriceResponse.setBaseAmtAftTax(baseAmtAftTax);
        auroraPriceResponse.setComp(isComp);
        auroraPriceResponse.setDate(dateToSet);
        auroraPriceResponse.setProgramId(ratePlanCode);
        auroraPriceResponse.setPropertyId(propertyId);
    	auroraPriceResponse.setPOApplicable(isPerpetualOffer);
        if (Boolean.TRUE.equals(isAvailable)) {
            auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
        } else {
        	auroraPriceResponse.setUnavailabilityReason(ServiceConstant.SOLDOUT_STRING);
            auroraPriceResponse.setStatus(AvailabilityStatus.SOLDOUT);
        }
        auroraPriceResponse.setPromo(promoCode);
        return auroraPriceResponse;
    }

    private static Map<String, Double> getAcrsBasePriceRoomTypeMap(SuccessfulSingleAvailability crsResponse,
            String basePriceRatePlan, boolean isPerpetualOffer) {
        Map<String, Double> acrsBasePriceRoomTypeMap = new HashMap<>();
        
        if(crsResponse == null) {
        	return acrsBasePriceRoomTypeMap;
        }

        if (isPerpetualOffer) {
			crsResponse.getData().getOffers().stream()
					.flatMap(offer -> offer.getProductUses().stream())
					.filter(productUse -> productUse.getRatePlanCode().equals(basePriceRatePlan))
					.forEach(productUse -> acrsBasePriceRoomTypeMap.putAll(getAcrsBasePriceFromProductUse(productUse,false)));
        } else {
			crsResponse.getData().getOffers().stream()
					.filter(offer -> StringUtils.isNotEmpty(offer.getRatePlanCode()))
					.filter(offer -> offer.getRatePlanCode().equals(basePriceRatePlan))
					.flatMap(offer -> offer.getProductUses().stream())
					.forEach(productUse -> acrsBasePriceRoomTypeMap.putAll(getAcrsBasePriceFromProductUse(productUse,false)));
        }
        return acrsBasePriceRoomTypeMap;
    }
    
    private static Map<String, Double> getAcrsBasePriceRoomTypeMapForNonPOFlow(SuccessfulSingleAvailability crsResponse, boolean includeTaxNCharge){
    	Map<String, Double> acrsBasePriceRoomTypeMap = new HashMap<>();
    	if(null == crsResponse) {
			return acrsBasePriceRoomTypeMap;
		}
		crsResponse.getData().getOffers().stream()
				.flatMap(offerSingle -> offerSingle.getProductUses().stream())
				.forEach(productUse -> acrsBasePriceRoomTypeMap.putAll(getAcrsBasePriceFromProductUse(productUse,includeTaxNCharge)));
    	return acrsBasePriceRoomTypeMap;
    }


	private static Map<String, Double> getAcrsBasePriceFromProductUse(ProductUseSingle productUse, boolean includeTaxNCharge) {
		Map<String, Double> acrsBasePriceRoomTypeMap = new HashMap<>();
		//Rates will be null for components
		if (productUse.getRates() != null) {
			productUse.getRates().getDailyRates().forEach(rate -> {
				String roomTypeId = productUse.getInventoryTypeCode();
				LocalDate start = rate.getStart();
				LocalDate end = rate.getEnd();
				Double basePrice = includeTaxNCharge ? Double.parseDouble(rate.getDailyTotalRate().getAmtAfTx())
						: Double.parseDouble(rate.getDailyTotalRate().getBsAmt());
				while (start.isBefore(end)) {
					Date date = ReservationUtil.convertLocalDateToDate(start);
					String roomDateKey = roomTypeId.concat(date.toString());
					acrsBasePriceRoomTypeMap.put(roomDateKey, basePrice);
					start = start.plusDays(1);
				}
			});
		}
		return acrsBasePriceRoomTypeMap;
	}

	public static BodyParameterSingle getBodyParameterSingleFromPricingRequest(AuroraPriceRequest pricingRequest,
																		 ReferenceDataDAOHelper referenceDataDAOHelper) {
		BodyParameterSingle bodyParameterSingle = new BodyParameterSingle();
		RequestedRatesSingle requestedRatesSingle = new RequestedRatesSingle();
		DataRqSingle data = new DataRqSingle();
		bodyParameterSingle.setData(data);

		boolean forcePerpetualOffer = pricingRequest.isPerpetualOffer();

		final String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(pricingRequest.getPropertyId());

		//inc - 4
		if (org.apache.commons.lang3.StringUtils.isNotBlank(pricingRequest.getPromo())) {
			requestedRatesSingle.setPromoCode(pricingRequest.getPromo());
		} else if (pricingRequest.getProgramId() != null) {
			if (ACRSConversionUtil.isAcrsGroupCodeGuid(pricingRequest.getProgramId())) {
				requestedRatesSingle.setGroupCode(
						referenceDataDAOHelper.createFormatedGroupCode(propertyCode, pricingRequest.getProgramId()));
			} else {
				List<String> ratePlanCodes = new ArrayList<>();

				String acrsRatePlanCode = pricingRequest.getProgramId();
				//If GSE guid's passed in the request, don't pass program id
				//We observed this happens in Resort pricing where this method gets called for each property
				//TODO: This scenario is already in discussion - applying a temporary fix now
				if (!CommonUtil.isUuid(acrsRatePlanCode)) {
					acrsRatePlanCode = referenceDataDAOHelper.retrieveRatePlanDetail(propertyCode,
							pricingRequest.getProgramId());

					forcePerpetualOffer =
							ACRSConversionUtil.isPORatePlan(acrsRatePlanCode) || pricingRequest.isPerpetualOffer();

					ratePlanCodes.add(acrsRatePlanCode);
					requestedRatesSingle.setRatePlanCodes(ratePlanCodes);
				}
			}
		}
		data.setRates(requestedRatesSingle);

		List<RequestedGuestCounts> reqGuestCounts = getRequestedGuestCountsListFromPricingRequest(pricingRequest);
		//for e.g. one 2 room and 4 adults. ICE sends 4 adults per each room.
		//if ignoreOccupancyLoadBalancing = false ACRS need guest count in total.
		// if ignoreOccupancyLoadBalancing = true ACRS need guest count per room
		// for ICE ignoreOccupancyLoadBalancing will be always true.
		// room >1 can be only in ICE and ICE sends guest count per room so RBS need to send
		// the guest per room instead of total.
		final int noOfRooms = pricingRequest.getNumRooms() > 0 ? pricingRequest.getNumRooms() : 1;

		List<RequestedProductSingle> products = new ArrayList<>();
		if (null != pricingRequest.getRoomTypeIds()) {
			products.addAll(pricingRequest.getRoomTypeIds().stream()
					.map(product -> createProductFromRoomTypeId(referenceDataDAOHelper.retrieveRoomTypeDetail(pricingRequest.getPropertyId(), product), reqGuestCounts, noOfRooms))
					.collect(Collectors.toList()));
		} else {
			products.add(createProductFromRoomTypeId(null, reqGuestCounts, noOfRooms));
		}

		data.setProducts(products);
		OptionsSingle optionsSingle = new OptionsSingle();
		if (pricingRequest.isIncludeDefaultRatePlans()) {
			optionsSingle.setIncludePublicRates(
					OptionsSingle.IncludePublicRatesEnum.FOLLOWCHANNEL);
		} else {
			optionsSingle.setIncludePublicRates(
					OptionsSingle.IncludePublicRatesEnum.NEVER);
		}
		optionsSingle.perpetualOffer(forcePerpetualOffer);
		optionsSingle.setDisabilityMode(DisabilityRequest.ACCESSIBLEANDNONACCESSIBLE);
		optionsSingle.setCfNumber(getConfirmationNumberForSingleAvail(pricingRequest));

		if(pricingRequest.isIncludeSoldOutRooms()) {
			SecurityIndicators securityIndicators = new SecurityIndicators();
			securityIndicators.setAllowPropertyForceSell(true);
			optionsSingle.setSecurityIndicators(securityIndicators);
		}

		optionsSingle.setLoyalty(getLoyaltySingleForSingleAvail(pricingRequest));
		data.setOptions(optionsSingle);
		return bodyParameterSingle;
	}

	public static List<RequestedGuestCounts> getRequestedGuestCountsListFromPricingRequest(AuroraPriceRequest request){
		List<RequestedGuestCounts> requestedGuestCountsList = new ArrayList<>();
		RequestedGuestCounts aqC10 = new RequestedGuestCounts();
		aqC10.setCount(request.getNumGuests());
		aqC10.setOtaCode(ServiceConstant.NUM_ADULTS_MAP);
		RequestedGuestCounts aqC8 = new RequestedGuestCounts();
		aqC8.setCount(request.getNumChildren());
		aqC8.setOtaCode(ServiceConstant.NUM_CHILD_MAP);
		requestedGuestCountsList.add(aqC10);
		requestedGuestCountsList.add(aqC8);

		return requestedGuestCountsList;
	}

	private static String getConfirmationNumberForSingleAvail(AuroraPriceRequest pricingRequest) {
		String confirmationNumber = null;
		if(org.apache.commons.lang3.StringUtils.isNotEmpty(pricingRequest.getConfirmationNumber())) {
			confirmationNumber = pricingRequest.getConfirmationNumber();
		} else if (org.apache.commons.lang3.StringUtils.isNotEmpty(pricingRequest.getOperaConfirmationNumber())){
			// In case of migrated reservation, confirmationNumber may equal the opera confirmation number.
			confirmationNumber = pricingRequest.getOperaConfirmationNumber();
		}
		return confirmationNumber;
	}

	private static LoyaltySingle getLoyaltySingleForSingleAvail(AuroraPriceRequest pricingRequest) {
		LoyaltySingle loyalty = new LoyaltySingle();
		if (org.apache.commons.lang3.StringUtils.isNotBlank(pricingRequest.getMlifeNumber()) && Integer.parseInt(pricingRequest.getMlifeNumber()) > 0) {
			loyalty.setLoyaltyId(pricingRequest.getMlifeNumber());
		}

		if(pricingRequest.getCustomerRank() > 0) {
			loyalty.setValueTierOverride(String.valueOf(pricingRequest.getCustomerRank()));
		}

		//customer dominant play
		if (org.apache.commons.lang3.StringUtils.isNotBlank(pricingRequest.getCustomerDominantPlay())) {
			if (ServiceConstant.SLOT.equalsIgnoreCase(pricingRequest.getCustomerDominantPlay())) {
				loyalty.setPlayDominanceOverride(
						LoyaltySingle.PlayDominanceOverrideEnum.SLOTS);
			} else {
				loyalty.setPlayDominanceOverride(
						LoyaltySingle.PlayDominanceOverrideEnum
								.valueOf(pricingRequest.getCustomerDominantPlay().toUpperCase()));
			}
		}
		return loyalty;
	}

	private static RequestedProductSingle createProductFromRoomTypeId(String roomTypeId,
															   List<RequestedGuestCounts> guestCounts, int roomNum) {
		RequestedProductSingle requestedProductSingle = new RequestedProductSingle();
		if (null != roomTypeId) {
			requestedProductSingle
					.setInventoryTypeCode(roomTypeId);
		}
		requestedProductSingle.setProductCode(ServiceConstant.PRODUCT_CODE_SR);
		requestedProductSingle.setGuestCounts(guestCounts);
		requestedProductSingle.setQuantity(roomNum);
		return requestedProductSingle;
	}

    public static AuroraPriceResponse auroraPriceResFromGroupReterieveRes(String propertyId, LocalDate date, String inventoryCode,String groupCode) {
		AuroraPriceResponse res = new AuroraPriceResponse();
		res.setDate(DateUtil.toDate(date));
		res.setPropertyId(propertyId);
		res.setRoomTypeId(inventoryCode);
		res.setProgramId(groupCode);
		res.setStatus(AvailabilityStatus.AVAILABLE);
		return res;
    }
}
