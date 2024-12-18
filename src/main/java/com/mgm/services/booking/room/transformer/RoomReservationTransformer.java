package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.CreditCardCodes;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapperImpl;
import com.mgm.services.booking.room.model.TripDetail;
import com.mgm.services.booking.room.model.*;
import com.mgm.services.booking.room.model.crs.reservation.Email;
import com.mgm.services.booking.room.model.crs.reservation.PaymentStatus;
import com.mgm.services.booking.room.model.crs.reservation.RoutingInstruction;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.loyalty.UpdatedPromotion;
import com.mgm.services.booking.room.model.refdata.RoutingInfoRequest;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.SourceRoomReservationBasicInfoRequestDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.reservation.RoomReservationAlert;
import com.mgm.services.booking.room.model.reservation.RoomReservationTrace;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.ReservationBasicInfo;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.validator.util.ReservationValidatorUtil.BookingType;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.common.*;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.messages.ModifyProfileRoomReservationRequest;
import com.mgmresorts.aurora.messages.SourceRoomReservationBasicInfoRequest;
import com.mgmresorts.aurora.messages.SourceRoomReservationBasicInfoResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class providing functions for reservation object transformations
 * required for API outputs.
 *
 */
@UtilityClass
@Log4j2
public class RoomReservationTransformer{

    private static final String SH = "SH";
    /**
     * Transforms aurora friendly room reservation object into reservation response
     * required for service response
     *
     * @param reservation   Room reservation object
     * @param appProperties Application properties
     * @return Room reservation response object
     */
    public static RoomReservationResponse transform(RoomReservation reservation, ApplicationProperties appProperties) {

        RoomReservationResponse response = new RoomReservationResponse();
        if (StringUtils.isEmpty(reservation.getInSessionReservationId())) {
            final String uuid = UUID.randomUUID().toString();
            reservation.setInSessionReservationId(uuid);
        }
        response.setItemId(reservation.getInSessionReservationId());
        response.setPropertyId(reservation.getPropertyId());
        response.setRoomTypeId(reservation.getRoomTypeId());
        response.setProgramId(reservation.getProgramId());
        response.setConfirmationNumber(reservation.getConfirmationNumber());
        response.setBookDate(reservation.getBookDate());
        response.setState(reservation.getState());
        if (reservation.getDepositCalc() != null) {
            response.setFreeCancellationEndDate(reservation.getDepositCalc().getForfeitDate());
        }
        response.setCancellationPenaltyApplies(ReservationUtil.isForfeit(reservation, appProperties));

        TripDetail tripDetail = new TripDetail();
        tripDetail.setCheckInDate(reservation.getCheckInDate());
        tripDetail.setCheckOutDate(reservation.getCheckOutDate());
        tripDetail.setNights(Math.toIntExact(ChronoUnit.DAYS.between(ReservationUtil.convertDateToLocalDate(reservation.getCheckInDate()), ReservationUtil.convertDateToLocalDate(reservation.getCheckOutDate()))));
        tripDetail.setNumGuests(reservation.getNumAdults());
        response.setTripDetails(tripDetail);

        RatesSummary rateSummary = transform(reservation);
        rateSummary.setResortFeePerNight(rateSummary.getResortFee() / tripDetail.getNights());

        response.setRates(rateSummary);

        // Set payments info when available; will be available for booked
        // reservation
        PaymentBasic payment = new PaymentBasic();
        if (null != reservation.getPayments() && !reservation.getPayments().isEmpty()) {
            Payment paymentCharge = reservation.getPayments().get(0);

            payment.setCardType(paymentCharge.getChargeCardType());
            payment.setCardMaskedNumber(paymentCharge.getChargeCardMaskedNumber());

            double amountPaid = ServiceConstant.ZERO_DOUBLE_VALUE;
            for (Payment charge : reservation.getPayments()) {
                if (charge.isDeposit()) {
                    amountPaid += charge.getChargeAmount();
                }
            }
            payment.setChargeAmount(amountPaid);
            response.setPayment(payment);
        }

        return response;
    }



    /**
     * Calculate and set charges and taxes for the reservation and itemizes it.
     *
     * @param reservation
     *            Room reservation object
     * @param rateSummary
     *            Rates summary object
     */
    private static void setCharges(RoomReservation reservation, RatesSummary rateSummary) {

        double componentCharge = 0;
        double resortFee = 0;
        double resortFeeTax = 0;
        double roomChargetax = 0;
        double occupancyFee = 0;
        double tourismFee = 0;
        double tourismFeeTax = 0;
        double casinoSurcharge = 0;
        double casinoSurchargeTax = 0;

        if (reservation.getChargesAndTaxesCalc().getCharges() != null) {
            for (RoomChargeItem charges : reservation.getChargesAndTaxesCalc().getCharges()) {
                for (ItemizedChargeItem chargeItem : charges.getItemized()) {
                    final String item = chargeItem.getItem();
                    final RoomChargeItemType itemType = chargeItem.getItemType();
                    //For GSE, occupancy tax and charge codes are same but for ACRS, they are different
                    //For GSE, casino tax and charge codes are same but for ACRS, they are different
                    //For GSE, tourism tax and charge codes are same but for ACRS, they are different
                    if (null != item && (item.equalsIgnoreCase(ServiceConstant.OCCUPANCY_FEE_ITEM) || ServiceConstant.ACRS_OCCUPANCY_FEE_ITEM.contains(item))) {
                        occupancyFee += chargeItem.getAmount();
                    } else if(null != item  && (item.equalsIgnoreCase(ServiceConstant.TOURISM_FEE_ITEM) || ServiceConstant.ACRS_TOURISM_FEE_ITEM.contains(item))) {
                        tourismFee += chargeItem.getAmount();
                    } else if(null != item  && (item.equalsIgnoreCase(ServiceConstant.CASINO_SURCHARGE_ITEM) || ServiceConstant.ACRS_CASINO_SURCHARGE_ITEM.contains(item))) {
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

        // Taxes
        // TODO We believe it is a bug that ACRS is responding with null, if
        // confirmed remove null check when bug is fixed
        // if not a bug then remove this note and leave nullcheck.
        if (null != reservation.getChargesAndTaxesCalc().getTaxesAndFees()) {
            for (RoomChargeItem charges : reservation.getChargesAndTaxesCalc().getTaxesAndFees()) {
                for (ItemizedChargeItem chargeItem : charges.getItemized()) {
                    final String item = chargeItem.getItem();
                    final RoomChargeItemType itemType = chargeItem.getItemType();
                    if (null != item && (item.equalsIgnoreCase(ServiceConstant.TOURISM_FEE_ITEM) || ServiceConstant.ACRS_TOURISM_FEE_ITEM.contains(item))) {
                        tourismFeeTax += chargeItem.getAmount();
                    } else if (null != item && (item.equalsIgnoreCase(ServiceConstant.CASINO_SURCHARGE_ITEM) || ServiceConstant.ACRS_CASINO_SURCHARGE_ITEM.contains(item))) {
                        casinoSurchargeTax += chargeItem.getAmount();
                    }else if (null != itemType && itemType.equals(RoomChargeItemType.ResortFeeTax)) {
                        resortFeeTax += chargeItem.getAmount();
                    } else if (null != itemType && itemType.equals(RoomChargeItemType.ExtraGuestChargeTax)) {
                        // do nothing
                        // Exclude ExtraGuestChargeTax as that is already part
                        // of the RoomChargeTax
                    } else {
                        roomChargetax += chargeItem.getAmount();
                    }
                }
            }
        }

        rateSummary.setResortFeeAndTax(resortFee + resortFeeTax);
        rateSummary.setTourismFeeAndTax(tourismFee + tourismFeeTax);
        rateSummary.setCasinoSurchargeAndTax(casinoSurcharge + casinoSurchargeTax);
        rateSummary.setRoomChargeTax(roomChargetax);

        if (reservation.getDepositCalc() != null) {
            rateSummary.setDepositDue(ReservationUtil.getDepositAmount(reservation));
        }
    }

    public static RoomReservation transform(ReservationPendingRes crsResponse, AcrsProperties acrsProperties) {
        RoomReservationPendingResMapperImpl pendingMapping = new RoomReservationPendingResMapperImpl();
        ReservationRetrieveResReservation transformedCrsResponse =
                pendingMapping.reservationPendingResToReservationRetrieveRes(crsResponse);
        return transform(transformedCrsResponse, acrsProperties);
    }

    // TODO - Move this to common transform, no need of having different
    // transformers for different use cases.
    // TODO - Get the hotel reservation object then transform
    public static RoomReservation transform(ReservationRetrieveResReservation crsResponse, AcrsProperties acrsProperties) {

        RoomReservation response = new RoomReservation();
        HotelReservationRetrieveResReservation hotelReservation = crsResponse.getData().getHotelReservation();
          String acrsPropertyCode = getPropertyCodeFromHotel(acrsProperties.getPseudoExceptionProperties(), hotelReservation.getHotels().get(0));
        response.setPropertyId(acrsPropertyCode);

        GuestsRes guestsRes = hotelReservation.getUserProfiles();

        LoyaltyProgram loyaltyProgram = guestsRes != null ? guestsRes.get(0).getLoyaltyProgram() : null;
        if (loyaltyProgram != null && loyaltyProgram.getPrograms() != null) {
            //response.setProgramId(loyaltyProgram.getPrograms().get(0)
            //.getProgramName());
        }

        SegmentRes segmentRes = hotelReservation.getSegments();
        SegmentResItem segment = BaseAcrsTransformer.getMainSegment(segmentRes);
        int segmentHolderId = null != segment.getSegmentHolderId()?segment.getSegmentHolderId():0;

        // userProfiles
        if (guestsRes != null) {
            Optional<GuestsResItem> primaryUserProfile = guestsRes.stream()
                    .filter(profile -> profile.getId() == segmentHolderId).findFirst();
            primaryUserProfile.ifPresent(userProfileRes -> response
                    .setProfile(BaseAcrsTransformer.acrsUserProfileResToProfile(userProfileRes,acrsProperties.getAllowedTitleList())));
        }
        ReservationIdsRes reservationIdsRes = hotelReservation.getReservationIds();
        response.setConfirmationNumber(reservationIdsRes.getCfNumber());

        //set external confirmation no
        response.setExtConfirmationNumber(reservationIdsRes.getExtCfNumber() != null ? reservationIdsRes.getExtCfNumber().getNumber() : null);

        if (StringUtils.isNotBlank(reservationIdsRes.getPmsCfNumber())) {
            response.setOperaConfirmationNumber(reservationIdsRes.getPmsCfNumber());
        }

        // partyConfirmationNumber
        // otaConfirmationNumber
        if (null != hotelReservation.getReservationIds().getExtCfNumber()) {
            String cnfNumber = hotelReservation.getReservationIds().getExtCfNumber().getNumber();
            if (BaseAcrsTransformer.isPartyReservation(cnfNumber)) {
                response.setPartyConfirmationNumber(cnfNumber);
            } else {
                response.setOtaConfirmationNumber(cnfNumber);
            }
        }

        response.setBookDate(CommonUtil.getDate(hotelReservation.getCreationDateTime(),
                ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));

        response.setState(BaseAcrsTransformer.acrsResStatusToReservationState(hotelReservation.getStatus()));
        response.setCheckInDate(ReservationUtil.convertLocalDateToDate(segment.getStart()));
        response.setCheckOutDate(ReservationUtil.convertLocalDateToDate(segment.getEnd()));
        segment.getOffer().getProductUses().get(0).getGuestCounts().forEach(guest -> {
            if (guest.getOtaCode().equals(ServiceConstant.NUM_ADULTS_MAP)) {
                response.setNumAdults(guest.getCount());
            } else if (guest.getOtaCode().equals(ServiceConstant.NUM_CHILD_MAP)) {
                response.setNumChildren(guest.getCount());
            }
        });
        response.setNumRooms(segment.getOffer().getProductUses().get(0).getQuantity().intValue());

        response.setRoomTypeId(segment.getOffer().getProductUses().get(0).getInventoryTypeCode());
        //CBSR-1617. Room number was missing in response. Assigning RoomNumber for ACRS Room reservation
        if(null != segment.getOffer().getProductUses().get(0).getAssignedRoom()) {
        	response.setRoomNumber(segment.getOffer().getProductUses().get(0).getAssignedRoom());
        }
        List<RatePlanRes> ratePlans = hotelReservation.getRatePlans();
        response.setBookings(BaseAcrsTransformer.acrsSegmentToRoomPrice(segment, ratePlans));
        ReservationCustomData  reservationCustomData = BaseAcrsTransformer.getCustomData(segment.getCustomData());
        if(null != reservationCustomData) {
            response.setProgramId(reservationCustomData.getParentProgram());
        }
        if (StringUtils.isNotEmpty(segment.getOffer().getGroupCode())) {
            response.setIsGroupCode(true);
        }
        // depositCalc
        response.setDepositCalc(BaseAcrsTransformer.calculateRoomAndComponentDeposit(segmentRes, hotelReservation.getImageStatus()));

        // charges and taxes
        response.setChargesAndTaxesCalc(BaseAcrsTransformer.getRoomAndComponentChargesAndTaxes(segmentRes,
                acrsProperties, hotelReservation.getRatePlans(), acrsPropertyCode));

        // TODO check rates.balanceUponCheckIn

        // Payments and AmountDue
        if (segment.getFormOfPayment() != null) {
            List<PaymentTransactionRes> depositPayments = segment.getFormOfPayment().getDepositPayments();
            response.setPayments(BaseAcrsTransformer.getPaymentsFromAcrs(depositPayments));
            response.setAmountDue(BaseAcrsTransformer.getAmountPaidAgainstDeposit(depositPayments));
            // Credit Card Charges
            response.setCreditCardCharges(BaseAcrsTransformer.getCardDetailsUsedInLastOperation(segment.getFormOfPayment()));

        }
        // agentInfo
        ReservationOwner owner = segment.getOwner();
        if (null != owner && null != owner.getAgencyDetails()
                && CollectionUtils.isNotEmpty(owner.getAgencyDetails().getIdentifiers())) {
            AgentInfo agentInfo = new AgentInfo();
            agentInfo.setAgentId(owner.getAgencyDetails().getIdentifiers().get(0).getId());
            agentInfo.setAgentType(owner.getAgencyDetails().getIdentifiers().get(0).getType().getValue());
            response.setAgentInfo(agentInfo);
        }

        PointOfSale creator = crsResponse.getData().getCreator();
        if (null != creator) {
            // set Origin for bookingSource and bookingChannel fill up.
            if (null != creator.getReportingData()) {
                response.setOrigin(creator.getReportingData().getSubChannel());
            }
            // rrUpSell

            if (CollectionUtils.isNotEmpty(creator.getCallCenterInformation())) {
                response.setRrUpSell(creator.getCallCenterInformation().get(0).getId());
            }
        }
        PointOfSale requestor = crsResponse.getData().getRequestor();
        String requestorMarketsSourceCode = null;
         if(null != requestor && null != requestor.getReportingData()){
             requestorMarketsSourceCode = requestor.getReportingData().getCode();
         }
         //markets
        response.setMarkets(BaseAcrsTransformer.createAcrsMarkets(requestorMarketsSourceCode, segment));

        // partyConfirmationNumber
        // otaConfirmationNumber
        if (null != hotelReservation.getReservationIds().getExtCfNumber()) {
            String cnfNumber = hotelReservation.getReservationIds().getExtCfNumber().getNumber();
            if (cnfNumber.startsWith(ServiceConstant.PARTY_CONFIRMATION_NUMBER_PREFIX)) {
                response.setPartyConfirmationNumber(cnfNumber);
            } else {
                response.setOtaConfirmationNumber(cnfNumber);
            }
        }
        // upgradeRoomTypeId
        response.setUpgradeRoomTypeId(segment.getOffer().getProductUses().get(0).getUpgradedInventoryTypeCode());
        // guaranteeCode
        response.setGuaranteeCode(segment.getOffer().getPolicyTypeCode());

        if(segment.getOffer().getIsPerpetualOffer() != null){
            response.setPerpetualPricing(segment.getOffer().getIsPerpetualOffer());
        }


        // additionalComments
        if (null != hotelReservation.getComments()) {
            response.setAdditionalComments(
                    hotelReservation.getComments().stream().filter(Objects::nonNull).map(Comment::getText)
                            .collect(Collectors.toList()).stream().flatMap(List::stream).collect(Collectors.toList()));
        }
        //INC-3
        //routing instruction
        response.setRoutingInstructions(BaseAcrsTransformer.createRoutingInstructionfromACRS(
                null == loyaltyProgram ? null : loyaltyProgram.getLoyaltyId(),
                segment.getOffer().getProductUses()));

        //customer rank,segment and Dominance.@TODO set rank and Segment as required
        if (null != loyaltyProgram && null != loyaltyProgram.getQualifier()) {
            response.setCustomerRank(
                    Integer.parseInt(Optional.ofNullable(loyaltyProgram.getQualifier().getTier()).orElse("0")));
            response.setCustomerSegment(
                    Integer.parseInt(Optional.ofNullable(loyaltyProgram.getQualifier().getTier()).orElse("0")));
            response.setCustomerDominantPlay(BaseAcrsTransformer.transFormAcrsDominentPlay(loyaltyProgram.getQualifier().getDominance()));

        }
        // depositPolicy is required for hold response.
        PoliciesRes policy = segment.getOffer().getPolicies();
        response.setDepositPolicyCalc(BaseAcrsTransformer.getDepositPolicyCalcFromPoliciesRes(policy));

        //INC-4 traces and alerts
        if (CollectionUtils.isNotEmpty(segment.getAlerts())) {
            response.setAlerts(segment.getAlerts().stream().map(RoomReservationTransformer::acrsAlertToReservationAlert)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(segment.getTraces())) {
            response.setTraces(segment.getTraces().stream()
                    .map(RoomReservationTransformer::acrsAlertToReservationTraces).collect(Collectors.toList()));
        }
        //promocode
        // Checking isPerpetualOffer. If true, do not parse promocode
        if(segment.getOffer().getIsPerpetualOffer() == null || !segment.getOffer().getIsPerpetualOffer()) {
        	response.setPromo(segment.getOffer().getPromoCode());
        }
       List<SegmentResItem>  componentSegments = BaseAcrsTransformer.getComponentSegments(segmentRes);
        if (CollectionUtils.isNotEmpty(componentSegments)) {
            // this is for add-on component form of payment
            response.setAddOnsDeposits(
                    BaseAcrsTransformer.createAddOnsDeposits(componentSegments));
            // add addons component details
            response.setPurchasedComponents(BaseAcrsTransformer.buildComponentDetailsFromACRSRes(componentSegments, hotelReservation.getProducts(),hotelReservation.getRatePlans(), acrsPropertyCode));
        }
        // spl req and room features
        if (CollectionUtils.isNotEmpty(segment.getServiceRequests())) {
            List<ReservationSplRequest> specialRequests = new ArrayList<>();
            for (ServiceRequests crsServiceRequest : segment.getServiceRequests()) {
                ReservationSplRequest splReq = new ReservationSplRequest();
                splReq.setCode(crsServiceRequest.getCode());
                if (crsServiceRequest.getType().equalsIgnoreCase(ServiceConstant.ROOMFEATURE_ACRS_TYPE)) {
                    splReq.setExactType(ServiceConstant.ROOMFEATURE_FORMAT);
                } else {
                    splReq.setExactType(ServiceConstant.SPECIALREQUEST_FORMAT);
                }
                splReq.setType(ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST);
                specialRequests.add(splReq);
            }
            response.setSpecialRequestObjList(specialRequests);
        }
        if(StringUtils.isNotEmpty(response.getPromo()) && StringUtils.isEmpty(response.getProgramId())) {
        	//In case of promo if the program id is not present at the reservation level, set it from bookings
        	//If the program id is same across bookings.
        	HashSet<String> bookingProgramIds = new HashSet<>();
			response.getBookings().stream().filter(booking -> booking.getProgramId() != null)
					.forEach(booking -> bookingProgramIds.add(booking.getProgramId()));
        	if(bookingProgramIds.size() == 1) {
        		response.setProgramId(bookingProgramIds.iterator().next());
        	}
        }
        //inc-4 add linkId for shared reservation.
        if (CollectionUtils.isNotEmpty(hotelReservation.getLinks())) {
            Optional<ReservationLinks> linkRes = hotelReservation.getLinks().stream()
                    .filter(link -> LinkType.SHARE.getValue().equalsIgnoreCase(link.getType().getValue())).findFirst();
            if (linkRes.isPresent()) {
                response.setShareId(linkRes.get().getId());
            }
        }
        return response;

    }

    public static String getPropertyCodeFromHotel(List<String> exceptionPropertiesCode, HotelRes hotelRes) {
        String hotelPropertyCode = hotelRes.getPropertyCode();
        if(CollectionUtils.isNotEmpty(exceptionPropertiesCode) &&
                exceptionPropertiesCode.contains(hotelPropertyCode)){
            // null check
            return  hotelRes.getMasterPropertyCode();
        }else{
            return hotelPropertyCode;
        }
    }

    private RoomReservationAlert acrsAlertToReservationAlert(Alerts alerts){
        RoomReservationAlert roomReservationAlert= new RoomReservationAlert();
        roomReservationAlert.setArea(alerts.getArea());
        roomReservationAlert.setCode(alerts.getCode());
        if(null != alerts.getDescription()) {
            if(CollectionUtils.isNotEmpty(alerts.getDescription().get(ServiceConstant.EN))) {
                roomReservationAlert.setDescription(alerts.getDescription().get(ServiceConstant.EN).get(0).toString());
            }
        }

        return roomReservationAlert;

    }
    private RoomReservationTrace acrsAlertToReservationTraces(Traces traces){
        RoomReservationTrace roomReservationTrace = new RoomReservationTrace();
        //date is not coming
        roomReservationTrace.setDate(CommonUtil.getDate(traces.getDate(),ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
        roomReservationTrace.setDepartmentCode(traces.getDepartmentCode());
        if(null != traces.getDescription()) {
            if(CollectionUtils.isNotEmpty(traces.getDescription().get(ServiceConstant.EN))) {
                roomReservationTrace.setText(traces.getDescription().get(ServiceConstant.EN).get(0).toString());
            }
        }
        if (null != traces.getResolvedDate()) {
            roomReservationTrace.setResolvedDate(CommonUtil.getDate(traces.getResolvedDate(), ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
        }
        if (null != traces.getResolvedUser()) {
            roomReservationTrace.setResolvedUser(traces.getResolvedUser());
        }
        return roomReservationTrace;

    }

    /**
     * This method is used to transform RBS roomReservation objects into ACRS
     * ReservationReq objects for use is making reservations within ACRS.
     *
     * @param roomReservation
     *            This is a room Reservation object as submitted to RBS from the
     *            front end with whatever customizations the customer has made
     *
     * @return This method returns a ReservationReq for use with ACRS create
     *         reservation API
     */
    public static ReservationPendingReq transformACRReservationReq(RoomReservation roomReservation,int maxAcrsCommentLength){
        // Create request objects
        ReservationPendingReq reservationReq = new ReservationPendingReq();
        HotelReservationPendingReq hotelReservationReq = new HotelReservationPendingReq();
        HotelReservationPendingReqHotelReservation hotelReservation = new HotelReservationPendingReqHotelReservation();

        // HotelReq
        HotelReq hotelReq = new HotelReq();
        hotelReq.setPropertyCode(roomReservation.getPropertyId());
        hotelReservation.addHotelsItem(hotelReq);

        //Setting the extCfNumber in ACRS Request for Child Reservations in Party Reservation
        if (null != roomReservation.getExtConfirmationNumber()) {
            ReservationIdsReq reservationIdsReq = new ReservationIdsReq();
            ExtCfNumber extCfNumber = new ExtCfNumber();
            extCfNumber.setNumber(roomReservation.getExtConfirmationNumber());
            reservationIdsReq.setExtCfNumber(extCfNumber);
            hotelReservation.setReservationIds(reservationIdsReq);
        }

        // Segment
        SegmentReq segmentReq = new SegmentReq();
        segmentReq.add(buildACRSSegmentFromRoomReservation(roomReservation));

        //INC -3 Components/add-ons
        if (CollectionUtils.isNotEmpty(roomReservation.getSpecialRequests())) {
            List<String> components = roomReservation.getSpecialRequests().stream()
                    .filter(ACRSConversionUtil::isAcrsComponentCodeGuid)
                    .filter(component -> ServiceConstant.COMPONENT_FORMAT
                            .equalsIgnoreCase(ACRSConversionUtil.getComponentType(component)))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(components)) {
                //ICEUCP-3292- while charges before create ICE don't send profile.
                // We can send segmentHolderId, bookerId = -1 to ACRS only if profile is there.
                Integer segmentHolderId = roomReservation.getProfile() != null ? -1 : null;
                Integer bookerId = segmentHolderId;
                components.forEach(compoent -> {
                    String ratePlanCode = ACRSConversionUtil.getComponentNRPlanCode(compoent);
                    String inventoryTypeCode = ACRSConversionUtil.getComponentCode(compoent);
                    SegmentReqItem mainSegment = segmentReq.get(0);
                    ProductUseReqItem productUse = mainSegment.getOffer().getProductUses().get(0);
                    Period period = RoomReservationTransformer.createACRSPeriod(roomReservation.getCheckInDate(),
                            roomReservation.getCheckOutDate());
                    // Override if show component date is provided for Pkg 2.0
                    if(CollectionUtils.isNotEmpty(roomReservation.getPkgComponents())){
                        Optional<PkgComponent> matchedComponent = roomReservation.getPkgComponents().stream()
                                .filter(x->ServiceConstant.SHOW_PKG_TYPE.equalsIgnoreCase(x.getType()))
                                .filter(x -> x.getId().equals(compoent)).findFirst();
                        if(matchedComponent.isPresent()){
                            PkgComponent pkgComponent = matchedComponent.get();
                            period = RoomReservationTransformer.createACRSPeriod(pkgComponent.getStart(),
                                    ReservationUtil.convertLocalDateToDate(ReservationUtil.convertDateToLocalDate( pkgComponent.getEnd()).plusDays(1)));
                        }
                    }
                    segmentReq
                            .add(RoomReservationTransformer.createComponentSegmentReq(mainSegment.getPropertyCode(), period,
                                    productUse.getGuestCounts(), ratePlanCode, inventoryTypeCode, segmentHolderId, bookerId, -1));
                });
            }
        }
        hotelReservation.setSegments(segmentReq);

        // GuestReq
        ReservationProfile profile = roomReservation.getProfile();
        if ( null != profile ) {
            GuestsPendingReq guestsPendingReq = new GuestsPendingReq();
            GuestsPendingReqItem guestsPendingReqItem = reservationProfileToACRSUserProfileReq(roomReservation.getProfile(),roomReservation.getCustomerRank(), roomReservation.getCustomerSegment(), roomReservation.getCustomerDominantPlay());
            guestsPendingReq.add(guestsPendingReqItem);
            hotelReservation.setUserProfiles(guestsPendingReq);
        }

        // ReservationType
        // TODO logic to determine which Type
        ReservationType reservationType = ReservationType.INDIVIDUAL;
        hotelReservation.setType(reservationType);

        // Comments
        hotelReservation.setComments(createACRSComments(roomReservation.getAdditionalComments(),roomReservation.getComments(),maxAcrsCommentLength));

        hotelReservationReq.setHotelReservation(hotelReservation);
        reservationReq.setData(hotelReservationReq);
        return reservationReq;
    }

    /**
     *
     * @param additionalComments
     * @param commentTxt
     * @param maxAcrsCommentLength
     * @return
     */
    public static List<Comment> createACRSComments(List<String> additionalComments, String commentTxt, int maxAcrsCommentLength) {

        List<Comment> commentList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(additionalComments) || StringUtils.isNotEmpty(commentTxt)) {
            Comment comment = new Comment();
            List<String> allComments = new ArrayList<>();
            List<String> splitComment = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(additionalComments)) {
                additionalComments.forEach(additionalComment -> {
                    if (additionalComment.length() > maxAcrsCommentLength) {
                        String[] splitComments = additionalComment.split("(?<=\\G.{" + maxAcrsCommentLength + "})");
                        splitComment.addAll(Arrays.asList(splitComments));
                    } else {
                        allComments.add(additionalComment);
                    }

                });
            }
            if (StringUtils.isNotEmpty(commentTxt)) {
                allComments.add(commentTxt);
            }
            buildSplitComments(commentList, comment, splitComment);
            buildAllComments(commentList, allComments);
        }
        return commentList;
    }

    private static void buildSplitComments(List<Comment> commentList, Comment comment, List<String> splitComment) {
        if(CollectionUtils.isNotEmpty(splitComment)){
            comment.setText(splitComment);
            comment.setType(TypeOfComments.valueOf("SUPPLEMENTARY_INFO"));
            commentList.add(comment);

        }
    }

    private static void buildAllComments(List<Comment> commentList, List<String> allComments) {
        if(CollectionUtils.isNotEmpty(allComments)){
            for(String comments: allComments) {
                Comment allComment = new Comment();
                allComment.setText(Collections.singletonList(comments));
                allComment.setType(TypeOfComments.valueOf("SUPPLEMENTARY_INFO"));
                commentList.add(allComment);
            }
        }
    }

    public static SegmentReqItem buildACRSSegmentFromRoomReservation(RoomReservation roomReservation) {
        return buildACRSSegmentFromRoomReservation(roomReservation, -1, -1);
    }

    /**
     * Build an SCRS Segment from a RBS room Reservation TODO move to
     * BaseAcrsTransformer
     *
     * @param roomReservation
     * @param segmentHolderId
     * @param bookerId
     * @return
     */
    public static SegmentReqItem buildACRSSegmentFromRoomReservation(RoomReservation roomReservation, int segmentHolderId,
                                                               int bookerId) {
        SegmentReqItem segment = new SegmentReqItem();
        // Profile can be null at the time of Charges Before Create - Handle Override
        // Price
        if (roomReservation.getProfile() != null) {
            segment.setSegmentHolderId(segmentHolderId);
            segment.setBookerId(bookerId);
        }
        segment.setId(-1);
        segment.setPropertyCode(roomReservation.getPropertyId());
        // Segment - Offer Req
        OfferReq offerReq = new OfferReq();
        offerReq.setPolicyTypeCode(roomReservation.getGuaranteeCode());
        if (roomReservation.getIsGroupCode()) {
            offerReq.setGroupCode(roomReservation.getProgramId());
        }

        // Segment - Offer Req - Product Use
        ProductUseReq productUseReq = buildACRSProductUseReq(roomReservation.getCheckInDate(),
                roomReservation.getCheckOutDate(), roomReservation.getRoomTypeId(), roomReservation.getNumRooms(),
                roomReservation.getNumChildren(), roomReservation.getNumAdults(), roomReservation.getIsGroupCode(),
                roomReservation.getProgramId(), roomReservation.getBookings(), roomReservation.getRoutingInstructions());

        // perpetual flag
        offerReq.setIsPerpetualOffer(roomReservation.isPerpetualPricing());
        offerReq.setProductUses(productUseReq);
        //promocode INC-4
        offerReq.setPromoCode(roomReservation.getPromo());
        segment.setOffer(offerReq);

        // set customData for overridden program
        if (CollectionUtils.isNotEmpty(roomReservation.getBookings())) {
            segment.setCustomData(
                    buildACRSReservationCustomData(roomReservation.getProgramId(), roomReservation.getBookings()));
        }
        // INC-3 special requests
        if (CollectionUtils.isNotEmpty(roomReservation.getSpecialRequestObjList())) {
            segment.setServiceRequests(buildACRSServiceRequestsFromSpecialRequestsLists(roomReservation.getSpecialRequestObjList()));
        }
        // alert inc-4
        if (CollectionUtils.isNotEmpty(roomReservation.getAlerts())) {
            segment.setAlerts(createACRSAlerts(roomReservation.getAlerts()));
        }

        // traces inc-4
        if (CollectionUtils.isNotEmpty(roomReservation.getTraces())) {
            segment.setTraces(createACRSTraces(roomReservation.getTraces()));
        }

        //ICEUCP-3292
        // If bookings has zero overridden price then set ForcedSell = true
        if(hasZeroOverriddenPrice(roomReservation.getBookings())){
            IndicatorsReq indicators = new IndicatorsReq();
            indicators.setIsForcedSell(true);
            segment.setIndicators(indicators);
        }

        return segment;
    }

    public boolean hasZeroOverriddenPrice(List<RoomPrice> bookings){
        return bookings != null && bookings.stream()
                .anyMatch(booking -> booking.getOverridePrice() == 0);
    }
    /**
     *
     * @param traces
     * @return
     */
    public static List<Traces> createACRSTraces(List<RoomReservationTrace> traces) {
        if(CollectionUtils.isNotEmpty(traces)) {
            return traces.stream().map(RoomReservationTransformer::createACRSTrace).collect(Collectors.toList());

        }else {
            return new ArrayList<>();
        }
    }

    private Traces createACRSTrace(RoomReservationTrace reaceReq) {
        Traces traces = new Traces();
        traces.setDate(CommonUtil.getDateStr(reaceReq.getDate(), ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
        traces.setDepartmentCode(reaceReq.getDepartmentCode());
        Description desc = new Description();
        desc.put(ServiceConstant.EN, Arrays.asList(reaceReq.getText()));
        traces.setDescription(desc);
        return traces;

    }

    /**
     *
     * @param alerts
     * @return
     */
    public static List<Alerts> createACRSAlerts(List<RoomReservationAlert> alerts) {
        if(CollectionUtils.isNotEmpty(alerts)) {
            return alerts.stream().map(RoomReservationTransformer::createACRSAlert).collect(Collectors.toList());
        }else {
            return new ArrayList<>();
        }
    }

    private Alerts createACRSAlert(RoomReservationAlert alertReq) {
        Alerts alerts = new Alerts();
        alerts.setArea(alertReq.getArea());
        alerts.setCode(alertReq.getCode());
        Description desc = new Description();
        desc.put(ServiceConstant.EN, Arrays.asList(alertReq.getDescription()));
        alerts.setDescription(desc);
        return alerts;

    }


    /**
     * it will create productUses request
     * @param checkInDate
     * @param checkOutDate
     * @param roomTypeId
     * @param roomCount
     * @param numChildren
     * @param numAdults
     * @param isGroup
     * @param programId
     * @param bookings
     * @param routingList
     * @return
     */
    public static ProductUseReq buildACRSProductUseReq(Date checkInDate, Date checkOutDate, String roomTypeId,
                                                                  int roomCount, int numChildren, int numAdults, boolean isGroup, String programId, List<RoomPrice> bookings, List<ReservationRoutingInstruction> routingList) {
        ProductUseReq productUseReq = new ProductUseReq();

        // for hold api bookings will be empty.
        if (CollectionUtils.isEmpty(bookings) || isGroup) {
            ProductUseReqItem productUse = createProductUse(checkInDate, checkOutDate, roomTypeId, roomCount, numChildren,
                    numAdults, null);
            // groupCode/ratePlanCode

            if (!isGroup) {
                productUse.setRatePlanCode(programId);
            } else if (CollectionUtils.isNotEmpty(bookings)){
                // add override price if any for group code
                List<RateReq> requestedProductRate = new ArrayList<>();
                for (RoomPrice booking : bookings) {
                    List<RateReq> overrideReq = getOverrideReq(booking);

                    if (CollectionUtils.isNotEmpty(overrideReq)) {
                        requestedProductRate.addAll(overrideReq);
                    }
                }
                productUse.setRequestedProductRates(requestedProductRate);
            }

            productUseReq.add(productUse);
        } else if (CollectionUtils.isNotEmpty(bookings)) {
            // create product use for the first booking
            int firstBookingIndex = 0;
            RoomPrice firstBooking = bookings.get(firstBookingIndex);
            if(ReservationUtil.isFirstDateAfterSecondDateExcludingTime(firstBooking.getDate(),checkInDate)) {
            	log.warn("Perpetual flow, checkinDate is outside the provided booking window, productUse will be returned empty");
            	return productUseReq;
            }
            
            // handle if checkInDat	e doesn't match first booking object (skip booking objects until match checkInDate
            while (!ReservationUtil.areDatesEqualExcludingTime(checkInDate, firstBooking.getDate())) {
                firstBookingIndex++;
                firstBooking = bookings.get(firstBookingIndex);
            }

            ProductUseReqItem productUse = buildProductUse(checkInDate, checkOutDate, roomTypeId, roomCount, numChildren,
                    numAdults, firstBooking);
            productUseReq.add(productUse);
            for (int i = ++firstBookingIndex; i < bookings.size(); i++) {
                RoomPrice booking = bookings.get(i);
                if (ReservationUtil.areDatesEqualExcludingTime(checkOutDate, booking.getDate())){
                    // booking date is equal to checkOut date meaning previous booking was final night on new productUse
                    break;
                }
                // if next booking has same effective rateplan then update end
                // date existing productUse.
                if (hasSameRatePlan(productUse, booking)) {
                    productUse.getPeriod()
                            .setEnd(ReservationUtil.convertDateToLocalDate(booking.getDate()).plusDays(1));
                    // add override price if any
                    List<RateReq> overrideReq = getOverrideReq(booking);
                    if (CollectionUtils.isNotEmpty(overrideReq)) {
                        productUse.getRequestedProductRates().addAll(overrideReq);
                    }
                } else {
                    // if next booking hasn't same effective rateplan then
                    // create new productUse
                    productUse = buildProductUse(checkInDate, checkOutDate, roomTypeId, roomCount, numChildren,
                            numAdults,booking);
                    productUseReq.add(productUse);
                }
            }
        }

        // TODO any change needed for new checkin/checkout dates?
        // add routing instructions in to product uses
        if (CollectionUtils.isNotEmpty(routingList)) {
            List<RoutingInstruction> acrsRoutingList = BaseAcrsTransformer
                    .buildACRSRoutingInstructions(routingList);
            acrsRoutingList.forEach(routing -> {
                productUseReq.forEach(productUse -> {
                    productUse.setRoutingInstructions(buildProductRoutingInstructions(routing, productUse, ReservationUtil.convertDateToLocalDate(checkOutDate)));
                });
            });
        }
        return productUseReq;
    }

    /**
     * If routing date range belongs into product date range then return the same routing with existing RIs.
     * If Product date range belongs into RI date range then create new RI with product date range and return with existing RIs.
     * @param routing
     * @param productUse
     * @return
     */
    private static List<RoutingInstruction> buildProductRoutingInstructions(RoutingInstruction routing,
                                                                                       ProductUseReqItem productUse, LocalDate checkOutDate) {
        List<RoutingInstruction> routingList = buildProductRIs(routing, productUse.getPeriod().getStart(), productUse.getPeriod().getEnd(), checkOutDate);
        if (CollectionUtils.isNotEmpty(productUse.getRoutingInstructions())) {
            routingList.addAll(productUse.getRoutingInstructions());
        }
        return routingList;
    }

    private static List<RoutingInstruction> buildProductRIs(RoutingInstruction routing, LocalDate productStart, LocalDate productEnd, LocalDate checkOutDate) {
        List<RoutingInstruction> routingList = new ArrayList<>();
        LocalDate routingStart = routing.getPeriod().getStart();
        LocalDate routingEnd = routing.getPeriod().getEnd();
        if (CommonUtil.isRoutingBelongsToProductDateRange(routingStart, routingEnd, productStart, productEnd, checkOutDate)) {
            routingList.add(routing);
        } else if (CommonUtil.isProductBelongsToRoutingDateRange(routingStart, routingEnd, productStart, productEnd)) {
            //split routing as product data range.
            // create new routing with product date range.
            RoutingInstruction productRouting = copyACRSRoutingInstruction(routing);
            productRouting.getPeriod().setStart(productStart);
            if((routingEnd.isAfter(productEnd) || routingEnd.isEqual(productEnd)) && productEnd.isBefore(checkOutDate)) {
                productRouting.getPeriod().setEnd(productEnd.minusDays(1));
            }
            routingList.add(productRouting);
        } else if (CommonUtil.isProductIntersectsRouting(routingStart, routingEnd, productStart, productEnd)) {
            //Intersection case
            RoutingInstruction productRouting = copyACRSRoutingInstruction(routing);
            if(productStart.isAfter(routingStart)) {
                productRouting.getPeriod().setStart(productStart);
            }
            if(productEnd.isBefore(routingEnd)) {
                productRouting.getPeriod().setEnd(productEnd.minusDays(1));
            }
            routingList.add(productRouting);
        }
        return routingList;
    }

    public static RoutingInstruction copyACRSRoutingInstruction(RoutingInstruction routing) {
        RoutingInstruction productRouting = new RoutingInstruction();
        Period period = new Period();
        period.setStart(routing.getPeriod().getStart());
        period.setEnd(routing.getPeriod().getEnd());
        productRouting.setPeriod(period);
        productRouting.setAuthorizerCode(routing.getAuthorizerCode());
        productRouting.setSource(routing.getSource());
        productRouting.setWindow(routing.getWindow());
        productRouting.setLimitCriteria(routing.getLimitCriteria());
        productRouting.setLimitValue(routing.getLimitValue());
        productRouting.setIsDaily(routing.isIsDaily());
        productRouting.setRoutingCode(routing.getRoutingCode());
        productRouting.setComment(routing.getComment());
        productRouting.setDow(routing.getDow());
        return productRouting;
    }

    /**
     * this will create new ACRS product use
     * @param checkInDate
     * @param checkOutDate
     * @param roomTypeId
     * @param roomCount
     * @param numChildren
     * @param numAdults
     * @param booking
     * @return
     */
    private static ProductUseReqItem buildProductUse(Date checkInDate, Date checkOutDate, String roomTypeId,
                                              int roomCount, int numChildren, int numAdults, RoomPrice booking) {
        ProductUseReqItem productUse = createProductUse(checkInDate, checkOutDate, roomTypeId, roomCount, numChildren,
                numAdults, booking.getDate());
        productUse.setRatePlanCode( Optional.ofNullable(booking.getOverrideProgramId()).orElse(booking.getProgramId()));
        List<RateReq> overrideReq = getOverrideReq(booking);
        productUse.setRequestedProductRates(overrideReq);
        return productUse;

    }
    /**
     * it will check whether booking has same effective ratePlan or not
     * @param produtuse
     * @param booking
     * @return
     */
    private static boolean hasSameRatePlan(ProductUseReqItem produtuse, RoomPrice booking) {
        return Optional.ofNullable(booking.getOverrideProgramId()).orElse(booking.getProgramId())
                .equalsIgnoreCase(produtuse.getRatePlanCode());

    }

    /**
     *
     * @param primaryProgram
     * @param bookings
     * @return
     */
    public static ReservationCustomData buildACRSReservationCustomData(String primaryProgram,
            List<RoomPrice> bookings) {
        ReservationCustomData customData = null;
        // Set Primary Program in all the cases, so that it will be used to fill
        // response.programId while retrieve the reservation.
        List<ActualBookingProgram> overriddenBookings = null;
        if (CollectionUtils.isNotEmpty(bookings)) {
            overriddenBookings = bookings.stream()
                    .filter(booking -> StringUtils.isNotEmpty(booking.getOverrideProgramId()))
                    .map(RoomReservationTransformer::createCustomProgramData).collect(Collectors.toList());

        }
        if (StringUtils.isNotEmpty(primaryProgram) || CollectionUtils.isNotEmpty(overriddenBookings)) {
            customData = new ReservationCustomData();
            customData.setParentProgram(primaryProgram);
            if (CollectionUtils.isNotEmpty(overriddenBookings)) {
                customData.setActualBookingPrograms(overriddenBookings);
            }
        }
        return customData;
    }

    /**
     *
     * @param booking
     * @return
     */
    private static ActualBookingProgram createCustomProgramData(RoomPrice booking) {
        ActualBookingProgram overidenProgram = new ActualBookingProgram();
        overidenProgram.setDate(ReservationUtil.convertDateToLocalDate(booking.getDate()));
        overidenProgram.setProgram(booking.getProgramId());
        overidenProgram.setPrice(booking.getPrice());
        return overidenProgram;
    }

    public static ProductUseReqItem createProductUse(Date checkInDate, Date checkOutDate, String roomTypeId, int roomCount,int numChildren, int numAdults, Date bookingDate) {
        // Product Use - Guest Count
        List<GuestCountReq> guestCountReqs =
                createACRSGuestCounts(numAdults, numChildren);
        Period period;
        if(bookingDate != null) {
            period = createACRSPeriodfromBookings(bookingDate);
        }else {
            period = createACRSPeriod(checkInDate, checkOutDate);
        }

        return createProductUse(period, roomTypeId, roomCount, guestCountReqs);
    }

    public static ProductUseReqItem createProductUse(Period period, String inventoryTypeCode, int roomCount, List<GuestCountReq> guestCountReqs) {
        ProductUseReqItem productUse = new ProductUseReqItem();
        productUse.setQuantity(roomCount <= 0 ? 1 : roomCount);
        productUse.setInventoryTypeCode(inventoryTypeCode);
        productUse.setGuestCounts(guestCountReqs);
        productUse.setPeriod(period);
        return productUse;
    }

    private static List<RateReq> getOverrideReq( RoomPrice booking) {
        List<RateReq> overrideReq = new ArrayList<>();
        //skip this block if its
        boolean overrideWithCompZeroPrice = StringUtils.isNotBlank(booking.getOverrideProgramId()) && booking.isComp() && booking.getOverridePrice() == 0;
        if (booking.getOverridePrice() > -1 && !overrideWithCompZeroPrice) {
            RateReq rateReq = new RateReq();
            BaseReq base = new BaseReq();
            base.setAmount(String.valueOf(booking.getOverridePrice()));
            base.setOverrideInd(true);
            if (null != booking.getDate()) {
                LocalDate date = ReservationUtil.convertDateToLocalDate(booking.getDate());
                rateReq.setStart(date);
                rateReq.setEnd(date.plusDays(1));
            }
            rateReq.setBase(base);
            overrideReq.add(rateReq);
        }
        return overrideReq;

    }

    public static PaymentInfoReq createACRSPaymentInfoFromCreditCardCharge(CreditCardCharge creditCardCharge, double amountDue, String guaranteeCode, Map<String, PaymentType> paymentTypeGuaranteeCodeMap) {
        PaymentInfoReq paymentInfoReq = new PaymentInfoReq();
        //GUARANTEE by card if cardInfo != null but card.amount =0
        //for override by 0.0 with addons card might not have 0.0 but amountDue to main segment will be 0.0
        if(null != creditCardCharge && amountDue == ServiceConstant.ZERO_DOUBLE_VALUE){
            paymentInfoReq.setPaymentType(PaymentType.NUMBER_5);
            paymentInfoReq.setPaymentIntent(PaymentIntent.GUARANTEE);
            paymentInfoReq.setPaymentCard(getCardInfo(creditCardCharge));
        }
        // GUARANTEE by company/ property (cardInfo == null and check the guarantee Code)
        // And payment by cash/DD
        else if(null == creditCardCharge && StringUtils.isNotEmpty(guaranteeCode)){
            paymentInfoReq.setPaymentIntent(PaymentIntent.GUARANTEE);
            PaymentType paymentType = getPaymentTypeByGuaranteeCode(guaranteeCode, paymentTypeGuaranteeCodeMap);
            if(null != paymentType){
                paymentInfoReq.setPaymentType(paymentType);
            }else{
                paymentInfoReq.setPaymentType(PaymentType.NUMBER_1); // cash
            }
            if(PaymentType.NUMBER_30 == paymentInfoReq.getPaymentType()){
                // currently hardCoded
                paymentInfoReq.setGuaranteePaymentId(ServiceConstant.MGM_STR);
            }
        } else if (null != creditCardCharge && amountDue > ServiceConstant.ZERO_DOUBLE_VALUE) {
            // DEPOSIT if cardInfo != null but card.amount >0
			// when ACRS will support overridden deposit then this amount will be card.amount
			paymentInfoReq.setAmount(String.valueOf(CommonUtil.round(amountDue, ServiceConstant.THREE_DECIMAL_PLACES)));
			paymentInfoReq.setPaymentStatus(PaymentStatus.PAYMENT_RECEIVED);
			paymentInfoReq.setPaymentIntent(PaymentIntent.GUARANTEE);
			paymentInfoReq.setPaymentType(PaymentType.NUMBER_5);
			paymentInfoReq.setPaymentCard(getCardInfo(creditCardCharge));
        }
       return paymentInfoReq;
    }

    private PaymentCard getCardInfo(CreditCardCharge creditCardCharge){
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(creditCardCharge.getHolder());
        CardNumberReq cardNumber = new CardNumberReq();
        paymentCard.setExpireDate(BaseAcrsTransformer.dateToACRSExpiryDateString(creditCardCharge.getExpiry()));
        // ACRS is accepting two letter code, e.g "MC"
        paymentCard.setCardCode(CreditCardCodes.getCodeFromValue(creditCardCharge.getType()));
        cardNumber.setToken(creditCardCharge.getCcToken());
        paymentCard.setCardNumber(cardNumber);
        paymentCard.setBillingAddress(createACRSBillingAddress(creditCardCharge.getHolderProfile()));
        return paymentCard;
    }


    private static PaymentType getPaymentTypeByGuaranteeCode(String guaranteeCode, Map<String, PaymentType> paymentTypeGuaranteeCodeMap) {
        return paymentTypeGuaranteeCodeMap.get(guaranteeCode);
    }

    private static AddressWithoutIndex createACRSBillingAddress(CardHolderProfile cardHolderProfile) {
        AddressWithoutIndex billingAddress = new AddressWithoutIndex();
        if (null != cardHolderProfile && null != cardHolderProfile.getAddress()) {
            AddressLines addLines = new AddressLines();
            ProfileAddress profileAddress = cardHolderProfile.getAddress();
            if(StringUtils.isNotBlank(profileAddress.getStreet1())) {
                addLines.add(profileAddress.getStreet1());
            }
            if (StringUtils.isNotBlank(profileAddress.getStreet2())) {
                addLines.add(profileAddress.getStreet2());
            }
            billingAddress.setAddressLines(addLines);
            if (StringUtils.isNotBlank(profileAddress.getCity())) {
                billingAddress.setCityName(profileAddress.getCity());
            }
            if (StringUtils.isNotBlank(profileAddress.getPostalCode())) {
                billingAddress.setPostalCode(profileAddress.getPostalCode());
            }
            if (StringUtils.isNotBlank(profileAddress.getState())) {
                billingAddress.setStateProv(profileAddress.getState());
            }
            if (StringUtils.isNotBlank(profileAddress.getCountry())) {
                billingAddress.setCountryCode(profileAddress.getCountry());
            }
        }
        return billingAddress;
    }

    public static List<PaymentTransactionReq> createACRSPaymentTransactionReqFromCreditCardCharges(
            List<CreditCardCharge> creditCardCharges, List<Payment> refundPayments) {
        String txnDate = CommonUtil.getDateStr(new Date(), ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS);
        return creditCardCharges.stream().map(creditCardCharge -> {
            PaymentTransactionReq req = createACRSPaymentTransactionReqFromCreditCardCharge(creditCardCharge,txnDate);
            if (CollectionUtils.isNotEmpty(refundPayments) && null != req) {
                Optional<Payment> refundPaymentTxn = refundPayments.stream().filter(
                                refundPayment -> refundPayment.getChargeCardNumber().equalsIgnoreCase(creditCardCharge.getNumber()))
                        .findFirst();
                refundPaymentTxn.ifPresent(payment -> req.setPaymentRecordId(payment.getTransactionId()));
            }
            return req;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static PaymentTransactionReq createACRSPaymentTransactionReqFromCreditCardCharge(CreditCardCharge creditCardCharge , String txnDate) {
        // set if paid by cash
        if (creditCardCharge.getAmount() != 0 && null == creditCardCharge.getNumber()) {
            PaymentTransactionReq paymentTransactionReq = new PaymentTransactionReq();
            paymentTransactionReq.setPaymentType(PaymentType.NUMBER_1);
            paymentTransactionReq.setPaymentIntent(PaymentIntent.DEPOSIT);
            paymentTransactionReq.setAmount(String.valueOf(CommonUtil.round(creditCardCharge.getAmount(),ServiceConstant.THREE_DECIMAL_PLACES)));
            paymentTransactionReq.setTransactionDate(txnDate);
          
            return paymentTransactionReq;
        }
        // set deposit payments if not GUARANTEE i.e. amount>0 and card number
        // is there
        else if (null != creditCardCharge.getNumber()) {
            boolean isRefund = creditCardCharge.getAmount() < 0;
            PaymentTransactionReq paymentTransactionReq = new PaymentTransactionReq();
            if (isRefund) {
                paymentTransactionReq.setPaymentIntent(PaymentIntent.REFUND);
                paymentTransactionReq.setAmount(String.valueOf(CommonUtil.round(
                        creditCardCharge.getAmount(), ServiceConstant.THREE_DECIMAL_PLACES) * (-1)));
            } else {
                paymentTransactionReq.setPaymentIntent(PaymentIntent.DEPOSIT);
                paymentTransactionReq.setAmount(String.valueOf(CommonUtil.round(
                        creditCardCharge.getAmount(), ServiceConstant.THREE_DECIMAL_PLACES)));
            }

            paymentTransactionReq.setPaymentStatus(PaymentStatus.WAITING_PAYMENT);
            paymentTransactionReq.setPaymentType(PaymentType.NUMBER_5);
            paymentTransactionReq.setTransactionDate(txnDate);
            PaymentCard paymentCard = new PaymentCard();
            paymentCard.setCardHolderName(creditCardCharge.getHolder());
            CardNumberReq cardNumber = new CardNumberReq();

            paymentCard.setExpireDate(BaseAcrsTransformer.dateToACRSExpiryDateString(creditCardCharge.getExpiry()));
            paymentCard.setCardCode(CreditCardCodes.getCodeFromValue(creditCardCharge.getType()));
            cardNumber.setToken(creditCardCharge.getCcToken());
            paymentCard.setCardNumber(cardNumber);

            // billing address
            paymentCard.setBillingAddress(createACRSBillingAddress(creditCardCharge.getHolderProfile()));
            paymentTransactionReq.setPaymentCard(paymentCard);
            return paymentTransactionReq;
        } else {
            return null; // GUARANTEE
        }
    }

    public static List<ServiceRequests> buildACRSServiceRequestsFromSpecialRequestsLists(List<ReservationSplRequest> list) {
        return list.stream().map(RoomReservationTransformer::buildACRSServiceRequestFromSplReqs).collect(Collectors.toList());
    }

    private static ServiceRequests buildACRSServiceRequestFromSplReqs(ReservationSplRequest splReq) {
        ServiceRequests serviceRequests = new ServiceRequests();
        serviceRequests.setCode(splReq.getCode());
        serviceRequests.setQuantity(1);
        //INC-4 type

        if (ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST.equalsIgnoreCase(splReq.getType()) && null != splReq.getExactType()) {
            if (ServiceConstant.ROOMFEATURE_FORMAT.equalsIgnoreCase(splReq.getExactType())) {
                serviceRequests.setType(ServiceConstant.ROOMFEATURE_ACRS_TYPE);
            } else {
                serviceRequests.setType(ServiceConstant.SPECIALREQ_ACRS_TYPE);
            }
        } else {
            if (ServiceConstant.ROOMFEATURE_FORMAT.equalsIgnoreCase(splReq.getType())) {
                serviceRequests.setType(ServiceConstant.ROOMFEATURE_ACRS_TYPE);
            } else {
                serviceRequests.setType(ServiceConstant.SPECIALREQ_ACRS_TYPE);
            }
        }
        Description description = new Description();
        // TODO lookup and provide descriptions by language
        List<String> descriptions = new ArrayList<>();
        descriptions.add(splReq.getDescription());
        description.put("en", descriptions);
        serviceRequests.setDescription(description);
        return serviceRequests;
    }

    public static GuestsPendingReqItem reservationProfileToACRSUserProfileReq(ReservationProfile profile, int customerRank, int customerSegment, String dominantPlay) {
        GuestsPendingReqItem userProfileReq = new GuestsPendingReqItem();
        // TODO are these correctly mapped?
        //Setting MlifeNo in GuestProfileId to support booking limit calculation.
        if(profile.getMlifeNo() > 0) {
            userProfileReq.setGuestProfileId(String.valueOf(profile.getMlifeNo()));
        }
        // TODO setting to -1 for now as only one profile per reservation
        userProfileReq.setId(-1);

        // Name
        PersonNamePending personName = new PersonNamePending();
        Optional<String> title = Optional.ofNullable(profile.getTitle());
        title.ifPresent(personName::setTitle);
        Optional<String> firstName = Optional.ofNullable(profile.getFirstName());
        firstName.ifPresent(personName::setGivenName);
        Optional<String> lastName = Optional.ofNullable(profile.getLastName());
        lastName.ifPresent(personName::setSurname);
        userProfileReq.setPersonName(personName);

        // dob
        if (profile.getDateOfBirth() != null) {
            userProfileReq
                    .setAge(DateUtil.calculateAge(ReservationUtil.convertDateToLocalDate(profile.getDateOfBirth())));
        }

        // Addresses
        if (CollectionUtils.isNotEmpty(profile.getAddresses())) {
            List<AddressWithoutIndex> addresses = profile.getAddresses().stream()
                    .map(RoomReservationTransformer::addressToACRSAddressWithoutIndexTransform)
                    .collect(Collectors.toList());
            userProfileReq.setAddresses(addresses);
        }

        // Telephones
        //Telephones is not mandatory in ICE.
        if (CollectionUtils.isNotEmpty(profile.getPhoneNumbers())) {
            List<Telephone> telephones = profile.getPhoneNumbers().stream()
                    .map(RoomReservationTransformer::phoneNumberToACRSTelephone).collect(Collectors.toList());
            userProfileReq.setTelephones(telephones);
        }

        // E-mails
        Email email = new Email();
        //Email is not mandatory in ICE.
        if (StringUtils.isNotBlank(profile.getEmailAddress1())) {
            email.setType(EmailType.NUMBER_1);
            email.setAddress(profile.getEmailAddress1());
            userProfileReq.addEmailsItem(email);
        }

        if (StringUtils.isNotBlank(profile.getEmailAddress2())) {
            Email email2 = new Email();
            email2.setType(EmailType.NUMBER_1);
            email2.setAddress(profile.getEmailAddress2());
            userProfileReq.addEmailsItem(email2);
        }

        // Loytalty Program
        //INC-3
        // customerRank
        // customerSegment
        // customerDominantPlay
        if (profile.getMlifeNo() > 0) {
            LoyaltyProgram loyaltyProgram = new LoyaltyProgram();
            loyaltyProgram.setLoyaltyId(Integer.toString(profile.getMlifeNo()));
            if (customerRank > 0 || customerSegment > 0) {
                LoyaltyProgramQualifier qualifier = new LoyaltyProgramQualifier();
                if (customerRank > 0) {
                    qualifier.setTier(Integer.toString(customerRank));
                } else if (customerSegment > 0) {
                    qualifier.setTier(Integer.toString(customerSegment));
                }
				if (ServiceConstant.SLOT.equals(dominantPlay.toUpperCase())) {
					qualifier.setDominance(ServiceConstant.SLOTS);
				} else {
					qualifier.setDominance(dominantPlay);
				}               
                loyaltyProgram.setQualifier(qualifier);
            }

            LoyaltyProgramProgramsItem program = new LoyaltyProgramProgramsItem();
            program.programName("PC");

            List<LoyaltyProgramProgramsItem> programList = new ArrayList<>();
            programList.add(program);

            loyaltyProgram.setPrograms(programList);

            userProfileReq.setLoyaltyProgram(loyaltyProgram);
        }

        // Add partner information
        if(CollectionUtils.isNotEmpty(profile.getPartnerAccounts())){
           List<AdditionalMembershipProgram> additionalMembershipPrograms =profile.getPartnerAccounts().stream().map(p -> createACRSPartnerAccounts(p)).collect(Collectors.toList());
            userProfileReq.setAdditionalMembershipPrograms(additionalMembershipPrograms);
        }

        return userProfileReq;
    }

    private static AdditionalMembershipProgram createACRSPartnerAccounts(PartnerAccounts p) {
        AdditionalMembershipProgram program = new AdditionalMembershipProgram();
        program.setId(p.getPartnerAccountNo());
        program.setProgramCode(p.getProgramCode());
        program.setLevel(p.getMembershipLevel());
        return program;
    }

    private static Telephone phoneNumberToACRSTelephone(ProfilePhone profilePhone) {
        Telephone telephone = new Telephone();
        // Number
        // TODO Do we need to parse phone number into number/areacode/extension?
        telephone.setNumber(profilePhone.getNumber());
        // Type
        // LocationType default to Home, will update below if ProfilePhone type is business
        telephone.setLocationType(PhoneLocationType.NUMBER_6);
        // Convert Fax to Fax(3), Mobile to Mobile (5), and everything else to Phone(1)
        if ( "Fax".equalsIgnoreCase(profilePhone.getType()) ) {
            telephone.setDeviceType(PhoneDeviceType.NUMBER_3);
        } else if ( "Mobile".equalsIgnoreCase(profilePhone.getType()) ) {
            telephone.setDeviceType(PhoneDeviceType.NUMBER_5);
        } else {
            if ( "Business".equalsIgnoreCase(profilePhone.getType()) ) {
                telephone.setLocationType(PhoneLocationType.NUMBER_7);
            }
            telephone.setDeviceType(PhoneDeviceType.NUMBER_1);
        }
        return telephone;
    }

    private static AddressWithoutIndex addressToACRSAddressWithoutIndexTransform(ProfileAddress address) {
        if ( null == address ) {
            return null;
        }
        AddressWithoutIndex addressWithoutIndex = new AddressWithoutIndex();
        if(StringUtils.isNotBlank(address.getCity())) {
            addressWithoutIndex.setCityName(address.getCity());
        }

        // TODO convert country to country code if not done already
        if(StringUtils.isNotBlank(address.getCountry())) {
            addressWithoutIndex.setCountryCode(address.getCountry());
        }
        if(StringUtils.isNotBlank(address.getState())) {
            addressWithoutIndex.setStateProv(address.getState());
        }
        AddressLines addressLines = new AddressLines();
        if(StringUtils.isNotBlank(address.getStreet1())) {
            addressLines.add(address.getStreet1());
        }
        //check for null, empty and whitespaces
        if(StringUtils.isNotBlank(address.getStreet2())) {
            addressLines.add(address.getStreet2());
        }
        addressWithoutIndex.setAddressLines(addressLines);
        if(StringUtils.isNotBlank(address.getPostalCode())) {
            addressWithoutIndex.setPostalCode(address.getPostalCode());
        }

        // Type conversion
        // In GSE we have the following types: Home, home, Business, Alternate, other, & Other
        // Map Business to 2 and everything else to 1
        if ( "business".equalsIgnoreCase(address.getType()) ) {
            addressWithoutIndex.setType(AddressType.NUMBER_2);
        } else {
            addressWithoutIndex.setType(AddressType.NUMBER_1);
        }

        // TODO Preferred flag mapping?

        // Return
        return addressWithoutIndex;
    }

    public static RoomReservation transform(ReservationRes acrsResponse, AcrsProperties acrsProperties) throws ParseException {
        final String channelCode = acrsResponse.getData().getCreator() != null ? acrsResponse.getData().getCreator().getChannelCode() : null;
        RoomReservation roomReservation = BaseAcrsTransformer
                .createRoomReservationFromHotelReservationRes(channelCode, acrsResponse.getData().getHotelReservation(), acrsProperties);
        // rrUpSell
        if (null != acrsResponse.getData().getRequestor()
                && CollectionUtils.isNotEmpty(acrsResponse.getData().getRequestor().getCallCenterInformation())) {
            roomReservation
                    .setRrUpSell(acrsResponse.getData().getRequestor().getCallCenterInformation().get(0).getId());
        }

        roomReservation.setCrsWarnings(acrsResponse.getWarnings());

        // set Origin for bookingSource and bookingChannel fill up.
        PointOfSale creator = acrsResponse.getData().getCreator();
        if (null != creator && null != creator.getReportingData()) {
            roomReservation.setOrigin(creator.getReportingData().getSubChannel());
        }

        return roomReservation;
    }

    /**
     * Builds SourceRoomReservationBasicInfoRequestDTO object.
     *
     * @param request request
     * @return SourceRoomReservationBasicInfoRequestDTO request
     */
    public static SourceRoomReservationBasicInfoRequestDTO buildSourceRoomReservationBasicInfoRequest(
            RoomReservationBasicInfoRequest request) {
        return SourceRoomReservationBasicInfoRequestDTO.builder().confirmationNumber(request.getConfirmationNumber())
                .operaPartyCode(request.getOperaPartyCode()).source(request.getSource())
                .mlifeNumber(request.getMlifeNumber()).customerId(request.getCustomerId()).build();

    }

    /**
     * Builds aurora request to access sourceRoomReservationBasicInfo.
     *
     * @param requestDTO DTO.
     * @return SourceRoomReservationBasicInfoRequest request.
     */
    public static SourceRoomReservationBasicInfoRequest buildAuroraSourceRoomReservationRequest(
            SourceRoomReservationBasicInfoRequestDTO requestDTO) {
        SourceRoomReservationBasicInfoRequest request = MessageFactory.createSourceRoomReservationBasicInfoRequest();
        request.setConformationNumber(requestDTO.getConfirmationNumber());
        request.setOperaPartyCode(requestDTO.getOperaPartyCode());
        return request;
    }

    /**
     * Transforms Aurora response to DTO.
     *
     * @param response aurora response.
     * @return ReservationAdditionalInfoResponse response.
     */
    public ReservationsBasicInfoResponse transform(SourceRoomReservationBasicInfoResponse response) {
        List<ReservationBasicInfo> reservations = new ArrayList<>();

        Arrays.stream(response.getRoomResvBasicInfos()).forEach(reservation -> {
            ReservationBasicInfo reservationAdditionalInfo = new ReservationBasicInfo();
            reservationAdditionalInfo.setOperaConfNo(reservation.getOperaConfNo());
            reservationAdditionalInfo.setConfNo(reservation.getGseConfNo());
            reservationAdditionalInfo.setExternalConfNo(reservation.getExternalConfNo());
            reservationAdditionalInfo.setPartyConfNo(reservation.getGsePartyConfNo());
            reservationAdditionalInfo.setOperaPartyCode(reservation.getOperaPartyCode());
            reservationAdditionalInfo.setGuestName(reservation.getGuestName());
            reservationAdditionalInfo.setGuestFName(reservation.getGuestFName());
            reservationAdditionalInfo.setGuestLname(reservation.getGuestLname());
            reservationAdditionalInfo.setRoomTypeCode(reservation.getRoomTypeCode());
            reservationAdditionalInfo.setRoomCount(reservation.getRoomCount());
            reservationAdditionalInfo.setAdultCount(reservation.getAdultCount());
            reservationAdditionalInfo.setChildCount(reservation.getChildCount());
            reservationAdditionalInfo.setCheckInDate(DateUtil.convertDateToString(ServiceConstant.ISO_8601_DATE_FORMAT,
                    reservation.getCheckInDate(), TimeZone.getDefault()));
            reservationAdditionalInfo.setCheckOutDate(DateUtil.convertDateToString(ServiceConstant.ISO_8601_DATE_FORMAT,
                    reservation.getCheckOutDate(), TimeZone.getDefault()));
            reservationAdditionalInfo.setResvNameId(reservation.getResvNameId());
            reservationAdditionalInfo.setReservationTypes(reservation.getReservationTypes());
            reservationAdditionalInfo.setPrimarySharerConfNo(reservation.getPrimarySharerConfNo());
            reservationAdditionalInfo.setStatus(reservation.getStatus());
            reservationAdditionalInfo.setResType(reservation.getResType());
            reservations.add(reservationAdditionalInfo);
        });
        return new ReservationsBasicInfoResponse(reservations);
    }

    /**
     * Creates DTO object for updateProfileInfo for room reservation.
     *
     * @param request request object.
     * @return UpdateProfileInfoRequestDTO dto.
     */
    public static UpdateProfileInfoRequestDTO createModifyProfileInfoRequestDTO(UpdateProfileInfoRequest request) {
        return UpdateProfileInfoRequestDTO.builder()
                .itineraryId(request.getItineraryId())
                .reservationId(request.getReservationId())
                .source(request.getSource())
                .propertyId(request.getPropertyId())
                .moveItinerary(request.isMoveItinerary())
                .confirmationNumber(request.getConfirmationNumber())
                .userProfile(request.getUserProfile())
                .originalReservation(request.getOriginalReservation())
                .build();
    }

    /**
     * Prepares aurora request from request dto.
     *
     * @param requestDTO request dto.
     * @return ModifyProfileRoomReservationRequest aurora request
     */
    public static ModifyProfileRoomReservationRequest createAuroraModifyProfileRoomReservationRequest(
            UpdateProfileInfoRequestDTO requestDTO) {
        ModifyProfileRoomReservationRequest modifyProfileRoomReservationRequest = MessageFactory
                .createModifyProfileRoomReservationRequest();
        modifyProfileRoomReservationRequest.setItineraryId(requestDTO.getItineraryId());
        modifyProfileRoomReservationRequest.setReservationId(requestDTO.getReservationId());
        if (null != requestDTO.getUserProfile()) {
            CustomerProfile profile = CustomerProfile.create();
            profile.setId(requestDTO.getUserProfile().getId());
            profile.setOperaId(requestDTO.getUserProfile().getOperaId());
            profile.setFirstName(requestDTO.getUserProfile().getFirstName());
            profile.setLastName(requestDTO.getUserProfile().getLastName());
            profile.setEmailAddress1(requestDTO.getUserProfile().getEmailAddress1());
            profile.setEmailAddress2(requestDTO.getUserProfile().getEmailAddress2());
            profile.setDateOfBirth(requestDTO.getUserProfile().getDateOfBirth());
            profile.setMlifeNo(requestDTO.getUserProfile().getMlifeNo());
            profile.setHgpNo(requestDTO.getUserProfile().getHgpNo());
            profile.setSwrrNo(requestDTO.getUserProfile().getSwrrNo());

            if (null != requestDTO.getUserProfile().getPhoneNumbers()) {
                List<CustomerPhoneNumber> phoneNumbers = new ArrayList<>();
                requestDTO.getUserProfile().getPhoneNumbers().forEach(phoneNumberDTO -> {
                    CustomerPhoneNumber phoneNumber = CustomerPhoneNumber.create();
                    if (null != phoneNumberDTO.getType()) {
                        phoneNumber.setType(CustomerPhoneType.valueOf(phoneNumberDTO.getType()));
                    }
                    phoneNumber.setNumber(phoneNumberDTO.getNumber());
                    phoneNumbers.add(phoneNumber);
                });
                profile.setPhoneNumbers(phoneNumbers
                        .toArray(new CustomerPhoneNumber[requestDTO.getUserProfile().getPhoneNumbers().size()]));
            }

            if (null != requestDTO.getUserProfile().getAddresses()) {
                List<CustomerAddress> customerAddresses = new ArrayList<>();
                requestDTO.getUserProfile().getAddresses().forEach(addressDTO -> {
                    CustomerAddress address = CustomerAddress.create();
                    address.setType(CustomerAddressType.valueOf(addressDTO.getType()));
                    address.setPreferred(addressDTO.isPreferred());
                    address.setStreet1(addressDTO.getStreet1());
                    address.setStreet2(addressDTO.getStreet2());
                    address.setCity(addressDTO.getCity());
                    address.setState(addressDTO.getState());
                    address.setCountry(addressDTO.getCountry());
                    address.setPostalCode(addressDTO.getPostalCode());
                    customerAddresses.add(address);
                });
                profile.setAddresses(customerAddresses
                        .toArray(new CustomerAddress[requestDTO.getUserProfile().getAddresses().size()]));
            }
            modifyProfileRoomReservationRequest.setCustomerProfile(profile);
        }
        return modifyProfileRoomReservationRequest;
    }


    /**
     * Method to transform room reservation object to build
     * RatesSummary
     * @param reservation - RoomReservation POJO
     * @return RatesSummary
     */
    public static RatesSummary transform(RoomReservation reservation) {

        RatesSummary rateSummary = new RatesSummary();

        List<PriceItemized> itemized = new ArrayList<>();
        double roomSubtotal = 0;
        double roomDiscSubtotal = 0;
        for (RoomPrice price : reservation.getBookings()) {
            PriceItemized priceItem = new PriceItemized();
            priceItem.setDate(price.getDate());
            priceItem.setComp(price.isComp());
            priceItem.setProgramId(price.getProgramId());
            priceItem.setBasePrice(price.getBasePrice());
            priceItem.setDiscountedPrice(price.getPrice());
            itemized.add(priceItem);

            roomSubtotal += price.getBasePrice();
            roomDiscSubtotal += (price.isComp() ? 0 : price.getPrice());
        }
        rateSummary.setItemized(itemized);

        rateSummary.setRoomSubtotal(roomSubtotal);
        rateSummary.setProgramDiscount(roomSubtotal - roomDiscSubtotal);

        setCharges(reservation, rateSummary);

        return rateSummary;


    }

    public static com.mgm.services.booking.room.model.request.TripDetail getTripDetailFromTripDetailsRequest(TripDetailsRequest tripDetailsRequest) {
        com.mgm.services.booking.room.model.request.TripDetail tripDetail = new com.mgm.services.booking.room.model.request.TripDetail();
        tripDetail.setNumAdults(tripDetailsRequest.getNumAdults());
        tripDetail.setNumChildren(tripDetailsRequest.getNumChildren());
        tripDetail.setCheckInDate(tripDetailsRequest.getCheckInDate());
        tripDetail.setCheckOutDate(tripDetailsRequest.getCheckOutDate());
        return tripDetail;
    }

    public static SegmentReqItem createComponentSegmentReq(String propertyCode, Period period, List<GuestCountReq> guestCount,
                                                     String ratePlanCode, String inventoryTypeCode, Integer segmentHolderId, Integer bookerId, Integer parentId) {
        SegmentReqItem segment = new SegmentReqItem();
        if(null != parentId){
            segment.setParentId(parentId);
        }
        if(null!= segmentHolderId) {
            segment.setSegmentHolderId(segmentHolderId);
        }
        if( null != bookerId) {
            segment.setBookerId(bookerId);
        }
        segment.setPropertyCode(propertyCode);
        OfferReq offer = new OfferReq();
        ProductUseReq productUseReq = new ProductUseReq();
        ProductUseReqItem productUse = new ProductUseReqItem();
        // date
        productUse.setPeriod(period);
        // NRP and inventoryCode
        productUse.setRatePlanCode(ratePlanCode);
        productUse.setInventoryTypeCode(inventoryTypeCode);
        // count
        productUse.setGuestCounts(guestCount);
        productUseReq.add(productUse);
        offer.setProductUses(productUseReq);
        segment.setOffer(offer);
        return segment;

    }

    private static Period createACRSPeriodfromBookings(Date date) {
        Period period = new Period();
        // Date to LocalDate
        // TODO verify timezones used for localdate conversion
        period.setStart(ReservationUtil.convertDateToLocalDate(date));
        period.setEnd(ReservationUtil.convertDateToLocalDate(date).plusDays(1));
        return period;
    }

    public static Period createACRSPeriod(Date checkInDate, Date checkOutDate) {
        Period period = new Period();
        // Date to LocalDate
        // TODO verify timezones used for localdate conversion
        period.setStart(ReservationUtil.convertDateToLocalDate(checkInDate));
        period.setEnd(ReservationUtil.convertDateToLocalDate(checkOutDate));
        return period;
    }

    public static List<GuestCountReq> createACRSGuestCounts(int numAdults, int numChildren) {
        List<GuestCountReq> guestCounts = new ArrayList<>();

        if (numAdults > 0) {
            GuestCountReq adultReq = new GuestCountReq();
            adultReq.setCount(numAdults);
            adultReq.setOtaCode(ServiceConstant.NUM_ADULTS_MAP);
            guestCounts.add(adultReq);
        }
        if (numChildren > 0) {
            GuestCountReq childReq = new GuestCountReq();
            childReq.setCount(numChildren);
            childReq.setOtaCode(ServiceConstant.NUM_CHILD_MAP);
            guestCounts.add(childReq);
        }
        return guestCounts;
    }

    //transform ReservationSearchRes to ReservationsBasicInfoResponse
    public static ReservationsBasicInfoResponse transform(List<HotelReservationSearchResPostSearchReservations> hotelReservationSearchResList,
                                                          String shareLinkId) {

        final List<String> sharedResvIds = shareLinkId != null ? collectSharedReservationConfirmationNumbers(hotelReservationSearchResList) : null;

        List<ReservationBasicInfo> reservationBasicInfos = hotelReservationSearchResList.stream()
                .map(hotelReservationSearchRes ->
                        hotelReservationSearchResToReservationBasicInfo(hotelReservationSearchRes, sharedResvIds))
                .collect(Collectors.toList());

        return new ReservationsBasicInfoResponse(reservationBasicInfos);
    }

    private static ReservationBasicInfo hotelReservationSearchResToReservationBasicInfo(HotelReservationSearchResPostSearchReservations hotelReservationSearchRes,
                                                                                        List<String> sharedResvIds){
        ReservationBasicInfo reservationBasicInfo = new ReservationBasicInfo();

        // TODO mappings
        //        private String operaPartyCode;
        //        private String resvNameId;
        //        private String primarySharerConfNo;
        //        private String resType;
        reservationBasicInfo.setStatus(BaseAcrsTransformer.acrsResStatusToReservationState(hotelReservationSearchRes.getStatus()).toString());

        // Reservation Ids (confNo, externalConfNo, partyConfNo, operaConfNo)
        ReservationIdsSearchRes reservationIdsSearchRes = hotelReservationSearchRes.getReservationIds();
        reservationBasicInfo.setConfNo(reservationIdsSearchRes.getCfNumber());
        if (null != reservationIdsSearchRes.getExtCfNumber()) {
            String externalCfNumber = reservationIdsSearchRes.getExtCfNumber().getNumber();
            reservationBasicInfo.setExternalConfNo(externalCfNumber);
            reservationBasicInfo.setPartyConfNo(externalCfNumber); // TODO is there a condition which indicates whether or not this is a party conf number?
        }
        reservationBasicInfo.setOperaConfNo(reservationIdsSearchRes.getPmsCfNumber());

        // User Profiles
        // TODO Which user profile do we use if there are multiple?
        Optional<GuestsSearchResItem> userProfileSearchRes = hotelReservationSearchRes.getUserProfiles().stream()
                .findFirst();

        if ( userProfileSearchRes.isPresent() ) {
            PersonName personName = userProfileSearchRes.get().getPersonName();
            String givenName = personName.getGivenName();
            String surname = personName.getSurname();
            reservationBasicInfo.setGuestFName(givenName);
            reservationBasicInfo.setGuestLname(surname);
            reservationBasicInfo.setGuestName(givenName + ServiceConstant.WHITESPACE_STRING + surname);
        }

        // Segments
        Optional<SegmentSearchRespostSearchReservationsItem> segmentOptional = hotelReservationSearchRes.getSegments().stream()
                .findFirst();

        if ( segmentOptional.isPresent() ) {
            SegmentSearchRespostSearchReservationsItem segment = segmentOptional.get();
            reservationBasicInfo.setCheckInDate(DateUtil.convertDateToString(ServiceConstant.ISO_8601_DATE_FORMAT,
                    ReservationUtil.convertLocalDateToDate(segment.getStart()),
                    TimeZone.getDefault()));
            reservationBasicInfo.setCheckOutDate(DateUtil.convertDateToString(ServiceConstant.ISO_8601_DATE_FORMAT,
                    ReservationUtil.convertLocalDateToDate(segment.getEnd()),
                    TimeZone.getDefault()));

            OfferSearchResPostSearchReservations offerSearchRes = segment.getOffer();

            if ( null != offerSearchRes ) {
                // Segment offer
                // TODO: Handle if more rest type present in ACRS response.
                reservationBasicInfo.setResType(offerSearchRes.getPolicyTypeCode());

                // TODO can there be more than one relevant ProductUse?
                 Optional<ProductUseSearchResItem> productUse2 = offerSearchRes.getProductUses().stream().findFirst();
                if ( productUse2.isPresent() ) {
                    ProductUseSearchResItem productUse = productUse2.get();
                    // Segment offer product use
                    reservationBasicInfo.setRoomTypeCode(productUse.getInventoryTypeCode());
                    reservationBasicInfo.setRoomCount(String.valueOf(productUse.getQuantity()));
                    int numAdults = productUse.getGuestCounts().stream()
                            .filter(guestCountRes -> ServiceConstant.NUM_ADULTS_MAP.equals(guestCountRes.getOtaCode()))
                            .map(GuestCountRes::getCount)
                            .findFirst().orElse(0);
                    int numChildren = productUse.getGuestCounts().stream()
                            .filter(guestCountRes -> ServiceConstant.NUM_CHILD_MAP.equals(guestCountRes.getOtaCode()))
                            .map(GuestCountRes::getCount)
                            .findFirst().orElse(0);
                    reservationBasicInfo.setAdultCount(numAdults);
                    reservationBasicInfo.setChildCount(numChildren);
                }
            }

            if(sharedResvIds != null) {
                reservationBasicInfo.setReservationTypes(new String[] { BookingType.SHAREWITH.toString() });

                //override ResType for shared reservations
                //check for primary resv
                if(!reservationBasicInfo.getConfNo().equals(sharedResvIds.get(0))) {
                    reservationBasicInfo.setResType(SH);
                }
                reservationBasicInfo.setPrimarySharerConfNo(sharedResvIds.get(0));
            }else {
                reservationBasicInfo.setReservationTypes(new String[] { BookingType.PARTY.toString() });
            }
        }

        return reservationBasicInfo;
    }
    public static boolean hasOverriddenProgram(List<RoomPrice> bookings) {
        if(CollectionUtils.isNotEmpty(bookings)) {
            return bookings.stream().anyMatch(booking -> null != booking.getOverrideProgramId());
        }
        return false;
    }
    //Create ReservationSearchReq from RoomReservation
    public static ReservationSearchReq buildSearchRequestfromRoomReservation(String extConfirmationNumber, String shareLinkId) {
        ReservationSearchReq reservationSearchReq = new ReservationSearchReq();
        ReservationSearchReqData reservationSearchReqData = new ReservationSearchReqData();
        if (null != extConfirmationNumber) {
            reservationSearchReqData.setCfNumber(extConfirmationNumber);
        }

        if (shareLinkId != null) {
            reservationSearchReqData.setIncludeSharedReservations(true);
            reservationSearchReqData.setIncludeJointReservations(true);
            reservationSearchReqData.setLinks(Arrays.asList(shareLinkId));
        }
        reservationSearchReq.setData(reservationSearchReqData);
        return reservationSearchReq;
    }
    
    //Create ReservationSearchReq from RoomReservation
    public static ReservationSearchReq buildSearchRequestWithOperaConfirmationNumber(String operaConfirmationNumber) {
        ReservationSearchReq reservationSearchReq = new ReservationSearchReq();
        ReservationSearchReqData reservationSearchReqData = new ReservationSearchReqData();
        if (null != operaConfirmationNumber) {
            reservationSearchReqData.setCfNumber(operaConfirmationNumber);
        }
       
        reservationSearchReq.setData(reservationSearchReqData);
        return reservationSearchReq;
    }

    //Create RefData RoutingInfo Request
    public static List<RoutingInfoRequest> buildRefDataRoutingInfoRequest(List<ReservationRoutingInstruction> routingInsList, String propertyId, boolean rbsToAcrs) {
        List<RoutingInfoRequest> refDataRequest = new ArrayList<>();
        for (ReservationRoutingInstruction routingIns : routingInsList) {
            if (rbsToAcrs) {
                final String routingAuthorizerId = routingIns.getAuthorizerId();
                if (null != routingAuthorizerId && CollectionUtils.isNotEmpty(Arrays.asList(routingIns.getRoutingCodes()))) {
                    for (String routingCodeArrangementId : routingIns.getRoutingCodes()) {
                        RoutingInfoRequest routingInfoRequest = new RoutingInfoRequest();
                        routingInfoRequest.setPropertyId(propertyId);
                        routingInfoRequest.setAuthorizerId(routingAuthorizerId);
                        routingInfoRequest.setRoutingCodeArrangementId(routingCodeArrangementId);
                        refDataRequest.add(routingInfoRequest);
                    }
                }
            } else {
                final String routingAuthorizerAppUserId = routingIns.getHostAuthorizerAppUserId();
                if (null != routingAuthorizerAppUserId && CollectionUtils.isNotEmpty(Arrays.asList(routingIns.getHostRoutingCodes()))) {
                    for (String routingCode : routingIns.getHostRoutingCodes()) {
                        RoutingInfoRequest routingInfoRequest = new RoutingInfoRequest();
                        routingInfoRequest.setPropertyId(propertyId);
                        routingInfoRequest.setAuthorizerAppUserId(routingAuthorizerAppUserId);
                        routingInfoRequest.setRoutingCode(routingCode);
                        refDataRequest.add(routingInfoRequest);
                    }
                }
            }
        }
        log.info("Ref Data Call for RoutingInfo request : {}", CommonUtil.convertObjectToJsonString(refDataRequest));
        return refDataRequest;
    }

    public static List<String> collectSharedReservationConfirmationNumbers(
            List<HotelReservationSearchResPostSearchReservations> hotelReservations) {
        return hotelReservations.stream()
                .sorted((h1, h2) -> h1.getCreationDateTime().compareTo(h2.getCreationDateTime()))
                .map(m -> m.getReservationIds().getCfNumber()).collect(Collectors.toList());
    }

    public static Collection<UpdatedPromotion> getUpdatablePromos(ReservationRetrieveResReservation existingReservation, RoomReservation newReservation, String confirmationNumber, AcrsProperties acrsProperties) {

        final Map<String, UpdatedPromotion> promosToRemove = new HashMap<>();
        final Map<String, UpdatedPromotion> promosToRedeem = new HashMap<>();

        // Existing Reservation - Mark as Not used
        if (null != existingReservation) {
            final HotelReservationRetrieveResReservation hotelReservation = existingReservation.getData().getHotelReservation();
            final String promoCode = getPromoCode(hotelReservation);
            final boolean isPatron = ACRSConversionUtil.isPatronPromo(promoCode);

            if (isPatron) {
                int mLifeNo = getMLifeNo(hotelReservation);
                if (mLifeNo > 0) {
                    promosToRemove.put(promoCode, UpdatedPromotion.builder()
                            .patronId(mLifeNo)
                            .promoId(ACRSConversionUtil.getPatronPromoId(promoCode))
                            .sourceReservationNo(confirmationNumber)
                            .propertyId(getPropertyCodeFromHotel(acrsProperties.getPseudoExceptionProperties(), hotelReservation.getHotels().get(0)))
                            .statusNo(6)    // Pending Status for Patron on Book
                            .build());
                }
            }
        }

        // New Reservation - Mark as used/Redeemed
        if (null != newReservation) {
            boolean isPatron = ACRSConversionUtil.isPatronPromo(newReservation.getPromo());
            if (isPatron) {
                int mLifeNo = newReservation.getProfile().getMlifeNo();
                if (mLifeNo > 0) {
                    promosToRedeem.put(newReservation.getPromo(), UpdatedPromotion.builder()
                            .patronId(mLifeNo)
                            .promoId(ACRSConversionUtil.getPatronPromoId(newReservation.getPromo()))
                            .sourceReservationNo(confirmationNumber)
                            .propertyId(newReservation.getPropertyId())
                            .statusNo(6)    // Pending Status for Patron on Book
                            .build());
                }
            }
        }

        // Don't consider promos to update which belongs to both new and existing Reservation
        if (promosToRedeem.size() > 0 && promosToRemove.size() > 0) {
            Collection<String> commonPromos = CollectionUtils.intersection(promosToRedeem.keySet(), promosToRemove.keySet());
            commonPromos.stream().forEach(x -> {
                promosToRedeem.remove(x);
                promosToRemove.remove(x);
            });
        }

        final Collection<UpdatedPromotion> allPromos = new ArrayList<>();
        allPromos.addAll(promosToRemove.values());
        allPromos.addAll(promosToRedeem.values());

        return allPromos;
    }

	public static Optional<UpdatedPromotion> getPatronPromoToRemove(String promoCode,
																 String propertyId,
																 int mlifeNo,
																 String confirmationNumber) {
		if (StringUtils.isEmpty(promoCode) || !ACRSConversionUtil.isPatronPromo(promoCode) ||
				StringUtils.isEmpty(propertyId) || StringUtils.isEmpty(confirmationNumber) || mlifeNo < 0) {
			return Optional.empty();
		}
		return Optional.of(UpdatedPromotion.builder()
				.patronId(mlifeNo)
				.promoId(ACRSConversionUtil.getPatronPromoId(promoCode))
				.sourceReservationNo(confirmationNumber)
				.propertyId(propertyId)
				.statusNo(6)    // Pending Status for Patron on Book
				.build());
	}

    private static String getPromoCode(HotelReservationRetrieveResReservation hotelReservation) {
        final SegmentRes segments = hotelReservation.getSegments();
        final SegmentResItem segment = BaseAcrsTransformer.getMainSegment(segments);
        return segment.getOffer().getPromoCode();
    }

    private static int getMLifeNo(HotelReservationRetrieveResReservation hotelReservation) {
        String mLifeNo = null;
        final GuestsRes guestsRes = hotelReservation.getUserProfiles();
        final SegmentRes segmentRes = hotelReservation.getSegments();
        final SegmentResItem segment = BaseAcrsTransformer.getMainSegment(segmentRes);
        final int segmentHolderId = (null != segment.getSegmentHolderId()) ? segment.getSegmentHolderId() : 0;

        // userProfiles
        if (guestsRes != null) {
            final Optional<GuestsResItem> profileOptional = guestsRes.stream()
                    .filter(profile -> profile.getId() == segmentHolderId).findFirst();
            if (profileOptional.isPresent()) {
                final LoyaltyProgram loyaltyProgram = profileOptional.get().getLoyaltyProgram();
                if (null != loyaltyProgram) {
                    mLifeNo = loyaltyProgram.getLoyaltyId();
                }
            }
        }

        if (org.apache.commons.lang.StringUtils.isEmpty(mLifeNo)) {
            mLifeNo = "-1";
        }
        return Integer.parseInt(mLifeNo);
    }
    /**
     * If routing date range belongs into product date range then return the same routing with existing RIs.
     * If Product date range belongs into RI date range then create new RI with product date range and return with existing RIs.
     * @param routing
     * @param productUse
     * @return
     */
    public static List<RoutingInstruction> buildProductRoutingInstructions(RoutingInstruction routing,
                                                                           ProductUseResItem productUse, LocalDate checkOutDate) {
		return buildProductRIs(routing, productUse.getPeriod().getStart(), productUse.getPeriod().getEnd(), checkOutDate);
    }

    public static RoomReservation copyRoomReservationForShare(RoomReservation source){
        RoomReservation copyReservation = new RoomReservation();
        if(CollectionUtils.isNotEmpty(source.getBookings())){
            copyReservation.setBookings(source.getBookings().stream()
                    .map(CommonUtil::copyRoomBooking)
                    .collect(Collectors.toList()));
        }
        copyReservation.setCheckInDate(source.getCheckInDate());
        copyReservation.setCheckOutDate(source.getCheckOutDate());
        copyReservation.setPropertyId(source.getPropertyId());
        copyReservation.setRoomTypeId(source.getRoomTypeId());
        copyReservation.setProgramId(source.getProgramId());
        copyReservation.setPerpetualPricing(source.isPerpetualPricing());
        copyReservation.setRoutingInstructions(source.getRoutingInstructions());
        copyReservation.setAlerts(source.getAlerts());
        copyReservation.setTraces(source.getTraces());
        copyReservation.setSpecialRequests(source.getSpecialRequests());
        copyReservation.setSpecialRequestObjList(source.getSpecialRequestObjList());
        copyReservation.setDepositCalc(source.getDepositCalc());
        copyReservation.setDepositPolicyCalc(source.getDepositPolicyCalc());
        copyReservation.setShareId(source.getShareId());
        copyReservation.setPromo(source.getPromo());
        copyReservation.setIsGroupCode(source.getIsGroupCode());
        copyReservation.setNumRooms(source.getNumRooms());
        copyReservation.setNumAdults(1);
        copyReservation.setSource(source.getSource());
        copyReservation.setAdditionalComments(source.getAdditionalComments());
        copyReservation.setComments(source.getComments());
        copyReservation.setAgentInfo(source.getAgentInfo());
        copyReservation.setMarkets(source.getMarkets());
        copyReservation.setRrUpSell(source.getRrUpSell());
        copyReservation.setAddOnsDeposits(source.getAddOnsDeposits());
        copyReservation.setChannelId(source.getChannelId());
        copyReservation.setGuaranteeCode(source.getGuaranteeCode());
        copyReservation.setCancellationPolicyInfo(source.getCancellationPolicyInfo());
        return copyReservation;

    }
    /**
     * It will merge SubTotalServCrg into taxList if the taxCode is not there in txList
     * @param mainSegment
     */
    public static void mergeSubTotalServCrgIntoTaxList(SegmentResItem mainSegment) {

        mainSegment.getOffer().getProductUses().forEach(productUse ->{
            //PackageRates
            if(null != productUse.getPackageRates()) {
                productUse.getPackageRates().getDailyRates().forEach(dailyRate -> {
                    mergeServiceCrgIntoTaxList(dailyRate.getDailyTotalRate().getTaxList(), dailyRate.getDailyTotalRate().getSubTotalServiceCharges());
                });
            }
            // productRates
            if(null != productUse.getProductRates()) {
                productUse.getProductRates().getDailyRates().forEach(dailyRate -> {
                    mergeServiceCrgIntoTaxList(dailyRate.getDailyTotalRate().getTaxList(), dailyRate.getDailyTotalRate().getSubTotalServiceCharges());
                });
            }
        });
    }

    private void mergeServiceCrgIntoTaxList(List<TaxAmount> taxList, List<SubTotalTaxAmt> subTotalServiceCharges) {
        if(CollectionUtils.isNotEmpty(subTotalServiceCharges)){
            Set<String> taxCodes = taxList.stream().map(TaxAmount::getTaxCode).collect(Collectors.toSet());
            List<TaxAmount> chargeList = subTotalServiceCharges.stream().filter(x -> !taxCodes.contains(x.getTaxCode()))
                    .collect(Collectors.toList());
            taxList.addAll(chargeList);
        }
    }

    /**
     *
     * @param packageRates
     *  ProductUsePackage Rates  which is a decorator of ProductUseRates.
     * @return
     *  This method will create a ProductUseRates from the Decorator object ProductUsePackageRates. In doing so we will
     *  lose the information in the decorator fields. The decorator fields are as follows: mainWithIncludedProducts
     */
    public static ProductUseRates transform(ProductUsePackageRates packageRates) {
        if (null == packageRates) {
            return null;
        }
        ProductUseRates productUseRates = new ProductUseRates();
        productUseRates.setDailyRates(packageRates.getDailyRates());
        productUseRates.setTotalRate(packageRates.getTotalRate());
        productUseRates.setTotalBaseOccRate(packageRates.getTotalBaseOccRate());
        productUseRates.setTotalExtraOccRate(packageRates.getTotalExtraOccRate());
        productUseRates.setBookingPattern(packageRates.getBookingPattern());
        productUseRates.setPricingFrequency(packageRates.getPricingFrequency());
        productUseRates.setPricingMethod(packageRates.getPricingMethod());
        productUseRates.setIsDefaultPricingBased(packageRates.isIsDefaultPricingBased());
        productUseRates.setTaxInclusionMode(packageRates.getTaxInclusionMode());
        productUseRates.setHasRateChange(packageRates.isHasRateChange());
        productUseRates.setRatesDefinition(packageRates.getRatesDefinition());
        productUseRates.setRequestedRates(packageRates.getRequestedRates());
        return productUseRates;
    }

    public static List<RateReq> transform(List<RateRes> existingRequestedRates) {
        return existingRequestedRates.stream()
                .map(RoomReservationTransformer::transform)
                .collect(Collectors.toList());
    }

    private static RateReq transform(RateRes rateRes) {
        RateReq response = new RateReq();
        response.setStart(rateRes.getStart());
        response.setEnd(rateRes.getEnd());
        response.setBase(transform(rateRes.getBase()));
        return response;
    }

    private static BaseReq transform(BaseRes base) {
        BaseReq response = new BaseReq();
        response.setAmount(base.getAmount());
        response.setOverrideInd(base.getOverrideInd());
        return response;
    }
}