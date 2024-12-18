package com.mgm.services.booking.room.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.ReservationDAO;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.service.RoomCartService;
import com.mgm.services.booking.room.service.RoomProgramService;

/**
 * Implementation class for PreReserveService exposing functionalities around
 * room reservation object preparation and resevation confirmation.
 *
 */
@Component
@Primary
public class RoomCartServiceImpl extends BasePriceServiceImpl implements RoomCartService {

    @Autowired
    private RoomProgramService programService;

    @Autowired
    private ReservationDAO reservationDao;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.PreReserveService#prereserveRoom(
     * com.mgm.services.booking.room.model.request.PreReserveRequest)
     */
    @Override
    public RoomReservation prepareRoomCartItem(RoomCartRequest request) {
        // Perform validation checks if program is available
        validateProgram(programService, request);

        return reservationDao.prepareRoomCartItem(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.PreReserveService#addRoomRequests(
     * com.mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation addRoomRequests(RoomReservation reservation) {

        return reservationDao.updateRoomReservation(reservation);
    }

    @Override
    public RoomReservation saveRoomCartItemInAurora(RoomReservation reservation) {
        return reservationDao.saveRoomReservation(reservation);
    }

}
