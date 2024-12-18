package com.mgm.services.booking.room.it;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.exception.ErrorCode;

public class IataIT extends BaseRoomBookingIntegrationTest {

    private final static String baseValidateServiceUrl = "/v1/iata/<CODE>";

    
    @Test
    public void test_Iata_getIataCodeDetailsWithEmptyCode_validateError() {
        client.get().uri(baseValidateServiceUrl.replaceAll("<CODE>", ServiceConstant.WHITESPACE_STRING))
                .headers(httpHeaders -> addAllHeaders(httpHeaders)).exchange().expectStatus().isBadRequest();
    }

    //@Test
    public void test_Iata_getIataCodeDetailsValidCode_validateResponse() {
        client.get().uri(baseValidateServiceUrl.replaceAll("<CODE>", defaultTestData.getIataCode()))
                .headers(httpHeaders -> addAllHeaders(httpHeaders)).exchange().expectStatus().is2xxSuccessful();
    }

    @Test
    public void test_Iata_getIataCodeDetailsWithInvalidCode_validateInvalidCodeError() {
        client.get().uri(baseValidateServiceUrl.replaceAll("<CODE>", "123"))
                .headers(httpHeaders -> addAllHeaders(httpHeaders)).exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo(ErrorCode.INVALID_IATA_CODE.getErrorCode());
    }

}
