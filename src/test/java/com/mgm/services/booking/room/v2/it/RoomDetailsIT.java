package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.ValidAvailabilityData;

/**
 * @author laknaray
 *
 */
public class RoomDetailsIT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        ValidAvailabilityData availData = getAvailability(1);
        queryParams.add("checkInDate", availData.getCheckInDate());
        queryParams.add("checkOutDate", availData.getCheckOutDate());
        queryParams.add("propertyId", availData.getPropertyId());
        queryParams.add("roomTypeId", availData.getRoomTypeId());
        return new ApiDetails(ApiDetails.Method.GET, "/v2/availability/components", queryParams, null);
    }

    @Test
    public void roomDetails_withValidParameters_validateSuccessResponse() {
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getApiDetails().getDefaultQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray().jsonPath("$.[0].id").exists();
    }
}
