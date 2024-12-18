package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RefundRoomReservationResponse;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;

/**
 * Service class to expose pre-modify and modify room reservations.
 */
public interface ModifyReservationService {

    /**
     * Pre-modify room reservation to get updated prices and totals.
     * 
     * @param preModifyRequest Pre-modify request
     * @return Room Reservation object
     */
    RoomReservation preModifyReservation(PreModifyRequest preModifyRequest);

    /**
     * Confirm modification of room reservation.
     * 
     * @param source      Source or channel
     * @param reservation In-flight room reservation
     * @return Room reservation object
     */
    RoomReservation modifyReservation(String source, RoomReservation reservation);

    /**
     * Updates the profile information in the room reservation with given profile
     * information and returns the updated room reservation V2 response.
     * 
     * @param request request object.
     * @param token JWT token string
     * @return UpdateProfileInfoResponse response.
     */
    UpdateProfileInfoResponse updateProfileInfo(UpdateProfileInfoRequest request, String token);

    /**
     * Modify room reservation with source system and return updated room
     * reservation object for v2 services.
     * 
     * @param modifyRoomReservationRequest Modify Room Reservation Request object
     * @return modify room reservation object with confirmation number
     */
    ModifyRoomReservationResponse modifyRoomReservationV2(ModifyRoomReservationRequest modifyRoomReservationRequest);

    ModifyRoomReservationResponse reservationModifyPendingV4(ModifyRoomReservationRequest modifyRoomReservationRequest);
    ModifyRoomReservationResponse reservationModifyPendingV5(PreviewCommitRequest commitRequest, String token);

    /**
     * Preview the reservation for modify room reservation flow.
     * 
     * @param preModifyV2Request modify request.
     * @param token token string.
     * @return ModifyRoomReservationResponse response.
     */
    ModifyRoomReservationResponse preModifyReservation(PreModifyV2Request preModifyV2Request, String token);
    
    /**
     * Commits the previewed room reservation changes in modify flow.
     * 
     * @param commitRequest Commit request
     * @param token JWT token
     * @return Returns modified room reservation response
     */
    ModifyRoomReservationResponse commitReservation(PreviewCommitRequest commitRequest, String token);

    /**
     * Associate transient reservation to a customer and return the updated room
     * reservation V2 response.
     * 
     * @param request request object.
     * @param token JWT token
     * @return UpdateProfileInfoResponse response.
     */
    UpdateProfileInfoResponse associateReservation(ReservationAssociateRequest request, String token);
    /**
     * Refund commit reservation to a customer and return the updated room
     * reservation V2 response.
     *
     * @param request request object.
     * @param token JWT token
     * @return RefundRoomReservationResponse response.
     */
    ModifyRoomReservationResponse commitPaymentReservation(PaymentRoomReservationRequest request);

}
