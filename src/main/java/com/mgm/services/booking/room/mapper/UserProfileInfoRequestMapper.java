package com.mgm.services.booking.room.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * Abstract class to room reservation model to user profile info request.
 * 
 * @author laknaray
 *
 */
@Mapper (componentModel = "spring")
public abstract class UserProfileInfoRequestMapper {

    /**
     * Interface method to be implement the transformation of room reservation object to update profile info request.
     * 
     * @param reservation room reservation object.
     * @return UpdateProfileInfoRequest response.
     */
    @Mapping(source = "profile", target = "userProfile")
    @Mapping(source = "id", target = "reservationId")
    @Mapping(source = "propertyId", target = "propertyId")
    public abstract UpdateProfileInfoRequest roomReservationModelToUpdateProfileInfoRequest(
            RoomReservation reservation);

}
