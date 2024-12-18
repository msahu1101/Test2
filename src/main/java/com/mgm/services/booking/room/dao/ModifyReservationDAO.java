package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * DAO interface to expose services for modification related functionalities for
 * room reservations.
 *
 */
public interface ModifyReservationDAO {

    /**
     * Service to update attributes on the room reservation to find updated room
     * charges and taxes.
     * 
     * @param preModifyRequest
     *            Pre-modify request
     * @return Update room reservation
     */
    RoomReservation preModifyReservation(PreModifyRequest preModifyRequest);

    /**
     * Service to confirm the modifications made on room reservation.
     * 
     * @param source
     *            Source channel making the request
     * @param reservation
     *            Room reservation object
     * @return Confirmed room reservation object
     */
    RoomReservation modifyReservation(String source, RoomReservation reservation);
    
    /**
     * Updates the room reservation with given profile in the request and responds
     * with updated reservation.
     * 
     * @param requestDTO request dto
     * @return RoomReservation reservation object
     */
    RoomReservation updateProfileInfo(UpdateProfileInfoRequestDTO requestDTO);
    
    /**
     * Modify room reservation with the given roomReservation object for v2.
     * 
     * @param reservation
     *            Room reservation object
     * @return Confirmed room reservation object
     */
    RoomReservation modifyRoomReservationV2(RoomReservation reservation);

    /**
     * Preview room reservation for a given confirmation number.
     * 
     * @param preModifyRequest preview request.
     * @return RoomReservation roomreservation object.
     */
    RoomReservation preModifyReservation(PreModifyV2Request preModifyRequest);

    /**
     * Refund room reservation for a given confirmation number.
     *
     * @param RefundRoomReservationRequest refund request.
     * @return RoomReservation roomreservation object.
     */
    RoomReservation commitPaymentReservation(CommitPaymentDTO existingReservation);
    RoomReservation modifyPendingRoomReservationV2(RoomReservation reservation);

}
