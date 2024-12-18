/**
 * Class to keep integration tests related to reservation under ReservationV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.ProgramEligibility;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;

/**
 * Class to keep integration tests related to get validate room offers under
 * ProgramV2Controller.
 * 
 * @author vararora
 *
 */
public class ValidateRoomOfferV2IT extends BaseRoomBookingV2IntegrationTest {
    
    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.PROGRAM_ID, defaultTestData.getProgramId());
        return new ApiDetails(ApiDetails.Method.GET, "/v2/offer/validate", queryParams, new PerpetualProgramRequest());
    }

    private RoomProgramValidateResponse validateRoomOffer(ProgramEligibility eligibility, boolean usePromoCode) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        queryParams.add(TestConstant.PROPERTY_ID, eligibility.getPropertyId());

        if (usePromoCode) {
            queryParams.add(TestConstant.PROMO_CODE, eligibility.getPromoCode());
        } else {
            queryParams.add(TestConstant.PROGRAM_ID, eligibility.getProgramId());
        }

        if (StringUtils.isNotEmpty(eligibility.getCustomerId())) {
            queryParams.add(TestConstant.CUSTOMER_ID, eligibility.getCustomerId());
        }

        return realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> {
                    validateSuccessResponse(response, getApiDetails().getBaseServiceUrl());
                    validateResponseHeaders(response);
                }).flatMap(clientResponse -> clientResponse.bodyToMono(RoomProgramValidateResponse.class)).block();
    }

    @Test
    public void validateRoomOffer_whenProgramIdIsInvalid_expectValidFalse() {

        ProgramEligibility eligibility = new ProgramEligibility();
        eligibility.setProgramId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertFalse(validateRoomResponse.isValid());
    }

    @Test
    public void validateRoomOffer_whenPromoCodeIsInvalid_expectValidFalse() {

        ProgramEligibility eligibility = new ProgramEligibility();
        eligibility.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        eligibility.setPromoCode("ABC");

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, true);

        assertFalse(validateRoomResponse.isValid());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndProgramIsNonMember_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getNonMemberProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberAndProgramIsNonMember_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getNonMemberProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndProgramIsTransient_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndPromoCodeIsTransient_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, true);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberAndProgramIsTransient_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberAndPromoCodeIsTransient_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, true);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndProgramIsCasino_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndPromoCodeIsCasino_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, true);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberAndProgramIsCasino_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberAndPromoCodeIsCasino_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, true);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeSapphireMemberAndProgramIsCasinoSapphire_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility()
                .getCasinoSapphireProgramSapphireMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeSapphireMemberAndProgramIsCasinoGold_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility()
                .getCasinoGoldProgramSapphireMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndProgramIsPatronPromo_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getPatronProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberPartOfPatronListAndProgramIsPatronPromo_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getPatronProgramListedMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberNotPartOfPatronListAndProgramIsPatronPromo_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getPatronProgramNonListedMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndProgramIsPerpetual_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getPerpetualProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberPartOfPOSegmentAndProgramIsPerpetual_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility()
                .getPerpetualProgramSameSegmentMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberNotPartOfPOSegmentAndProgramIsPerpetual_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility()
                .getPerpetualProgramDiffSegmentMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsTransientAndProgramIsMyvegas_expectEligibilityFalse() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getMyvegasProgramTransientUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenUserIsMlifeMemberAndProgramIsMyvegas_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getMyvegasProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
        assertNotNull(validateRoomResponse.getProgramId());
        assertNotNull(validateRoomResponse.getPropertyId());
    }

    @Test
    public void validateRoomOffer_whenEnableJwbCookieTrue_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, ServiceConstant.COOKIE,
                TestConstant.ENABLE_JWB + "=true");

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
    }

    @Test
    public void validateRoomOffer_whenEnableJwbHeaderTrue_expectEligibilityTrue() {

        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();

        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, TestConstant.ENABLE_JWB,
                "true");

        assertTrue(validateRoomResponse.isValid());
        assertEquals(eligibility.isExpectedResult(), validateRoomResponse.isEligible());
    }

    @Test
    public void validateRoomOffer_whenProgramIsPartOfHDEPackageAndPackageActive_expectHdePackageTrueAndValidTrue() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getHdePackageFlow();
        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);
        assertTrue(validateRoomResponse.isHdePackage());
        assertTrue(validateRoomResponse.isValid());
    }

    @Test
    public void validateRoomOffer_whenProgramIsNotPartOfHDEPackage_expectHdePackageFalse() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramTransientUser();
        RoomProgramValidateResponse validateRoomResponse = validateRoomOffer(eligibility, false);
        assertFalse(validateRoomResponse.isHdePackage());
    }

    private RoomProgramValidateResponse validateRoomOffer(ProgramEligibility eligibility, String headerKey,
            String headerValue) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.PROPERTY_ID, eligibility.getPropertyId());
        queryParams.add(TestConstant.PROGRAM_ID, eligibility.getProgramId());

        return realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(headerKey, headerValue);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> {
                    validateSuccessResponse(response, getApiDetails().getBaseServiceUrl());
                    validateResponseHeaders(response);
                }).flatMap(clientResponse -> clientResponse.bodyToMono(RoomProgramValidateResponse.class)).block();
    }

}
