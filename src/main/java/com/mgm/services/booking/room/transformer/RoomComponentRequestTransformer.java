package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.util.DateUtil;

import lombok.experimental.UtilityClass;

import java.util.Date;

/**
 * Transformer class to create RoomComponentRequest from RoomReservationResponse
 * class.
 */
@UtilityClass
public class RoomComponentRequestTransformer {

    /**
     * Creates and returns RoomComponentRequest from RoomReservationResponse
     * class.
     * 
     * @param source
     *            Source header
     * @param response
     *            Room reservation response
     * @return Returns RoomComponentRequest
     */
    public static RoomComponentRequest getRoomComponentRequest(String source, RoomReservationResponse response) {
        final RoomComponentRequest componentRequest = new RoomComponentRequest();
        componentRequest.setSource(source);
        componentRequest.setPropertyId(response.getPropertyId());
        componentRequest.setRoomTypeId(response.getRoomTypeId());
        componentRequest.setTravelStartDate(response.getTripDetails().getCheckInDate());
        componentRequest.setTravelEndDate(response.getTripDetails().getCheckOutDate());
        return componentRequest;

    }

    /**
     * Creates and returns RoomComponentRequest from PreReserveRequest class.
     * 
     * @param source
     *            Source header
     * @param prereserveRequest
     *            Pre-reserve request
     * @return Returns RoomComponentRequest
     */
    public static RoomComponentRequest getRoomComponentRequest(String source, RoomCartRequest prereserveRequest) {
        final RoomComponentRequest componentRequest = new RoomComponentRequest();
        componentRequest.setSource(source);
        componentRequest.setPropertyId(prereserveRequest.getPropertyId());
        componentRequest.setRoomTypeId(prereserveRequest.getRoomTypeId());
        componentRequest.setTravelStartDate(DateUtil.toDate(prereserveRequest.getCheckInDate()));
        componentRequest.setTravelEndDate(DateUtil.toDate(prereserveRequest.getCheckOutDate()));
        return componentRequest;

    }

    /**
     * Creates and return RoomComponentRequest from RoomComponentV2Request object.
     * 
     * @param source
     *            source header
     * @param roomComponentV2Request
     *            room component v2 request
     * @return returns RoomComponentRequest
     */
    public static RoomComponentRequest getRoomComponentRequest(String source, RoomComponentV2Request roomComponentV2Request) {
        final RoomComponentRequest componentRequest = new RoomComponentRequest();
        componentRequest.setSource(source);
        componentRequest.setPropertyId(roomComponentV2Request.getPropertyId());
        componentRequest.setRoomTypeId(roomComponentV2Request.getRoomTypeId());
        componentRequest.setProgramId(roomComponentV2Request.getProgramId());
        componentRequest.setTravelStartDate(DateUtil.toDate(roomComponentV2Request.getCheckInDate()));
        componentRequest.setTravelEndDate(DateUtil.toDate(roomComponentV2Request.getCheckOutDate()));
        componentRequest.setMlifeNumber(roomComponentV2Request.getMlifeNumber());
        return componentRequest;
    }

    public static RoomComponentRequest getRoomComponentRequest(String propertyId, String roomTypeId,
                                                               String ratePlanId, Date checkInDate,
                                                               Date checkOutDate, String mlifeNumber,
                                                               String source) {
        final RoomComponentRequest componentRequest = new RoomComponentRequest();
        componentRequest.setSource(source);
        componentRequest.setPropertyId(propertyId);
        componentRequest.setRoomTypeId(roomTypeId);
        componentRequest.setProgramId(ratePlanId);
        componentRequest.setTravelStartDate(checkInDate);
        componentRequest.setTravelEndDate(checkOutDate);
        componentRequest.setMlifeNumber(mlifeNumber);
        return componentRequest;
    }
}
