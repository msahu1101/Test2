package com.mgm.services.booking.room.mapper;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.ReservationUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Mapper interface for room reservation request object conversion to model.
 * @author swakulka
 *
 */
@Mapper(componentModel = "spring")

public abstract class CalculateRoomChargesRequestMapper {
    
    @Autowired
    private ApplicationProperties appProps;
    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    /**
     * Method to implement for conversion of room reservation request object to the model object.
     * 
     * @param request - RoomReservationChargesRequest
     * @return RoomReservation object
     */
    @Mapping(source = "request.tripDetails.checkInDate", target = "checkInDate", dateFormat = "mm/DD/yyyy")
    @Mapping(source = "request.tripDetails.checkOutDate", target = "checkOutDate", dateFormat = "mm/DD/yyyy")
    @Mapping(source = "request.tripDetails.numAdults", target = "numAdults")
    @Mapping(source = "request.tripDetails.numChildren", target = "numChildren")
    @Mapping(source = "request.tripDetails.numRooms", target = "numRooms")
    public abstract RoomReservation calculateRoomChargesRequestToModel(CalculateRoomChargesRequest request);

    /**
     * Handling for borgata taxes. Add special requests which are required to
     * get additional tax elements
     * 
     * @param reservation
     *            - Room reservation
     * @param request
     *            - Charges request
     */
    @AfterMapping
    public void updateSpecialRequests(@MappingTarget RoomReservation reservation, CalculateRoomChargesRequest request) {

        if (null != reservation.getSpecialRequests() && !referenceDataDAOHelper.isPropertyManagedByAcrs(reservation.getPropertyId())) {
            ReservationUtil.addSpecialRequests(reservation, appProps);
        }
    }
}
