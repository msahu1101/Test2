package com.mgm.services.booking.room.dao.impl;

import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomReservationChargesDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.messages.RoomReservationBookingStage;
import com.mgmresorts.aurora.messages.UpdateRoomReservationRequest;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * DAO implementation class to provide methods for calculating room charges
 * 
 * @author swakulka
 *
 */
@Component
@Log4j2
public class RoomReservationChargesDAOStrategyGSEImpl extends AuroraBaseDAO implements RoomReservationChargesDAOStrategy {

    /**
     * Implementation method to calculate room charges for given request. The method
     * invokes UpdateRoomReservation api on GSE to return the updates charges, taxes
     * and fees
     * 
     * @param roomReservation - RoomReservation object
     * @return RoomReservation model
     */
    @Override
    @LogExecutionTime
    public RoomReservation calculateRoomReservationCharges(RoomReservation roomReservation) {

        UpdateRoomReservationRequest updateRoomReservationRequest = MessageFactory.createUpdateRoomReservationRequest();

        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(roomReservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        updateRoomReservationRequest.setReservation(auroraRoomReservation);
        updateRoomReservationRequest.setStage(RoomReservationBookingStage.Checkout);

        log.info("Sent the request to updateRoomReservation as : {}", updateRoomReservationRequest.toJsonString());

        try {
            final com.mgmresorts.aurora.messages.UpdateRoomReservationResponse reservationResponse = getAuroraClient(
                    roomReservation.getSource()).updateRoomReservation(updateRoomReservationRequest);
            log.info("Received the response from updateRoomReservation as : {}", reservationResponse.toJsonString());

            roomReservation = CommonUtil.copyProperties(reservationResponse.getReservation(), RoomReservation.class);
            populateChargesAndTaxesCalcWithSpecialRequests(roomReservation);

        } catch (EAuroraException ex) {
            log.error("Exception occured while calling updateRoomReservation API on GSE.." + ex);
            handleAuroraError(ex);
        }

        return roomReservation;

    }

}
