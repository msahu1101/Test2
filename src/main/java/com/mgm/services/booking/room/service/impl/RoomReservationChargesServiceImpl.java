package com.mgm.services.booking.room.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mgm.services.booking.room.dao.RoomReservationChargesDAO;
import com.mgm.services.booking.room.mapper.RoomReservationChargesRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationChargesResponseMapper;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;
import com.mgm.services.booking.room.service.RoomReservationChargesService;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class to provide method for serving room reservation charges request.
 * @author swakulka
 *
 */
@Service
@Log4j2
public class RoomReservationChargesServiceImpl implements RoomReservationChargesService {

    @Autowired
    private RoomReservationChargesDAO roomReservationChargesDAO;

    @Autowired
    private RoomReservationChargesRequestMapper reservationChargesRequestMapper;

    @Autowired
    private RoomReservationChargesResponseMapper reservationChargesResponseMapper;

    @Autowired
    private CommonServiceImpl commonServiceImpl;
    /**
     * Method converts the request object into model, invokes DAO layer 
     * and returns the response object consisting of room reservation
     * charges information.
     * 
     * @param roomReservationChargesRequest - request object
     * @return RoomReservationChargesResponse object
     */
    @Override
    public RoomReservationChargesResponse calculateRoomReservationCharges(
            RoomReservationChargesRequest roomReservationChargesRequest) {

        // if specific program is not applied, find the dominant program
        if (StringUtils.isEmpty(roomReservationChargesRequest.getProgramId())
                && null != roomReservationChargesRequest.getBookings()) {
            String dominantProgramId = findDominantProgram(roomReservationChargesRequest.getBookings());
            log.info("dominantProgramId {}", dominantProgramId);
            roomReservationChargesRequest.setProgramId(dominantProgramId);
        }

		RoomReservation roomReservationRequest = reservationChargesRequestMapper
				.roomReservationChargesRequestToModel(roomReservationChargesRequest);
		
		if (null != roomReservationChargesRequest.getMlifeNumber() && (null != roomReservationRequest.getProfile())) {
			roomReservationRequest.getProfile()
					.setMlifeNo(Integer.parseInt(roomReservationChargesRequest.getMlifeNumber()));
		}
        //Check if booking limit is applied
        commonServiceImpl.checkBookingLimitApplied(roomReservationRequest);
		RoomReservation roomReservation = roomReservationChargesDAO
				.calculateRoomReservationCharges(roomReservationRequest);

        return reservationChargesResponseMapper.reservationModelToRoomReservationChargesResponse(roomReservation);

    }

    /**
     * Filter programs with rate table as true and summarize program id by count,
     * fetch the max entry and map to value.
     * 
     * @param bookings
     *            list of RoomPrice objects
     * @return dominant programId
     */
    private String findDominantProgram(List<RoomPrice> bookings) {

        return bookings.stream()
                // filter out programs with rate table as true
                .filter(r -> !r.isProgramIdIsRateTable() && StringUtils.isNotEmpty(r.getProgramId()))
                // summarize program id by count
                .collect(Collectors.groupingBy(RoomPrice::getProgramId, Collectors.counting()))
                // fetch the max entry
                .entrySet().stream().max(Map.Entry.comparingByValue())
                // map to value
                .map(Map.Entry::getKey).orElse(null);
    }

}
