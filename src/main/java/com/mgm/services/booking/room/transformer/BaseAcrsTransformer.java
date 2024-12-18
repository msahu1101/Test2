package com.mgm.services.booking.room.transformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.crs.searchoffers.RatePlanPricing;
import com.mgm.services.booking.room.model.crs.searchoffers.RatePlanSingle;
import com.mgm.services.booking.room.properties.AcrsProperties;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.CreditCardCodes;
import com.mgm.services.booking.room.constant.PaymentStatus;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.ComponentPrices;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.reservation.DepositPolicy;

import com.mgm.services.booking.room.model.request.ActualBookingProgram;
import com.mgm.services.booking.room.model.request.ReservationCustomData;
import com.mgm.services.booking.room.model.reservation.AddOnDeposit;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.reservation.CardHolderProfile;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.ReservationSplRequest;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomMarket;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import com.mgm.services.common.util.DateUtil;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Utility class providing transformation for common ACRS entities for rooms
 */
@UtilityClass
@Log4j2
public class BaseAcrsTransformer {
    public static RoomReservation createRoomReservationFromHotelReservationRes(String channelCode, HotelReservationRes hotelReservationRes, AcrsProperties acrsProperties)
            throws ParseException {
        RoomReservation roomReservation = new RoomReservation();
        // State & dates
        roomReservation.setState(acrsResStatusToReservationState(hotelReservationRes.getStatus()));
        roomReservation.setCreatedAt(acrsStringDateToEpochTime(hotelReservationRes.getCreationDateTime()));
        roomReservation.setUpdatedAt(acrsStringDateToEpochTime(hotelReservationRes.getLastUpdateDateTime()));
        roomReservation.setBookDate(new Date(acrsStringDateToEpochTime(hotelReservationRes.getCreationDateTime())));

        // Reservation Ids
        ReservationIdsRes reservationIdsRes = hotelReservationRes.getReservationIds();
        roomReservation.setConfirmationNumber(reservationIdsRes.getCfNumber());
        if(null != reservationIdsRes.getExtCfNumber()){
            if (isPartyReservation(reservationIdsRes.getExtCfNumber().getNumber())) {
                roomReservation.setPartyConfirmationNumber(reservationIdsRes.getExtCfNumber().getNumber());
            }
        }

        if (null != reservationIdsRes.getPmsCfNumber()) {
            roomReservation.setOperaConfirmationNumber(reservationIdsRes.getPmsCfNumber());
        }
        // segments
        SegmentRes segmentRes = hotelReservationRes.getSegments();
		SegmentResItem segment = getMainSegment(segmentRes);
        Integer segmentHolderId = segment.getSegmentHolderId();

        // PropertyCode
        String acrsPropertyCode = RoomReservationTransformer.getPropertyCodeFromHotel(acrsProperties.getPseudoExceptionProperties(), hotelReservationRes.getHotels().get(0));
        roomReservation.setPropertyId(acrsPropertyCode);

        // Check in/out Date
        roomReservation.setCheckInDate(ReservationUtil.convertLocalDateToDate(segment.getStart()));
        roomReservation.setCheckOutDate(ReservationUtil.convertLocalDateToDate(segment.getEnd()));

        // Program ID
        ReservationCustomData  reservationCustomData = getCustomData(segment.getCustomData());
        if(null != reservationCustomData) {
            roomReservation.setProgramId(reservationCustomData.getParentProgram());
        }else if (segment.getOffer().getGroupCode() == null || segment.getOffer().getGroupCode().isEmpty()) {
            roomReservation.setProgramId(segment.getOffer().getProductUses().get(0).getRatePlanCode());
        } else {
            roomReservation.setProgramId(segment.getOffer().getGroupCode());

        }
        if (StringUtils.isNotEmpty(segment.getOffer().getGroupCode())) {
            roomReservation.setIsGroupCode(true);
        }
        //promocode
        roomReservation.setPromo(segment.getOffer().getPromoCode());

        // Room Type ID
        roomReservation.setRoomTypeId(segment.getOffer().getProductUses().get(0).getInventoryTypeCode());
        roomReservation.setUpgradeRoomTypeId(segment.getOffer().getProductUses().get(0).getUpgradedInventoryTypeCode());

        // GuaranteeCode not available in CRS
         roomReservation.setGuaranteeCode(segment.getOffer().getPolicyTypeCode());
        // num Adults and num Children
        segment.getOffer().getProductUses().get(0).getGuestCounts().forEach(guest -> {
            if (null != guest.getOtaCode()) {
          if (guest.getOtaCode().equals(ServiceConstant.NUM_ADULTS_MAP)) {
              roomReservation.setNumAdults(guest.getCount());
          } else if (guest.getOtaCode().equals(ServiceConstant.NUM_CHILD_MAP)) {
              roomReservation.setNumChildren(guest.getCount());
          }
            }
        });
        // num room
        roomReservation.setNumRooms(segment.getOffer().getProductUses().get(0).getQuantity());
        // Create Bookings on reservation from segment
        List<RatePlanRes> ratePlans = hotelReservationRes.getRatePlans();
        roomReservation.setBookings(acrsSegmentToRoomPrice(segment, ratePlans));
        //isGroupCode
        roomReservation.setIsGroupCode(StringUtils.isNotEmpty(segment.getOffer().getGroupCode()));
        if(segment.getOffer().getIsPerpetualOffer() != null) {
        	roomReservation.setPerpetualPricing(segment.getOffer().getIsPerpetualOffer());
        }
       // additionalComments
        if (null != hotelReservationRes.getComments()) {
            roomReservation.setAdditionalComments(
              hotelReservationRes.getComments().stream().filter(Objects::nonNull).map(Comment::getText)
                .collect(Collectors.toList()).stream().flatMap(List::stream).collect(Collectors.toList()));
        }
        // userProfiles
        GuestsRes guestsRes = hotelReservationRes.getUserProfiles();
        Optional<GuestsResItem> primaryUserProfile = guestsRes.stream()
                .filter(profile -> Objects.equals(profile.getId(), segmentHolderId))
                .findFirst();
        primaryUserProfile
          .ifPresent(userProfileRes -> roomReservation.setProfile(acrsUserProfileResToProfile(userProfileRes,acrsProperties.getAllowedTitleList())));

        // Charges and Taxes
        roomReservation.setChargesAndTaxesCalc(getRoomAndComponentChargesAndTaxes(segmentRes, acrsProperties,
                hotelReservationRes.getRatePlans(),acrsPropertyCode));

        // CreditCardCharges
        if (null != segment.getFormOfPayment()) {
            // Payments txns
            if (CollectionUtils.isNotEmpty(segment.getFormOfPayment().getDepositPayments())) {
                // data.hotelReservation.segments[].formOfPayment.depositPayments
                roomReservation.setPayments(getPaymentsFromAcrs(segment.getFormOfPayment().getDepositPayments()));
                roomReservation.setCreditCardCharges(getCardDetailsUsedInLastOperation(segment.getFormOfPayment()));
            }
        }

        // DepositCalc
        roomReservation.setDepositCalc(calculateRoomAndComponentDeposit(segmentRes, hotelReservationRes.getImageStatus()));

        // AmountDue
        if (null != segment.getFormOfPayment() && null != segment.getFormOfPayment().getDepositPayments()) {
            roomReservation.setAmountDue(getAmountPaidAgainstDeposit(segment.getFormOfPayment().getDepositPayments()));
        }

        // agent code
        ReservationOwner owner = segment.getOwner();
        if (null != owner && null != owner.getAgencyDetails()
                && CollectionUtils.isNotEmpty(owner.getAgencyDetails().getIdentifiers())) {
            AgentInfo agentInfo = new AgentInfo();
            agentInfo.setAgentId(owner.getAgencyDetails().getIdentifiers().get(0).getId());
            agentInfo.setAgentType(owner.getAgencyDetails().getIdentifiers().get(0).getType().getValue());
            roomReservation.setAgentInfo(agentInfo);
        }
        //Market codes
        if(channelCode != null) {
        	roomReservation.setMarkets(createAcrsMarkets(channelCode, segment));
        }

        //customer rank,segment etc.
        LoyaltyProgram loyaltyProgram = hotelReservationRes.getUserProfiles().get(0)
                .getLoyaltyProgram();
        if (null != loyaltyProgram && null != loyaltyProgram.getQualifier()) {
            roomReservation.setCustomerRank(Integer.parseInt(Optional.ofNullable(loyaltyProgram.getQualifier().getTier()).orElse("0")));
            roomReservation.setCustomerSegment(Integer.parseInt(Optional.ofNullable(loyaltyProgram.getQualifier().getTier()).orElse("0")));
            roomReservation
                    .setCustomerDominantPlay(transFormAcrsDominentPlay(loyaltyProgram.getQualifier().getDominance()));
        }

		// routing instruction
        roomReservation.setRoutingInstructions(createRoutingInstructionfromACRS(
                null == loyaltyProgram ? null : loyaltyProgram.getLoyaltyId(), segment.getOffer().getProductUses()));

        //inc-4 add linkId for shared reservation.
        roomReservation.setShareId(getLinkedId(hotelReservationRes.getLinks()));
        // Special Requests and room features
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
            roomReservation.setSpecialRequestObjList(specialRequests);
        }
        // this is for add-on component form of payment
        List<SegmentResItem> componentSegments = getComponentSegments(segmentRes);
        if (CollectionUtils.isNotEmpty(componentSegments)) {
            roomReservation.setAddOnsDeposits(createAddOnsDeposits(componentSegments));
            roomReservation.setPurchasedComponents(
                    buildComponentDetailsFromACRSRes(componentSegments, hotelReservationRes.getProducts(), hotelReservationRes.getRatePlans(), acrsPropertyCode));
        }
        // depositPolicy
        PoliciesRes policy = segment.getOffer().getPolicies();
        roomReservation.setDepositPolicyCalc(getDepositPolicyCalcFromPoliciesRes(policy));
        return roomReservation;
    }

    public static boolean isPartyReservation(String extCfNumber) {
        boolean isPartyReservation = false;
        if(StringUtils.isNotEmpty(extCfNumber)){
            if(extCfNumber.startsWith(ServiceConstant.PARTY_CONFIRMATION_NUMBER_PREFIX)){
                isPartyReservation = true;
            }
        }
        return isPartyReservation;
    }

    public  String getLinkedId(List<ReservationLinks> links) {
        String linkId = null;
        if (CollectionUtils.isNotEmpty(links)) {
            Optional<ReservationLinks> linkRes = links.stream()
                    .filter(link -> LinkType.SHARE.getValue().equalsIgnoreCase(link.getType().getValue())).findFirst();
            if (linkRes.isPresent()) {
                linkId = linkRes.get().getId();
            }
        }
        return linkId;
    }

    static DepositPolicy getDepositPolicyCalcFromPoliciesRes(PoliciesRes policy) {
        if (null == policy) {
            return null;
        }
        DepositPolicy depositPolicyCalc = new DepositPolicy();

        GuaranteeRes guaranteeRes = policy.getGuarantee();
        depositPolicyCalc.setCreditCardRequired(null != guaranteeRes && guaranteeRes.getIsRequired());

        DepositRes depositRes = policy.getDeposit();
        depositPolicyCalc.setDepositRequired(null != depositRes && policy.getDeposit().getIsRequired());

        return depositPolicyCalc;
    }

    /**
     * it will return card details used in last operation like create or modify reservation
     * @param formOfPaymentRes
     * @return
     */
    public static List<CreditCardCharge> getCardDetailsUsedInLastOperation(
            FormOfPaymentRes formOfPaymentRes) {
        List<CreditCardCharge> billings = new ArrayList<>();
        List<PaymentTransactionRes> sortedTxnList = new ArrayList<>();
        if(formOfPaymentRes.getDepositPayments() != null) {
        	sortedTxnList = formOfPaymentRes.getDepositPayments().stream()
                .sorted(Comparator.comparing(PaymentTransactionRes::getId).reversed()).collect(Collectors.toList());
        }
        Optional<PaymentTransactionRes> lastTxnWithTxnDate = sortedTxnList.stream()
                .filter(x -> StringUtils.isNotEmpty(x.getTransactionDate())).findFirst();
        // Migrated reservation might not have txnDate
        Optional<PaymentTransactionRes> lastTxnWithOutTxnDate = sortedTxnList.stream().findFirst();
        if (lastTxnWithTxnDate.isPresent()) {
            String lastTxnDateStr = lastTxnWithTxnDate.get().getTransactionDate();

            List<PaymentTransactionRes> lastTxns = sortedTxnList.stream()
                    .filter(x -> StringUtils.equals(lastTxnDateStr, x.getTransactionDate()))
                    .collect(Collectors.toList());
            // If payment is done with multi card then use payment history else use payment info
            if (CollectionUtils.isNotEmpty(lastTxns) && lastTxns.size() >1) {
                List<CreditCardCharge> lastCreditCardChargesTransactions = lastTxns.stream()
                .map(BaseAcrsTransformer::createCreditCardChargeFromPaymentTransactionRes)
                .collect(Collectors.toList());
                // for cash payment billing will be empty
                List<CreditCardCharge> cardPayments = lastCreditCardChargesTransactions.stream().filter(txn -> StringUtils.isNotEmpty(txn.getNumber())).collect(Collectors.toList());
                billings.addAll(cardPayments);
            }else{
                billings.addAll(createCreditCardChargeFromACRSPaymentInfo(formOfPaymentRes.getPaymentInfo()));
            }
        }else if(lastTxnWithOutTxnDate.isPresent()){
            billings.add(
                    createCreditCardChargeFromPaymentTransactionRes(lastTxnWithOutTxnDate.get())
            );
        } else {
            billings.addAll(createCreditCardChargeFromACRSPaymentInfo(formOfPaymentRes.getPaymentInfo()));
        }

        return billings;
    }
    /**
     *
     * @param txn
     * @return
     */
    public static CreditCardCharge createCreditCardChargeFromPaymentTransactionRes(PaymentTransactionRes txn) {
		PaymentCardRes paymentCard = txn.getPaymentCard();
		CreditCardCharge creditCardCharge = createCreditCardChargeFromPaymentCardRes(paymentCard);

		double paymentAmount = Double.parseDouble(txn.getAmount());
		if (txn.getPaymentIntent() == PaymentIntent.REFUND) {
			paymentAmount *= -1;
		}
		creditCardCharge.setAmount(paymentAmount);

        return creditCardCharge;
    }

     /* This will return List<AddOnDeposit> containing add-on segment id and deposit amount
     * @param componentSegments
     * @return
     */
    public static List<AddOnDeposit> createAddOnsDeposits(List<SegmentResItem> componentSegments) {
        List<AddOnDeposit> addOnDeposits = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(componentSegments)) {
            componentSegments.stream()
                    .filter(x -> x.getOffer().getPolicies() != null && x.getOffer().getPolicies().getDeposit() != null)
                    .forEach(addOn -> {
                        if (null != addOn.getOffer().getPolicies().getDeposit().getAmount()) {
                            AddOnDeposit addOnDeposit = new AddOnDeposit();
                            addOnDeposit.setId(addOn.getId());
                            addOnDeposit.setDepositAmount(addOn.getOffer().getPolicies().getDeposit().getAmount());
                            addOnDeposits.add(addOnDeposit);
                        }
                    });
        }
        return addOnDeposits;
    }
    public static RoomReservation createRoomReservationFromHotelReservationRes(HotelReservationRes hotelReservationRes, AcrsProperties acrsProperties)
            throws ParseException {
        return createRoomReservationFromHotelReservationRes(null, hotelReservationRes, acrsProperties);
    }

	public static List<Payment> getPaymentsFromAcrs(List<PaymentTransactionRes> depositPayments) {
		if (CollectionUtils.isEmpty(depositPayments)) {
			return new ArrayList<>();
		}
		// we should return only non-zero amounts
		return depositPayments.stream()
				.filter(depositPayment -> null != depositPayment.getAmount() &&
                        Double.parseDouble(depositPayment.getAmount()) != 0)
				.map(BaseAcrsTransformer::transform)
				.collect(Collectors.toList());
	}

	public static Payment transform(PaymentTransactionRes depositPayment) {
		Payment payment = new Payment();
		double paymentAmount = Double.parseDouble(depositPayment.getAmount());
        PaymentCardRes paymentCard = depositPayment.getPaymentCard();
		if ( depositPayment.getPaymentIntent() == PaymentIntent.REFUND ) {
			paymentAmount *= -1;
		}
		payment.setChargeAmount(paymentAmount);
        if(null != depositPayment.getPaymentStatus()) {
            payment.setStatus(updateStatus(depositPayment.getPaymentStatus().getValue()));
        }
		if (null != paymentCard) {
			payment.setChargeCardHolder(paymentCard.getCardHolderName());
			if(null!= paymentCard.getCardNumber() && StringUtils.isNotEmpty(paymentCard.getCardNumber().getToken())) {
                payment.setChargeCardMaskedNumber(getMaskedToken(paymentCard.getCardNumber().getToken()));
                payment.setChargeCardNumber(paymentCard.getCardNumber().getToken());
            }
			payment.setChargeCardType(CreditCardCodes.getCreditCardCodesFromCode(depositPayment.getPaymentCard().getCardCode()).getValue());
			payment.setDeposit(true);
			payment.setChargeCardExpiry(acrsExpiryStringToDate(depositPayment.getPaymentCard().getExpireDate()));
            payment.setDccTransDate(depositTransactionStringToDate(depositPayment.getTransactionDate()));
            payment.setPaymentTxnId(depositPayment.getPaymentRecordId());
		}
		return payment;
	}

    private static String updateStatus(String status) {
		return com.mgm.services.booking.room.model.crs.reservation.PaymentStatus.PAYMENT_RECEIVED.getValue().equals(status) ?
				PaymentStatus.Settled.getValue() : status;
	}

	//TODO: Move this to common util
    public String getMaskedToken(String token) {
        token = token.replaceAll(".(?=.{4})", "X");
        return token;
    }

    /**
     * create createCreditCard Charge From ACRS PaymentInfo while getting room
     * charge
     *
     * @param paymentInfo
     * @return
     */
	public static List<CreditCardCharge> createCreditCardChargeFromACRSPaymentInfo(PaymentInfoRes paymentInfo) {
        boolean hasPaymentCardInfo = paymentInfo != null
                && null != paymentInfo.getPaymentCard();
	    if (!hasPaymentCardInfo) {
			return new ArrayList<>();
		}
		CreditCardCharge creditCardCharge = createCreditCardChargeFromPaymentCardRes(paymentInfo.getPaymentCard());
		if (null != paymentInfo.getAmount()) {
			creditCardCharge.setAmount(Double.parseDouble(paymentInfo.getAmount()));
		}
		return Collections.singletonList(creditCardCharge);
	}

	public static CreditCardCharge createCreditCardChargeFromPaymentCardRes(PaymentCardRes paymentCard) {
		CreditCardCharge creditCardCharge = new CreditCardCharge();
		if (null == paymentCard) {
			return creditCardCharge;
		}
		creditCardCharge.setType(CreditCardCodes.getCreditCardCodesFromCode(paymentCard.getCardCode()).getValue());
        creditCardCharge.setExpiry(acrsExpiryStringToDate(paymentCard.getExpireDate()));
		if (null != paymentCard.getCardNumber() && StringUtils.isNotEmpty(paymentCard.getCardNumber().getToken())) {
			creditCardCharge.setMaskedNumber(getMaskedToken(paymentCard.getCardNumber().getToken()));
			creditCardCharge.setNumber(paymentCard.getCardNumber().getToken());
			creditCardCharge.setCcToken(paymentCard.getCardNumber().getToken());

		}
		String cardHolderName = paymentCard.getCardHolderName();
		if (null != cardHolderName) {
			cardHolderName = cardHolderName.trim();
			creditCardCharge.setHolder(cardHolderName);
			CardHolderProfile cardHolderProfile = new CardHolderProfile();
			if (cardHolderName.contains(ServiceConstant.WHITESPACE_STRING)) {
				cardHolderProfile.setFirstName(paymentCard.getCardHolderName().split(ServiceConstant.WHITESPACE_STRING)[0]);
				cardHolderProfile.setLastName(paymentCard.getCardHolderName().split(ServiceConstant.WHITESPACE_STRING)[1]);
			} else {
				cardHolderProfile.setFirstName(cardHolderName);
			}

			if (null != paymentCard.getBillingAddress()) {
				AddressWithoutIndex address = paymentCard.getBillingAddress();
				ProfileAddress profileAddress = createProfileAddressFromAcrs(address);
				cardHolderProfile.setAddress(profileAddress);
			}
			creditCardCharge.setHolderProfile(cardHolderProfile);
		}
		return creditCardCharge;
	}

    private static long acrsStringDateToEpochTime(String createDateTime) throws DateTimeParseException {
        return Instant.parse(createDateTime + ".00Z").toEpochMilli();
    }

	private static Date acrsExpiryStringToDate(String expiryString) {
		if (StringUtils.isEmpty(expiryString)) {
			return null;
		}
		try {
			return CommonUtil.getDateWithTimeZone(expiryString, ServiceConstant.MMYY_DATE_FORMAT);
		} catch (ParseException e) {
			log.warn("Unable to parse expireDate `{}` to Date object, expecting format `{}`. Populating " +
					"expiry as null in response.", expiryString, ServiceConstant.MMYY_DATE_FORMAT);
			return null;
		}
	}

	private static Date depositTransactionStringToDate(String transactionDate) {
		if (StringUtils.isEmpty(transactionDate)) {
			return null;
		}
		try {
			return CommonUtil.getDateWithTimeZone(transactionDate, ServiceConstant.ISO_8601_DATE_FORMAT);
		} catch (ParseException e) {
			log.error("Unable to parse transactionDate `{}` to Date object, expecting format `{}`. Throwing " +
							"SystemException.", transactionDate, ServiceConstant.ISO_8601_DATE_FORMAT);
			throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
		}
	}

    public static String dateToACRSExpiryDateString(Date expiryDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ServiceConstant.MMYY_DATE_FORMAT);
        return simpleDateFormat.format(expiryDate);
    }

    public static ReservationState acrsResStatusToReservationState(ResStatus resStatus) {
        switch (resStatus) {
        case BK:
        case MY:
            return ReservationState.Booked;
        case CL:
            return ReservationState.Cancelled;
        default:
            return ReservationState.Saved;
        }
    }

    public static List<RoomPrice> acrsSegmentToRoomPrice(SegmentResItem segment, List<RatePlanRes> ratePlans) {
        List<RoomPrice> bookings = new ArrayList<>();
        LocalDate currentDate = segment.getStart();
        LocalDate lastNight = segment.getEnd();
        // endDate is checkout date so we don't need to create a roomPrice for
        // that day.
        lastNight = lastNight.plusDays(-1);
        while (!currentDate.isAfter(lastNight)) {
            LocalDate finalCurrentDate = currentDate;

            final Optional<ProductUseResItem> firstProductUseOptional = segment.getOffer().getProductUses().stream()
                    .filter(productUse -> Objects.nonNull(productUse.getProductRates()))
                    .filter(productUse -> !finalCurrentDate.isBefore(productUse.getPeriod().getStart()))
                    .filter(productUse -> finalCurrentDate.isBefore(productUse.getPeriod().getEnd()))
                    .findFirst();

            if (!firstProductUseOptional.isPresent()) {
                // Unable to find ProductUse for date
                log.error("Unable to find productUse in segment for date: {}.", finalCurrentDate);
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }

            final ProductUseResItem firstProductUse = firstProductUseOptional.get();

            ReservationCustomData reservationCustomData = getCustomData(segment.getCustomData());

            RoomPrice roomPrice = createRoomPriceFromProductUseForDate(firstProductUse, finalCurrentDate,
                    segment.getOffer().getGroupCode(), reservationCustomData, ratePlans);

            bookings.add(roomPrice);
            currentDate = currentDate.plusDays(1);
        }
        return bookings;
    }

    private RoomPrice createRoomPriceFromProductUseForDate(ProductUseResItem productUse, final LocalDate roomPriceDate,
                                                           String groupCode,
                                                           ReservationCustomData reservationCustomData,
                                                           List<RatePlanRes> ratePlans){
        int roomCount = Optional.ofNullable(productUse.getQuantity()).orElse(1);
        // If packageRates is non-null then we need to take rates from there instead of productRates
		ProductUseRates productUseRates = getProductUseRatesFromProductUse(productUse);

        Optional<DailyRateDetails> dailyRateDetails = productUseRates.getDailyRates().stream()
                .filter(rateChange -> !roomPriceDate.isBefore(rateChange.getStart()))
                .filter(rateChange -> roomPriceDate.isBefore(rateChange.getEnd()))
                .findFirst()
                .map(DailyRate::getDailyTotalRate);

        double amountToPay;
        double price = -1.0;
        double overridePrice = -1.0;
        double resortFee = -1.0;
        if (dailyRateDetails.isPresent()){
            amountToPay = Double.parseDouble(dailyRateDetails.get().getBsAmt());
            price = amountToPay;
            resortFee = getResortFeeFromDailyRateDetail(dailyRateDetails.get());
            // Check if price is overridden
            Optional<Double> originalBaseRateOptional =
                    getOriginalBaseAmountFromRequestedRates(productUseRates.getRequestedRates(),
                    roomPriceDate);
            if (originalBaseRateOptional.isPresent()) {
                price = originalBaseRateOptional.get();
                overridePrice = amountToPay;
            }
        }

        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setDate(ReservationUtil.convertLocalDateToDate(roomPriceDate));
        roomPrice.setPrice(price);
        roomPrice.setOverridePrice(getOverriddenPrice(overridePrice, roomCount));
        roomPrice.setResortFee(resortFee);

        // if group code there set it else rateplan code
        String ratePlanCode = productUse.getRatePlanCode();
        roomPrice.setProgramId(ratePlanCode);

        // If comp ratePlan set isComp = true
        // for group code we should not set comp
        if(StringUtils.isEmpty(groupCode)) {
            roomPrice.setComp(checkIfComp(ratePlanCode, ratePlans));
        }

        // if customData is not null then set actual program from customData
        if (null != reservationCustomData
                && !CollectionUtils.isEmpty(reservationCustomData.getActualBookingPrograms())) {
            roomPrice = updateRoomPriceWithCustomData(reservationCustomData, roomPrice);
        }

        if (null != groupCode) {
            roomPrice.setProgramId(groupCode);
        }

        return roomPrice;
    }

	public static ProductUseRates getProductUseRatesFromProductUse(ProductUseResItem productUse) {
		if (null != productUse.getPackageRates()) {
			return RoomReservationTransformer.transform(productUse.getPackageRates());
		} else {
			return productUse.getProductRates();
		}
	}

	private static double getOverriddenPrice(double overridePrice, int roomCount) {
        if(roomCount == 1){
            return  overridePrice;
        }else{
            return BigDecimal.valueOf(overridePrice).divide(BigDecimal.valueOf(roomCount), 2,
                    RoundingMode.HALF_UP).doubleValue();
        }
    }
    private RoomPrice updateRoomPriceWithCustomData(ReservationCustomData reservationCustomData, RoomPrice roomPrice) {
        Optional<ActualBookingProgram> actualProgram = reservationCustomData.getActualBookingPrograms().stream()
                .filter(booking -> ReservationUtil.areDatesEqualExcludingTime(
                        ReservationUtil.convertLocalDateToDate(booking.getDate()), roomPrice.getDate()))
                .findFirst();

        if (!actualProgram.isPresent()) {
            // no changes from customData so just return inputted roomPrice
            return roomPrice;
        }

        ActualBookingProgram actualBookingProgram = actualProgram.get();

        double price = roomPrice.getPrice();
        double overridePrice = roomPrice.getOverridePrice();
        String overrideProgramId = roomPrice.getProgramId();
        String programId = actualBookingProgram.getProgram();
        if (-1.0 == overridePrice) {
            // OverrideInd can be false if overridden price matches current price for
            // RatePlanCode, causing overridePrice to stored in the price field
            overridePrice = price;
        }
        if (actualBookingProgram.getPrice() != price) {
            // we need this because we want the price to reflect the original price from the original
            // programId and not the priced value that we are overriding in the overrideProgramId if any
            price = actualBookingProgram.getPrice();
        }
        roomPrice.setProgramId(programId);
        roomPrice.setPrice(price);
        roomPrice.setOverrideProgramId(overrideProgramId);
        roomPrice.setOverridePrice(overridePrice);
        return roomPrice;
    }

    private Optional<Double> getOriginalBaseAmountFromRequestedRates(List<RateRes> requestedRates,
                                                                   LocalDate targetNight) {
        if (null == requestedRates) {
            return Optional.empty();
        }

        Optional<BaseRes> baseResOptional = requestedRates.stream()
                .filter(requestedRate -> !targetNight.isBefore(requestedRate.getStart()))
                .filter(requestedRate -> targetNight.isBefore(requestedRate.getEnd()))
                .findFirst()
                .map(RateRes::getBase);

		if (baseResOptional.isPresent()) {
			BaseRes baseRes = baseResOptional.get();
			if (Boolean.TRUE.equals(baseRes.getOverrideInd()) && CollectionUtils.isNotEmpty(baseRes.getOriginalBaseRates())) {
				// rate is overridden
				return baseResOptional.get().getOriginalBaseRates().stream()
						.filter(originalBaseRate1 -> !targetNight.isBefore(originalBaseRate1.getStart()))
						.filter(originalBaseRate1 -> targetNight.isBefore(originalBaseRate1.getEnd()))
						.findFirst()
						.map(OriginalBaseRate::getBsAmt)
						.map(Double::parseDouble);
			}
        }

        return Optional.empty();
    }

    private double getResortFeeFromDailyRateDetail(DailyRateDetails dailyRateDetails) {
	    if(CollectionUtils.isNotEmpty(dailyRateDetails.getTaxList())){
            return dailyRateDetails.getTaxList().stream()
                    .filter(tax -> ServiceConstant.ACRS_RESORT_FEE.equalsIgnoreCase(tax.getTaxCode()))
                    .map(TaxAmount::getAmount)
                    .map(Double::parseDouble)
                    .findFirst()
                    .orElse(ServiceConstant.ZERO_DOUBLE_VALUE);
        }else {
	        return ServiceConstant.ZERO_DOUBLE_VALUE;
        }
    }

    public static boolean checkIfComp(String ratePlanCode, List<RatePlanRes> ratePlans) {
		if (CollectionUtils.isEmpty(ratePlans)) {
			// Possible in case of group code reservation.
			return false;
		}
		Optional<RatePlanRes> ratePlanResOptional = ratePlans.stream()
				.filter(ratePlanRes -> ratePlanRes.getCode().equalsIgnoreCase(ratePlanCode))
				.findFirst();

		if (!ratePlanResOptional.isPresent()) {
			log.error("Unable to find segment ratePlanCode {} in hotelReservations.getRatePlans.", ratePlanCode);
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}
		RatePlanRes ratePlanRes = ratePlanResOptional.get();
		boolean areAmountsConfidential = ratePlanRes.isAreAmountsConfidential();
		boolean isCompGamingBucket = GamingBucket.COMP.equals(ratePlanRes.getGamingBucket());

		boolean isConsideredComp = isCompGamingBucket || areAmountsConfidential;
		if (log.isDebugEnabled() && isConsideredComp) {
			log.debug("COMP Identified for ratePlanCode `{}` with gamingBucket `{}` and areAmountsConfidential `{}`",
					ratePlanCode, ratePlanRes.getGamingBucket(), ratePlanRes.isAreAmountsConfidential());
		}
		return isConsideredComp;
	}

	public static boolean checkIfCompByString(String ratePlanCode) {
		return ratePlanCode != null && ratePlanCode.startsWith(ServiceConstant.COMP_STRING);
	}

	public static boolean checkIfCompUsingRatePlanSingle(String ratePlanCode, List<RatePlanSingle> ratePlanSingles) {
		if (CollectionUtils.isEmpty(ratePlanSingles)) {
			// Possible in the case of group code rate plan responses.
			return false;
		}
		List<RatePlanRes> ratePlanResList = ratePlanSingles.stream()
				.map(BaseAcrsTransformer::transformCompRelatedFields)
				.collect(Collectors.toList());

		return checkIfComp(ratePlanCode, ratePlanResList);
	}

	public static boolean checkIfCompUsingRatePlanPricing(String ratePlanCode, List<RatePlanPricing> ratePlanPricings) {
		if (CollectionUtils.isEmpty(ratePlanPricings)) {
			// Possible in the case of group code rate plan responses.
			return false;
		}
		List<RatePlanRes> ratePlanResList = ratePlanPricings.stream()
				.map(BaseAcrsTransformer::transformCompRelatedFields)
				.collect(Collectors.toList());

		return checkIfComp(ratePlanCode, ratePlanResList);
	}

	private static RatePlanRes transformCompRelatedFields(RatePlanSingle ratePlanSingle) {
		RatePlanRes ratePlanRes = new RatePlanRes();
		ratePlanRes.setCode(ratePlanSingle.getCode());
		ratePlanRes.areAmountsConfidential(ratePlanSingle.isAreAmtsConfidential());
		if (StringUtils.isNotEmpty(ratePlanSingle.getGamingBucket())) {
			String gamingBucketString = StringUtils.capitalize(ratePlanSingle.getGamingBucket().toLowerCase());
			ratePlanRes.setGamingBucket(GamingBucket.fromValue(gamingBucketString));
		}
		return ratePlanRes;
	}

	private static RatePlanRes transformCompRelatedFields(RatePlanPricing ratePlanPricing) {
		RatePlanRes ratePlanRes = new RatePlanRes();
		ratePlanRes.setCode(ratePlanPricing.getCode());
		ratePlanRes.areAmountsConfidential(ratePlanPricing.isAreAmtsConfidential());
		// RatePlanPricing Does not have gaming bucket at time of writing. May need to be added later if supported by
		// the object in the future.
		return ratePlanRes;
	}

	public static ReservationCustomData getCustomData(CustomData acrsCustomData) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        String customDataStrValue;
        ReservationCustomData reservationCustomData = null;
        try {
            customDataStrValue = mapper.writeValueAsString(acrsCustomData);
            reservationCustomData = mapper.readValue(customDataStrValue, ReservationCustomData.class);

        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
        return reservationCustomData;
    }

    public static ReservationProfile acrsUserProfileResToProfile(GuestsResItem userProfileRes, Set<String> allowedTitleList) {
        ReservationProfile reservationProfile = new ReservationProfile();
        reservationProfile.setId(-1);
        if (null != userProfileRes.getLoyaltyProgram()) {
            reservationProfile.setMlifeNo(Integer.parseInt(userProfileRes.getLoyaltyProgram().getLoyaltyId()));
        }
        if (!StringUtils.SPACE.equalsIgnoreCase(userProfileRes.getPersonName().getTitle())) {
            String title = userProfileRes.getPersonName().getTitle();
            if(StringUtils.isNotEmpty(title)) {
                String allowedTitle = title.replace(".", "");
                boolean result = ReservationUtil.allowedTitleList(allowedTitle, allowedTitleList);
                if (result) {
                    reservationProfile.setTitle(allowedTitle);
                }
            }
        }
        reservationProfile.setFirstName(userProfileRes.getPersonName().getGivenName());
        reservationProfile.setLastName(userProfileRes.getPersonName().getSurname());
        if (!CollectionUtils.isEmpty(userProfileRes.getEmails())) {
            reservationProfile.setEmailAddress1(userProfileRes.getEmails().get(0).getAddress());
            if (userProfileRes.getEmails().size() > 1) {
                reservationProfile.setEmailAddress2(userProfileRes.getEmails().get(1).getAddress());
            }
        }
        // set address details
        if (CollectionUtils.isNotEmpty(userProfileRes.getAddresses())) {
            reservationProfile.setAddresses(userProfileRes.getAddresses().stream()
                    .map(BaseAcrsTransformer::createProfileAddressFromAcrs).collect(Collectors.toList()));
        }

        // phone number
        if (CollectionUtils.isNotEmpty(userProfileRes.getTelephones())) {
            reservationProfile.setPhoneNumbers(userProfileRes.getTelephones().stream()
                    .map(BaseAcrsTransformer::createProfilePhonesFromAcrs).collect(Collectors.toList()));
        }
        // set partner info
        reservationProfile.setPartnerAccounts(createPartnerAccountsFrmACRSRes(userProfileRes.getAdditionalMembershipPrograms()));
        return reservationProfile;
    }


    public static List<PartnerAccounts> createPartnerAccountsFrmACRSRes(List<AdditionalMembershipProgram> additionalMembershipPrograms){
        List<PartnerAccounts> partnerAccounts = null;
        if(CollectionUtils.isNotEmpty(additionalMembershipPrograms)){
            partnerAccounts = additionalMembershipPrograms.stream()
                    .map(BaseAcrsTransformer::createPartnerProfile)
                    .collect(Collectors.toList());
        }
        return partnerAccounts;
    }

    public static PartnerAccounts createPartnerProfile(AdditionalMembershipProgram additionalMembershipProgram) {
        PartnerAccounts partnerAcc = new PartnerAccounts();
        partnerAcc.setPartnerAccountNo(additionalMembershipProgram.getId());
        partnerAcc.setProgramCode(additionalMembershipProgram.getProgramCode());
        partnerAcc.setMembershipLevel(additionalMembershipProgram.getLevel());
        return partnerAcc;
	}


    public ProfilePhone createProfilePhonesFromAcrs(Telephone phone) {
        ProfilePhone profilePhone = new ProfilePhone();
        if (null != phone.getDeviceType()) {
            // Convert for Fax to Fax(3), Mobile to Mobile (5), and everything
            // else to Phone(1)

            if (PhoneDeviceType.NUMBER_3.equals(phone.getDeviceType())) {
                profilePhone.setType("Fax");
            } else if (PhoneDeviceType.NUMBER_5.equals(phone.getDeviceType())) {
                profilePhone.setType("Mobile");
            } else {
                if (PhoneLocationType.NUMBER_7.getValue().equals(phone.getLocationType().getValue())) {
                    profilePhone.setType("Business");
                } else if (PhoneLocationType.NUMBER_6.getValue().equals(phone.getLocationType().getValue())) {
                    profilePhone.setType("Home");
                } else {
                    profilePhone.setType("Other");
                }
            }
        }
        profilePhone.setNumber(phone.getNumber());
        return profilePhone;
    }

    public ProfileAddress createProfileAddressFromAcrs(AddressWithoutIndex address) {
        ProfileAddress profileAddress = new ProfileAddress();
        profileAddress.setCity(address.getCityName());
        profileAddress.setCountry(address.getCountryCode());
        profileAddress.setPostalCode(address.getPostalCode());
        profileAddress.setState(address.getStateProv());
        if(CollectionUtils.isNotEmpty(address.getAddressLines())) {
            profileAddress.setStreet1(address.getAddressLines().get(0));
            if (address.getAddressLines().size() > 1) {
                profileAddress.setStreet2(address.getAddressLines().get(1));
            }
        }

        if (null != address.getType()) {
            if (AddressType.NUMBER_1.equals(address.getType())) {
                profileAddress.setType("Home");
            } else {
                profileAddress.setType("Business");
            }
        }
        profileAddress.setPreferred(true);
        return profileAddress;
    }

    public static RoomChargesAndTaxes getRoomAndComponentChargesAndTaxes(List<SegmentResItem> segmentList, AcrsProperties acrsProperties,
                                                                         List<RatePlanRes> ratePlans, String acrsPropertyCode) {

        //Segment subList, containing Components, starting from 2nd Segment onwards
    	RoomChargesAndTaxes allChargesAndTaxes = getChargesAndTaxes(getMainSegment(segmentList),
                RoomChargeItemType.RoomCharge, acrsProperties, ratePlans,acrsPropertyCode);
    	getComponentSegments(segmentList).forEach(componentSegment -> {
                RoomChargesAndTaxes componentChargeAndTax = getChargesAndTaxes(componentSegment,
                        RoomChargeItemType.ComponentCharge, acrsProperties, ratePlans,acrsPropertyCode);
                // add componentChargeItem and charge value date wise with
                // roomChargesAndTaxes
                // add componentChargeTaxItem and tax value date wise with
                // roomChargesAndTaxes
                addComponentChargeAndTax(allChargesAndTaxes, componentChargeAndTax);
            });

        return allChargesAndTaxes;
    }

    public void addComponentChargeAndTax(RoomChargesAndTaxes allChargesAndTaxes,
            RoomChargesAndTaxes componentChargeAndTax) {
        componentChargeAndTax.getCharges().forEach(componentCharge -> addComponentCharge(allChargesAndTaxes,
				componentCharge));
        componentChargeAndTax.getTaxesAndFees().forEach(componentTax -> addComponentTax(allChargesAndTaxes,
				componentTax));
    }

    private void addComponentCharge(RoomChargesAndTaxes allChargesAndTaxes, RoomChargeItem componentCharge) {
        Optional<RoomChargeItem> allCharges = allChargesAndTaxes.getCharges().stream()
                .filter(charge -> charge.getDate().compareTo(componentCharge.getDate()) == 0).findFirst();

        if (allCharges.isPresent()) {
            allCharges.get().setAmount(allCharges.get().getAmount() + componentCharge.getAmount());
            allCharges.get().getItemized().addAll(componentCharge.getItemized());
        }
    }

    private void addComponentTax(RoomChargesAndTaxes allChargesAndTaxes, RoomChargeItem componentTax) {
        Optional<RoomChargeItem> allTaxs = allChargesAndTaxes.getTaxesAndFees().stream()
                .filter(charge -> charge.getDate().compareTo(componentTax.getDate()) == 0).findFirst();

        if (allTaxs.isPresent()) {
            allTaxs.get().setAmount(allTaxs.get().getAmount() + componentTax.getAmount());
            allTaxs.get().getItemized().addAll(componentTax.getItemized());
        }
    }

    public static RoomChargesAndTaxes getChargesAndTaxes(SegmentResItem segment, RoomChargeItemType roomChargeItemType,
                                                         AcrsProperties acrsProperties, List<RatePlanRes> ratePlans, String acrsPropertyCode) {
        RoomChargesAndTaxes roomChargesAndTaxes = new RoomChargesAndTaxes();
        List<RoomChargeItem> charges = new ArrayList<>();
        roomChargesAndTaxes.setCharges(charges);
        List<RoomChargeItem> taxAndFees = new ArrayList<>();
        roomChargesAndTaxes.setTaxesAndFees(taxAndFees);

        LocalDate currentDate = segment.getStart();
        LocalDate lastNight = segment.getEnd();

        List<TaxDefinition> taxDefinitions = segment.getTaxDefinitions();
        // endDate is checkout date so we don't need to create a roomPrice for that day.
        while (currentDate.isBefore(lastNight)) {
            LocalDate finalCurrentDate = currentDate;

            List<ProductUseResItem> relevantProductUses = segment.getOffer().getProductUses().stream()
                    .filter(productUse -> !finalCurrentDate.isBefore(productUse.getPeriod().getStart()))
                    .filter(productUse -> finalCurrentDate.isBefore(productUse.getPeriod().getEnd()))
                    .filter(productUse -> Boolean.TRUE.equals(productUse.getIsMainProduct()))
                    .collect(Collectors.toList());

            if (relevantProductUses.isEmpty()) {
                log.error("No relevant productUses on reservation to for date: {}", finalCurrentDate);
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else if (relevantProductUses.size() > 1) {
                log.error("Multiple productUses with isMainProduct=true for date: {}", finalCurrentDate);
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }

            // There should only be one ProductUse at this point in relevantProductUses
			ProductUseResItem relevantProductUse = relevantProductUses.get(0);

            if (roomChargeItemType == RoomChargeItemType.ComponentCharge &&
					relevantProductUse.getProductRates().getPricingFrequency()!= PricingFrequency.PERNIGHT){
                // this can be either checkedin or checkout date
                LocalDate currentDateComponent = relevantProductUse.getProductRates().getDailyRates().get(0).getStart();
                boolean productUseIsComp = isRatePlanComp(relevantProductUse.getRatePlanCode(), ratePlans);
                RateChange relevantDailyRate = getDailyRateFromProductUseForDate(relevantProductUse, currentDateComponent);
                charges.add(createRoomChargeItemForDate(relevantDailyRate, currentDateComponent,
                        acrsPropertyCode, acrsProperties, productUseIsComp, roomChargeItemType));
                taxAndFees.add(createTaxRoomChargeItemForDate(relevantProductUse, taxDefinitions, currentDateComponent,
                        acrsPropertyCode, acrsProperties, productUseIsComp, roomChargeItemType));
                break;
            } else {
                boolean productUseIsComp = isRatePlanComp(relevantProductUse.getRatePlanCode(), ratePlans);
                RateChange relevantDailyRate = getDailyRateFromProductUseForDate(relevantProductUse, finalCurrentDate);
                charges.add(createRoomChargeItemForDate(relevantDailyRate, finalCurrentDate, acrsPropertyCode,
                        acrsProperties, productUseIsComp, roomChargeItemType));
                taxAndFees.add(createTaxRoomChargeItemForDate(relevantProductUse, taxDefinitions, finalCurrentDate,
                        acrsPropertyCode, acrsProperties, productUseIsComp, roomChargeItemType));
            }

            currentDate = currentDate.plusDays(1);
        }

        return roomChargesAndTaxes;
    }

    private static RateChange getDailyRateFromProductUseForDate(ProductUseResItem relevantProductUse, LocalDate finalCurrentDate) {
        // Get from PackageRates if available
		Optional<ProductUseRates> productUseRates = Optional.ofNullable(RoomReservationTransformer.transform(relevantProductUse.getPackageRates()));

        if (!productUseRates.isPresent()) {
            // PackageRates is null so collect from productRates
            productUseRates = Optional.ofNullable(relevantProductUse.getProductRates());

            if (!productUseRates.isPresent()) {
                log.error("Unable to find packageRates or productRates for date: {}.", finalCurrentDate);
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }
        }

        Optional<RateChange> relevantDailyRate = productUseRates.get().getDailyRates().stream()
                .filter(dailyRate -> !finalCurrentDate.isBefore(dailyRate.getStart()))
                .filter(dailyRate -> finalCurrentDate.isBefore(dailyRate.getEnd()))
                .findFirst();

        if(!relevantDailyRate.isPresent()){
            log.error("Unable to find DailyRate for date: {}", finalCurrentDate);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }

        return relevantDailyRate.get();
    }

    private static RoomChargeItem createTaxRoomChargeItemForDate(ProductUseResItem relevantProductUse,
                                                                 List<TaxDefinition> taxDefinitions,
                                                                 LocalDate currentDate,
                                                                 String propertyCode, AcrsProperties acrsProperties,
                                                                 boolean productUseIsComp, RoomChargeItemType roomChargeItemType) {
        // Collect sources of Information for currentDate
        RateChange relevantDailyRate = getDailyRateFromProductUseForDate(relevantProductUse, currentDate);
        List<TaxAmount> taxList = relevantDailyRate.getDailyTotalRate().getTaxList();

        // Create ItemizedChargeItem list for currentDate
        List<ItemizedChargeItem> itemizedTaxAndFeesItemList = new ArrayList<>();
        Map<RoomChargeItemType, List<String>> taxTypeTaxCodesMap =
                ReservationUtil.getTaxTypeTaxCodeMapByPropertyCode(acrsProperties, propertyCode);

        boolean isMainSegment =true;
        // Populate itemizedTaxAndFeesItemList with values
        if(CollectionUtils.isNotEmpty(taxList)) {
            for (TaxAmount tax : taxList) {
                RoomChargeItemType chargeItemType;

                double taxAmount = Double.parseDouble(tax.getAmount());
                if (roomChargeItemType == RoomChargeItemType.ComponentCharge) {
                    chargeItemType = RoomChargeItemType.ComponentChargeTax;
                    isMainSegment = false;
                } else {
                    chargeItemType = ReservationUtil.getTaxAndChargeTypeByTaxAndChargeCode(taxTypeTaxCodesMap, tax.getTaxCode());
                    if (chargeItemType == RoomChargeItemType.RoomChargeTax && productUseIsComp) {
                        // If it is a COMP charge, RoomChargeTax should be zero.
                        taxAmount = 0.0d;
                    }
                }
                if (null != chargeItemType) {
                    ItemizedChargeItem existingTaxType = getFeesTaxType(chargeItemType,
                            itemizedTaxAndFeesItemList);
                    if (null != existingTaxType && !ReservationUtil.checkExceptionTaxCode(acrsProperties,
                            tax.getTaxCode(), propertyCode)) {
                        existingTaxType
                                .setAmount(existingTaxType.getAmount() + taxAmount);
                    } else {
                        ItemizedChargeItem feeTaxCharge = new ItemizedChargeItem();
                        feeTaxCharge.setAmount(taxAmount);
                        itemizedTaxAndFeesItemList.add(feeTaxCharge);
                        feeTaxCharge.setItemType(chargeItemType);
                        TaxDefinition relevantTaxDefinition = getTaxDefinitionById(taxDefinitions, tax.getTaxDefIds());
                        if (roomChargeItemType == RoomChargeItemType.ComponentCharge) {
                            PricingFrequency pricingFrequency = relevantProductUse.getProductRates().getPricingFrequency();
                            feeTaxCharge.setItem(relevantTaxDefinition.getName());
                            feeTaxCharge.setPricingApplied(getComponentAppliedPrice(pricingFrequency, relevantProductUse.getProductRates().getBookingPattern()));
                            feeTaxCharge.setShortDescription(getShortDescriptionFromTaxDefinition(relevantTaxDefinition));
                        } else if (isMainSegment) {
                            //For main segment we will be setting code.
                            feeTaxCharge.setItem(relevantTaxDefinition.getCode());
                        } else {
                            feeTaxCharge.setItem(relevantTaxDefinition.getName());
                        }
                    }
                }
            }
        }

        // Create Response RoomChargeItem
        RoomChargeItem taxAndFeesItem = new RoomChargeItem();
        taxAndFeesItem.setDate(DateUtil.toDate(currentDate));
        taxAndFeesItem.setAmount(
                itemizedTaxAndFeesItemList.stream().mapToDouble(ItemizedChargeItem::getAmount).sum());
        taxAndFeesItem.setItemized(itemizedTaxAndFeesItemList);

        return taxAndFeesItem;
    }

    private static String getShortDescriptionFromTaxDefinition(TaxDefinition relevantTaxDefinition) {
        if (null == relevantTaxDefinition.getDescription()) {
            return ServiceConstant.EMPTY_STRING;
        }

        List<String> englishDescription =
                relevantTaxDefinition.getDescription().get(ServiceConstant.EN);

        return (null == englishDescription) ? ServiceConstant.EMPTY_STRING : englishDescription.toString();
    }

    private static TaxDefinition getTaxDefinitionById(List<TaxDefinition> taxDefinitions, List<Integer> taxDefIds) {
        Optional<TaxDefinition> taxDefinitionOptional = taxDefinitions.stream()
                .filter(taxDefinition -> taxDefIds.contains(taxDefinition.getId()))
                .findFirst();

        if (!taxDefinitionOptional.isPresent()) {
            log.error("Unable to find matching Tax definition for taxDefIds: {}", taxDefIds);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
        return taxDefinitionOptional.get();
    }

    private static boolean isRatePlanComp(String ratePlanCode, List<RatePlanRes> ratePlans) {
        return null != ratePlanCode && ratePlans.stream()
                .filter(ratePlan -> StringUtils.equals(ratePlan.getCode(), ratePlanCode))
                .anyMatch(BaseAcrsTransformer::isRatePlanComp);
    }

	private static boolean isRatePlanComp(RatePlanRes ratePlan) {
		if (null == ratePlan) {
			return false;
		}
		// check for Comp gaming bucket
		if (GamingBucket.COMP.equals(ratePlan.getGamingBucket())) {
			return true;
		}
		// check areAmtsConfidential flag and return false if false
		return Boolean.TRUE.equals(ratePlan.isAreAmountsConfidential());
	}

    private static RoomChargeItem createRoomChargeItemForDate(RateChange relevantDailyRate, LocalDate finalCurrentDate,
                                                              String propertyCode, AcrsProperties acrsProperties,
                                                              boolean isComp, RoomChargeItemType roomChargeItemType) {
        DailyRateDetails dailyTotalRate = relevantDailyRate.getDailyTotalRate();

        // Create Itemized list for Date
        List<ItemizedChargeItem> itemizedChargeItemList = new ArrayList<>();

        // First Itemized: RoomCharge
        ItemizedChargeItem roomCharge = new ItemizedChargeItem();
        roomCharge.setAmount((!isComp) ? Double.parseDouble(dailyTotalRate.getBsAmt()) : 0.0d);
        roomCharge.setItemType(roomChargeItemType);
        roomCharge.setItem(ServiceConstant.ROOM_CHARGE_ITEM);
        itemizedChargeItemList.add(roomCharge);

        // add other Itemized ItemizedChargeItems
        List<TaxAmount> taxList = relevantDailyRate.getDailyTotalRate().getTaxList();
        itemizedChargeItemList.addAll(ReservationUtil.getOtherCharges(propertyCode, taxList, acrsProperties));

        // Create Response RoomChargeItem
        RoomChargeItem roomChargeItem = new RoomChargeItem();
        roomChargeItem.setDate(DateUtil.toDate(finalCurrentDate));
        roomChargeItem.setItemized(itemizedChargeItemList);
        roomChargeItem.setAmount(
                itemizedChargeItemList.stream().mapToDouble(ItemizedChargeItem::getAmount).sum());

        return roomChargeItem;
    }


    private ItemizedChargeItem getFeesTaxType(RoomChargeItemType chargeItemType,
            List<ItemizedChargeItem> itemizedTaxNfeesItemList) {
		return itemizedTaxNfeesItemList.stream()
				.filter(taxItem -> chargeItemType.name().equals(taxItem.getItemType().name()))
				.findFirst()
				.orElse(null);
    }

	public static List<RoomMarket> createAcrsMarkets(String marketsSourceCode, SegmentResItem segment1) {
        List<RoomMarket> markets = new ArrayList<>();
              if (CollectionUtils.isNotEmpty(segment1.getOffer().getNightlyCorpMarketSegments())) {
                  LocalDate currentDate = segment1.getStart();
                  LocalDate lastNight = segment1.getEnd();
                  // endDate is checkout date so we don't need to consider that day
                  lastNight = lastNight.plusDays(-1);
                  while (!currentDate.isAfter(lastNight)) {
                	  final RoomMarket market = new RoomMarket();
                      LocalDate finalCurrentDate = currentDate;
                      market.setSourceCode(marketsSourceCode);
					  market.setMarketCode(getMarketCodeByDate(segment1.getOffer(), finalCurrentDate));
					  market.setDate(DateUtil.toDate(currentDate));
                      markets.add(market);
                      currentDate = currentDate.plusDays(1);
                  }
              }else{
                  RoomMarket market = new RoomMarket();
                  market.setSourceCode(marketsSourceCode);
                  markets.add(market);
              }
        return markets;
    }

	private static String getMarketCodeByDate(OfferRes offer, LocalDate finalCurrentDate) {
		return offer.getNightlyCorpMarketSegments().stream()
				.filter(nightMarket -> !finalCurrentDate.isBefore(nightMarket.getStart()))
				.filter(nightMarket -> !finalCurrentDate.isAfter(nightMarket.getEnd()))
				.map(NightlyCorporateMarketSegment::getValue)
				.findFirst()
				.orElse(null);
	}

	public static double getAmountPaidAgainstDeposit(List<PaymentTransactionRes> depositPayments) {
        double totalAmountPaid = ServiceConstant.ZERO_DOUBLE_VALUE;
        if (CollectionUtils.isNotEmpty(depositPayments)) {
         // for cash payments it will return 0.0
            Optional<PaymentTransactionRes> ccPayment = depositPayments.stream().filter(txn -> PaymentType.NUMBER_5.equals(txn.getPaymentType()))
                    .filter(transaction -> null != transaction.getAmount()).findFirst();
            if(ccPayment.isPresent()) {
                double totalDeposit = depositPayments.stream().filter(
                                payment -> PaymentIntent.DEPOSIT.getValue().equalsIgnoreCase(payment.getPaymentIntent().getValue()))
                        .mapToDouble(paymentTransactionRes -> Double.parseDouble(paymentTransactionRes.getAmount())).sum();
                double totalRefund = depositPayments.stream().filter(
                                payment -> PaymentIntent.REFUND.getValue().equalsIgnoreCase(payment.getPaymentIntent().getValue()))
                        .mapToDouble(paymentTransactionRes -> Double.parseDouble(paymentTransactionRes.getAmount())).sum();
                totalAmountPaid = totalDeposit - totalRefund;
            }
        }
        return totalAmountPaid;
    }
    public static Deposit calculateRoomAndComponentDeposit(List<SegmentResItem> segmentList, ImageStatus imageStatus) {
		SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(segmentList);
        Deposit allDepositCalc = createAcrsDepositCal(mainSegment);
        // get total paid amount from main segment
        double amountPaidAgainstDeposit = ServiceConstant.ZERO_DOUBLE_VALUE;

        FormOfPaymentRes segmentFormOfPayment = mainSegment.getFormOfPayment();
        if (null != segmentFormOfPayment) {
            amountPaidAgainstDeposit = getAmountPaidAgainstDeposit(segmentFormOfPayment.getDepositPayments());
        }
        List<SegmentResItem> componentSegmentList = getComponentSegments(segmentList);
        if (CollectionUtils.isNotEmpty(componentSegmentList)) {
            componentSegmentList.forEach(component -> {
                Deposit componentDeposit = createAcrsDepositCal(component);
                allDepositCalc.setAmount(allDepositCalc.getAmount() + componentDeposit.getAmount());
                allDepositCalc
                        .setForfeitAmount(allDepositCalc.getForfeitAmount() + componentDeposit.getForfeitAmount());
            });
        }

        // setOverrideAmount
        if (imageStatus == ImageStatus.COMMITTED) {
            allDepositCalc.setOverrideAmount(
                    (amountPaidAgainstDeposit != allDepositCalc.getAmount()) ? amountPaidAgainstDeposit : -1.0);
        }

        return allDepositCalc;
    }

    //Mike
    // actual override amount will set inside calling function
    public static Deposit createAcrsDepositCal(SegmentResItem segment) {
        Deposit depositCalc = new Deposit();
        PoliciesRes segmentOfferPolicies = segment.getOffer().getPolicies();
        if (null != segmentOfferPolicies) {
            DepositRes depositPolicy = segmentOfferPolicies.getDeposit();
            if (null != depositPolicy) {
                double depositCalcAmount = Double.parseDouble(depositPolicy.getAmount());
                depositCalc.setAmount(depositCalcAmount);
                depositCalc.setOverrideAmount(-1);
                depositCalc.setDueDate(
                        CommonUtil.getDate(depositPolicy.getDeadline().getDateTime(),
                                ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
            }
            else {
                depositCalc.setOverrideAmount(-1);
            }
            CancellationNoShowRes cancellation = segmentOfferPolicies.getCancellationNoShow();
            if (null != cancellation) {
                depositCalc.setForfeitAmount(
                        Double.parseDouble(cancellation.getAmount()));
                depositCalc.setForfeitDate(CommonUtil.getDate(
                        cancellation.getDeadline().getDateTime(),
                        ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS));
            }
            depositCalc.setForfeitDate(
                    (depositCalc.getForfeitDate() != null) ? depositCalc.getForfeitDate() : depositCalc.getDueDate());
        }
        else {
            depositCalc.setOverrideAmount(-1);
        }
        return depositCalc;
    }

    public static AgentInfo getAgentInfoFromAcrs(TravelAgencyInformation travelAgencyInfo) {
        AgentInfo agentInfo = new AgentInfo();
        if (CollectionUtils.isNotEmpty(travelAgencyInfo.getIdentifiers())) {
            agentInfo.setAgentId(travelAgencyInfo.getIdentifiers().get(0).getId());
            agentInfo.setAgentType(travelAgencyInfo.getIdentifiers().get(0).getType().name());
        }
        return agentInfo;
    }
  /**
   * This will convert ACRS routings to RBS routings
   * If two manual routing dates are consecutive(with same auth and routing code) then those will be merged
   * @param mlifeNo
   * @param productUses
   * @return
   */
    public static List<com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction> createRoutingInstructionfromACRS(String mlifeNo,
            ProductUseRes productUses) {
        List<RoutingInstruction> routingInstructions = productUses.stream()
                .filter(x -> CollectionUtils.isNotEmpty(x.getRoutingInstructions()))
                .flatMap(x -> x.getRoutingInstructions().stream()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(routingInstructions)) {
            return routingInstructions.stream()
                    .map(routingInstruction -> createRoutingInstFromACRS(mlifeNo, routingInstruction))
                    .filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }


    /**
     * createRoutingInstruction from ACRS
     *
     * @param acrsRouting
     * @return
     */
    private static com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction createRoutingInstFromACRS(String mlifeNo,
            RoutingInstruction acrsRouting) {
        com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction routing = new com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction();

        if(StringUtils.isNotBlank(acrsRouting.getWindow())){
        	routing.setWindow(Integer.parseInt(acrsRouting.getWindow()));
        }
        if (acrsRouting.getLimitCriteria() != null) {
          if(acrsRouting.getLimitCriteria().equals(RoutingInstructionWithoutId.LimitCriteriaEnum.FLAT)) {
            //ICE doesn't support FLAT value, so converting to "Value"
            routing.setLimitType(ServiceConstant.FLAT_LIMITTYPE_ICE);
          }else {
            routing.setLimitType(acrsRouting.getLimitCriteria().getValue());
          }
        }
        if (acrsRouting.getLimitValue() != null) {
          routing.setLimit(acrsRouting.getLimitValue());
        }
        if (acrsRouting.isIsDaily() != null) {
          routing.setDailyYN(acrsRouting.isIsDaily());
        }
        //Two Options from ACRS: System and Client
        if (null != acrsRouting.getSource() && acrsRouting.getSource().equals(RoutingInstructionWithoutId.SourceEnum.SYSTEM)) {
        	//If it is system we are setting as Program to Support ICE.
        	routing.setSource(ServiceConstant.ROUTING_INSTRUCTIONS_PROGRAM);
            routing.setIsSystemRouting(true);
        } else {
        	//This should be Client.
        	routing.setSource(acrsRouting.getSource().toString());
            routing.setIsSystemRouting(false);
        }
		//Logic for Days of week
        final String acrsDow = acrsRouting.getDow();
        if (null != acrsDow) {
            for (int i = 0; i < acrsDow.length(); i++) {
                if (i == 0) {
                    routing.setApplicableMonday(acrsDow.charAt(i) == 'M');
                }
                if (i == 1) {
                    routing.setApplicableTuesday(acrsDow.charAt(i) == 'T');
                }
                if (i == 2) {
                    routing.setApplicableWednesday(acrsDow.charAt(i) == 'W');
                }
                if (i == 3) {
                    routing.setApplicableThursday(acrsDow.charAt(i) == 'T');
                }
                if (i == 4) {
                    routing.setApplicableFriday(acrsDow.charAt(i) == 'F');
                }
                if (i == 5) {
                    routing.setApplicableSaturday(acrsDow.charAt(i) == 'S');
                }
                if (i == 6) {
                    routing.setApplicableSunday(acrsDow.charAt(i) == 'S');
                }
            }
        }
        routing.setHostAuthorizerAppUserId(acrsRouting.getAuthorizerCode());
        routing.setHostRoutingCodes(new String[] { acrsRouting.getRoutingCode() });
        routing.setStartDate(DateUtil.toDate(acrsRouting.getPeriod().getStart()));
        routing.setEndDate(DateUtil.toDate(acrsRouting.getPeriod().getEnd()));
        routing.setComments(acrsRouting.getComment());
        routing.setMemberShipNumber(mlifeNo);
        return routing;
    }

    public static List<RoutingInstruction> buildACRSRoutingInstructions(
            List<com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction> list) {
        List<RoutingInstruction> acrsRIs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            acrsRIs = list.stream()
					.map(BaseAcrsTransformer::createACRSRoutingInstruction)
					.collect(Collectors.toList());
        }
        return acrsRIs;
    }


	private RoutingInstruction createACRSRoutingInstruction(
			com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction routing) {
	    RoutingInstruction acrsRouting = new RoutingInstruction();
		Period period = new Period();
		if (routing.getStartDate() != null) {
			period.setStart(routing.getStartDate().toInstant().atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE))
					.toLocalDate());
		}
		if (routing.getEndDate() != null) {
			period.setEnd(routing.getEndDate().toInstant().atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE))
					.toLocalDate());
		}
		acrsRouting.setPeriod(period);
		acrsRouting.setAuthorizerCode(routing.getHostAuthorizerAppUserId());
        if (null != routing.getSource()) {
            if (routing.getSource().equalsIgnoreCase(ServiceConstant.ROUTING_INSTRUCTIONS_PROGRAM)) {
                acrsRouting.setSource(RoutingInstructionWithoutId.SourceEnum.SYSTEM);
            } else {
                acrsRouting.setSource(RoutingInstructionWithoutId.SourceEnum.fromValue(routing.getSource()));
            }
        }
		acrsRouting.setWindow(Integer.toString(routing.getWindow()));
		if (StringUtils.isNotEmpty(routing.getLimitType())) {
		    if (routing.getLimitType().equalsIgnoreCase(ServiceConstant.FLAT_LIMITTYPE_ICE)) {
                acrsRouting.setLimitCriteria(RoutingInstructionWithoutId.LimitCriteriaEnum.FLAT);
            } else {
                acrsRouting.setLimitCriteria(RoutingInstructionWithoutId.LimitCriteriaEnum.fromValue(routing.getLimitType()));
            }
		}
        if (routing.getLimit() != null) {
            acrsRouting.setLimitValue(routing.getLimit());
        }
		acrsRouting.setIsDaily(routing.isDailyYN());
		if (routing.getHostRoutingCodes() != null && routing.getHostRoutingCodes().length > 0) {
			acrsRouting.setRoutingCode(routing.getHostRoutingCodes()[0]);
		}
        if(StringUtils.isNotEmpty(routing.getComments())) {
            acrsRouting.setComment(routing.getComments());
        }
		//Logic for Days of week
        String dayDetails = (routing.isApplicableMonday() ? "M" : "-") +
                (routing.isApplicableTuesday() ? "T" : "-") +
                (routing.isApplicableWednesday() ? "W" : "-") +
                (routing.isApplicableThursday() ? "T" : "-") +
                (routing.isApplicableFriday() ? "F" : "-") +
                (routing.isApplicableSaturday() ? "S" : "-") +
                (routing.isApplicableSunday() ? "S" : "-");
        acrsRouting.setDow(dayDetails);
        return acrsRouting;
	}

    /**
     * it will return the main segment from the SegmentRes
     *
     * @param segmentList
     * @return
     */
    public static SegmentResItem getMainSegment(List<SegmentResItem> segmentList) {
        Optional<SegmentResItem> mainSegment = segmentList.stream().filter(segmentObj -> null == segmentObj.getParentId())
                .findFirst();
        if (mainSegment.isPresent()) {
            return mainSegment.get();
        } else {
            throw new InvalidParameterException("No valid segment attached to HotelReservationRes.");
        }
    }

    /**
     * it will return the active ComponentSegmentsfrom the SegmentRes
     * @param segmentList
     * @return
     */
    public static List<SegmentResItem> getComponentSegments(List<SegmentResItem> segmentList) {
		SegmentResItem mainSegment = getMainSegment(segmentList);
        return segmentList.stream().filter(
                segmentObj -> null != segmentObj.getParentId() && !isSegmentDeletedOnModify(segmentObj,mainSegment.getSegmentStatus(),mainSegment.getCancellationDateTime()))
                .collect(Collectors.toList());
    }

    /**
     * For delete and cancel flow , status will be CL in both case. We have to differentiate
     * deleted and canceled addons by CancellationReasons code.
     * while deleting add-on segment on modify we set CancellationReasons code = SEGMENT_DELETED_ON_MODIFY
     * @param segmentObj
     * @return
     */
    private static boolean isSegmentDeletedOnModify(SegmentResItem segmentObj, SegmentStatus resvStatus, String resvCancellationDateTime) {
        boolean isDeletedOnModify = false;
        SegmentStatus addOnSegmentStatus = segmentObj.getSegmentStatus();
        String addOnCancelDateTime = segmentObj.getCancellationDateTime();
        // main segment is not canceled but add segment is canceled
        // means addon got canceled on modify.
        if(!SegmentStatus.CL.equals(resvStatus)){
            if(SegmentStatus.CL.equals(addOnSegmentStatus)){
                isDeletedOnModify = true;
            }
        }else{
            // reservation is canceled
            if(StringUtils.isNotEmpty(resvCancellationDateTime) &&
                    StringUtils.isNotEmpty(addOnCancelDateTime)){
                // get time diff between two cancelDateTime
                // if main segment and add-ons segment cancel time are different
                // means addon got canceled on modify
                // if time diff > 5 sec means isDeletedOnModify = true
                try {
                    isDeletedOnModify = ReservationUtil.getTimeDiffInSec(resvCancellationDateTime, addOnCancelDateTime) > ServiceConstant.CANCEL_MAIN_ADDON_SEC_DIFF;
                }catch (DateTimeParseException  e){
                    log.error("Error while parsing segment cancelDateTime.");
                }
            }
        }
        return isDeletedOnModify;
    }

    public static String transFormAcrsDominentPlay(String dominance) {
        if (ServiceConstant.SLOTS.equalsIgnoreCase(dominance)) {
            return ServiceConstant.SLOT_RES;
        } else {
            return dominance;
        }
    }
    public static String transFormDominentPlayForACRS(String dominance) {
        if (ServiceConstant.SLOT_RES.equalsIgnoreCase(dominance)) {
            return ServiceConstant.SLOTS;
        } else {
            return dominance;
        }
    }

    /**
     * buildComponentDetailsFromACRSRes
     * @param componentSegments
     * @param products
     * @param ratePlans
     * @return
     */
    public static List<PurchasedComponent> buildComponentDetailsFromACRSRes(List<SegmentResItem> componentSegments,
                                                                            List<Product> products, List<RatePlanRes> ratePlans, String acrsPropertyCode) {

        return componentSegments.stream()
                .map(componentSegment ->
            createPurchasedComponentFromSegment(products, ratePlans, componentSegment, acrsPropertyCode))
                .collect(Collectors.toList());
    }

    private static PurchasedComponent createPurchasedComponentFromSegment(List<Product> products,
                                                                          List<RatePlanRes> ratePlans, SegmentResItem componentSegment, String acrsPropertyCode) {
		ProductUseResItem productUse = componentSegment.getOffer().getProductUses().get(0);
        String inventoryTypeCode = productUse.getInventoryTypeCode();
        String ratePlanCode = productUse.getRatePlanCode();

        PurchasedComponent roomBookingComponent = new PurchasedComponent();
        // set desc

        // set ratePlan name
        Optional<RatePlanRes> ratePlan = ratePlans.stream().filter(r -> ratePlanCode.equalsIgnoreCase(r.getCode()))
                .findFirst();
        if(ratePlan.isPresent() && null != ratePlan.get().getName() && CollectionUtils.isNotEmpty(ratePlan.get().getName().values())){
            RatePlanRes ratePlanInfo = ratePlan.get();
            String ratePlanName=ratePlanInfo.getName().values().toString().replaceAll("[\\[\\]]", "");
            String shortDesc = null != ratePlanInfo.getDescription()? ratePlanInfo.getDescription().values().toString().replaceAll("[\\[\\]]", "") : null;
            String longDesc = null != ratePlanInfo.getLongDescription() ? ratePlanInfo.getLongDescription().values().toString().replaceAll("[\\[\\]]", "") : null;
            roomBookingComponent.setRatePlanName(ratePlanName);
           //CBSR-1906
            roomBookingComponent.setShortDescription(shortDesc);
            roomBookingComponent.setLongDescription(longDesc);
	    roomBookingComponent.setRatePlanCode(ratePlan.get().getCode());
        }
        // For F1 components
        if (productUse.getRatePlanCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1)
                || productUse.getRatePlanCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_HDN)) {
            roomBookingComponent.setCode(productUse.getRatePlanCode());
        } else {
            roomBookingComponent.setCode(inventoryTypeCode);
        }
        // id
        final String componentId = ACRSConversionUtil.createComponentGuid(inventoryTypeCode,
                productUse.getRatePlanCode(), ServiceConstant.COMPONENT_FORMAT, acrsPropertyCode);
        roomBookingComponent.setId(componentId);
        roomBookingComponent.setActive(true);
        // deposit
        if (null != componentSegment.getOffer().getPolicies()
                && null != componentSegment.getOffer().getPolicies().getDeposit()) {
            DepositRes deposit = componentSegment.getOffer().getPolicies().getDeposit();
            roomBookingComponent.setDepositAmount(Double.parseDouble(deposit.getAmount()));
            roomBookingComponent.setIsDepositRequired(deposit.getIsRequired());
        }

        // price frequency
        ProductUseRates productRates = productUse.getProductRates();
        PricingFrequency pricingFrequency = productRates.getPricingFrequency();
        roomBookingComponent.setPricingApplied(getComponentAppliedPrice(pricingFrequency, productRates.getBookingPattern()));

        // prices
        roomBookingComponent.setPrices(getComponentPricesFromACRSSegment(componentSegment));

        // total trip price and tax
        TotalRateDetails totalRate = productUse.getProductRates().getTotalRate();
        if (null != totalRate) {
            if(null != totalRate.getTotalTaxes()) {
                roomBookingComponent.setTripTax(Float.parseFloat(totalRate.getTotalTaxes()));
            }
            roomBookingComponent.setTripPrice(Float.parseFloat( totalRate.getBsAmt()));
            roomBookingComponent.setPrice(roomBookingComponent.getPrices().get(0).getAmount());
        }

        return roomBookingComponent;
    }

    /**
     * getComponentPricesFromACRSSegment
     * @param segment
     * @return
     */
    public static ComponentPrices getComponentPricesFromACRSSegment(SegmentResItem segment) {
        ComponentPrices componentPrices = new ComponentPrices();
        LocalDate currentDate = segment.getStart();
        LocalDate lastNight = segment.getEnd();
        // endDate is checkout date so we don't need to create a roomPrice for
        // that day.
        lastNight = lastNight.plusDays(-1);
        while (!currentDate.isAfter(lastNight)) {
            LocalDate finalCurrentDate = currentDate;

            final Optional<ProductUseResItem> productUseOptional = segment.getOffer().getProductUses().stream()
                    .filter(productUse -> Objects.nonNull(productUse.getProductRates()))
                    .filter(productUse -> !finalCurrentDate.isBefore(productUse.getPeriod().getStart()))
                    .filter(productUse -> finalCurrentDate.isBefore(productUse.getPeriod().getEnd()))
                    .findFirst();

            if (!productUseOptional.isPresent()) {
                log.error("Error getting componentPricesFromACRSSegment unable to find valid productUse for date: {}",
                        finalCurrentDate);
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }

            final ProductUseResItem firstProductUse = productUseOptional.get();
			ProductUseRates productUseRates = getProductUseRatesFromProductUse(firstProductUse);

            Optional<DailyRateDetails> dailyRateDetails = productUseRates.getDailyRates().stream()
                    .filter(rateChange -> !finalCurrentDate.isBefore(rateChange.getStart()))
                    .filter(rateChange -> finalCurrentDate.isBefore(rateChange.getEnd())).findFirst()
                    .map(DailyRate::getDailyTotalRate);
            if (dailyRateDetails.isPresent()) {
                ComponentPrice componentPrice = new ComponentPrice();
                componentPrice.setAmount(Double.parseDouble(dailyRateDetails.get().getBsAmt()));
                String totaltax = dailyRateDetails.get().getTotalTaxes();
                componentPrice.setTax(Double.parseDouble(null != totaltax ? totaltax : "0"));
                componentPrice.setDate(DateUtil.toDate(finalCurrentDate));
                componentPrices.add(componentPrice);
            }

            currentDate = currentDate.plusDays(1);
        }
        return componentPrices;
    }
    public static List<PurchasedComponent> buildComponentDetails(List<ReservationSplRequest> specialRequestObjList){
        List<PurchasedComponent> components = new ArrayList<>();
        specialRequestObjList.forEach(x ->{
            PurchasedComponent purchasedComponent =  new PurchasedComponent();
            purchasedComponent.setId(x.getId());
            purchasedComponent.setCode(x.getCode());
			// Web requires this flag to be true in order to display it as checked on the UI.
			purchasedComponent.setActive(true);
            //Overiding shortDescription with Long description to resove Null text issue on WEB for special requests.
            purchasedComponent.setShortDescription(StringUtils.isEmpty(x.getShortDescription()) ? x.getDescription() : x.getShortDescription());
            purchasedComponent.setLongDescription(x.getDescription());
            purchasedComponent.setPricingApplied(x.getPricingApplied());
            components.add(purchasedComponent);
        });
        return components;

    }
    public static String getComponentAppliedPrice(PricingFrequency pricingFrequency, BookingPattern bookingPattern ) {
        // PER STAY
        if (pricingFrequency == PricingFrequency.PERNIGHT) {
            return ServiceConstant.NIGHTLY_PRICING_APPLIED;
        } else{
            if (pricingFrequency == PricingFrequency.PERSTAY) {
                // PER STAY
                return ServiceConstant.CHECKIN_PRICING_APPLIED;
            } else {
                // PER USE
                if (bookingPattern == BookingPattern.DAYOFCHECKIN) {
                    return ServiceConstant.CHECKIN_PRICING_APPLIED;
                } else {
                   return ServiceConstant.CHECKOUT_PRICING_APPLIED;
                }
            }
        }
    }

    public static String acrsResPmsStatusToReservationOperaState(Map<SegmentPmsStatus, String> segmentPmsStateOperaStateMap, SegmentPmsStatus segmentPmsStatus, ResStatus status) {
        String pmsStatus;
        if(ResStatus.CL.equals(status)){
            pmsStatus = ServiceConstant.CANCELED;
        }else{
            pmsStatus =  segmentPmsStateOperaStateMap.getOrDefault(segmentPmsStatus,ServiceConstant.RESERVED_STRING);
        }
        return pmsStatus;
    }
    /*
        getRatePlans
     */
    public static Map<String, AddonComponentRateInfo> getRatePlans(List<RatePlanPricing> ratePlans) {
        final Map<String, AddonComponentRateInfo> ratePlansMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ratePlans)){
            ratePlansMap.putAll(ratePlans.stream()
                    .filter(rate->null!=rate.getName()
                            && CollectionUtils.isNotEmpty(rate.getName().get("en")))
                            .filter(rate->null!=rate.getDesc()
                                    && CollectionUtils.isNotEmpty(rate.getDesc().get("en")))
                    .collect(Collectors.toMap(com.mgm.services.booking.room.model.crs.searchoffers.RatePlanCommon::getCode, BaseAcrsTransformer::createAddonsComponentRateDetails)));
        }
        return ratePlansMap;
    }

    private static AddonComponentRateInfo createAddonsComponentRateDetails(RatePlanPricing rate) {
        String name = (String)rate.getName().get("en").get(0);
        String shortDesc = (String)rate.getDesc().get("en").get(0);
        String longDesc = null != rate.getLongDesc()? (String)rate.getLongDesc().get("en").get(0) : null;
        return new AddonComponentRateInfo(name,shortDesc,longDesc);
    }

    @Data
      class AddonComponentRateInfo {
        private String name;
        private String shortDesc;
        private String longDesc;
        public AddonComponentRateInfo(String name, String shortDesc, String longDesc){
            this.name = name;
            this.shortDesc = shortDesc;
            this.longDesc =  longDesc;
        }
    }
}


