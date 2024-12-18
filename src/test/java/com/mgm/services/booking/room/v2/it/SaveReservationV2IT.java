/**
 * Class to keep integration tests related to reservation under ReservationV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.SaveReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.SaveReservationResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Class to keep integration tests related to reservation under
 * SaveRoomReservationV2Controller.
 * 
 * @author jayveera
 *
 */
@Log4j2
public class SaveReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.POST, "/v2/reservation/save", null,
                createRequestBasic("/createroomreservationrequest-basic.json"));
    }

    /**
     * saveRoomReservation with the basic props.
     * 
     */
    @Test
    public void reservationV2_withTransientUser_returnsRoomReservation() {
        //getAvailability(TestConstant.ONE_NIGHT);
        SaveReservationRequest request = new SaveReservationRequest();
        request.setRoomReservation(createRequestBasic("/createroomreservationrequest-basic.json").getRoomReservation());
        SaveReservationResponse response = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(request)).headers(httpHeaders -> addAllHeaders(httpHeaders,
                        TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to save reservation {}", error);
                    throw new TestExecutionException("Error on reserving room for endpoint "
                            + getApiDetails().getBaseServiceUrl() + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    log.info("Response code for reservation: {}", clientResponse.statusCode());
                    validateSuccessResponse(clientResponse, getApiDetails().getBaseServiceUrl());
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("Reservation Response: {}", entity.getBody());
                        return convert(entity.getBody(), SaveReservationResponse.class);
                    });

                }).block();

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservation savedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, savedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(savedReservation.getBookings()));
        assertNotNull("Itinerary Id cannot be null", response.getRoomReservation().getItineraryId());
    }

}
