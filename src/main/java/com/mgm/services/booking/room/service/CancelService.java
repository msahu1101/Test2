package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;

/**
 * Service interface exposing service to cancel room reservations.
 */
public interface CancelService {

    /**
     * Cancels existing room reservation and returns updated room reservation
     * object.
     * 
     * @param cancelRequest Cancel request
     * @return Room reservation object
     */
    RoomReservation cancelReservation(CancelRequest cancelRequest);

    /**
     * Cancels existing room reservation and returns v2 version of room reservation
     * object.
     * 
     * @param cancelRequest Cancel request
     * @param findItineraryId find itinerary id
     * @return Room reservation object
     */
    CancelRoomReservationV2Response cancelReservation(CancelV2Request cancelRequest, boolean findItineraryId);

    /**
     * Ignore a priced room reservation and returns v2 version of room reservation
     * object.
     *
     * @param cancelRequest Cancel request
     * @return HttpServletResponse
     */
    boolean ignoreReservation(ReleaseV2Request cancelRequest);

    /**
     * Cancels existing room reservation and returns v2 version of room reservation
     * object.
     * 
     * @param cancelRequest Cancel request
     * @param token token string
     * @return Room reservation object
     */
    CancelRoomReservationV2Response cancelReservation(CancelV3Request cancelRequest, String token);

    CancelRoomReservationV2Response cancelPreviewReservation(CancelV4Request cancelRequest, String token);
    CancelRoomReservationV2Response cancelCommitReservation(CancelV4Request cancelRequest, String token);

    /**
     * Ignore a priced room reservation and returns v2 version of room reservation
     * object.
     *
     * @return HttpServletResponse
     */
    boolean ignoreReservationV3(ReleaseV3Request releaseRequest);

    /**
     * Cancels existing F1 inventory and returns v2 version of room reservation
     * object.
     *
     * @param cancelRequest Cancel request
     * @param token token string
     * @return Room reservation object
     */
    CancelRoomReservationV2Response cancelReservationF1(CancelV3Request cancelRequest, String token);
}
