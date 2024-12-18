package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.*;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.ocrs.*;
import com.mgm.services.booking.room.model.opera.OperaInfoResponse;
import com.mgm.services.booking.room.model.opera.RoutingInstruction;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.SourceRoomReservationBasicInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.PartnerProgramConfig;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.PropertyConfig.PropertyValue;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.DateUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Log4j2
public class FindReservationDAOImpl extends BaseStrategyDAO implements FindReservationDAO {

	private static final int MLIFE_WINDOW_NUMBER = 104;
	private final FindReservationDAOStrategyGSEImpl gseStrategy;
	private final FindReservationDAOStrategyACRSImpl acrsStrategy;
	private final OCRSDAO ocrsDao;
	private final OperaInfoDAO operaInfoDAO;
	private final RefDataDAO refDataDAO;
	private final PropertyConfig propertyConfig;
	private final FindReservationServiceHelper findReservationServiceHelper;
	private final ApplicationProperties appProperties;
	private final ServiceConversionHelper serviceConversionHelper;
	private final RoomProgramDAO roomProgramDao;
	private final RoomContentDAO roomContentDao;
	private final PartnerProgramConfig partnerProgramConfig;
	private final ApplicationProperties applicationProperties;

	@Autowired
	public FindReservationDAOImpl(FindReservationDAOStrategyACRSImpl acrsStrategy, ApplicationProperties applicationProperties,
								  ApplicationProperties appProperties, FindReservationServiceHelper findReservationServiceHelper,
								  FindReservationDAOStrategyGSEImpl gseStrategy, OCRSDAO ocrsDao, OperaInfoDAO operaInfoDAO,
								  PartnerProgramConfig partnerProgramConfig, PropertyConfig propertyConfig,
								  RefDataDAO refDataDAO, RoomContentDAO roomContentDao, RoomProgramDAO roomProgramDao,
								  ServiceConversionHelper serviceConversionHelper, AcrsProperties acrsProperties,
								  ReferenceDataDAOHelper referenceDataDAOHelper) {
		this.acrsStrategy = acrsStrategy;
		this.applicationProperties = applicationProperties;
		this.appProperties = appProperties;
		this.findReservationServiceHelper = findReservationServiceHelper;
		this.gseStrategy = gseStrategy;
		this.ocrsDao = ocrsDao;
		this.operaInfoDAO = operaInfoDAO;
		this.partnerProgramConfig = partnerProgramConfig;
		this.propertyConfig = propertyConfig;
		this.refDataDAO = refDataDAO;
		this.roomContentDao = roomContentDao;
		this.roomProgramDao = roomProgramDao;
		this.serviceConversionHelper = serviceConversionHelper;
		this.acrsProperties = acrsProperties;
		this.referenceDataDAOHelper = referenceDataDAOHelper;
	}

	@Override
	public RoomReservation findRoomReservation(FindReservationRequest reservationRequest) {
        RoomReservation roomReservation = null;

        // If not managed by Acrs per configuration search GSE
        if (!isPropertyManagedByAcrs(reservationRequest.getSource())) {
            try {
                roomReservation = findRoomReservation(reservationRequest, gseStrategy);
            } catch (BusinessException businessException) {
                // if reservation not found, then continue on to search ACRS, throw all other exceptions
                if (ErrorCode.RESERVATION_NOT_FOUND != businessException.getErrorCode() || !isAcrsEnabled() ) {
                    throw businessException;
                }
            }
        }

        // If reservation not found in GSE, GSE not search, or the reservation returned is managed by ACRS
        //  and not GSE then search ACRS.
        if (null == roomReservation || isPropertyManagedByAcrs(roomReservation.getPropertyId())) {
            roomReservation = findRoomReservation(reservationRequest, acrsStrategy);
        }

        return roomReservation;
    }

    private RoomReservation findRoomReservation(FindReservationRequest reservationRequest, FindReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("findRoomReservation", reservationRequest.getConfirmationNumber(), strategy));
        return strategy.findRoomReservation(reservationRequest);
    }

    @Override
    public RoomReservation findRoomReservation(FindReservationV2Request reservationRequest) {
        String requestConfirmationNumber = null;
        if (StringUtils.isNotEmpty(reservationRequest.getConfirmationNumber())) {
            requestConfirmationNumber = reservationRequest.getConfirmationNumber();
        }
        // get reservation from OCRS which can help property switch logic
        OcrsReservation ocrsReservation = ocrsDao.getOCRSReservation(reservationRequest.getConfirmationNumber());
        
        //1. add GSE/ACRS confirmation number and propertyId in the request from OCRS.
        FindReservationV2Request modifiedRequest = updateRequestByOCRSReservation(reservationRequest, ocrsReservation);

		RoomReservation roomReservation;
        try {
			if (modifiedRequest.isTcolvTravelClickResv()) {
				log.info("{} confirmation number reservation is a TCOLV Travel Click reservation. Hence throwing exception", reservationRequest.getConfirmationNumber());
				throw new BusinessException(ErrorCode.RESERVATION_BLACKLISTED);
			}
			roomReservation = findReservation(reservationRequest, modifiedRequest, ocrsReservation);
			log.info("Retrieved reservation successfully: {}", roomReservation.getId());
        } catch (BusinessException businessException) {
            if (!ErrorCode.RESERVATION_BLACKLISTED.equals(businessException.getErrorCode())) {
				log.error("Unable to findRoomReservation due to BusinessException: ", businessException);
                throw businessException;
            }
			log.info("confirmationNumber {} Blacklisted; Building from OCRS.",
                    reservationRequest.getConfirmationNumber());
			if (null == ocrsReservation) {
				log.error("Blacklisted confirmationNumber {} not found in OCRS either.", reservationRequest.getConfirmationNumber());
				throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
			}
			roomReservation = buildBlackListedReservationFromOcrsResponse(reservationRequest, ocrsReservation);
		}

		Optional<BusinessException> propertyNoLongerManagedByMGM = getPropertyNoLongerManagedByMGMException(roomReservation);
		if (propertyNoLongerManagedByMGM.isPresent()) {
			throw propertyNoLongerManagedByMGM.get();
		}

		// Setting the request Confirmation Number
		if (null != requestConfirmationNumber) {
			roomReservation.setRequestConfirmationNumber(requestConfirmationNumber);
		}

		return roomReservation;
	}

	private List<PartnerAccounts> getPartnerAccountsList(OcrsReservation ocrsReservation) {
		if (null == ocrsReservation || null == ocrsReservation.getResProfiles()) {
			return new ArrayList<>();
		}
		List<ResProfile> resProfiles = ocrsReservation.getResProfiles().getResProfile();
		if (CollectionUtils.isEmpty(resProfiles)) {
			return new ArrayList<>();
		}
		Profile ocrsProfile = resProfiles.get(0).getProfile();
		if (null == ocrsProfile) {
			return new ArrayList<>();
		}
		Memberships memberships = ocrsProfile.getMemberships();
		if (null == memberships || CollectionUtils.isEmpty(memberships.getMembership())) {
			return new ArrayList<>();
		}
		return memberships.getMembership().stream()
				.filter(Objects::nonNull)
				.map(FindReservationDAOImpl::transform)
				.collect(Collectors.toList());
	}

	private static PartnerAccounts transform(SelectedMembership selectedMemberShip) {
		PartnerAccounts partnerAccounts = new PartnerAccounts();
		partnerAccounts.setPartnerAccountNo(selectedMemberShip.getAccountID());
		partnerAccounts.setProgramCode(selectedMemberShip.getProgramCode());
		partnerAccounts.setMembershipLevel(selectedMemberShip.getLevelCode());
		return partnerAccounts;
	}

	private Optional<BusinessException> getPropertyNoLongerManagedByMGMException(RoomReservation roomReservation) {
		Optional<BusinessException> businessExceptionOptional = Optional.empty();
		if (null == roomReservation || StringUtils.isEmpty(roomReservation.getPropertyId())) {
			return businessExceptionOptional;
		}
		Optional<ErrorCode> propertyHandOverErrorCode = getPropertyHandOverErrorCode(roomReservation);
		if (propertyHandOverErrorCode.isPresent()) {
			businessExceptionOptional = Optional.of(new BusinessException(propertyHandOverErrorCode.get()));
		}
		return businessExceptionOptional;
	}

	private Optional<ErrorCode> getPropertyHandOverErrorCode(RoomReservation roomReservation) {
		Optional<ErrorCode> propertyHandOverErrorCode = Optional.empty();
		if (null == appProperties.getTransferredPropertyIds() || !appProperties.getTransferredPropertyIds().contains(roomReservation.getPropertyId())) {
			return propertyHandOverErrorCode;
		}
		Date handOverDate = appProperties.getHandoverDate(roomReservation.getPropertyId());
		if (null == handOverDate || null == roomReservation.getCheckInDate()) {
			return propertyHandOverErrorCode;
		}

		boolean returnReservation = ReservationUtil.returnReservationForTransferredProperty(roomReservation, handOverDate, appProperties);
		if (!returnReservation) {
			String handOverErrorCode = appProperties.getHandOverErrorCode(roomReservation.getPropertyId());
			if (StringUtils.isNotEmpty(handOverErrorCode)) {
				propertyHandOverErrorCode = Optional.of(ErrorCode.getErrorCode(handOverErrorCode));
			}
		}
		return propertyHandOverErrorCode;
	}

	private RoomReservation buildBlackListedReservationFromOcrsResponse(FindReservationV2Request reservationRequest, OcrsReservation ocrsReservation) {
		RoomReservation roomReservation = new RoomReservation();
		String propertyId = appProperties
				.getPropertyIdFromHotelCode(ocrsReservation.getHotelReference().getHotelCode());
		roomReservation.setPropertyId(propertyId);
		//CBSR-2474 - Invoke content API to resolve property id and room type id because incorrect details are being populated
		//from RBS cache.
		if (null != ocrsReservation.getRoomStays() && CollectionUtils.isNotEmpty(ocrsReservation.getRoomStays().getRoomStay())) {
			com.mgm.services.booking.room.model.content.Room roomDetails = roomContentDao.
					getRoomContent(ocrsReservation.getRoomStays().getRoomStay().get(0).getRoomInventoryCode(),
							ocrsReservation.getHotelReference().getHotelCode(), true);
			if (null != roomDetails) {
				roomReservation.setPropertyId(roomDetails.getPropertyId());
				roomReservation.setRoomTypeId(roomDetails.getId());
			}
			//CBSR-932 Room Type details and Property Details are incorrectly resolved on GraphQL side for OTA reservations
			//Hence returning operaRoomCode in RBS Response to correctly resolve property details and Room Type Details
			roomReservation.setOperaRoomCode(ocrsReservation.getRoomStays().getRoomStay().get(0).getRoomInventoryCode());
		}
		findReservationServiceHelper.updateRoomReservation(ocrsReservation, roomReservation, reservationRequest.getConfirmationNumber());
		return roomReservation;
	}

	private RoomReservation findReservation(FindReservationV2Request reservationRequest,
											FindReservationV2Request modifiedRequest, OcrsReservation ocrsReservation) {

		RoomReservation roomReservation = null;
		OperaInfoResponse operaInfoResponse = null;

        /*
        	External Call to Opera Retrieve
         */
		if (isAcrsEnabled()) {
			operaInfoResponse = operaInfoDAO.getOperaInfo(reservationRequest.getConfirmationNumber(), modifiedRequest.getPropertyCode());
			if (null != operaInfoResponse) {
				updateFromOperaInfoResponse(reservationRequest, modifiedRequest, ocrsReservation, operaInfoResponse);
            }
        }

		/*
			External search for Reservation in ACRS

			Note: May also make external SearchReservation call to ACRS
		 */
		if (null == modifiedRequest.getPropertyId() || isPropertyManagedByAcrs(modifiedRequest.getPropertyId())) {
			String modifiedRequestConfirmationNumber = getConfirmationNumberFromSearchReservation(reservationRequest, modifiedRequest);
			if (StringUtils.isNotEmpty(modifiedRequestConfirmationNumber)) {
				modifiedRequest.setConfirmationNumber(modifiedRequestConfirmationNumber);
			}
			// Make Acrs Remote call and decorate response
			roomReservation = getRoomReservationFromAcrsAndDecorateWithOperaInfo(modifiedRequest, operaInfoResponse);
		}

        /*
        	External Search for Reservation in GSE if not already found
        	Note: modifiedRequest may be modified in this method.
         */
		if (null == roomReservation && !applicationProperties.isGseDisabled()) {
			roomReservation = getRoomReservationFromGSEAndDecorateWithOcrsDetails(modifiedRequest, ocrsReservation);
		}

		// Need to throw RESERVATION_NOT_FOUND in case of swallowed Exceptions in ACRS and GSE disabled
		if (null == roomReservation) {
			log.error("Unable to find reservation with confirmation number {}", modifiedRequest.getConfirmationNumber());
			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		/*
			Modify fields in the response of found reservation before returning
		*/

		// set mgmId from OCRS
		if (null != ocrsReservation && null != roomReservation.getProfile()) {
			roomReservation.getProfile().setMgmId(getMgmIdFromOcrs(ocrsReservation, modifiedRequest.getOperaConfNumber()));
			Profile ocrsProfile = findReservationServiceHelper.getPrimaryProfile(ocrsReservation);
			if(ocrsProfile!=null && ocrsProfile.getMfResortProfileID()!=null && roomReservation.getProfile().getOperaId() == null) {
				roomReservation.getProfile().setOperaId(ocrsProfile.getMfResortProfileID());
			}
		}

		// Filter out only configured partner accounts from profile
		if (null != roomReservation.getProfile() && CollectionUtils.isNotEmpty(roomReservation.getProfile().getPartnerAccounts())) {
			List<PartnerAccounts> partnerAccounts = roomReservation.getProfile().getPartnerAccounts();
			List<String> partnerProgramCodes = partnerProgramConfig.getPartnerProgramValues().stream()
					.map(PartnerProgramConfig.PartnerProgramValue::getProgramCode)
					.collect(Collectors.toList());
			List<PartnerAccounts> filteredPartnerList = partnerAccounts.stream()
					.filter(membership ->
							(StringUtils.isNotEmpty(membership.getProgramCode()) &&
									partnerProgramCodes.contains(membership.getProgramCode().toUpperCase())))
					.collect(Collectors.toList());
			roomReservation.getProfile().setPartnerAccounts(filteredPartnerList);
		}

		// Makes External Call to check F1 package status
		if (isF1Package(roomReservation)) {
			roomReservation.setF1Package(true);
		}
		return roomReservation;
	}

	private RoomReservation getRoomReservationFromGSEAndDecorateWithOcrsDetails(FindReservationV2Request modifiedRequest,
																				OcrsReservation ocrsReservation) {
		RoomReservation roomReservation = null;
		boolean retryWithCacheOnly = false;
		// Send Request to GSE
		try {
			roomReservation = findRoomReservation(modifiedRequest, gseStrategy);
		} catch (BusinessException businessException) {
			if (ErrorCode.RESERVATION_NOT_FOUND == businessException.getErrorCode() && !modifiedRequest.isCacheOnly()
					&& modifiedRequest.getConfirmationNumber().startsWith("M")) {
				retryWithCacheOnly = true;
			} else {
				throw businessException;
			}
		}
		// Retry GSE request with cacheOnly=true if necessary
		if (retryWithCacheOnly) {
			log.info("Retrying GSE Retrieve reservation with cacheOnly flag as true");
			modifiedRequest.setCacheOnly(true);
			roomReservation = findRoomReservation(modifiedRequest, gseStrategy);
		}

		// Set PartnerAccounts from OCRS for GSE reservation
		if (null != roomReservation && null != roomReservation.getProfile()) {
			List<PartnerAccounts> partnerAccountsList = getPartnerAccountsList(ocrsReservation);
			// Only modify if new list not empty
			if (CollectionUtils.isNotEmpty(partnerAccountsList)) {
				roomReservation.getProfile().setPartnerAccounts(partnerAccountsList);
			}
		}
		return roomReservation;
	}

	private RoomReservation getRoomReservationFromAcrsAndDecorateWithOperaInfo(FindReservationV2Request modifiedRequest,
																			   OperaInfoResponse operaInfoResponse) {
		RoomReservation roomReservation = null;
		// Find reservation in ACRS
		try {
			roomReservation = findRoomReservation(modifiedRequest, acrsStrategy);
		} catch (BusinessException businessException) {
			if (!ErrorCode.RESERVATION_NOT_FOUND.equals(businessException.getErrorCode())) {
				throw businessException;
			}
			// Booking not found, continue to search GSE if enabled
			log.warn("Unable to find reservation with confirmation number '{}' in ACRS.", modifiedRequest.getConfirmationNumber());
		} catch (Exception exception) {
			log.warn("System Exception occurred during find reservation with confirmation number '{}' in ACRS."
					, modifiedRequest.getConfirmationNumber(), exception);
			throw exception;
		}

		// Add to ACRS response from Opera Retrieve response collected earlier
		if (null != roomReservation) {
			if (null != operaInfoResponse) {
				//set routing instructions
				// Note: May make External call for routing Authorizer IDs in `setOperaRoutingInstructions()` method
				roomReservation.setRoutingInstructions(setOperaRoutingInstructions(operaInfoResponse.getRoutingInstructionList(),
						roomReservation.getRoutingInstructions(), roomReservation.getPropertyId()));

				// Set opera profile id from OperaRetrieve response
				if (null != operaInfoResponse.getOperaProfileId() && null != roomReservation.getProfile()
						&& roomReservation.getProfile().getMlifeNo() == 0 && acrsProperties.isActiveCustomerOperaId()) {
					roomReservation.getProfile().setOperaId(operaInfoResponse.getOperaProfileId());
				}
			}
			// Manipulate roomReservation.purchasedComponents to set `setNonEditable` flag if necessary
			roomReservation = setWebSuppressibleComponentFlag(roomReservation);
		}
		return roomReservation;
	}

	private String getConfirmationNumberFromSearchReservation(FindReservationV2Request reservationRequest, FindReservationV2Request modifiedRequest) {
		String modifiedRequestConfirmationNumber = null;
		//search api call only if acrs confirmation number is not present
		//This scenario occurs only when reservation is created in Opera and retrieval is done in ICE or WEB,
		//OCRS does not receive the updated ACRS conf no due to event not triggered from Opera
		if (isPropertyManagedByAcrs(modifiedRequest.getPropertyId())
				&& reservationRequest.getConfirmationNumber().equalsIgnoreCase(modifiedRequest.getOperaConfNumber())) {
			try {
				modifiedRequestConfirmationNumber = searchRoomConfirmationNumber(modifiedRequest, acrsStrategy);
			} catch (BusinessException businessException) {
				// if reservation not found, then continue on to search ACRS, throw all other exceptions
				if (ErrorCode.RESERVATION_NOT_FOUND != businessException.getErrorCode() || !isAcrsEnabled()) {
					throw businessException;
				}
			}
		}
		return modifiedRequestConfirmationNumber;
	}

	private RoomReservation setWebSuppressibleComponentFlag(RoomReservation findRoomReservation) {
    	if (null != findRoomReservation && null != findRoomReservation.getPurchasedComponents()) {
			findRoomReservation.getPurchasedComponents().stream()
					.filter(component -> ReservationUtil.isSuppressWebComponent(component.getId(), acrsProperties))
					.forEach(specialRequest -> specialRequest.setNonEditable(true));
    	}
    	return findRoomReservation;
    }

    private String getMgmIdFromOcrs(OcrsReservation ocrsReservation, String operaConfNumber) {
        
        if (StringUtils.isEmpty(operaConfNumber)) {
            return StringUtils.EMPTY;
        }

        if (ocrsReservation.getReservationID()
                .equals(operaConfNumber)) {
            return ocrsReservation.getMgmProfile()
                    .getMgmId();
        }

        Optional<AdditionalGuest> guest = ocrsReservation.getMgmProfile()
                .getAdditionalGuests()
                .stream()
                .filter(g -> StringUtils.isNotEmpty(g.getReservationID()) && g.getReservationID()
                        .equals(operaConfNumber))
                .findFirst();
        if (guest.isPresent()) {
            return guest.get()
                    .getMgmId();
        }

        return StringUtils.EMPTY;
    }
    
    @Deprecated
    private RoomReservation findRoomReservationForPartyInfo(FindReservationV2Request reservationRequest) {
        
        // get reservation from OCRS which can help property switch logic
        OcrsReservation ocrsReservation = ocrsDao.getOCRSReservation(reservationRequest.getConfirmationNumber());
        
        //1. add GSE/ACRS confirmation number and propertyId in the request from OCRS.
        FindReservationV2Request modifiedRequest = updateRequestByOCRSReservation(reservationRequest, ocrsReservation);
        
        RoomReservation roomReservation = null;
        
        //2. get reservation from GSE or ACRS
        if (!isPropertyManagedByAcrs(modifiedRequest.getPropertyId())) {
            try {
                roomReservation = findRoomReservation(modifiedRequest, gseStrategy);
            } catch (BusinessException businessException) {
                // if reservation not found, then continue on to search ACRS, throw all other exceptions
                if (ErrorCode.RESERVATION_NOT_FOUND != businessException.getErrorCode() || !isAcrsEnabled() ) {
                    throw businessException;
                }
            }
        }
        return roomReservation;
    }
    
    private FindReservationV2Request updateRequestByOCRSReservation(FindReservationV2Request reservationRequest, OcrsReservation operaResv) {
        FindReservationV2Request updatedRequest = new FindReservationV2Request();
        updatedRequest.setSource(reservationRequest.getSource());
        updatedRequest.setPerpetualPricing(reservationRequest.isPerpetualPricing());
        updatedRequest.setConfirmationNumber(reservationRequest.getConfirmationNumber());
        updatedRequest.setCacheOnly(reservationRequest.isCacheOnly());
        updatedRequest.setChannel(reservationRequest.getChannel());
        updatedRequest.setCustomerTier(reservationRequest.getCustomerTier());
        updatedRequest.setFirstName(reservationRequest.getFirstName());
        updatedRequest.setLastName(reservationRequest.getLastName());
        updatedRequest.setMlifeNumber(reservationRequest.getMlifeNumber());
		updatedRequest.setPropertyId(reservationRequest.getPropertyId());

        if (null != operaResv) {

			// override propertyId from OperaResv
            // OCRS operaResv.getHotelReference().getHotelCode() return
            // propertyCode like 001, 021, 275 etc.
			String hotelCode = operaResv.getHotelReference().getHotelCode();
			String propertyCode = "MV" + hotelCode;
			updatedRequest.setPropertyCode(propertyCode);
            PropertyValue propertyValue = propertyConfig.getPropertyValuesMap().get(propertyCode);
            // if propertyValue is not null means its an ACRS property
            if (null != propertyValue) {
                // set guid in request
                updatedRequest.setPropertyId(propertyValue.getGsePropertyIds().get(0));
            }
            //CBSR-1412 Set the Property Id for TCOLV reservation lookup
			if (StringUtils.isNotEmpty(hotelCode)
					&& StringUtils.equalsIgnoreCase(hotelCode, appProperties.getTcolvHotelCode())) {
				updatedRequest.setPropertyId(appProperties.getPropertyIdFromHotelCode(hotelCode));
                updatedRequest.setTcolvReservation(true);
            }

			// Override confirmation numbers from OperaResv
            for (ResGuest rg : operaResv.getResGuests().getResGuest()) {

                if (null != rg.getReservationReferences()) {

                    if (operaResv.getHotelReference() != null && StringUtils.isNotEmpty(operaResv.getHotelReference().
                            getHotelCode()) && StringUtils.equalsIgnoreCase(operaResv.getHotelReference().getHotelCode(), appProperties.getTcolvHotelCode())) {
                        Optional<ReservationReference> tcolvAuroraCnfNumber = rg.getReservationReferences()
                                .getReservationReference().stream()
                                .filter(resvRef -> ServiceConstant.OCRS_AURORA_RESV_REF_TYPE.equalsIgnoreCase(resvRef.getType()))
                                .findFirst();
                        if (tcolvAuroraCnfNumber.isPresent() && !tcolvAuroraCnfNumber.get().getReferenceNumber().startsWith("M")) {
                            updatedRequest.setTcolvTravelClickResv(true);
                        }
                    }

                    for (ReservationReference rr : rg.getReservationReferences().getReservationReference()) {

                        if (rr.getReferenceNumber().equals(reservationRequest.getConfirmationNumber())) {

							// if reservation is found in ocrs, set opera confirmation number from respective guest obj
							updatedRequest.setOperaConfNumber(rg.getReservationID());

                            // if ACRS confirmation number is present, set it as confirmation number instead
                            Optional<ReservationReference> acrsGuestResvRef = rg.getReservationReferences()
                                    .getReservationReference().stream()
                                    .filter(resvRef -> acrsProperties.getOperaReservationReferenceTypes()
                                            .contains(resvRef.getType()))
                                    .findFirst();

                            if (acrsGuestResvRef.isPresent() && isPropertyManagedByAcrs(updatedRequest.getPropertyId())) {
                                // set confirmationNumber from OCRS ACRS reference if property is managed by ACRS
                                updatedRequest.setConfirmationNumber(acrsGuestResvRef.get().getReferenceNumber());
                                //CBSR-2132: If OCRS response has the MGMACRS tag with the ACRS confirmation number, set the ACRS cnf number
                                updatedRequest.setAcrsConfirmationNumber(acrsGuestResvRef.get().getReferenceNumber());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return updatedRequest;

    }
    
      
    private RoomReservation findRoomReservation(FindReservationV2Request reservationRequest, FindReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("findRoomReservationV2", reservationRequest.getConfirmationNumber(), strategy));
        return strategy.findRoomReservation(reservationRequest);
    }

    @Override
    public ReservationsBasicInfoResponse getRoomReservationsBasicInfoList(
            SourceRoomReservationBasicInfoRequestDTO request) {

        // get reservation from OCRS which can help property switch logic
        OcrsReservation ocrsReservation = ocrsDao.getOCRSReservation(request.getConfirmationNumber());

        String propertyId = null;

        if (null != ocrsReservation) {

            // Get propertyId
            // OCRS operaResv.getHotelReference().getHotelCode() return
            // propertyCode like 001, 021, 275 etc.
            String propertyCode = "MV" + ocrsReservation.getHotelReference().getHotelCode();
            PropertyValue propertyValue = propertyConfig.getPropertyValuesMap().get(propertyCode);
            // if propertyValue is not null means its an ACRS property
            if (null != propertyValue) {
                // set guid in request
                propertyId = propertyValue.getGsePropertyIds().get(0);
            }
        }
       
        // reservation not found in OCRS
        OperaInfoResponse operaInfoResponse = null;
        if (null == ocrsReservation && isAcrsEnabled()) {
            // adding the opera retrieve call for acrs find
            operaInfoResponse = operaInfoDAO.getOperaInfo(request.getConfirmationNumber(), null);
            if (null != operaInfoResponse) {
                if (StringUtils.isNotEmpty(operaInfoResponse.getResort())) {
                    // set propertyId
                    String propertyCode = "MV" + operaInfoResponse.getResort();
                    PropertyValue propertyValue = propertyConfig.getPropertyValuesMap().get(propertyCode);
                    // if propertyValue is not null means its an ACRS property
                    if (null != propertyValue) {
                        // set guid in request
                        propertyId = propertyValue.getGsePropertyIds().get(0);
                    }
                }
            }
        }

        SourceRoomReservationBasicInfoRequestDTO basicInfoRequest = SourceRoomReservationBasicInfoRequestDTO.builder()
                .confirmationNumber(request.getConfirmationNumber())
                .customerId(request.getCustomerId())
                .mlifeNumber(request.getMlifeNumber())
                .operaPartyCode(request.getOperaPartyCode())
                .source(request.getSource())
                .build();

        ReservationsBasicInfoResponse basicInfoResponse = null;
        
        // If property is not enabled for ACRS OR propertyId is null, go to GSE
        if (!isPropertyManagedByAcrs(propertyId) && !applicationProperties.isGseDisabled()) {
            try {
                basicInfoResponse = getRoomReservationsBasicInfoList(basicInfoRequest, gseStrategy);
            } catch (BusinessException businessException) {
                // if reservation not found, then continue on to search ACRS, throw all other exceptions
                if (ErrorCode.RESERVATION_NOT_FOUND != businessException.getErrorCode() || !isAcrsEnabled() ) {
                    throw businessException;
                }
            }
        }

        // If property is enabled for ACRS OR GSE didn't return reservation with ACRS enabled, fetch from ACRS
        if (isPropertyManagedByAcrs(propertyId) || (isAcrsEnabled() && null == basicInfoResponse)) {
            basicInfoResponse = getRoomReservationsBasicInfoList(basicInfoRequest, acrsStrategy);
        }

        return basicInfoResponse;
    }

    private ReservationsBasicInfoResponse getRoomReservationsBasicInfoList(
            SourceRoomReservationBasicInfoRequestDTO reservationRequest, FindReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("getRoomReservationsBasicInfoList", reservationRequest.getConfirmationNumber(), strategy));
        return strategy.getRoomReservationsBasicInfoList(reservationRequest);
    }

    private String createStrategyLogEntry(String method, String uniqueId, FindReservationDAOStrategy strategy) {
        String strategyString = (strategy instanceof FindReservationDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "FindReservationDAOImpl > "
                + method
                + " | Conf#: "
                + uniqueId
                + " | "
                + strategyString;
    }

	private String searchRoomConfirmationNumber(
    		FindReservationV2Request searchReservationRequest, FindReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("searchRoomReservationByOperaConfNumber", searchReservationRequest.getConfirmationNumber(), strategy));
        return strategy.searchRoomReservationByExternalConfirmationNo(searchReservationRequest);
    }

	private List<ReservationRoutingInstruction> setOperaRoutingInstructions(List<RoutingInstruction> routingInstructionList,
																			List<ReservationRoutingInstruction> acrsRoutingInstructionList,
																			String propertyId) {
		if (null == routingInstructionList || routingInstructionList.isEmpty()) {
			return acrsRoutingInstructionList;
		}
		return routingInstructionList.stream()
				.map(routingInstruction -> getReservationRoutingInstruction(acrsRoutingInstructionList, propertyId, routingInstruction))
				.collect(Collectors.toList());
    }

	private ReservationRoutingInstruction getReservationRoutingInstruction(List<ReservationRoutingInstruction> acrsRoutingInstructionList,
																		   String propertyId, RoutingInstruction routingInstruction) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		boolean hostRiPresent = false;
		ReservationRoutingInstruction reservationRoutingInstruction = new ReservationRoutingInstruction();
		reservationRoutingInstruction.setId(routingInstruction.getId());
		reservationRoutingInstruction.setName(routingInstruction.getName());
		reservationRoutingInstruction.setSource(routingInstruction.getSource());
		if (StringUtils.isNotEmpty(routingInstruction.getEndDate())) {
			reservationRoutingInstruction.setEndDate(DateUtil.toDate(LocalDate.parse(routingInstruction.getEndDate(), formatter)));
		}
		if (StringUtils.isNotEmpty(routingInstruction.getStartDate())) {
			reservationRoutingInstruction.setStartDate(DateUtil.toDate(LocalDate.parse(routingInstruction.getStartDate(), formatter)));
		}
		reservationRoutingInstruction.setAuthorizerId(refDataDAO.getRoutingAuthPhoenixId(routingInstruction.getAuthorizer(), propertyId));
		if (null != routingInstruction.getRoutingCodes() && routingInstruction.getRoutingCodes().length > 0) {
			reservationRoutingInstruction.setRoutingCodes(routingInstruction.getRoutingCodes());
		}
		if (null != routingInstruction.getHostRoutingCodes() && routingInstruction.getHostRoutingCodes().length > 0) {
			reservationRoutingInstruction.setHostRoutingCodes(routingInstruction.getHostRoutingCodes());
			reservationRoutingInstruction.setRoutingCodes(routingInstruction.getHostRoutingCodes());
			hostRiPresent = true;
		}
		reservationRoutingInstruction.setWindow(routingInstruction.getWindow());
		reservationRoutingInstruction.setLimitType(routingInstruction.getLimitType());
		reservationRoutingInstruction.setComments(routingInstruction.getComments());
		reservationRoutingInstruction.setMemberShipNumber(routingInstruction.getMemberShipNumber());
		if (StringUtils.isNotEmpty(routingInstruction.getLimit())) {
			reservationRoutingInstruction.setLimit(routingInstruction.getLimit());
		}
		reservationRoutingInstruction.setDailyYN(routingInstruction.isDailyYN());
		reservationRoutingInstruction.setIsSystemRouting(routingInstruction.getIsSystemRouting());
		reservationRoutingInstruction.setApplicableSunday(routingInstruction.isApplicableSunday());
		reservationRoutingInstruction.setApplicableMonday(routingInstruction.isApplicableMonday());
		reservationRoutingInstruction.setApplicableTuesday(routingInstruction.isApplicableTuesday());
		reservationRoutingInstruction.setApplicableWednesday(routingInstruction.isApplicableWednesday());
		reservationRoutingInstruction.setApplicableThursday(routingInstruction.isApplicableThursday());
		reservationRoutingInstruction.setApplicableFriday(routingInstruction.isApplicableFriday());
		reservationRoutingInstruction.setApplicableSaturday(routingInstruction.isApplicableSaturday());
		Optional<ReservationRoutingInstruction> acrsRI = getResvRoutingInstruction(acrsRoutingInstructionList,
				reservationRoutingInstruction, hostRiPresent);
		if (acrsRI.isPresent()) {
			ReservationRoutingInstruction acrsRoutingInstruction = acrsRI.get();
			if (acrsRoutingInstruction.getIsSystemRouting()) {
				reservationRoutingInstruction.setIsSystemRouting(acrsRoutingInstruction.getIsSystemRouting());
				reservationRoutingInstruction.setSource(acrsRoutingInstruction.getSource());
			}
		}
		return reservationRoutingInstruction;
	}

	/*
		Makes External Call
	 */
    private boolean isF1Package(RoomReservation roomReservation) {
        boolean f1Package = false;
        if (null != roomReservation) {
            if (StringUtils.isNotEmpty(roomReservation.getProgramId())) {
				f1Package = isF1Package(roomReservation, roomReservation.getProgramId());
            } else {
				f1Package = isF1Package(roomReservation, roomReservation.getBookings());
            }
        }
        return f1Package;
    }

	/*
		Makes External call
	 */
	private boolean isF1Package(RoomReservation roomReservation, List<RoomPrice> bookings) {
		if (CollectionUtils.isEmpty(bookings)) {
			return false;
		}

		List<String> distinctProgramIds = bookings.stream()
				.map(RoomPrice::getProgramId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		Optional<String> f1PackageProgramIdOptional = distinctProgramIds.stream()
				.filter(programId -> isF1Package(roomReservation, programId))
				.findAny();

		return f1PackageProgramIdOptional.isPresent();
	}

	/*
		Makes External call in roomProgramDao.validateProgramV2()
	 */
    private boolean isF1Package(RoomReservation roomReservation, String programId) {
        boolean f1Package = false;
        try {
        	RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
        			.getRoomProgramValidateRequest(roomReservation, programId);
        	serviceConversionHelper.convertGuids(validateRequest);

        	RoomProgramValidateResponse validateResponse = roomProgramDao.validateProgramV2(validateRequest);

			if (CollectionUtils.isNotEmpty(validateResponse.getRatePlanTags())
					&& validateResponse.getRatePlanTags().contains(appProperties.getF1PackageTag())) {
				Optional<String> productCode = appProperties.getValidF1ProductCodes().stream()
						.filter(x -> validateResponse.getRatePlanTags().contains(x))
						.findFirst();
        		if (productCode.isPresent()) {
        			f1Package = true;
        			roomReservation.setRatePlanTags(validateResponse.getRatePlanTags());
        			roomReservation.setProgramId(programId);
        		}
        	}
        } catch (Exception ex) {
			log.warn("Error occurred while validating program to set f1Package flag: ", ex);
        }
        return f1Package;
    }

    private void updatePropertyId(FindReservationV2Request modifiedRequest, OperaInfoResponse operaInfoResponse) {
        if (StringUtils.isNotEmpty(operaInfoResponse.getResort())) {
            // set propertyId
            String propertyCode = "MV" + operaInfoResponse.getResort();
            PropertyValue propertyValue = propertyConfig.getPropertyValuesMap().get(propertyCode);
            if (null != propertyValue) {
                // set guid in request
                modifiedRequest.setPropertyId(propertyValue.getGsePropertyIds().get(0));
            }
        }
    }

	private void updateFromOperaInfoResponse(FindReservationV2Request reservationRequest,
											 FindReservationV2Request modifiedRequest,
											 OcrsReservation ocrsReservation, OperaInfoResponse operaInfoResponse) {
		updatePropertyId(modifiedRequest, operaInfoResponse);
		if (StringUtils.isNotEmpty(operaInfoResponse.getOperaConfirmationNumber())) {
			modifiedRequest.setOperaConfNumber(operaInfoResponse.getOperaConfirmationNumber());
			//CBSR-2132: If no OCRS Response is returned or OCRS response does not have the MGMACRS tag with the ACRS confirmation number,
			// we will set the request confirmation number as the opera confirmation number from the Opera retrieve FA.
			if (null == ocrsReservation || StringUtils.isEmpty(modifiedRequest.getAcrsConfirmationNumber())) {
				reservationRequest.setConfirmationNumber(operaInfoResponse.getOperaConfirmationNumber());
				modifiedRequest.setConfirmationNumber((operaInfoResponse.getOperaConfirmationNumber()));
			}
		}
	}

	private Optional<ReservationRoutingInstruction> getResvRoutingInstruction(List<ReservationRoutingInstruction> acrsRoutingInstructionList,
																			  ReservationRoutingInstruction reservationRoutingInstruction,
																			  boolean hostRiPresent) {
		Optional<ReservationRoutingInstruction> acrsRI;
		if (hostRiPresent) {
			acrsRI = acrsRoutingInstructionList.stream()
					.filter(y -> null != y.getHostRoutingCodes() && null != reservationRoutingInstruction.getHostRoutingCodes() && StringUtils.equalsIgnoreCase(y.getHostRoutingCodes()[0], reservationRoutingInstruction.getHostRoutingCodes()[0])
							&& ((y.getWindow() == MLIFE_WINDOW_NUMBER) || StringUtils.equalsIgnoreCase(y.getAuthorizerId(), reservationRoutingInstruction.getAuthorizerId()))
							&& y.isDailyYN() == reservationRoutingInstruction.isDailyYN()
							&& y.getWindow() == reservationRoutingInstruction.getWindow()
							&& y.isApplicableSunday() == reservationRoutingInstruction.isApplicableSunday()
							&& y.isApplicableMonday() == reservationRoutingInstruction.isApplicableMonday()
							&& y.isApplicableTuesday() == reservationRoutingInstruction.isApplicableTuesday()
							&& y.isApplicableWednesday() == reservationRoutingInstruction.isApplicableWednesday()
							&& y.isApplicableThursday() == reservationRoutingInstruction.isApplicableThursday()
							&& y.isApplicableFriday() == reservationRoutingInstruction.isApplicableFriday()
							&& y.isApplicableSaturday() == reservationRoutingInstruction.isApplicableSaturday())
					.findFirst();
		} else {
			acrsRI = acrsRoutingInstructionList.stream()
					.filter(x -> null != x.getRoutingCodes() && null != reservationRoutingInstruction.getRoutingCodes() && StringUtils.equalsIgnoreCase(x.getRoutingCodes()[0], reservationRoutingInstruction.getRoutingCodes()[0])
							// Added for Mlife window check
							&& ((x.getWindow() == MLIFE_WINDOW_NUMBER) || StringUtils.equalsIgnoreCase(x.getAuthorizerId(), reservationRoutingInstruction.getAuthorizerId()))
							&& ((x.getWindow() == MLIFE_WINDOW_NUMBER) || StringUtils.equalsIgnoreCase(x.getLimit(), reservationRoutingInstruction.getLimit()))
							&& x.isDailyYN() == reservationRoutingInstruction.isDailyYN()
							&& x.getWindow() == reservationRoutingInstruction.getWindow()
							&& x.isApplicableSunday() == reservationRoutingInstruction.isApplicableSunday()
							&& x.isApplicableMonday() == reservationRoutingInstruction.isApplicableMonday()
							&& x.isApplicableTuesday() == reservationRoutingInstruction.isApplicableTuesday()
							&& x.isApplicableWednesday() == reservationRoutingInstruction.isApplicableWednesday()
							&& x.isApplicableThursday() == reservationRoutingInstruction.isApplicableThursday()
							&& x.isApplicableFriday() == reservationRoutingInstruction.isApplicableFriday()
							&& x.isApplicableSaturday() == reservationRoutingInstruction.isApplicableSaturday())
					.findFirst();
		}
		return acrsRI;
	}
}