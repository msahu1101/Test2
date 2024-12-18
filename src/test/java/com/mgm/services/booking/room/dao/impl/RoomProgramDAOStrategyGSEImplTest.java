package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;

@RunWith(MockitoJUnitRunner.class)
public class RoomProgramDAOStrategyGSEImplTest extends BaseRoomBookingTest {

    @Mock
    RoomProgramCacheService roomProgramCacheService;
    
    @Mock
    SecretsProperties secretProps;
    
    @Mock
    ApplicationProperties appProps;
    
    @InjectMocks
    RoomProgramDAOStrategyGSEImpl daoStrategyGSEImpl;

    /**
     * Test validateProgram program when bookBy date has passed.
     */
    @Test
    public void validateProgramExpiredTest() {

        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(-1));
        program.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        validateRequest.setCustomerId(-1);

        RoomProgramValidateResponse result = daoStrategyGSEImpl.validateProgram(validateRequest);

        assertTrue(result.isExpired());
        assertFalse(result.isValid());

    }

    /**
     * Test validateProgram and isProgramApplicable when an invalid programId is
     * passed.
     */
    @Test
    public void validateProgramInvalidTest() {
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(null);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");

        RoomProgramValidateResponse result = daoStrategyGSEImpl.validateProgram(validateRequest);

        assertFalse(result.isEligible());
        assertFalse(result.isValid());

    }

    /**
     * Test validateProgram and isProgramApplicable when an invalid promoCode is
     * passed.
     */
    @Test
    public void validatePromoCodeInvalidTest() {

        RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            public String getProgramByPromoCode(String propertyId, String promoCode) {
                return StringUtils.EMPTY;
            }
        };

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        validateRequest.setPromoCode("BOGO");

        RoomProgramValidateResponse result = dao.validateProgram(validateRequest);

        assertFalse(result.isEligible());
        assertFalse(result.isValid());

    }

    /**
     * Test validateProgram program when program is not active
     */
    @Test
    public void validateProgramNonActiveTest() {

        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(-1));
        program.setActiveFlag(false);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        validateRequest.setCustomerId(-1);

        RoomProgramValidateResponse result = daoStrategyGSEImpl.validateProgram(validateRequest);

        assertFalse(result.isExpired());
        assertFalse(result.isValid());

    }

    /**
     * Test validateProgramV2 when program is having patronPromoId
     */
    @Test
    public void test_validateProgramV2_withAProgramWithPatronPromoId_returnPatronProgramFlagTrue() {

        RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
                return true;
            }
            @Override
            protected RoomProgramValidateResponse checkEligibilityForPatronProgram(RoomProgramValidateRequest validateRequest,
                    RoomProgramValidateResponse validateResponse) {
                return validateResponse;
            }
        };

        dao.roomProgramCacheService = roomProgramCacheService;
        com.mgm.services.booking.room.model.phoenix.RoomProgram patronProgram = getProgram();
        patronProgram.setPatronPromoId("360322");

        when(roomProgramCacheService.getRoomProgram("74fd80f9-5831-4038-8949-9abacf39dc24")).thenReturn(patronProgram);

        RoomProgramValidateResponse result = dao.validateProgramV2(getValidateRequest("74fd80f9-5831-4038-8949-9abacf39dc24"));

        assertTrue(result.isPatronProgram());

    }

    /**
     * Test validateProgram and isProgramApplicable when a valid promoCode is passed
     * but not eligible.
     */
    @Test
    public void validatePromoCodeEligibleForGuestTest() {

        final RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            public String getProgramByPromoCode(String propertyId, String promoCode) {
                return "def64735-d34f-4daa-afd3-bcab36a318f0";
            }

            @Override
            protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
                return true;
            }
        };

        dao.roomProgramCacheService = roomProgramCacheService;
        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(5));
        program.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        validateRequest.setPromoCode("BOGO");
        validateRequest.setCustomerId(-1);

        RoomProgramValidateResponse result = dao.validateProgram(validateRequest);

        assertTrue(result.isEligible());
        assertTrue(result.isValid());

        assertTrue(dao.isProgramApplicable(validateRequest));

    }

    /**
     * Test validateProgram and isProgramApplicable when a valid and eligible
     * promoCode is passed.
     */
    @Test
    public void validatePromoCodeInEligibleForGuestTest() {

        final RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            public String getProgramByPromoCode(String propertyId, String promoCode) {
                return "def64735-d34f-4daa-afd3-bcab36a318f0";
            }

            @Override
            protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
                return false;
            }
        };

        dao.roomProgramCacheService = roomProgramCacheService;
        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(5));
        program.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        validateRequest.setPromoCode("BOGO");
        validateRequest.setCustomerId(-1);

        RoomProgramValidateResponse result = dao.validateProgram(validateRequest);

        assertFalse(result.isEligible());
        assertTrue(result.isValid());

        assertFalse(dao.isProgramApplicable(validateRequest));

    }

    /**
     * Test validateProgram and isProgramApplicable when a valid and eligible
     * programId is passed.
     */
    @Test
    public void validateProgramEligbleForGuestTest() {

        final RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            public String getProgramByPromoCode(String propertyId, String promoCode) {
                return "def64735-d34f-4daa-afd3-bcab36a318f0";
            }

            @Override
            protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
                return true;
            }
        };

        dao.roomProgramCacheService = roomProgramCacheService;
        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(5));
        program.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        validateRequest.setCustomerId(-1);

        RoomProgramValidateResponse result = dao.validateProgram(validateRequest);

        assertTrue(result.isEligible());
        assertTrue(result.isValid());

        assertTrue(dao.isProgramApplicable(validateRequest));
    }

    /**
     * Test validateProgram and isProgramApplicable when a valid promoCode is passed
     * but not eligible.
     */
    @Test
    public void validateProgramInEligbleForGuestTest() {

        final RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            public String getProgramByPromoCode(String propertyId, String promoCode) {
                return "def64735-d34f-4daa-afd3-bcab36a318f0";
            }

            @Override
            protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
                return false;
            }
        };

        dao.roomProgramCacheService = roomProgramCacheService;
        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(5));
        program.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        validateRequest.setCustomerId(-1);

        RoomProgramValidateResponse result = dao.validateProgram(validateRequest);

        assertFalse(result.isEligible());
        assertTrue(result.isValid());

        assertFalse(dao.isProgramApplicable(validateRequest));
    }

    /**
     * Test validateProgramV2 when program is not having patronPromoId
     */
    @Test
    public void test_validateProgramV2_withAProgramWithoutPatronPromoId_returnPatronProgramFlagFalse() {

        final RoomProgramDAOStrategyGSEImpl dao = new RoomProgramDAOStrategyGSEImpl() {
            @Override
            public String getProgramByPromoCode(String propertyId, String promoCode) {
                return "def64735-d34f-4daa-afd3-bcab36a318f0";
            }

            @Override
            protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
                return true;
            }
        };

        dao.roomProgramCacheService = roomProgramCacheService;
        when(roomProgramCacheService.getRoomProgram("a93b736d-0786-4eec-acd5-30ff706bab49")).thenReturn(getProgram());

        RoomProgramValidateResponse result = dao.validateProgramV2(getValidateRequest("a93b736d-0786-4eec-acd5-30ff706bab49"));
        assertFalse(result.isPatronProgram());

    }


    private RoomProgramValidateRequest getValidateRequest(String programId) {
        RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId(programId);
        validateRequest.setCustomerId(-1);
        return validateRequest;
    }


    private com.mgm.services.booking.room.model.phoenix.RoomProgram getProgram() {
        com.mgm.services.booking.room.model.phoenix.RoomProgram patronProgram = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        patronProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        patronProgram.setTravelPeriodStart(getFutureDate(10));
        patronProgram.setTravelPeriodEnd(getFutureDate(12));
        patronProgram.setBookBy(getFutureDate(5));
        patronProgram.setActiveFlag(true);
        return patronProgram;
    }
    
    private List<CustomerPromotion> getCustomerPromos() {
        
        List<CustomerPromotion> patronPromos = new ArrayList<>();
        CustomerPromotion promo1 = new CustomerPromotion();
        promo1.setPromoId("12345");
        CustomerPromotion promo2 = new CustomerPromotion();
        promo2.setPromoId("98765");
        patronPromos.add(promo1);
        patronPromos.add(promo2);
        
        return patronPromos;
    }
    
    @Test
    public void getRoomPrograms_whenChannelIsWebWithGseSegmentsEnabledAndAvailable_expectPatronProgramsLinkedByGseSegment() {
        RoomProgramsRequestDTO offersRequest = new RoomProgramsRequestDTO("mgmri", "web", null, 0, null, false, false);
        
        when(appProps.getEnableNewSegmentsKey()).thenReturn("rbs-enableNewSegments");
    	when(appProps.getRbsEnv()).thenReturn("q");
        when(secretProps.getSecretValue(Mockito.anyString())).thenReturn("false");
        
        List<RoomProgram> programs = new ArrayList<>();
        RoomProgram program1 = getProgram();
        program1.setId("program1");
        program1.setSegmentId("123456789");
        programs.add(program1);
        RoomProgram program2 = getProgram();
        program2.setId("program2");
        program2.setSegmentId("123456789");
        programs.add(program2);
        
        when(roomProgramCacheService.getProgramsByPatronPromoIds(Mockito.any())).thenReturn(programs);
        
        RoomProgramsResponseDTO offersResponse = daoStrategyGSEImpl.getRoomPrograms(offersRequest, getCustomerPromos(),null);
        
        assertEquals(1, offersResponse.getPatronPrograms().size());
        assertEquals("123456789", offersResponse.getPatronPrograms().get(0).getProgramId());
    }
    
    @Test
    public void getRoomPrograms_whenChannelIsWebWithGseSegmentsEnabledButNotAvailable_expectPatronProgramsNotLinkedByGseSegment() {
        RoomProgramsRequestDTO offersRequest = new RoomProgramsRequestDTO("mgmri", "web", null, 0, null, false, false);
        
        when(appProps.getEnableNewSegmentsKey()).thenReturn("rbs-enableNewSegments");
    	when(appProps.getRbsEnv()).thenReturn("q");
        when(secretProps.getSecretValue(Mockito.anyString())).thenReturn("false");
        
        List<RoomProgram> programs = new ArrayList<>();
        RoomProgram program1 = getProgram();
        program1.setId("program1");
        programs.add(program1);
        RoomProgram program2 = getProgram();
        program2.setId("program2");
        programs.add(program2);
        
        when(roomProgramCacheService.getProgramsByPatronPromoIds(Mockito.any())).thenReturn(programs);
        
        RoomProgramsResponseDTO offersResponse = daoStrategyGSEImpl.getRoomPrograms(offersRequest, getCustomerPromos(),null);
        
        assertEquals(2, offersResponse.getPatronPrograms().size());
        assertTrue(offersResponse.getPatronPrograms().stream().anyMatch(p -> p.getProgramId().equals("program1")));
        assertTrue(offersResponse.getPatronPrograms().stream().anyMatch(p -> p.getProgramId().equals("program2")));
    }
    
    @Test
    public void getRoomPrograms_whenChannelIsWebWithGseSegmentsEnabledButSegmentHas1Program_expectPatronProgramsNotLinkedByGseSegment() {
        RoomProgramsRequestDTO offersRequest = new RoomProgramsRequestDTO("mgmri", "web", null, 0, null, false, false);
        
        when(appProps.getEnableNewSegmentsKey()).thenReturn("rbs-enableNewSegments");
    	when(appProps.getRbsEnv()).thenReturn("q");
        when(secretProps.getSecretValue(Mockito.anyString())).thenReturn("false");
        
        List<RoomProgram> programs = new ArrayList<>();
        RoomProgram program1 = getProgram();
        program1.setId("program1");
        program1.setSegmentId("123456789");
        programs.add(program1);
        RoomProgram program2 = getProgram();
        program2.setId("program2");
        program2.setSegmentId("1234567890");
        programs.add(program2);
        
        when(roomProgramCacheService.getProgramsByPatronPromoIds(Mockito.any())).thenReturn(programs);
        
        RoomProgramsResponseDTO offersResponse = daoStrategyGSEImpl.getRoomPrograms(offersRequest, getCustomerPromos(),null);
        
        assertEquals(2, offersResponse.getPatronPrograms().size());
        assertTrue(offersResponse.getPatronPrograms().stream().anyMatch(p -> p.getProgramId().equals("program1")));
        assertTrue(offersResponse.getPatronPrograms().stream().anyMatch(p -> p.getProgramId().equals("program2")));
    }
    
    @Test
    public void getRoomPrograms_whenChannelIsWebWithNewSegmentsEnabled_expectPatronProgramsWithRatePlanCode() {
        RoomProgramsRequestDTO offersRequest = new RoomProgramsRequestDTO("mgmri", "web", null, 0, null, false, false);
        
        when(appProps.getEnableNewSegmentsKey()).thenReturn("rbs-enableNewSegments");
    	when(appProps.getRbsEnv()).thenReturn("q");
        when(secretProps.getSecretValue(Mockito.anyString())).thenReturn("false");
        
        List<RoomProgram> programs = new ArrayList<>();
        RoomProgram program1 = getProgram();
        program1.setId("program1");
        program1.setPromoCode("ZAAART");
        programs.add(program1);
        RoomProgram program2 = getProgram();
        program2.setId("program2");
        program2.setPromoCode("TMLIFE");
        programs.add(program2);
        
        when(roomProgramCacheService.getProgramsByPatronPromoIds(Mockito.any())).thenReturn(programs);
        
        RoomProgramsResponseDTO offersResponse = daoStrategyGSEImpl.getRoomPrograms(offersRequest, getCustomerPromos(),null);
        
        assertEquals(2, offersResponse.getPatronPrograms().size());
        assertTrue(offersResponse.getPatronPrograms().stream().anyMatch(p -> p.getRatePlanCode().equals("ZAAART")));
        assertTrue(offersResponse.getPatronPrograms().stream().anyMatch(p -> p.getRatePlanCode().equals("TMLIFE")));
    }
}
