package com.mgm.services.booking.room.it;

import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.common.exception.ErrorCode;

public class TokenIT extends BaseRoomBookingIntegrationTest {

    @Test
    public void test_V1Token_getTokenWithNoClientId_validaNoClientIdError() {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClientSecret(oktaClientSecret);
        tokenRequest.setTransientFlag(oktaTransientFlag);
        validateErrorDetails(ServiceConstant.TOKEN_URL, tokenRequest, ErrorCode.NO_CLIENT_ID.getErrorCode(),
                ErrorCode.NO_CLIENT_ID.getDescription());

    }

    @Test
    public void test_V1Token_getTokenWithNoClientSecret_validaNoClientSecretError() {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClientId(oktaClientId);
        tokenRequest.setTransientFlag(oktaTransientFlag);
        tokenRequest.setClientSecret(null);
        validateErrorDetails(ServiceConstant.TOKEN_URL, tokenRequest, ErrorCode.NO_CLIENT_SECRET.getErrorCode(),
                ErrorCode.NO_CLIENT_SECRET.getDescription());

    }

    @Test
    public void test_V1Token_getTokenWithValidParameters_validaTokenResponse() {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClientId(oktaClientId);
        tokenRequest.setClientSecret(oktaClientSecret);
        tokenRequest.setTransientFlag(oktaTransientFlag);
        client.post().uri(ServiceConstant.TOKEN_URL)
                .headers(httpHeaders -> addAllHeaders(httpHeaders, false, false, true))
                .body(BodyInserters.fromValue(tokenRequest)).exchange().expectStatus().is2xxSuccessful().expectHeader()
                .exists(ServiceConstant.X_STATE_TOKEN);

    }

    @Test
    public void test_V1Token_getTokenWithInvalidCredentials_validaInvalidClientCredentialsError() {
        TokenRequest request = getObjectFromJSON("/token-invalid-requestbody.json", TokenRequest.class);
        client.post().uri(ServiceConstant.TOKEN_URL)
                .headers(httpHeaders -> addAllHeaders(httpHeaders, false, false, true))
                .body(BodyInserters.fromValue(request)).exchange().expectStatus().isBadRequest().expectHeader()
                .doesNotExist(ServiceConstant.X_STATE_TOKEN).expectBody().jsonPath("$.code")
                .isEqualTo(ErrorCode.INVALID_CLIENT_CREDENTIALS.getErrorCode()).jsonPath("$.msg")
                .isEqualTo(ErrorCode.INVALID_CLIENT_CREDENTIALS.getDescription());

    }

    private void validateErrorDetails(String uri, Object request, String errorCode, String errorMsg) {
        client.post().uri(uri).headers(httpHeaders -> addAllHeaders(httpHeaders)).body(BodyInserters.fromValue(request))
                .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.code").isEqualTo(errorCode)
                .jsonPath("$.msg").isEqualTo(errorMsg);
    }

}
