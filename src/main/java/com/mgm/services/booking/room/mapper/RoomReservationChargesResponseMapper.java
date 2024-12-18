package com.mgm.services.booking.room.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.ReservationUtil;

/**
 * Mapper interface to convert Room reservation model to room reservation
 * charges response
 * 
 * @author swakulka
 *
 */
@Mapper(componentModel = "spring")
public abstract class RoomReservationChargesResponseMapper {
    
    @Autowired
    private ApplicationProperties appProps;

    /**
     * Method to implement for converting room reservation model to room reservation
     * charges response.
     * 
     * @param reservation - RoomReservation object
     * @return RoomReservationChargesResponse object
     */
    @Mapping(source = "checkInDate", target = "tripDetails.checkInDate", dateFormat = "mm/DD/yyyy")
    @Mapping(source = "checkOutDate", target = "tripDetails.checkOutDate", dateFormat = "mm/DD/yyyy")
    @Mapping(source = "numAdults", target = "tripDetails.numAdults")
    @Mapping(source = "numChildren", target = "tripDetails.numChildren")
    @Mapping(source = "numRooms", target = "tripDetails.numRooms")
    @Mapping(source = "chargesAndTaxesCalc", target = "chargesAndTaxes")
    @Mapping(source = "depositCalc", target = "depositDetails")
    @Mapping(source = "depositPolicyCalc", target = "depositPolicy")
    public abstract RoomReservationChargesResponse reservationModelToRoomReservationChargesResponse(RoomReservation reservation);
    
    /**
     * Handling for borgata taxes. Remove special requests which were added to
     * get additional tax elements. This will prevent client applications from
     * displaying internal components used for deriving additional taxes
     * 
     * @param reservation
     *            - Room reservation object
     */
    @BeforeMapping
    public void updateSpecialRequests(RoomReservation reservation) {
        if (null != reservation && null != reservation.getSpecialRequests()) {
            ReservationUtil.removeSpecialRequests(reservation, appProps);
        }
    }

    @AfterMapping
    public void populateRatesSummary(@MappingTarget RoomReservationChargesResponse roomReservationChargesResponse,
            RoomReservation reservation) {

        roomReservationChargesResponse.setRatesSummary(ReservationUtil.getRateSummary(reservation));

    }

}
