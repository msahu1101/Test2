package com.mgm.services.booking.room.mapper;

import java.util.DoubleSummaryStatistics;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CalculateRoomChargesResponse;
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
public abstract class CalculateRoomChargesResponseMapper {
    
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
    @Mapping(source = "itineraryId", target = "shoppedItineraryId")
    @Mapping(source = "chargesAndTaxesCalc", target = "chargesAndTaxes")
    @Mapping(source = "depositCalc", target = "depositDetails")
    @Mapping(source = "depositPolicyCalc", target = "depositPolicy")
    public abstract CalculateRoomChargesResponse reservationModelToCalculateRoomChargesResponseMapper(RoomReservation reservation);
    
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

        if (null != reservation.getSpecialRequests()) {
            ReservationUtil.removeSpecialRequests(reservation, appProps);
        }
    }

    @AfterMapping
    public void populateRatesSummary(@MappingTarget CalculateRoomChargesResponse roomReservationChargesResponse,
            RoomReservation reservation) {
        roomReservationChargesResponse.setRatesSummary(ReservationUtil.getRateSummary(reservation));
        roomReservationChargesResponse.getBookings().stream()
                .forEach(b -> b.setDiscounted(b.getPrice() < b.getBasePrice()));
        if(CollectionUtils.isNotEmpty(roomReservationChargesResponse.getAvailableComponents())) {
            roomReservationChargesResponse.getAvailableComponents().stream().forEach(c -> {
                DoubleSummaryStatistics tripPrice = c.getPrices().stream()
                        .collect(Collectors.summarizingDouble(ComponentPrice::getAmount));
                c.setTripPrice(tripPrice.getSum());
                c.setPrice(tripPrice.getAverage());
                double tripTax = c.getPrices().stream().collect(Collectors.summarizingDouble(ComponentPrice::getTax))
                        .getSum();
                c.setTripTax(tripTax);
            });
        }

    }

}
