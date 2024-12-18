package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.transformer.RoomComponentRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.controller.BaseController;

/**
 * Controller to lookup room reservations based on confirmation number.
 * will remove Nullable
 *
 */
@RestController
@RequestMapping("/v1/reserve")
public class FindReservationController extends BaseController {
	
	@Nullable 
    @Autowired
    private FindReservationService findReservationService;
	@Nullable
    @Autowired
    private ComponentService componentService;
    
    @Autowired
    private ApplicationProperties appProperties;

    /**
     * Lookup service to find reservation by confirmation number, first name and
     * last name.
     * 
     * @param source
     *            Source header
     * @param reservationRequest
     *            Find reservation request
     * @param result
     *            Binding result
     * @return Returns the reservation info found
     */
    @GetMapping("/room")
    public RoomReservationResponse reserveRoom(@RequestHeader String source,
            @Valid FindReservationRequest reservationRequest, BindingResult result) {

        preprocess(source, reservationRequest, result);

        RoomReservation reservation = findReservationService.findRoomReservation(reservationRequest);

        final RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);

        // Create room component request
        final RoomComponentRequest componentRequest = RoomComponentRequestTransformer.getRoomComponentRequest(source,
                response);

        // Get available component requests and mark selected ones
        final List<RoomRequest> roomRequests = getRoomRequests(reservation, componentRequest);

        response.setRoomRequests(roomRequests);

        return response;

    }

    private List<RoomRequest> getRoomRequests(RoomReservation reservation, RoomComponentRequest componentRequest) {
        // Special handling for borgata
        ReservationUtil.removeSpecialRequests(reservation, appProperties);
        
        final List<String> componentIds = reservation.getSpecialRequests();
        final List<RoomRequest> roomRequests = new ArrayList<>();
        componentService.getAvailableRoomComponents(componentRequest).forEach(roomRequest -> {
            if (componentIds.contains(roomRequest.getId())) {
                roomRequest.setSelected(true);
            }
            roomRequests.add(roomRequest);
        });
        return roomRequests;
    }

}
