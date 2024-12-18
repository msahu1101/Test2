package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.exception.AuroraError;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.CancelReservationDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgmresorts.aurora.messages.CancelRoomReservationRequest;
import com.mgmresorts.aurora.messages.CancelRoomReservationResponse;
import com.mgmresorts.aurora.messages.GetCustomerItineraryByRoomConfirmationNumberRequest;
import com.mgmresorts.aurora.messages.GetCustomerItineraryByRoomConfirmationNumberResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.service.EAuroraException;
import io.micrometer.core.instrument.util.StringUtils;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for CancelReservationDAO for all services related to
 * room reservation cancellation.
 */
@Component
@Log4j2
public class CancelReservationDAOStrategyGSEImpl extends AuroraBaseDAO implements CancelReservationDAOStrategy {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.mgm.services.booking.room.dao.CancelReservationDAO#cancelReservation(
     * com.mgm.services.booking.room.model.request.CancelRequest)
     */
    @Override
    public RoomReservation cancelReservation(CancelRequest cancelRequest, String propertyId) {

        // If the itineraryId and reservationId is available from request, then
        // don't call the service to fetch the customer itinerary again
        if (StringUtils.isNotEmpty(cancelRequest.getItineraryId())
                && StringUtils.isNotEmpty(cancelRequest.getReservationId())) {
            return confirmCancel(cancelRequest);

        }

        GetCustomerItineraryByRoomConfirmationNumberRequest request = MessageFactory
                .createGetCustomerItineraryByRoomConfirmationNumberRequest();

        final String confirmationNumber = cancelRequest.getConfirmationNumber();
        request.setConfirmationNumber(confirmationNumber);

        // First fetch the reservation which should be canceled.
        // Unfortunately, this is needed as cancel API needs the reservation id
        // instead of confirmation number
        try {
            log.info("Sent the request to getCustomerItineraryByRoomConfirmationNumber as : {}",
                    request.toJsonString());
            final GetCustomerItineraryByRoomConfirmationNumberResponse response = getAuroraClient(
                    cancelRequest.getSource()).getCustomerItineraryByRoomConfirmationNumber(request);

            log.info("Received the response from getCustomerItineraryByRoomConfirmationNumber as : {}",
                    response.toJsonString());

            if (null == response.getItinerary() || null == response.getItinerary().getRoomReservations()) {
                log.error("No reservation returned from aurora for given confirmation number");
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }

            for (com.mgmresorts.aurora.common.RoomReservation auroraReservation : response.getItinerary()
                    .getRoomReservations()) {
                // Revalidate to ensure that confirmation number, first name
                // and last name matches
                if (isValidConfirmationNumber(cancelRequest.getConfirmationNumber(), auroraReservation)
                        && isValidName(cancelRequest, auroraReservation)) {
                    cancelRequest.setItineraryId(response.getItinerary().getId());
                    cancelRequest.setReservationId(auroraReservation.getId());
                    cancelRequest.setCustomerId(response.getItinerary().getCustomerId());

                    // if validations are fine, proceed to confirm the
                    // cancellation
                    return confirmCancel(cancelRequest);
                }
            }

        } catch (EAuroraException ex) {
            log.error("Exception trying to lookup room reservation : ", ex);
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        // If we are at this point, reservation is not found
        log.info("First name or last name didn't match to return the reservation");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    private boolean isValidConfirmationNumber(String confirmationNumber,
                                              com.mgmresorts.aurora.common.RoomReservation auroraReservation) {

        return (confirmationNumber.equals(auroraReservation.getConfirmationNumber())
                || confirmationNumber.equals(auroraReservation.getOperaConfirmationNumber())
                || confirmationNumber.equals(auroraReservation.getOTAConfirmationNumber()));
    }

    private boolean isValidName(CancelRequest cancelRequest,
                                com.mgmresorts.aurora.common.RoomReservation auroraReservation) {

        return (cancelRequest.getFirstName().equalsIgnoreCase(auroraReservation.getProfile().getFirstName())
                && cancelRequest.getLastName().equalsIgnoreCase(auroraReservation.getProfile().getLastName()));
    }

    /**
     * Integrates with GSE to confirm the cancellation of the specified room
     * reservation.
     *
     * @param cancelRequest the cancel request object
     * @return Cancelled room reservation object
     */

    @LogExecutionTime
    private RoomReservation confirmCancel(CancelRequest cancelRequest) {

        CancelRoomReservationRequest request = MessageFactory.createCancelRoomReservationRequest();
        request.setCustomerId(cancelRequest.getCustomerId());

        request.setItineraryId(cancelRequest.getItineraryId());
        request.setReservationId(cancelRequest.getReservationId());
        request.setCancellationReason(null);
        request.setOverrideDepositForfeit(false);

        log.info("Sent the request to cancelRoomReservation as : {}", request.toJsonString());

        final CancelRoomReservationResponse response = getAuroraClient(cancelRequest.getSource())
                .cancelRoomReservation(request);

        log.info("Received the response from cancelRoomReservation as : {}", response.toJsonString());

        if (null != response.getItinerary() && null != response.getItinerary().getRoomReservations()) {

            for (com.mgmresorts.aurora.common.RoomReservation reservation : response.getItinerary()
                    .getRoomReservations()) {
                // take the reservation matching with the input reservation
                // id
                if (cancelRequest.getReservationId().equals(reservation.getId())) {
                    return CommonUtil.copyProperties(reservation, RoomReservation.class);

                }
            }
        }

        log.info("Reservation not found after cancellation");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.mgm.services.booking.room.dao.CancelReservationDAO#cancelReservation(
     * com.mgm.services.booking.room.model.request.CancelRequest)
     */
    @Override
    @LogExecutionTime
    public RoomReservation cancelReservation(CancelV2Request cancelRequest) {

        // If the reservationId is available from the request, then
        // don't call the service to fetch the customer itinerary again
        if (StringUtils.isNotEmpty(cancelRequest.getReservationId())) {
            return confirmCancel(cancelRequest);
        }

        GetCustomerItineraryByRoomConfirmationNumberRequest request = MessageFactory
                .createGetCustomerItineraryByRoomConfirmationNumberRequest();

        final String confirmationNumber = cancelRequest.getConfirmationNumber();
        request.setConfirmationNumber(confirmationNumber);
        request.setCacheOnly(true);

        // First fetch the reservation which should be canceled.
        // Unfortunately, this is needed as cancel API needs the reservation id
        // instead of confirmation number
        try {
            log.info("Sent the request to getCustomerItineraryByRoomConfirmationNumber as : {}",
                    request.toJsonString());
            final GetCustomerItineraryByRoomConfirmationNumberResponse response = getAuroraClient(
                    cancelRequest.getSource()).getCustomerItineraryByRoomConfirmationNumber(request);

            log.info("Received the response from getCustomerItineraryByRoomConfirmationNumber as : {}",
                    response.toJsonString());
            if (null == response.getItinerary() || null == response.getItinerary().getRoomReservations()) {
                log.error("No reservation returned from aurora for given confirmation number");
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }

            for (com.mgmresorts.aurora.common.RoomReservation auroraReservation : response.getItinerary()
                    .getRoomReservations()) {
                // Re-validate to ensure that confirmation number
                if (isValidConfirmationNumber(cancelRequest.getConfirmationNumber(), auroraReservation)) {
                    cancelRequest.setItineraryId(response.getItinerary().getId());
                    cancelRequest.setReservationId(auroraReservation.getId());
                    cancelRequest.setCustomerId(response.getItinerary().getCustomerId());

                    // if validations are fine, proceed to confirm the
                    // cancellation
                    return confirmCancel(cancelRequest);
                }
            }
        } catch (EAuroraException ex) {
            log.error("Exception trying to lookup room reservation for cancel reservation flow : ", ex);
            if ("BookingNotFound".equals(ex.getErrorCode().name())) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                handleAuroraError(ex);
            }
        }
        log.info("Throwing exception in case of reservation not found");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Override
    @LogExecutionTime
    public boolean ignoreReservation(ReleaseV2Request cancelRequest)
    {
        return true;
    }

    /**
     * Integrates with GSE to confirm the cancellation of the specified room
     * reservation for V2 API.
     *
     * @param cancelRequest the cancel v2 request object
     * @return Cancelled room reservation object
     */
    private RoomReservation confirmCancel(CancelV2Request cancelRequest) {

        CancelRoomReservationRequest request = MessageFactory.createCancelRoomReservationRequest();
        request.setCustomerId(cancelRequest.getCustomerId());

        request.setItineraryId(cancelRequest.getItineraryId());
        request.setReservationId(cancelRequest.getReservationId());
        if (org.apache.commons.lang.StringUtils.equalsIgnoreCase(ServiceConstant.ICE, cancelRequest.getSource())) {
            request.setCancellationReason(cancelRequest.getCancellationReason());
        }
        else {
            request.setCancellationReason(ServiceConstant.WEB_CANCELLATION_REASON);
        }
        request.setOverrideDepositForfeit(cancelRequest.isOverrideDepositForfeit());
        CancelRoomReservationResponse response = null;
        try {
            log.info("Sent the request to cancelRoomReservation as : {}", request.toJsonString());
            response = getAuroraClient(cancelRequest.getSource()).cancelRoomReservation(request);
            log.info("Received the response from cancelRoomReservation as : {}", response.toJsonString());
        } catch (EAuroraException ex) {
            log.error("Exception trying to cancel room reservation : ", ex);
            if ("InvalidReservationState".equals(ex.getErrorCode().name())) {
                throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
            } else if ("InvalidItineraryId".equals(ex.getErrorCode().name())) {
                throw new BusinessException(ErrorCode.INVALID_COMBINATION_RESERVATION_INPUT_PARAMS);
            } else if (AuroraError.PAYMENTAUTHORIZATIONFAILED.name().equalsIgnoreCase(ex.getErrorCode().name())) {
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED, ex.getMessage());
            } else {
                handleAuroraError(ex);
            }
        }

        if (null != response.getItinerary() && null != response.getItinerary().getRoomReservations()) {
            for (com.mgmresorts.aurora.common.RoomReservation reservation : response.getItinerary()
                    .getRoomReservations()) {
                // take the reservation matching with the input reservation
                // id
                if (cancelRequest.getReservationId().equals(reservation.getId())) {
                    RoomReservation cancelReservationResponse = CommonUtil.copyProperties(reservation,
                            RoomReservation.class);
                    cancelReservationResponse.setCustomerId(response.getItinerary().getCustomerId());
                    return cancelReservationResponse;

                }
            }
        }
        log.info("Reservation not found after cancellation");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

}