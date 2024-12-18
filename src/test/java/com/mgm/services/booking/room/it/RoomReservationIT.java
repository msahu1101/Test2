package com.mgm.services.booking.room.it;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.web.reactive.function.BodyInserters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.request.ReservationRequest;
import com.mgm.services.booking.room.model.response.ConsolidatedRoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RoomReservationIT extends BaseRoomBookingIntegrationTest {

    @Test
    public void test_roomReservation_makeRoomReservation_validateRoomReservationResponse() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource(TestConstant.JWB_CHECKOUT_REQUEST_FILENAME).getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").exists().jsonPath("$.booked[0].rates.reservationTotal")
                    .exists().jsonPath("$.booked[0].payment.chargeAmount").exists().consumeWith(consumer -> {
                        String responseJson = new String(consumer.getResponseBodyContent());
                        ConsolidatedRoomReservationResponse response;
                        try {
                            response = new ObjectMapper().readValue(responseJson,
                                    ConsolidatedRoomReservationResponse.class);
                        } catch (Exception e) {
                            throw new TestExecutionException(e.getMessage(), e);
                        }
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    });
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_roomReservation_makeRoomReservationWithFullPayment_validateRoomReservationResponse() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource(TestConstant.JWB_CHECKOUT_REQUEST_FILENAME).getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.setFullPayment(true);

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").exists().jsonPath("$.booked[0].rates.reservationTotal")
                    .exists().jsonPath("$.booked[0].payment.chargeAmount").exists().consumeWith(consumer -> {
                        String responseJson = new String(consumer.getResponseBodyContent());
                        ConsolidatedRoomReservationResponse response;
                        try {
                            response = new ObjectMapper().readValue(responseJson,
                                    ConsolidatedRoomReservationResponse.class);
                        } catch (Exception e) {
                            throw new TestExecutionException(e.getMessage(), e);
                        }
                        log.info("Reservation Total: {}, Payment Charged: {}",
                                response.getBooked().get(0).getRates().getReservationTotal(),
                                response.getBooked().get(0).getPayment().getChargeAmount());
                        assertEquals("Full amount was charged",
                                response.getBooked().get(0).getRates().getReservationTotal(),
                                response.getBooked().get(0).getPayment().getChargeAmount(), 0.1);
                    });
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithInvalidIataCode_validateInvalidIataCodeError() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource(TestConstant.JWB_CHECKOUT_REQUEST_FILENAME).getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.setIata("123");

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isBadRequest().expectBody();
        try {
            body.jsonPath("$.code").isEqualTo(ErrorCode.INVALID_IATA_CODE.getErrorCode());
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithValidIataCode_validateRoomReservationResponse() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource(TestConstant.JWB_CHECKOUT_REQUEST_FILENAME).getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.setIata(defaultTestData.getIataCode());

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").exists().consumeWith(consumer -> {
                log.info(new String(consumer.getResponseBodyContent()));
            });
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithInternationalTransaction_validateRoomReservationResponse() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-intl.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").exists().consumeWith(consumer -> {
                log.info(new String(consumer.getResponseBodyContent()));
            });
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    //@Test // Disabling due to lack of support from AFS service
    public void test_roomReservation_makeRoomReservationFailureAtAntiFraudServices_validateAntiFraudError() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-intl.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.getProfile().setEmail(ServiceConstant.INVALID_TX_EMAIL);

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").doesNotExist().consumeWith(consumer -> {
                log.info(new String(consumer.getResponseBodyContent()));
            }).jsonPath("$.failed[0].failure.code").isEqualTo(ErrorCode.TRANSACTION_NOT_AUTHORIZED.getErrorCode());
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_roomReservation_makeRoomReservationWithPaymentFailure_validatePaymentFailureError() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-intl.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.getBilling().getPayment().setCardNumber(defaultTestData.getInvalidCreditCard());

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").doesNotExist().jsonPath("$.failed[0].failure.code")
                    .isEqualTo(ErrorCode.PAYMENT_FAILED.getErrorCode()).consumeWith(consumer -> {
                        log.info(new String(consumer.getResponseBodyContent()));
                    });
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

//    @Test
    public void test_roomReservation_makeRoomReservationWithJwbEnabled_validateRoomReservationResponse() {
        makeTestPreReservationForJwb(true, false);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-jwb.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        String randomEmailId = getRandomEmailId(reservationRequest.getProfile().getEmail());
        reservationRequest.getProfile().setEmail(randomEmailId);

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.booked[0].confirmationNumber").exists().jsonPath("$.booked[0].rates.programDiscount")
                    .exists().consumeWith(consumer -> {
                        String responseJson = new String(consumer.getResponseBodyContent());
                        ConsolidatedRoomReservationResponse response;
                        try {
                            response = new ObjectMapper().readValue(responseJson,
                                    ConsolidatedRoomReservationResponse.class);
                        } catch (Exception e) {
                            throw new TestExecutionException(e.getMessage(), e);
                        }
                        assertNotEquals(response.getBooked().get(0).getRates().getProgramDiscount(), 0.0);
                        log.info(new String(responseJson));
                    });
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    // @Test
    public void test_roomReservation_makeRoomReservationWithJwbStagedUser_validateRoomReservationResponse() {
        String customerEmailId = "testrandomjwb@mailinator.com";
        makeTestPreReservationForJwbStagedUser(customerEmailId, true, false);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-jwb.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.getProfile().setEmail(customerEmailId);

        BodyContentSpec body = client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody();
        try {

            body.jsonPath("$.booked[0].confirmationNumber").exists().jsonPath("$.booked[0].rates.programDiscount")
                    .exists().consumeWith(consumer -> {
                        String responseJson = new String(consumer.getResponseBodyContent());
                        ConsolidatedRoomReservationResponse response;
                        try {
                            response = new ObjectMapper().readValue(responseJson,
                                    ConsolidatedRoomReservationResponse.class);
                        } catch (Exception e) {
                            throw new TestExecutionException(e.getMessage(), e);
                        }
                        assertNotEquals(response.getBooked().get(0).getRates().getProgramDiscount(), 0.0);
                        log.info(new String(responseJson));

                    });

        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_roomReservation_makeRoomReservationWithJwbEnabledAndInvalidPassword_validateErrorResponse() {
        makeTestPreReservationForJwb(true, false);
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-jwb.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.getProfile().setPassword("Mlife");

        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().is4xxClientError();

    }

    @Test
    public void test_roomReservation_makeRoomReservationWithJwbEnabledAndInvalidSecrectAnswer_validateErrorResponse() {
        makeTestPreReservationForJwb(true, false);
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody-jwb.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.getProfile().setSecurityAnswer("au");

        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().is4xxClientError();

    }

    @Test
    public void test_roomReservation_makeRoomReservationWithImproperAddress_validateReservationResponse() {
        makeTestPreReservation(true, null);
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        reservationRequest.getBilling().getAddress().setState(null);
        reservationRequest.getBilling().getAddress().setPostalCode(null);
        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.booked[0].confirmationNumber").exists()
                .jsonPath("$.booked[0].rates.reservationTotal").exists().jsonPath("$.booked[0].payment.chargeAmount")
                .exists().consumeWith(consumer -> {
                    String responseJson = new String(consumer.getResponseBodyContent());
                    try {
                        ConsolidatedRoomReservationResponse response = new ObjectMapper().readValue(responseJson,
                                ConsolidatedRoomReservationResponse.class);
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    } catch (Exception e) {
                        log.error("Error whiile parsing Json object");
                    }
                    log.info(new String(responseJson));

                });
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithGoldUser_validateReservationResponse() {
        // goldmgmuser@yopmail.com/Password1 - 79622933 - Gold
        makeTestPreReservation(true, "79622933");
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.booked[0].confirmationNumber").exists()
                .jsonPath("$.booked[0].rates.reservationTotal").exists().jsonPath("$.booked[0].payment.chargeAmount")
                .exists().consumeWith(consumer -> {
                    String responseJson = new String(consumer.getResponseBodyContent());
                    try {
                        ConsolidatedRoomReservationResponse response = new ObjectMapper().readValue(responseJson,
                                ConsolidatedRoomReservationResponse.class);
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    } catch (Exception e) {
                        log.error("Error whiile parsing Json object");
                    }
                    log.info(new String(responseJson));

                });
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithSapphireUser_validateReservationResponse() {
        // mgmsapphireuser@yopmail.com| Password1 - 76005779 - Sapphire
        makeTestPreReservation(true, "76005779");
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.booked[0].confirmationNumber").exists()
                .jsonPath("$.booked[0].rates.reservationTotal").exists().jsonPath("$.booked[0].payment.chargeAmount")
                .exists().consumeWith(consumer -> {
                    String responseJson = new String(consumer.getResponseBodyContent());
                    try {
                        ConsolidatedRoomReservationResponse response = new ObjectMapper().readValue(responseJson,
                                ConsolidatedRoomReservationResponse.class);
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    } catch (Exception e) {
                        log.error("Error whiile parsing Json object");
                    }
                    log.info(new String(responseJson));
                });
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithPearlUser_validateReservationResponse() {
        // mgmnoir@yopmail.com|Password1 - 75906600 - Pearl
        makeTestPreReservation(true, "75906600");
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.booked[0].confirmationNumber").exists()
                .jsonPath("$.booked[0].rates.reservationTotal").exists().jsonPath("$.booked[0].payment.chargeAmount")
                .exists().consumeWith(consumer -> {
                    String responseJson = new String(consumer.getResponseBodyContent());
                    try {
                        ConsolidatedRoomReservationResponse response = new ObjectMapper().readValue(responseJson,
                                ConsolidatedRoomReservationResponse.class);
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    } catch (Exception e) {
                        log.error("Error whiile parsing Json object");
                    }
                    log.info(new String(responseJson));
                });
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithDiamondUser_validateReservationResponse() {
        // noirmgmuser@yopmail.com/Password1 - 79622935 - Diamond
        makeTestPreReservation(true, "79622935");
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.booked[0].confirmationNumber").exists()
                .jsonPath("$.booked[0].rates.reservationTotal").exists().jsonPath("$.booked[0].payment.chargeAmount")
                .exists().consumeWith(consumer -> {
                    String responseJson = new String(consumer.getResponseBodyContent());
                    try {
                        ConsolidatedRoomReservationResponse response = new ObjectMapper().readValue(responseJson,
                                ConsolidatedRoomReservationResponse.class);
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    } catch (Exception e) {
                        log.error("Error whiile parsing Json object");
                    }
                    log.info(new String(responseJson));
                });
    }

    @Test
    public void test_roomReservation_makeRoomReservationWithTwoRooms_validateReservationResponse() {
        makeTestPreReservation(true, null);
        addSecondRoomToCart();
        // Checkout the room for the given reservation-id and
        File requestFile = new File(getClass().getResource("/room-checkout-requestbody.json").getPath());
        ReservationRequest reservationRequest = convert(requestFile, ReservationRequest.class);
        client.post().uri(builder -> builder.path("/v1/reserve/room").build())
                .body(BodyInserters.fromValue(reservationRequest)).headers(httpHeaders -> addAllHeaders(httpHeaders))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.booked[0].confirmationNumber").exists()
                .jsonPath("$.booked[0].rates.reservationTotal").exists().jsonPath("$.booked[0].payment.chargeAmount")
                .exists().consumeWith(consumer -> {
                    String responseJson = new String(consumer.getResponseBodyContent());
                    try {
                        ConsolidatedRoomReservationResponse response = new ObjectMapper().readValue(responseJson,
                                ConsolidatedRoomReservationResponse.class);
                        assertEquals("Full amount was not charged",
                                response.getBooked().get(0).getPayment().getChargeAmount(),
                                response.getBooked().get(0).getRates().getDepositDue(), 0.1);
                    } catch (Exception e) {
                        log.error("Error whiile parsing Json object");
                    }
                    log.info(new String(responseJson));
                });
    }

}
