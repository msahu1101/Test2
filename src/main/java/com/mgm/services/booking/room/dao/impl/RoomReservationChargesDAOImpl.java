package com.mgm.services.booking.room.dao.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.mgm.services.booking.room.dao.RoomReservationChargesDAO;
import com.mgm.services.booking.room.dao.RoomReservationChargesDAOStrategy;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

@Component
@Log4j2
public class RoomReservationChargesDAOImpl extends BaseStrategyDAO implements RoomReservationChargesDAO {

    @Autowired private RoomReservationChargesDAOStrategyGSEImpl gseStrategy;
    @Autowired private RoomReservationChargesDAOStrategyACRSImpl acrsStrategy;

    @Override public RoomReservation calculateRoomReservationCharges(RoomReservation roomReservation) {
        RoomReservationChargesDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(roomReservation.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return calculateRoomReservationCharges(roomReservation, strategy);
    }

    private RoomReservation calculateRoomReservationCharges(RoomReservation roomReservation, RoomReservationChargesDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("calculateRoomReservationCharges", roomReservation.getId(), strategy));
        return strategy.calculateRoomReservationCharges(roomReservation);
    }

    private String createStrategyLogEntry(String method, String uniqueId, RoomReservationChargesDAOStrategy strategy) {
        String strategyString = (strategy instanceof RoomReservationChargesDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "RoomReservationChargesDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }
}
