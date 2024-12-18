/**
 * Class to keep integration tests related to reservation under ReservationV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;

/**
 * Class to keep integration tests related to get perpetual offers under
 * ProgramV2Controller.
 * 
 * @author vararora
 *
 */
public class PerpetualOffersV2IT extends BaseRoomBookingV2IntegrationTest {
    
    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.CUSTOMER_ID, customerId);
        return new ApiDetails(ApiDetails.Method.GET, "/v2/programs/default-perpetual", queryParams,
                new PerpetualProgramRequest());
    }

    @Test
    public void perpetualOffer_successfulResponse_successfulAvailability() {
        client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CUSTOMER_ID, customerId).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.CHANNEL_WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray();
    }
}
