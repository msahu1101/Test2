package com.mgm.services.booking.room.dao.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.ReservationDAO;
import com.mgm.services.booking.room.dao.ReservationDAOStrategy;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.PartyRoomReservation;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

@Component
@Log4j2
public class ReservationDAOImpl extends BaseStrategyDAO implements ReservationDAO {
    @Autowired ReservationDAOStrategyACRSImpl acrsStrategy;
    @Autowired ReservationDAOStrategyGSEImpl gseStrategy;

    @Override public RoomReservation prepareRoomCartItem(RoomCartRequest request) {
        ReservationDAOStrategy strategy = gseStrategy;
        if( isPropertyManagedByAcrs(request.getPropertyId()) ){
            strategy = acrsStrategy;
        }
        return prepareRoomCartItem(request, strategy);
    }

    private RoomReservation prepareRoomCartItem(RoomCartRequest request, ReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("prepareRoomCartItem", "ItinIds:[" + String.join(",", request.getAuroraItineraryIds()) + "]", strategy));
        return strategy.prepareRoomCartItem(request);
    }

    @Override public RoomReservation updateRoomReservation(RoomReservation reservation) {
        ReservationDAOStrategy strategy = gseStrategy;
        if( isPropertyManagedByAcrs(reservation.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return updateRoomReservation(reservation, strategy);
    }

    private RoomReservation updateRoomReservation(RoomReservation reservation, ReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("updateRoomReservation", reservation.getId(), strategy));
        return strategy.updateRoomReservation(reservation);
    }

    @Override public RoomReservation makeRoomReservation(RoomReservation reservation) {
        ReservationDAOStrategy strategy = gseStrategy;
        if( isPropertyManagedByAcrs(reservation.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return makeRoomReservation(reservation, strategy);
    }

    private RoomReservation makeRoomReservation(RoomReservation reservation, ReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("makeRoomReservation", reservation.getId(), strategy));
        return strategy.makeRoomReservation(reservation);
    }

    @Override public RoomReservation saveRoomReservation(RoomReservation reservation) {
        ReservationDAOStrategy strategy = gseStrategy;
        if( isPropertyManagedByAcrs(reservation.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return saveRoomReservation(reservation, strategy);
    }

    private RoomReservation saveRoomReservation(RoomReservation reservation, ReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("saveRoomReservation", reservation.getId(), strategy));
        return strategy.saveRoomReservation(reservation);
    }

    @Override
    public RoomReservation makeRoomReservationV2(RoomReservation reservation) {
        ReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(reservation.getPropertyId())) {
            strategy = acrsStrategy;
        }
        return makeRoomReservationV2(reservation, strategy);
    }

    @Override
    public RoomReservation makeRoomReservationV4(RoomReservation reservation) {
        return acrsStrategy.makeRoomReservationV2(reservation);
    }

    private RoomReservation makeRoomReservationV2(RoomReservation reservation, ReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("makeRoomReservationV2", reservation.getId(), strategy));
        return strategy.makeRoomReservationV2(reservation);
    }
    
    @Override
    public PartyRoomReservation makePartyRoomReservation(RoomReservation reservation, boolean splitCreditCardDetails) {
        ReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(reservation.getPropertyId())) {
            strategy = acrsStrategy;
        }
        return makePartyRoomReservation(reservation, splitCreditCardDetails, strategy);
    }

    @Override
    public PartyRoomReservation makePartyRoomReservationV4(RoomReservation reservation, boolean splitCreditCardDetails) {
        return acrsStrategy.makePartyRoomReservationV4(reservation, splitCreditCardDetails);
    }

    private PartyRoomReservation makePartyRoomReservation(RoomReservation reservation, boolean splitCreditCardDetails, ReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("makePartyRoomReservation", reservation.getId(), strategy));
        return strategy.makePartyRoomReservation(reservation, splitCreditCardDetails);
    }

    private String createStrategyLogEntry(String method, String uniqueId, ReservationDAOStrategy strategy) {
        String strategyString = (strategy instanceof ReservationDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "ReservationDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }
}