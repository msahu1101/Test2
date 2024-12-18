package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;

public class ModifyProfileReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.PUT, "/v2/reservation/profile", null, new UpdateProfileInfoRequest());
    }

    @Test
    public void modifyReservationUpdateProfileInfo_updateProfile_validate() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        File file = new File(getClass().getResource("/modifyprofileroomreservation-request.json").getPath());

        UpdateProfileInfoRequest updateProfileRequest = convert(file, UpdateProfileInfoRequest.class);
        if (null != customerId) {
            updateProfileRequest.getUserProfile().setId(NumberUtils.toLong(customerId));
        }
        updateProfileRequest
                .setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        updateProfileRequest.setReservationId(createRoomReservationResponse.getRoomReservation().getId());
        updateProfileRequest.setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        updateProfileRequest.getUserProfile().setFirstName("IT Test - First Name");
        updateProfileRequest.getUserProfile().setLastName("IT Test - Last Name");
        UpdateProfileInfoResponse updateProfileResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(updateProfileRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(UpdateProfileInfoResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same after update.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                updateProfileResponse.getRoomReservation().getConfirmationNumber());
        assertEquals("First Name should be equal", "IT Test - First Name",
                updateProfileResponse.getRoomReservation().getProfile().getFirstName());
        assertEquals("Last Name should be equal", "IT Test - Last Name",
                updateProfileResponse.getRoomReservation().getProfile().getLastName());
        assertTrue("Booking source should be ICE",
                StringUtils.equals(updateProfileResponse.getRoomReservation().getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE",
                StringUtils.equals(updateProfileResponse.getRoomReservation().getBookingChannel(), TestConstant.ICE));
    }
}
