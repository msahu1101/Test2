package com.mgm.services.booking.room.mapper;

import com.mgm.services.booking.room.model.crs.reservation.*;
import org.mapstruct.Mapper;

import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomReservationPendingResMapper {
    /**
     * Interface for convert HotelReservationPendingRes response to HotelReservationRes
     * @param reservation
     * @return
     */
    HotelReservationRes pendingResvToHotelReservationRes(HotelReservationPendingRes reservation);
    /**
     * Interface for convert ReservationPendingRes response to ReservationRetrieveRes
     * @param reservationPendingRes
     * @return
     */
    ReservationRetrieveResReservation reservationPendingResToReservationRetrieveRes(ReservationPendingRes reservationPendingRes);
    
    default SegmentRes pendingSegmentsToSegmentsRes(SegmentRes segmentRes) {
        return segmentRes;
    }
    default List<Comment> pendingCommentsToCommentsRes(List<Comment> comments) {
        return comments;
    }
    default IdentificationInfo pendingUserProfilesToUserProfilesRes(IdentificationInfo identificationInfo) {
        return identificationInfo;
    }

}
