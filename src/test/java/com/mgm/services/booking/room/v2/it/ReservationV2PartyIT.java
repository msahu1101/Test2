/**
 * Class to keep integration tests related to partyReservation under ReservationV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.CreatePartyRoomReservationRequest;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Class to keep integration tests related to partyReservation under
 * ReservationV2Controller.
 * 
 * @author laknaray
 *
 */
@Log4j2
public class ReservationV2PartyIT extends BaseRoomBookingV2IntegrationTest {

    /**
     * 
     */
    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.POST, "/v2/reservation/party", null, new CreatePartyRoomReservationRequest());
    }

    /**
     * makePartyRoomReservation with the basic props for transient user.
     * 
     */
    @Test
    public void partyReservation_withTransientUser_returnsRoomReservation() {
        // Creating a reservation
        CreatePartyRoomReservationResponse response = makePartyRoomReservation(
                createPartyRequestBasic("/createpartyroomreservationrequest-basic-transient.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        List<RoomReservationV2Response> bookedReservations = response.getRoomReservations();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservations);
        assertEquals(TestConstant.INCORRECT_RESERVATIONS, reservationCount(response), 2);
        bookedReservations.stream().forEach(bookedResponse -> {
            assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBookings()));
            assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedResponse.getConfirmationNumber());
            assertFalse(TestConstant.EMPTY_BILLING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBilling()));
            assertFalse(TestConstant.EMPTY_PAYMENTS_OBJECT, CollectionUtils.isEmpty(bookedResponse.getPayments()));
            assertTrue("Booking source should be ICE",
                    StringUtils.equals(bookedResponse.getBookingSource(), TestConstant.ICE));
            assertTrue("Booking channel should be ICE",
                    StringUtils.equals(bookedResponse.getBookingChannel(), TestConstant.ICE));
        });
    }

    /**
     * makePartyRoomReservation with the basic props.
     * 
     */
    @Test
    public void partyReservation_withBasicDetails_returnsRoomReservation() {
        // Creating a reservation
        CreatePartyRoomReservationResponse response = makePartyRoomReservation(
                createPartyRequestBasic("/createpartyroomreservationrequest-basic.json"));

        log.info("Party Reservation Response: {}", CommonUtil.convertObjectToJsonString(response));
        
        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        List<RoomReservationV2Response> bookedReservations = response.getRoomReservations();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservations);
        assertEquals(TestConstant.INCORRECT_RESERVATIONS, reservationCount(response), 3);
        bookedReservations.stream().forEach(bookedResponse -> {
            assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBookings()));
            assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedResponse.getConfirmationNumber());
            assertFalse(TestConstant.EMPTY_BILLING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBilling()));
            assertFalse(TestConstant.EMPTY_PAYMENTS_OBJECT, CollectionUtils.isEmpty(bookedResponse.getPayments()));
        });
    }

    /**
     * makePartyRoomReservation with the basic props along with the below
     * 
     * <li>with special requests</li>
     */
    @Test
    public void partyReservation_withSpecialRequests_returnsRoomReservationWithSpecialRequests() {
        // Creating a reservation with special requests
        CreatePartyRoomReservationResponse response = makePartyRoomReservation(
                createPartyRequestBasic("/createpartyroomreservationrequest-specialrequest.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        List<RoomReservationV2Response> bookedReservations = response.getRoomReservations();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservations);
        assertEquals(TestConstant.INCORRECT_RESERVATIONS, reservationCount(response), 2);
        bookedReservations.stream().forEach(bookedResponse -> {
            assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBookings()));
            assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedResponse.getConfirmationNumber());
            assertArrayEquals(TestConstant.INCORRECT_SPECIAL_REQUESTS, bookedResponse.getSpecialRequests().toArray(),
                    defaultTestData.getSpecialRequests().split(","));
            assertFalse(TestConstant.EMPTY_BILLING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBilling()));
            assertFalse(TestConstant.EMPTY_PAYMENTS_OBJECT, CollectionUtils.isEmpty(bookedResponse.getPayments()));
        });
    }

    /**
     * makePartyRoomReservation with the basic props along with the below
     * 
     * <li>with multiple phone numbers</li>
     * <li>with both email address</li>
     * <li>with multiple addresses</li>
     */
    @Test
    public void partyReservation_withUpdateProfile_returnsReservationWithUpdatedProfile() {
        // Creating a reservation with updated profile
        CreatePartyRoomReservationResponse response = makePartyRoomReservation(
                createPartyRequestBasic("/createpartyroomreservationrequest-updatedprofile.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        List<RoomReservationV2Response> bookedReservations = response.getRoomReservations();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservations);
        assertEquals(TestConstant.INCORRECT_RESERVATIONS, reservationCount(response), 3);
        bookedReservations.stream().forEach(bookedResponse -> {
            assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBookings()));
            assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedResponse.getConfirmationNumber());
            assertEquals(TestConstant.INCORRECT_PHONENUMBERS, bookedResponse.getProfile().getPhoneNumbers().size(), 2);
            assertEquals(TestConstant.INCORRECT_ADDRESSES, bookedResponse.getProfile().getAddresses().size(), 2);
            assertFalse(TestConstant.EMPTY_BILLING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBilling()));
            assertFalse(TestConstant.EMPTY_PAYMENTS_OBJECT, CollectionUtils.isEmpty(bookedResponse.getPayments()));
        });
    }

    /**
     * makePartyRoomReservation with the basic props along with the below
     * 
     * <li>with more than one night stay</li>
     */
    @Test
    public void partyReservation_withMultipleDays_returnsReservationWithMultiNights() {
        // Creating a reservation with more than one night stay
        CreatePartyRoomReservationResponse response = makePartyRoomReservation(createPartyRequestMultiNight(
                "/createpartyroomreservationrequest-multinightstay.json", TestConstant.TWO_NIGHTS));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        List<RoomReservationV2Response> bookedReservations = response.getRoomReservations();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservations);
        assertEquals(TestConstant.INCORRECT_RESERVATIONS, reservationCount(response), 2);
        bookedReservations.stream().forEach(bookedResponse -> {
            assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBookings()));
            assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedResponse.getConfirmationNumber());
            assertEquals(TestConstant.INCORRECT_BOOKINGS, bookedResponse.getBookings().size(), 2);
            assertEquals(TestConstant.INCORRECT_MARKETS, bookedResponse.getMarkets().size(), 2);
            assertFalse(TestConstant.EMPTY_BILLING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBilling()));
            assertFalse(TestConstant.EMPTY_PAYMENTS_OBJECT, CollectionUtils.isEmpty(bookedResponse.getPayments()));
        });
    }

    /**
     * makePartyRoomReservation with the basic props but <i>splitCreditCardDetails</i>
     * as false and <i>with out guaranteeCode</i>
     */
    @Test
    public void partyReservation_withoutGuaranteeCode_returnReservationWithGuaranteeCode() {
        // Creating a reservation without guaranteeCode and with rrUpcell
        CreatePartyRoomReservationResponse response = makePartyRoomReservation(
                createPartyRequestBasic("/createpartyroomreservationrequest-noguaranteecode.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        List<RoomReservationV2Response> bookedReservations = response.getRoomReservations();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservations);
        assertEquals(TestConstant.INCORRECT_RESERVATIONS, reservationCount(response), 2);
        bookedReservations.stream().forEach(bookedResponse -> {
            assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedResponse.getBookings()));
            assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedResponse.getConfirmationNumber());
            assertNotNull(TestConstant.EMPTY_GUARANTEECODE, bookedResponse.getGuaranteeCode());
        });
    }

    /**
     * Count and return the total number of objects returned by source system,
     * either it can be a success or failure.
     * 
     * @param response 
     *            CreatePartyRoomReservationResponse object
     * @return number of reservations (success/failure)
     */
    private int reservationCount(CreatePartyRoomReservationResponse response) {
        int count = 0;
        if (null != response) {
            if (null != response.getRoomReservations()) {
                count += response.getRoomReservations().size();
            }
            if (null != response.getFailedReservations()) {
                count += response.getFailedReservations().size();
            }
        }
        return count;
    }
}
