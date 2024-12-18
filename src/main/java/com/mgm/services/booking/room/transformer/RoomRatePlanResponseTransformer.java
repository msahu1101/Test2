package com.mgm.services.booking.room.transformer;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.ApplicableRoomProgram;
import com.mgm.services.booking.room.model.response.CVSResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferDetail;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.util.ValidationUtil;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RoomRatePlanResponseTransformer {

    private enum RatePlanCategory {Casino, Transient}
    private enum RatePlanStatus {Active, Update, Delete}
    private enum CasinoRatePlanNamePrefix {CO, CA}
    private enum OfferTye {Program, Segment}

    private final int CVS_SEGMENT_5XX_START = 501;
    private final int CVS_SEGMENT_5XX_END = 545;
    private final int CVS_SEGMENT_5XX_DIFF = 500;

    public static ApplicableProgramsResponse getRoomRatePlanResponse(CVSResponse customerValues,
                                                                     ENRRatePlanSearchResponse[] hostResponses,
                                                                     String mlifeNo) {
        final ApplicableProgramsResponse response = new ApplicableProgramsResponse();
        final Set<String> programIds = new HashSet<>();
        for (ENRRatePlanSearchResponse ratePlan : hostResponses) {
            // Map the active offers
            if (!RatePlanStatus.Delete.name().equalsIgnoreCase(ratePlan.getStatus())) {

                boolean isFound = StringUtils.isNotEmpty(mlifeNo) || !ratePlan.isLoyaltyNumberRequired();
                isFound &= isApplicablePORatePlan(customerValues, ratePlan);

                if (isFound) {
                   final ApplicableRoomProgram program = ApplicableRoomProgram.builder()
                            .id(ratePlan.getRatePlanId())
                            .propertyId(ratePlan.getPropertyId())
                            .name(ratePlan.getName())
                            .promo(ratePlan.getPromo())
                            .category(getCategory(ratePlan).name())
                            .tags(CollectionUtils.isNotEmpty(ratePlan.getRatePlanTags()) ? ratePlan.getRatePlanTags() : new ArrayList<>())
                            .rateCode(ratePlan.getRateCode()).build();
                    response.getPrograms().add(program);
                    programIds.add(program.getId());
                }
            }
        }
        response.getProgramIds().addAll(programIds);

        return response;
    }

    private static boolean isApplicablePORatePlan(CVSResponse customerValues, ENRRatePlanSearchResponse ratePlan) {

        final String rateCode = ratePlan.getRateCode();
        if (ACRSConversionUtil.isPORatePlan(rateCode)) {

            // Don't consider the entries which is a PO ratePlan and also attached with promo (promo != null).
            // A similar entry will be there which will have only the PO ratePlan and promo = null.
            if (StringUtils.isNotEmpty(ratePlan.getPromo())) {
                return false;
            }

            // If not having any CVS values hence don't return the PO RatePlan
            if (customerValues == null) {
                return false;
            }

            final int rpSegment = ACRSConversionUtil.getPOSegment(rateCode);
            int cvsSegment = customerValues.getSegmentOrRank(ratePlan.getPropertyId());

            //Turn cvsSegments from 501 - 545 to 001 - 045
            if (cvsSegment >= CVS_SEGMENT_5XX_START && cvsSegment <= CVS_SEGMENT_5XX_END) {
                cvsSegment = cvsSegment - CVS_SEGMENT_5XX_DIFF;
            }

            if (rpSegment == cvsSegment) {

                final CVSResponse.DOMINANT_PLAY_TYPE rpDominantPlay = ACRSConversionUtil.getPODominantPlay(rateCode);
                final CVSResponse.DOMINANT_PLAY_TYPE cvsDominantPlay = customerValues.getDominantPlay(ratePlan.getPropertyId());

                if (rpDominantPlay == cvsDominantPlay) {
                    return true;
                }
            }
        }
        else {
            // Not a PO RatePlan, hence return the RatePlan
            return true;
        }

        return false;
    }

    public static RoomProgramsResponseDTO getCustomerOffersResponse(
            ENRRatePlanSearchResponse[] hostResponses, RoomProgramsRequestDTO request,
            CVSResponse customerValues, List<CustomerPromotion> patronOffers) {

        final RoomProgramsResponseDTO response = new RoomProgramsResponseDTO();

        // Get applicable Patron RatePlans
        response.setPatronPrograms(getPatronOffers(hostResponses, request, patronOffers));

        // Get applicable PO RatePlans
        response.setPoPrograms(getPoOffers(hostResponses, customerValues));

        // Get applicable Ice RatePlans
        response.setIceChannelPrograms(getIceChannelOffers(hostResponses, request));

        return response;
    }

    private static List<RoomProgramDTO> getIceChannelOffers(ENRRatePlanSearchResponse[] hostResponses, RoomProgramsRequestDTO request) {

        final List<RoomProgramDTO> iceRatePlans = new ArrayList<>();

        if (StringUtils.equalsIgnoreCase(ServiceConstant.ICE, request.getSource())) {
            final Set<String> excludedRatePlans = new HashSet<>();
            for (ENRRatePlanSearchResponse ratePlan : hostResponses) {
                // Map the active offers
                final boolean isActive = ratePlan.getIsActive() != null ? ratePlan.getIsActive() : true;
                if (isActive) {
                    // Exclude all Promos and PO RatePlans
                    if (StringUtils.isNotEmpty(ratePlan.getPromo()) || ACRSConversionUtil.isPORatePlan(ratePlan.getRateCode())) {
                        excludedRatePlans.add(ratePlan.getRatePlanId());
                    }
                }
            }

            for (ENRRatePlanSearchResponse ratePlan : hostResponses) {
                // Map the active offers
                final boolean isActive = ratePlan.getIsActive() != null ? ratePlan.getIsActive() : true;
                if (isActive && !excludedRatePlans.contains(ratePlan.getRatePlanId())) {

                    // Adding ICE channel Offers
                    boolean isFound = StringUtils.isNotEmpty(request.getMlifeNumber()) || isPublicOffer(ratePlan);
                    if (isFound) {
                        final RoomProgramDTO program = RoomProgramDTO.builder()
                                .programId(ratePlan.getRatePlanId())
                                .ratePlanCode(ratePlan.getRateCode())
                                .propertyId(ratePlan.getPropertyId()).build();
                        iceRatePlans.add(program);
                    }
                }
            }
        }

        return iceRatePlans;
    }

    private static List<RoomProgramDTO> getPoOffers(ENRRatePlanSearchResponse[] hostResponses,
                                                      CVSResponse customerValues) {

        final Map<String, RoomProgramDTO> poRatePlans = new HashMap<>();
        if (!ObjectUtils.isEmpty(customerValues)) {
            for (ENRRatePlanSearchResponse ratePlan : hostResponses) {

                // Map the active offers and look for the ratePlan entries
                final boolean isActive = ratePlan.getIsActive() == null || ratePlan.getIsActive();
                if (StringUtils.isEmpty(ratePlan.getPromo()) && isActive) {

                    // Adding PO Offers by matching dominant play and segment/rank
                    if (ACRSConversionUtil.isPORatePlan(ratePlan.getRateCode())) {

                        if (isApplicablePORatePlan(customerValues, ratePlan)) {
                            final RoomProgramDTO program = RoomProgramDTO.builder()
                                    .programId(ratePlan.getRatePlanId())
                                    .ratePlanCode(ratePlan.getRateCode())
                                    .propertyId(ratePlan.getPropertyId()).build();
                            poRatePlans.put(program.getProgramId(), program);
                        }

                        // Skip other PO Programs which belongs to other Rank/Segment
                    }
                }
            }

            // Should not return cash rate if the corresponding comp exists
            removeCashPORatePlans(poRatePlans);

        }

        return poRatePlans.values().stream().collect(Collectors.toList());
    }

    private static void removeCashPORatePlans(Map<String, RoomProgramDTO> poRatePlans) {

        final Set<String> removableIds = new HashSet<>();
        Set<String> compProgramPropertyList = new HashSet<>();
        // Finding if a comp rate plan is present
        for (RoomProgramDTO ratePlan : poRatePlans.values()) {
            if (ratePlan.getRatePlanCode().startsWith(ServiceConstant.COMP) ||
                    ratePlan.getRatePlanCode().startsWith(ServiceConstant.COMP_STRING)) {
                compProgramPropertyList.add(ratePlan.getPropertyId());
            }
        }
        // If a comp rate plan for a specific property is present then only remove the
        // corresponding cash rate plan
        if (!compProgramPropertyList.isEmpty()) {
            for (RoomProgramDTO program : poRatePlans.values()) {
                if (compProgramPropertyList.contains(program.getPropertyId())) {
                    final String ratePlanCode = program.getRatePlanCode();
                    if (ratePlanCode.startsWith(ServiceConstant.CASH) ||
                            ratePlanCode.startsWith(ServiceConstant.CASH_STRING) ||
                            ratePlanCode.startsWith(ServiceConstant.CASH_CH)) {
                        final String programId = program.getProgramId();
                        removableIds.add(programId);
                    }
                }
            }
            for (String id : removableIds) {
                poRatePlans.remove(id);
            }
        }
    }

    private static List<RoomProgramDTO> getPatronOffers(ENRRatePlanSearchResponse[] hostResponses,
                                                          RoomProgramsRequestDTO request, List<CustomerPromotion> patronPromos) {
        final List<RoomProgramDTO> patronRatePlans = new ArrayList<>();
        if (StringUtils.isNotEmpty(request.getMlifeNumber())) {
            for (ENRRatePlanSearchResponse ratePlan : hostResponses) {
                final boolean isActive = ratePlan.getIsActive() == null || ratePlan.getIsActive();
                final String aCrsPromo = ratePlan.getPromo();
                if (isActive && ACRSConversionUtil.isPatronPromo(aCrsPromo)) {
                    patronPromos.stream()
                            .filter(
                                    promo -> StringUtils.equalsIgnoreCase(promo.getPromoId(), String.valueOf(ACRSConversionUtil.getPatronPromoId(aCrsPromo)))
                                            && StringUtils.equalsIgnoreCase(promo.getPropertyId(), ratePlan.getPropertyId()))
                            .findAny().ifPresent(promo -> {
                        final RoomProgramDTO program = RoomProgramDTO.builder()
                                .programId(ratePlan.getRatePlanId())
                                .ratePlanCode(ratePlan.getRateCode())
                                .promo(ratePlan.getPromo())
                                .propertyId(ratePlan.getPropertyId()).build();
                        patronRatePlans.add(program);
                    });
                }
            }
        }

        return patronRatePlans;
    }

    private static boolean isPublicOffer(ENRRatePlanSearchResponse ratePlan) {
        final Boolean isPublic = ratePlan.getIsPublic();
        return (isPublic != null && isPublic) || !ratePlan.isLoyaltyNumberRequired();
    }

    public static CustomerOfferResponse getCustomerOfferRatePlanResponse(ENRRatePlanSearchResponse[] hostResponses, long customerId, String mlifeNo) {
        final CustomerOfferResponse response = new CustomerOfferResponse();
        final Map<String, CustomerOfferDetail> allOffers = new LinkedHashMap<>(); // Hash map to remove duplicate entries returning from enr search function app

        for (ENRRatePlanSearchResponse hostResponse : hostResponses) {
            // Map the active offers
            if (!RatePlanStatus.Delete.name().equalsIgnoreCase(hostResponse.getStatus())) {

                //Patron Offers and PO Offers
                //TODO: ACRS mapping needed 1. Rateplan to Promo, 2. Customer to Offer segment/rank/dominantPlay mapping, 3. GTP Rateplan

                // public offers
                boolean isFound = StringUtils.isNotEmpty(mlifeNo) || !hostResponse.isLoyaltyNumberRequired();
                if(isFound) {
                    final CustomerOfferDetail offer = new CustomerOfferDetail();
                    offer.setOfferType(OfferTye.Program.name());
                    offer.setId(hostResponse.getRatePlanId());
                    offer.setName(hostResponse.getName());
                    offer.setPropertyId(hostResponse.getPropertyId());
                    offer.setStartDate(ReservationUtil.convertStringDateTimeToDate(hostResponse.getTravelStartDate()));
                    offer.setEndDate(ReservationUtil.convertStringDateTimeToDate(hostResponse.getTravelEndDate()));
                    allOffers.put(offer.getId(), offer);
                }
            }
        }
        response.setOffers(new ArrayList<CustomerOfferDetail>(allOffers.values()));

        //TODO: Apply Single Used Segment and rolledOnToSegment concept -- Need Content API for Segment and RatePlan association

        return response;
    }

	public static RoomProgramValidateResponse getValidateProgramResponse(RoomProgramValidateRequest request,
			ENRRatePlanSearchResponse[] ratePlans, boolean enablePromoSearch) {

		final RoomProgramValidateResponse response = new RoomProgramValidateResponse();
        final String propertyId = request.getPropertyId();
        final String programId = request.getProgramId();
		boolean isValid = false;
		boolean isExpired = true;
        response.setPropertyId(propertyId);
        response.setProgramId(programId);

		if (ValidationUtil.isUuid(programId) || ACRSConversionUtil.isAcrsGroupCodeGuid(programId)) {
            // Group Id is always validated as true
            response.setGroupCode(ACRSConversionUtil.getGroupCode(programId));
			isValid = true;
			isExpired = false;

		} else if (StringUtils.isNotEmpty(request.getPromo())) {

			if (enablePromoSearch) {
                ratePlans = (null != ratePlans) ? ratePlans: new ENRRatePlanSearchResponse[0];
				final List<ENRRatePlanSearchResponse> responsePromos = Arrays.stream(ratePlans)
						.filter(x -> StringUtils.equalsIgnoreCase(x.getPromo(), request.getPromo()))
						.collect(Collectors.toList());
				ratePlans = responsePromos.toArray(new ENRRatePlanSearchResponse[0]);

				if (responsePromos.size() > 0) {
					isValid = true;
					final ENRRatePlanSearchResponse matchedEntry = findMatchedPromoRatePlan(request, responsePromos);
					isExpired = isExpiredRatePlan(matchedEntry);
					response.setProgramId(matchedEntry.getRatePlanId());
                    response.setPropertyId(matchedEntry.getPropertyId());
                    response.setPatronProgram(ACRSConversionUtil.isPatronPromo(matchedEntry.getPromo()));
                    response.setPromo(matchedEntry.getPromo());
                    response.setRatePlanTags(CollectionUtils.isNotEmpty(matchedEntry.getRatePlanTags()) ?
                            matchedEntry.getRatePlanTags() : null);
                    response.setLoyaltyNumberRequired(matchedEntry.isLoyaltyNumberRequired());
				}

			} else {
				// If promo is not enabled then return valid
				isValid = true;
				isExpired = false;
				response.setProgramId(programId);
				response.setPromo(request.getPromo());
			}

		} else if (ArrayUtils.isNotEmpty(ratePlans)) {
			isValid = true;
            ENRRatePlanSearchResponse enrRatePlan = ratePlans[0];
            isExpired = isExpiredRatePlan(enrRatePlan);
            response.setPropertyId(enrRatePlan.getPropertyId());
            response.setProgramId(enrRatePlan.getRatePlanId());
            // set rate plan tags
            response.setRatePlanTags(CollectionUtils.isNotEmpty(enrRatePlan.getRatePlanTags()) ?
                    enrRatePlan.getRatePlanTags() : null);
            if (StringUtils.isNotEmpty(enrRatePlan.getPromo())) {
                response.setPatronProgram(ACRSConversionUtil.isPatronPromo(enrRatePlan.getPromo()));
                response.setPromo(enrRatePlan.getPromo());
            }
            response.setLoyaltyNumberRequired(enrRatePlan.isLoyaltyNumberRequired());
		}

		response.setValid(isValid);
		response.setEligible(isValid);
		response.setExpired(isExpired);
		response.setMyvegas(isMyVegasOfferApplicable(ratePlans));
		response.setRatePlanCode(ACRSConversionUtil.getRatePlanCode(response.getProgramId()));

		return response;
	}

    private ENRRatePlanSearchResponse getSelectedRatePlanEntry(ENRRatePlanSearchResponse patronRatePlanEntry,
                                                               ENRRatePlanSearchResponse otherPromoRatePlanEntry,
                                                               ENRRatePlanSearchResponse poRatePlanEntry,
                                                               ENRRatePlanSearchResponse ratePlan) {
        if (null != patronRatePlanEntry)
            return patronRatePlanEntry;
        else if (null != otherPromoRatePlanEntry)
            return otherPromoRatePlanEntry;
        else if (null != poRatePlanEntry)
            return poRatePlanEntry;
        else
            return ratePlan;
    }

    /**
     * This method transform the Content Program information into Room Program Detail Object
     * @param responseRatePlans
     * @return
     */
    public static List<RoomOfferDetails> buildRoomProgramDetail(ENRRatePlanSearchResponse [] responseRatePlans,
                                                                List<String> programIds) {

        final List<RoomOfferDetails> allRatePlans = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(responseRatePlans)) {
            for (String ratePlanId : programIds) {
                List<ENRRatePlanSearchResponse> updatedRatePlans = Arrays.stream(responseRatePlans).filter(
                        y -> StringUtils.isNotEmpty(y.getRatePlanId()) &&
                                y.getRatePlanId().equalsIgnoreCase(ratePlanId)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(updatedRatePlans)) {
                    ENRRatePlanSearchResponse patronRatePlanEntry = null;
                    ENRRatePlanSearchResponse otherPromoRatePlanEntry = null;
                    ENRRatePlanSearchResponse poRatePlanEntry = null;
                    for (ENRRatePlanSearchResponse ratePlan: updatedRatePlans) {
                        if (ACRSConversionUtil.isPatronPromo(ratePlan.getPromo())) {
                            patronRatePlanEntry = ratePlan;
                        } else if (StringUtils.isNotEmpty(ratePlan.getPromo())) {
                            otherPromoRatePlanEntry = ratePlan;
                        }
                        if (ACRSConversionUtil.isPORatePlan(ratePlan.getRateCode())) {
                            poRatePlanEntry = ratePlan;
                        }
                    }
                    final ENRRatePlanSearchResponse selectedRatePlanEntry =
                            getSelectedRatePlanEntry(patronRatePlanEntry, otherPromoRatePlanEntry,
                                    poRatePlanEntry,updatedRatePlans.get(0));
                    allRatePlans.add(RoomOfferDetails.builder()
                            .id(selectedRatePlanEntry.getRatePlanId())
                            .name(selectedRatePlanEntry.getName())
                            .propertyId(selectedRatePlanEntry.getPropertyId())
                            .active(selectedRatePlanEntry.getIsActive() == null || selectedRatePlanEntry.getIsActive())
                            .description(selectedRatePlanEntry.getDescription())
                            .shortDescription(selectedRatePlanEntry.getDescription())
                            .termsAndConditions(selectedRatePlanEntry.getBookingMessage())
                            .agentText(selectedRatePlanEntry.getLongDescription())
                            .learnMoreDescription(selectedRatePlanEntry.getLongDescription())
                            //.minNights(program.getMinNights())
                            //.maxNights(program.getMaxNights())
                            //.barProgram()
                            .publicProgram(selectedRatePlanEntry.getIsPublic() == null || selectedRatePlanEntry.getIsPublic())
                            .patronPromoId(null != patronRatePlanEntry ? patronRatePlanEntry.getPromo() : null)
                            .promo(null != patronRatePlanEntry ? patronRatePlanEntry.getPromo() :
                                    (null != otherPromoRatePlanEntry ? otherPromoRatePlanEntry.getPromo() : null))
                            //.promoCode()
                            //.operaGuaranteeCode()
                            .periodStartDate(ReservationUtil.convertStringDateTimeToDate(selectedRatePlanEntry.getBookingStartDate()))
                            .periodEndDate(ReservationUtil.convertStringDateTimeToDate(selectedRatePlanEntry.getBookingEndDate()))
                            .travelPeriodStart(ReservationUtil.convertStringDateTimeToDate(selectedRatePlanEntry.getTravelStartDate()))
                            .travelPeriodEnd(ReservationUtil.convertStringDateTimeToDate(selectedRatePlanEntry.getTravelEndDate()))
                            .customerRank(null != poRatePlanEntry ? ACRSConversionUtil.getPOSegment(poRatePlanEntry.getRateCode()) : null)
                            .segmentFrom(null != poRatePlanEntry ? ACRSConversionUtil.getPOSegment(poRatePlanEntry.getRateCode()) : null)
                            .segmentTo(null != poRatePlanEntry ? ACRSConversionUtil.getPOSegment(poRatePlanEntry.getRateCode()) : null)
                            .bookableOnline(true)
                            .bookableByProperty(true)
                            .viewOnline(true)
                            .viewableByProperty(true)
                            .availableInIce(true)
                            //.multiRateSequenceNo()
                            .roomIds(selectedRatePlanEntry.getRoomTypeCodes() != null ?
                                    selectedRatePlanEntry.getRoomTypeCodes().stream()
                                            .map(x -> ACRSConversionUtil.createRoomCodeGuid(x, selectedRatePlanEntry.getPropertyCode()))
                                            .collect(Collectors.toList()) : Collections.EMPTY_LIST)
                            .tags(CollectionUtils.isNotEmpty(selectedRatePlanEntry.getRatePlanTags()) ? selectedRatePlanEntry.getRatePlanTags() : null)
                            .build());
                }
            }
        }
        return allRatePlans;
    }

    public static Map<String, String> transformProgramToPromoAssociation(List<String> programIds, ENRRatePlanSearchResponse[] ratePlansRes) {

        final Map<String, String> associationMap = new HashMap<>();

        if (ArrayUtils.isNotEmpty(ratePlansRes)) {
            // Add program and promo association
            final ENRRatePlanSearchResponse[] ratePlans = ratePlansRes;
            if (null != ratePlans) {
                for (ENRRatePlanSearchResponse ratePlan : ratePlans) {
                    final String promo = ratePlan.getPromo();
                    final String ratePlanId = ratePlan.getRatePlanId();
                    if (!org.apache.commons.lang3.StringUtils.isAnyEmpty(promo, ratePlanId)) {
                        associationMap.put(ratePlanId, promo);
                    }
                }
            }
        }

        // Add missing programIds which does not have promo association
        programIds.stream().forEach(x-> {
            if (org.apache.commons.lang3.StringUtils.isEmpty(associationMap.get(x))) {
                associationMap.put(x, null);
            }
        });

        return associationMap;
    }


    private static ENRRatePlanSearchResponse findMatchedPromoRatePlan(
            RoomProgramValidateRequest request, List<ENRRatePlanSearchResponse> responsePromos) {

        Assert.notEmpty(responsePromos, "Searched Promo RatePlans should not be empty");
        ENRRatePlanSearchResponse matchedEntry = responsePromos.get(0);

        // Find by not null RatePlanId
        Optional<ENRRatePlanSearchResponse> foundRatePlan = responsePromos.stream()
                .filter(x -> StringUtils.isNotEmpty(x.getRatePlanId()))
                .findFirst();
        if (foundRatePlan.isPresent()) {
            matchedEntry = foundRatePlan.get();
        }

        if (StringUtils.isNotEmpty(request.getProgramId())) {

            // Find by RatePlanId exact match
            foundRatePlan = responsePromos.stream()
                    .filter(x -> StringUtils.equalsIgnoreCase(x.getRatePlanId(), request.getProgramId()))
                    .findFirst();

            if (foundRatePlan.isPresent()) {
                matchedEntry = foundRatePlan.get();
            }
        }

        return matchedEntry;
    }

    private static boolean isExpiredRatePlan(ENRRatePlanSearchResponse ratePlan) {
        if (null != ratePlan) {
            final Date travelEndDate = ReservationUtil.convertStringDateTimeToDate(ratePlan.getTravelEndDate());
            if (travelEndDate == null || !travelEndDate.before(new Date())) {
                return false;
            }
        }
        return true;
    }

    private boolean isMyVegasOfferApplicable(final ENRRatePlanSearchResponse [] responses) {

        if (null != responses) {
            for (ENRRatePlanSearchResponse response: responses) {
                final Optional<String> myVegasRatePlan = Arrays.asList(ServiceConstant.MY_VEGAS_KEYWORDS).stream().filter(x -> {
                    final String longDescription = response.getLongDescription();
                    final String description = response.getDescription();
                    return (null != longDescription && longDescription.toUpperCase().contains(x))
                            ||
                            (null != description && description.toUpperCase().contains(x));
                }).findAny();

                if (myVegasRatePlan.isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static RatePlanCategory getCategory(ENRRatePlanSearchResponse response) {
        RatePlanCategory category = response.isLoyaltyNumberRequired() ? RatePlanCategory.Casino : RatePlanCategory.Transient;
        if (null != response.getRateCode()) {
            for(CasinoRatePlanNamePrefix ratePlanPrefix : CasinoRatePlanNamePrefix.values()) {
                if(response.getRateCode().toUpperCase().startsWith(ratePlanPrefix.name())) {
                    category = RatePlanCategory.Casino;
                    break;
                }
            }
        }
        return category;
    }
}
