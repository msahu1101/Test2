package com.mgm.services.booking.room.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomAndComponentChargesDAO;
import com.mgm.services.booking.room.dao.RoomAndComponentChargesDAOStrategy;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class RoomAndComponentChargesDAOImpl extends BaseStrategyDAO implements RoomAndComponentChargesDAO {

    @Autowired
    private RoomAndComponentChargesDAOStrategyACRSImpl acrsStrategy;
    @Autowired
    private RoomAndComponentChargesDAOStrategyGSEImpl gseStrategy;

    @Override
    public RoomReservation calculateRoomAndComponentCharges(RoomReservation roomReservation) {
        RoomAndComponentChargesDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(roomReservation.getPropertyId())) {
            strategy = acrsStrategy;
        }
        return calculateRoomChargesWithComponent(roomReservation, strategy);
    }



    private RoomReservation calculateRoomChargesWithComponent(RoomReservation roomReservation,
            RoomAndComponentChargesDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("calculateRoomReservationCharges", roomReservation.getId(), strategy));
        
        return strategy.calculateRoomAndComponentCharges(roomReservation);
    }
    private String createStrategyLogEntry(String method, String uniqueId, RoomAndComponentChargesDAOStrategy strategy) {
        String strategyString = (strategy instanceof RoomAndComponentChargesDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "RoomReservationChargesDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }
}
