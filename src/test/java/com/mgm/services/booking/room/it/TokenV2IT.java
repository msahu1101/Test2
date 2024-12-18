package com.mgm.services.booking.room.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TokenV2IT extends BaseRoomBookingIntegrationTest {

    private static final String baseServiceUrl = ServiceConstant.TOKEN_V2_URL;

    //@Test Enable this when cross slot issue is resolved
    public void test_V2Token_getTokenNoCredentials_validateTokenGenerated() {

        HttpHeaders headers = addBaseHeaders();

        client.post().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(headers)).exchange().expectStatus()
                .is2xxSuccessful().expectHeader().exists(ServiceConstant.X_STATE_TOKEN);

        validateResponseWithGuestTransition(addBaseHeaders());
    }

    @Test
    public void test_V2Token_getTokenWithWrongOktaSessionId_validateInvalidSessionIdError() {

        HttpHeaders headers = addBaseHeaders();
        headers.add(TestConstant.OKTA_OSID, "123");
        validateErrorResponseWithInvalidCredentials(headers, ErrorCode.INVALID_USER_SESSION);
    }

    @Test
    public void test_V2Token_getTokenWithWrongOktaAccessToken_validateInvalidOktaAccessTokenError() {

        HttpHeaders headers = addBaseHeaders();
        headers.add(TestConstant.OKTA_ACCESS_TOKEN, "123");
        validateErrorResponseWithInvalidCredentials(headers, ErrorCode.INVALID_ACCESS_TOKEN);
    }

    @Test
    public void test_V2Token_getTokenWithOktaSessionId_validateTokenFromHeader() {
        client.post().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(addBaseHeaders()))
                .header(TestConstant.OKTA_OSID, getOktaSessionId()).exchange().expectStatus().is2xxSuccessful()
                .expectHeader().exists(ServiceConstant.X_STATE_TOKEN);

    }

    //@Test Enable this when cross slot issue is resolved
    public void test_V2Token_getTokenWithGuestTransitionSessionId_validateTokenFromHeader() {
        HttpHeaders headers = addBaseHeaders();
        headers.add(TestConstant.OKTA_OSID, getOktaSessionId());
        validateResponseWithGuestTransition(headers);
    }

    @Test
    public void test_V2Token_getTokenWithGuestTransitionAndInvalidSessionId_validateTokenNotGenerated() {
        HttpHeaders headers = addBaseHeaders();
        headers.add(TestConstant.OKTA_OSID, "123");
        validateGuestTransitionWithInvalidCredentials(headers);

    }

    @Test
    public void test_V2Token_getTokenWithOktaAccessToken_validateTokenFromHeader() {
        client.post().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(addBaseHeaders()))
                .header(TestConstant.OKTA_ACCESS_TOKEN, getOktaAccessToken()).exchange().expectStatus()
                .is2xxSuccessful().expectHeader().exists(ServiceConstant.X_STATE_TOKEN);
    }

    //@Test Enable this when cross slot issue is resolved
    public void test_V2Token_getTokenWithGuestTransitionAccessToken_validateTokenFromHeader() {
        HttpHeaders headers = addBaseHeaders();
        headers.add(TestConstant.OKTA_ACCESS_TOKEN, getOktaAccessToken());
        validateResponseWithGuestTransition(headers);
    }

    @Test
    public void test_V2Token_getTokenWithGuestTransitionAndInvalidAccessToken_validateTokenNotGenerated() {
        HttpHeaders headers = addBaseHeaders();
        headers.add(TestConstant.OKTA_ACCESS_TOKEN, "123");
        validateGuestTransitionWithInvalidCredentials(headers);
    }

    /**
     * This method validates that if POST flow has not added a x-state-token due
     * to wrong credentials, then the PUT flow should not add one for the same
     * credentials
     * 
     * @param allHeaders
     */
    private void validateErrorResponseWithInvalidCredentials(HttpHeaders allHeaders, ErrorCode errorCode) {
        client.post().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(allHeaders)).exchange()
                .expectStatus().isBadRequest().expectHeader().doesNotExist(ServiceConstant.X_STATE_TOKEN).expectBody()
                .jsonPath("$.code").isEqualTo(errorCode.getErrorCode()).jsonPath("$.msg")
                .isEqualTo(errorCode.getDescription());

        // Validate Put should not add x-state-token if not already present for
        // the same scenario
        client.put().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(allHeaders)).exchange()
                .expectStatus().isBadRequest().expectHeader().doesNotExist(ServiceConstant.X_STATE_TOKEN).expectBody()
                .jsonPath("$.code").isEqualTo(ErrorCode.INVALID_STATE_TOKEN.getErrorCode()).jsonPath("$.msg")
                .isEqualTo(ErrorCode.INVALID_STATE_TOKEN.getDescription());
    }

    /**
     * This method validates that when the PUT API is called for a guest session
     * with valid credentials, then x-state-token should be returned
     * 
     * @param allHeaders
     */
    public void validateResponseWithGuestTransition(HttpHeaders allHeaders) {
        client.post().uri(baseServiceUrl).headers(httpHeaders -> addBaseHeaders(httpHeaders)).exchange().expectStatus()
                .is2xxSuccessful().expectHeader().exists(ServiceConstant.X_STATE_TOKEN).expectBody()
                .consumeWith(consumer -> {
                    String oldStateToken = consumer.getResponseHeaders().get(ServiceConstant.X_STATE_TOKEN).get(0);
                    client.put().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(allHeaders))
                            .header(ServiceConstant.X_STATE_TOKEN, oldStateToken).exchange().expectStatus()
                            .is2xxSuccessful().expectBody().consumeWith(putConsumer -> {
                                HttpHeaders putResponseHeaders = putConsumer.getResponseHeaders();
                                String newStateToken = putResponseHeaders.get(ServiceConstant.X_STATE_TOKEN).get(0);
                                assertTrue("X-State-Token should not be blank", StringUtils.isNotBlank(newStateToken));
                                assertNotSame("X-STATE-TOKEN should not match", oldStateToken, newStateToken);
                            });
                });

    }

    /**
     * This method validates that when the PUT API is called for a guest session
     * with invalid credentials, then x-state-token should not be returned
     * 
     * @param allHeaders
     */
    public void validateGuestTransitionWithInvalidCredentials(HttpHeaders allHeaders) {
        client.post().uri(baseServiceUrl).headers(httpHeaders -> addBaseHeaders(httpHeaders)).exchange().expectStatus()
                .is2xxSuccessful().expectHeader().exists(ServiceConstant.X_STATE_TOKEN).expectBody()
                .consumeWith(consumer -> {
                    String oldStateToken = consumer.getResponseHeaders().get(ServiceConstant.X_STATE_TOKEN).get(0);
                    client.put().uri(baseServiceUrl).headers(httpHeaders -> httpHeaders.addAll(allHeaders))
                            .header(ServiceConstant.X_STATE_TOKEN, oldStateToken).exchange().expectStatus()
                            .isBadRequest().expectHeader().doesNotExist(ServiceConstant.X_STATE_TOKEN);
                });

    }

    @Test
    public void getTokenCookieTest() {
        client.post().uri(baseServiceUrl).headers(httpHeaders -> {
            addAllHeaders(httpHeaders, false, false, false);
            httpHeaders.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEBCLIENT);
        }).exchange().expectStatus().is2xxSuccessful().expectHeader().exists(ServiceConstant.SET_COOKIE);

    }

    @Test
    public void test_V2Token_getTokenWitCookieAndOsidCookie_validateTokenGeneratedInHeader() {

        String osid = getOktaSessionId();

        ClientResponse response = realClient.post().uri(baseServiceUrl).headers(httpHeaders -> {
            addAllHeaders(httpHeaders, false, false, false);
            httpHeaders.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEBCLIENT);
            httpHeaders.add(ServiceConstant.COOKIE, "osid=" + osid);
        }).exchange().block();

        log.info("Status Code: {}, Response: {}", response.rawStatusCode(), response.toEntity(String.class));
        log.info(response.cookies());

        // Make a call as webclient channel and get token
        String token = response.cookies().getFirst(ServiceConstant.X_STATE_TOKEN).getValue();

        // Assert to ensure token is returned as cookie
        assertNotNull("Token should be available as cookie", token);

        // Make default-perpetual api call with cookie header and test if's it
        // accepted properly. This call will fail if there's no mlifeNumber
        client.get().uri("/v1/offers/room/default-perpetual").headers(headers -> {
            addAllHeaders(headers, true, false, false);
            headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEBCLIENT);
            headers.add(ServiceConstant.COOKIE, "x-state-token=" + token);
        }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray();

    }

    @Test
    public void cookieExchangeTest() {
        ClientResponse response = realClient.post().uri(baseServiceUrl).headers(httpHeaders -> {
            addAllHeaders(httpHeaders, false, false, false);
            httpHeaders.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEBCLIENT);
        }).exchange().block();

        // Make a call as webclient channel and get token
        String token = response.cookies().getFirst(ServiceConstant.X_STATE_TOKEN).getValue();

        // Assert to ensure token is returned as cookie
        assertNotNull("Token should be available as cookie", token);

        // Make another api call with cookie header and test if's it accepted
        // properly
        client.get().uri("/v1/offers/room").headers(headers -> {
            addAllHeaders(headers, true, false, false);
            headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEBCLIENT);
            headers.add(ServiceConstant.COOKIE, "x-state-token=" + token);
        }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray().jsonPath("$.[0].id").exists()
                .jsonPath("$.[0].type").exists();

    }

    public HttpHeaders addBaseHeaders(HttpHeaders headers) {
        headers = (headers == null) ? new HttpHeaders() : headers;
        headers.add(TestConstant.X_API_KEY, apiKey);
        headers.add(TestConstant.HEADER_CHANNEL_V1, TestConstant.CHANNEL_WEB);
        return headers;
    }

    public HttpHeaders addBaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        return addBaseHeaders(headers);
    }

}
