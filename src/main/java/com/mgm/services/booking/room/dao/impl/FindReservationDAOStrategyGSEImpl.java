package com.mgm.services.booking.room.dao.impl;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.FindReservationDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.dto.SourceRoomReservationBasicInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.messages.GetCustomerItineraryByRoomConfirmationNumberRequest;
import com.mgmresorts.aurora.messages.GetCustomerItineraryByRoomConfirmationNumberResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.messages.SourceRoomReservationBasicInfoRequest;
import com.mgmresorts.aurora.messages.SourceRoomReservationBasicInfoResponse;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for FindReservationDAO providing find reservation
 * functionalities.
 * 
 */
@Component
@Log4j2
public class FindReservationDAOStrategyGSEImpl extends AuroraBaseDAO implements FindReservationDAOStrategy {
    
    @Autowired
    private RoomProgramCacheService programCacheService;
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.FindReservationDAO#findRoomReservation(
     * com.mgm.services.booking.room.model.request.FindReservationRequest)
     */
    @Override
    public RoomReservation findRoomReservation(FindReservationRequest reservationRequest) {

        GetCustomerItineraryByRoomConfirmationNumberRequest request = MessageFactory
                .createGetCustomerItineraryByRoomConfirmationNumberRequest();

        final String confirmationNumber = reservationRequest.getConfirmationNumber();
        request.setConfirmationNumber(confirmationNumber);

        try {
            log.info("Sent the request to getCustomerItineraryByRoomConfirmationNumber as : {}",
                    request.toJsonString());
            final GetCustomerItineraryByRoomConfirmationNumberResponse response = getAuroraClient(
                    reservationRequest.getSource()).getCustomerItineraryByRoomConfirmationNumber(request);

            log.info("Received the response from getCustomerItineraryByRoomConfirmationNumber as : {}",
                    response.toJsonString());

            if (null == response.getItinerary() || null == response.getItinerary().getRoomReservations()) {
                log.error("No reservation returned from aurora for given confirmation number");
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }

            for (com.mgmresorts.aurora.common.RoomReservation auroraReservation : response.getItinerary()
                    .getRoomReservations()) {
                if (matchConfirmationNumber(confirmationNumber, auroraReservation)
                        && matchFirstName(reservationRequest.getFirstName(), auroraReservation)
                        && matchLastName(reservationRequest.getLastName(), auroraReservation)) {

                    return CommonUtil.copyProperties(auroraReservation, RoomReservation.class);
                }
            }

        } catch (EAuroraException ex) {
            log.error("Exception trying to lookup room reservation : ", ex);
            if (ex.getMessage().contains("BookingNotFound") || ex.getMessage().contains("MalformedBackendDTO")
            		|| ex.getMessage().contains("InvalidMlifeNumber")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, ex);
            }
        }

        log.info("First name or last name didn't match to return the reservation");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    private boolean matchFirstName(String firstName, com.mgmresorts.aurora.common.RoomReservation auroraReservation) {
        return firstName.equalsIgnoreCase(auroraReservation.getProfile().getFirstName());
    }

    private boolean matchLastName(String lastName, com.mgmresorts.aurora.common.RoomReservation auroraReservation) {
        return lastName.equalsIgnoreCase(auroraReservation.getProfile().getLastName());
    }

    private boolean matchConfirmationNumber(final String confirmationNumber,
            com.mgmresorts.aurora.common.RoomReservation auroraReservation) {
        return confirmationNumber.equalsIgnoreCase(auroraReservation.getConfirmationNumber())
                || confirmationNumber.equalsIgnoreCase(auroraReservation.getOperaConfirmationNumber())
                || confirmationNumber.equalsIgnoreCase(auroraReservation.getOTAConfirmationNumber());
    }

    @Override
    @LogExecutionTime
    public RoomReservation findRoomReservation(FindReservationV2Request reservationRequest) {
        GetCustomerItineraryByRoomConfirmationNumberRequest request = MessageFactory
                .createGetCustomerItineraryByRoomConfirmationNumberRequest();

        final String confirmationNumber = reservationRequest.getConfirmationNumber();
        // Use opera conf number for lookup if available and original conf number is not GSE conf number
        if (StringUtils.isNotBlank(reservationRequest.getOperaConfNumber()) && !isGseConfNumber(confirmationNumber)) {
            request.setConfirmationNumber(reservationRequest.getOperaConfNumber());
        } else {
            request.setConfirmationNumber(confirmationNumber.toUpperCase(Locale.ENGLISH));
        }
        
        request.setCacheOnly(reservationRequest.isCacheOnly());
        
        //CBSR-1412 set the property id for TCOLV if the lookup is for TCOLV.
        if(reservationRequest.isTcolvReservation()) {
        	request.setPropertyId(reservationRequest.getPropertyId());
        }

        try {
            log.info("Sent the request to getCustomerItineraryByRoomConfirmationNumber as : {}",
                    request.toJsonString());
            final GetCustomerItineraryByRoomConfirmationNumberResponse response = getAuroraClient(
                    reservationRequest.getSource()).getCustomerItineraryByRoomConfirmationNumber(request);

            log.info("Received the response from getCustomerItineraryByRoomConfirmationNumber as : {}",
                    response.toJsonString());

            if (null == response.getItinerary() || null == response.getItinerary().getRoomReservations()) {
                log.error("No reservation returned from aurora for given confirmation number");
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }

            for (com.mgmresorts.aurora.common.RoomReservation auroraReservation : response.getItinerary()
                    .getRoomReservations()) {
                if (matchConfirmationNumber(request.getConfirmationNumber(), auroraReservation)) {
                    log.info("Found a match for the confirmation number:: {}, in Aurora reservation id::{}",
                            confirmationNumber, auroraReservation.getId());
                    RoomReservation roomReservation = CommonUtil.copyProperties(auroraReservation,
                            RoomReservation.class);
                    long customerId = auroraReservation.getProfile().getId() == 0 ? response.getItinerary().getCustomerId() : auroraReservation.getProfile().getId();
                    roomReservation.setCustomerId(customerId);
                    replaceCCToken(roomReservation);
                    populateChargesAndTaxesCalcWithSpecialRequests(roomReservation);
                    roomReservation.setPerpetualPricing(roomReservation.getBookings().stream()
                            .anyMatch(b -> programCacheService.isProgramPO(b.getProgramId())));
                    return roomReservation;
                }
            }
        } catch (EAuroraException ex) {
            log.error("Exception trying to lookup room reservation : ", ex);
            if (ex.getMessage().contains(ServiceConstant.MIRAGE_OXI_DEACTIVATED_MESSAGE)) {
            	throw new BusinessException(ErrorCode.TRANSFERRED_MIRAGE_PROPERTY);
            }
            if (ex.getMessage().contains(ServiceConstant.GOLDSTRIKE_OXI_DEACTIVATED_MESSAGE)) {
            	throw new BusinessException(ErrorCode.TRANSFERRED_GOLDSTRIKE_PROPERTY);
            }
            if (ex.getMessage().contains("BookingNotFound")|| ex.getMessage().contains("MalformedBackendDTO")
            		|| ex.getMessage().contains("InvalidMlifeNumber")|| ex.getMessage().contains(ServiceConstant.OXI_ERROR_MESSAGE)
                    || ex.getMessage().contains(ServiceConstant.OXI_DEACTIVATED_MESSAGE)) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else if (ex.getMessage().contains("BlacklistReservation")) {
                throw new BusinessException(ErrorCode.RESERVATION_BLACKLISTED);
            } else {
                handleAuroraError(ex);
            }
        }

        log.info("First name or last name didn't match to return the reservation");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);

    }
    
    // Confirmation number is considered to be from GSE if it starts with 'M'
    private boolean isGseConfNumber(String confNumber) {
        return confNumber.startsWith("M") || confNumber.startsWith("m");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ReservationDAO#getRoomReservation(com.mgm.
     * services.booking.room.model.request.dto.
     * SourceRoomReservationBasicInfoRequestDTO)
     */
    @Override

    @LogExecutionTime
    public ReservationsBasicInfoResponse getRoomReservationsBasicInfoList(
            SourceRoomReservationBasicInfoRequestDTO requestDTO) {
        try {
            SourceRoomReservationBasicInfoRequest buildAuroraSourceRoomReservationRequest = RoomReservationTransformer
                    .buildAuroraSourceRoomReservationRequest(requestDTO);
            log.info("Sent the request to sourceRoomReservationBasicInfo as : {}",
                    buildAuroraSourceRoomReservationRequest.toJsonString());
            SourceRoomReservationBasicInfoResponse response = getAuroraClient(requestDTO.getSource())
                    .sourceRoomReservationBasicInfo(buildAuroraSourceRoomReservationRequest);
            log.info("Received the response from sourceRoomReservationBasicInfo as : {}", response.toJsonString());
            if (response.getRoomResvBasicInfos().length == 0) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }
            return RoomReservationTransformer.transform(response);
        } catch (EAuroraException ex) {
            log.error("Exception trying to lookup room reservation : ", ex);
            if (ex.getMessage().contains("BookingNotFound") || ex.getMessage().contains("<MalformedBackendDTO>[No reservation basic information")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else if (ex.getMessage().contains("BlacklistReservation")) {
                throw new BusinessException(ErrorCode.RESERVATION_BLACKLISTED);
            } else {
                handleAuroraError(ex);
            }
        }
        return null;
    }
    
    /* No action to be taken at GSE level for search API */
    @Override
    public String searchRoomReservationByExternalConfirmationNo(FindReservationV2Request searchReservationRequest) {
    	log.info("searchRoomReservationByExternalConfirmationNo - GSE search");
    	return null;
    }

}
