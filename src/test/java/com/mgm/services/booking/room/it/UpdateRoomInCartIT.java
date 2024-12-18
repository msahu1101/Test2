package com.mgm.services.booking.room.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ProgramEligibility;
import com.mgm.services.booking.room.model.ValidAvailabilityData;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.request.RoomCartUpdateRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.response.CartResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UpdateRoomInCartIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/cart/room/";

    @Test
    public void test_UpdateRoomInCart_PrereserveRoom_validateUpdateRoomResponse() {

        String reservationId = addTestRoomToCart();

        // Modify(Add/Remove) the room for the given reservation-id and
        // X_AUTH_TOKEN

        RoomCartUpdateRequest putRequest = new RoomCartUpdateRequest();
        List<RoomRequest> roomRequests = new ArrayList<RoomRequest>();
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setId(defaultTestData.getRoomId());
        roomRequest.setSelected(true);
        roomRequests.add(roomRequest);
        putRequest.setRoomRequests(roomRequests);

        Mono<RoomReservationResponse> resultPut = realClient.put()
                .uri(builder -> builder.path(baseServiceUrl + reservationId).build())
                .body(BodyInserters.fromValue(putRequest)).headers(headers -> {
                    addApiGtwyHeaders(headers);
                    headers.add(TestConstant.HEADER_SOURCE_V1, defaultTestData.getSource());
                    headers.add(TestConstant.X_STATE_TOKEN, STATE_TOKEN);
                    headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEB);
                }).exchange().log().flatMap(clientResponse -> clientResponse.bodyToMono(RoomReservationResponse.class));

        RoomReservationResponse responsePut = resultPut.block();

        assertEquals("Reservation Id should match", reservationId, responsePut.getItemId());

    }

    @Test
    public void test_UpdateRoomInCart_updateCartWithEmptyCartItems_validateNoCartItemError() {
        validateGetRequestErrorDetails(baseServiceUrl, ErrorCode.NO_CART_ITEMS.getErrorCode(),
                ErrorCode.NO_CART_ITEMS.getDescription());
    }

    @Test
    public void test_UpdateRoomInCart_addRoomToTheCart_validateUpdatedRoomAddedToCart() {

        String reservationId = addTestRoomToCart();
        assertTrue("The room was found in the cart", checkifRoomAvailableInCart(reservationId));
    }

    @Test
    public void test_UpdateRoomInCart_deleteRoomInTheCart_validateRemovedTheRoomDeletedToCart() {
        // Add a room to cart
        String reservationId = addTestRoomToCart();
        // Check if it shows up in cart
        assertFalse("The room was not found in the cart. Add/Get not working as expected",
                !checkifRoomAvailableInCart(reservationId));
        // Delete the room from the cart
        realClient.delete().uri(builder -> builder.path(baseServiceUrl + reservationId).build()).headers(headers -> {
            addApiGtwyHeaders(headers);
            headers.add(TestConstant.HEADER_SOURCE_V1, defaultTestData.getSource());
            headers.add(TestConstant.X_STATE_TOKEN, STATE_TOKEN);
            headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEB);
        }).exchange().block();
        // Check if the room is deleted from the cart
        assertFalse("The room was found in the cart. Delete not working as expected",
                checkifRoomAvailableInCart(reservationId));

    }

    private boolean checkifRoomAvailableInCart(String reservationId) {
        CartResponse cartResponse = realClient.get().uri(builder -> builder.path(baseServiceUrl).build())
                .headers(headers -> {
                    addApiGtwyHeaders(headers);
                    headers.add(TestConstant.HEADER_SOURCE_V1, defaultTestData.getSource());
                    headers.add(TestConstant.X_STATE_TOKEN, STATE_TOKEN);
                    headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEB);
                }).exchange().flatMap(clientResponse -> clientResponse.bodyToMono(CartResponse.class)).block();

        boolean isRoomFoundInCart = false;
        if (cartResponse.getItems() != null) {
            for (RoomReservationResponse room : cartResponse.getItems()) {
                if (room.getItemId().equalsIgnoreCase(reservationId)) {
                    isRoomFoundInCart = true;
                }
            }
        }
        return isRoomFoundInCart;

    }

    private String addTestRoomToCart() {
        // Pre-reserve the room for the dates and get the reservation-id and
        // X_AUTH_TOKEN
        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());
        ValidAvailabilityData data = getAvailabilityTestData(request, false, false);
        
        RoomCartRequest preReserveRequest = new RoomCartRequest();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        final LocalDate checkInLDate = LocalDate.parse(data.getCheckInDate(), formatter);
        final LocalDate checkOutLDate = LocalDate.parse(data.getCheckOutDate(), formatter);

        preReserveRequest.setCheckInDate(checkInLDate);
        preReserveRequest.setCheckOutDate(checkOutLDate);
        preReserveRequest.setPropertyId(defaultTestData.getPropertyId());
        preReserveRequest.setRoomTypeId(data.getRoomTypeId());
        preReserveRequest.setProgramId(defaultTestData.getProgramId());
        preReserveRequest.setNumGuests(defaultTestData.getNumAdults());

        Mono<RoomReservationResponse> result = realClient.post().uri(builder -> builder.path(baseServiceUrl).build())
                .body(BodyInserters.fromValue(preReserveRequest)).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().flatMap(clientResponse -> clientResponse.bodyToMono(RoomReservationResponse.class));

        RoomReservationResponse response = result.block();

        log.info("RESERVATION_ID: {}", response.getItemId());

        return response.getItemId();
    }

}
