package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.service.CommonService;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.impl.CommonServiceImpl;
import com.mgm.services.common.exception.BusinessException;
import com.mgmresorts.aurora.common.ErrorCode;
import com.mgmresorts.aurora.common.ErrorType;
import com.mgmresorts.aurora.service.EAuroraException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.mgm.services.booking.room.model.reservation.RoomChargeItemType.ComponentCharge;
import static com.mgm.services.booking.room.model.reservation.RoomChargeItemType.RoomCharge;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RoomAndComponentChargesDaoStrategyGSEImplTest {

    @Mock
    private ComponentDAOStrategyGSEImpl componentDAOStrategyGSE ;

    @Mock
    private ReservationDAOStrategyGSEImpl reservationDAOStrategyGSEImpl;

    @Mock
    private RoomCacheService roomCacheService;

    @Mock
    private RoomProgramCacheService roomProgramCacheService;

    @Mock
    private CommonServiceImpl commonService;

    @Mock
    private RoomPriceDAOStrategyGSEImpl roomPriceDAOStrategyGSEImpl;

    @InjectMocks
    private RoomAndComponentChargesDAOStrategyGSEImpl gseDAOImpl;

    static Logger logger = LoggerFactory.getLogger(RoomAndComponentChargesDaoStrategyGSEImplTest.class);

    @Test
    public void calculateRoomAndComponentChargesTest() {

        try {
            Date roomPriceDate = new Date();
            RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
            List<RoomRequest> roomRequestList = roomRequestListCreate();
            Room room = getRoom();
            RoomProgram roomProgram = getRoomProgram();
            Date tempDate = new Date();
            RoomReservation updateRoomResvResp = new RoomReservation();

            RoomChargesAndTaxes roomChargesAndTaxes = mockRoomChargesAndTaxes(tempDate);
            updateRoomResvResp.setChargesAndTaxesCalc(roomChargesAndTaxes);

            List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
            when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
            doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
            when(roomProgramCacheService.getRoomProgram(Mockito.any())).thenReturn(roomProgram);
            when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
            when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
            when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenReturn(updateRoomResvResp);
            when(reservationDAOStrategyGSEImpl.saveRoomReservation(Mockito.any())).thenReturn(roomReservation);
            RoomReservation response = gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getAvailableComponents().get(0));

        } catch (Exception e) {
            Assert.fail("calculateRoomAndComponentChargesTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    private static RoomChargesAndTaxes mockRoomChargesAndTaxes(Date tempDate) {
        RoomChargesAndTaxes roomChargesAndTaxes = new RoomChargesAndTaxes();
        List<RoomChargeItem> listRoomChargeItem2 = new ArrayList<>();
        RoomChargeItem roomChargeItem2 = new RoomChargeItem();
        roomChargeItem2.setDate(tempDate);
        List<ItemizedChargeItem> itemizedChargeItems2 =new ArrayList<>();
        ItemizedChargeItem itemizedChargeItem2 = new ItemizedChargeItem();
        itemizedChargeItem2.setAmount(10);
        itemizedChargeItem2.setItemType(RoomCharge);
        itemizedChargeItem2.setItem("testRoom");
        itemizedChargeItems2.add(itemizedChargeItem2);
        ItemizedChargeItem itemizedChargeItem3 = new ItemizedChargeItem();
        itemizedChargeItem3.setAmount(6);
        itemizedChargeItem3.setItemType(ComponentCharge);
        itemizedChargeItem3.setItem("testComponentName");
        itemizedChargeItems2.add(itemizedChargeItem3);
        roomChargeItem2.setItemized(itemizedChargeItems2);
        listRoomChargeItem2.add(roomChargeItem2);
        roomChargesAndTaxes.setCharges(listRoomChargeItem2);
        roomChargesAndTaxes.setTaxesAndFees(listRoomChargeItem2);
        return roomChargesAndTaxes;
    }

    @Test
    public void calculateRoomAndComponentChargesErrorTest() {

        try {
            Date roomPriceDate = new Date();
            RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
            List<RoomRequest> roomRequestList = roomRequestListCreate();
            Room room = getRoom();
            List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,-1);
            doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
            when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
            when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
            when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);

            Assertions.assertThrows(BusinessException.class, () -> {
                gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            });

        } catch (Exception e) {
            Assert.fail("calculateRoomAndComponentChargesTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    @Test
    public void calculateRoomAndComponentChargesUpdateRoomReservationPricingRuleErrorTest() {
        Date roomPriceDate = new Date();
        RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
        List<RoomRequest> roomRequestList = roomRequestListCreate();
        Room room = getRoom();
        List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
        when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
        when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
        EAuroraException mockException = new EAuroraException(ErrorType.System,
                ErrorCode.InvalidRoomPricingRuleId, "description", "extInfo");
        when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenThrow(mockException);

        // updateRoomReservation
        try {
            gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.fail("Expected Exception was not thrown - No Exception.");
        } catch(BusinessException bex) {
            if (bex.getErrorCode() != com.mgm.services.common.exception.ErrorCode.INVALID_ROOM_PRICING_RULE_ID) {
                Assert.fail("Expected Exception was not thrown - Incorrect BusinessException.");
            }
        } catch (Exception ex) {
            Assert.fail("Expected Exception was not thrown - Incorrect Exception.");
        }
    }

    @Test
    public void calculateRoomAndComponentChargesSaveRoomReservationPricingRuleErrorTest() {
        // set mocks
        Date roomPriceDate = new Date();
        RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
        List<RoomRequest> roomRequestList = roomRequestListCreate();
        Room room = getRoom();
        List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
        when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
        when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
        EAuroraException mockException = new EAuroraException(ErrorType.System,
                ErrorCode.InvalidRoomPricingRuleId, "description", "extInfo");
        RoomReservation mockUpdatedRoomReservation = roomReservationObjectCreate(roomPriceDate);
        mockUpdatedRoomReservation.setChargesAndTaxesCalc(mockRoomChargesAndTaxes(roomPriceDate));
        when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenReturn(mockUpdatedRoomReservation);
        when(reservationDAOStrategyGSEImpl.saveRoomReservation(Mockito.any())).thenThrow(mockException);
        when(roomProgramCacheService.isProgramPO(Mockito.any())).thenReturn(true);

        // execute test
        try {
            gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.fail("Expected Exception was not thrown - No Exception.");
        } catch(BusinessException bex) {
            if (bex.getErrorCode() != com.mgm.services.common.exception.ErrorCode.INVALID_ROOM_PRICING_RULE_ID) {
                Assert.fail("Expected Exception was not thrown - Incorrect BusinessException.");
            }
        } catch (Exception ex) {
            Assert.fail("Expected Exception was not thrown - Incorrect Exception.");
        }
    }

    @Test
    public void calculateRoomAndComponentChargesUpdateRoomReservationMalformedDTOErrorTest() {
        Date roomPriceDate = new Date();
        RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
        List<RoomRequest> roomRequestList = roomRequestListCreate();
        Room room = getRoom();
        List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
        when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
        when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
        EAuroraException mockException = new EAuroraException(ErrorType.System,
                ErrorCode.MalformedDTO, "request not valid", "extInfo");
        when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenThrow(mockException);

        // updateRoomReservation
        try {
            gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.fail("Expected Exception was not thrown - No Exception.");
        } catch(BusinessException bex) {
            if (bex.getErrorCode() != com.mgm.services.common.exception.ErrorCode.INVALID_REQUEST_PARAMS) {
                Assert.fail("Expected Exception was not thrown - Incorrect BusinessException.");
            }
        } catch (Exception ex) {
            Assert.fail("Expected Exception was not thrown - Incorrect Exception.");
        }
    }

    @Test
    public void calculateRoomAndComponentChargesSaveRoomReservationMalformedDTOErrorTest() {
        // set mocks
        Date roomPriceDate = new Date();
        RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
        List<RoomRequest> roomRequestList = roomRequestListCreate();
        Room room = getRoom();
        List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
        when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
        when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
        EAuroraException mockException = new EAuroraException(ErrorType.System,
                ErrorCode.MalformedDTO, "request not valid", "extInfo");
        RoomReservation mockUpdatedRoomReservation = roomReservationObjectCreate(roomPriceDate);
        mockUpdatedRoomReservation.setChargesAndTaxesCalc(mockRoomChargesAndTaxes(roomPriceDate));
        when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenReturn(mockUpdatedRoomReservation);
        when(reservationDAOStrategyGSEImpl.saveRoomReservation(Mockito.any())).thenThrow(mockException);
        when(roomProgramCacheService.isProgramPO(Mockito.any())).thenReturn(true);

        // execute test
        try {
            gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.fail("Expected Exception was not thrown - No Exception.");
        } catch(BusinessException bex) {
            if (bex.getErrorCode() != com.mgm.services.common.exception.ErrorCode.INVALID_REQUEST_PARAMS) {
                Assert.fail("Expected Exception was not thrown - Incorrect BusinessException.");
            }
        } catch (Exception ex) {
            Assert.fail("Expected Exception was not thrown - Incorrect Exception.");
        }
    }

    @Test
    public void calculateRoomAndComponentChargesUpdateRoomReservationBlacklistErrorTest() {
        Date roomPriceDate = new Date();
        RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
        List<RoomRequest> roomRequestList = roomRequestListCreate();
        Room room = getRoom();
        List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
        when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
        when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
        EAuroraException mockException = new EAuroraException(ErrorType.System,
                ErrorCode.BlacklistReservation, "Blacklisted", "extInfo");
        when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenThrow(mockException);

        // updateRoomReservation
        try {
            gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.fail("Expected Exception was not thrown - No Exception.");
        } catch(BusinessException bex) {
            if (bex.getErrorCode() != com.mgm.services.common.exception.ErrorCode.RESERVATION_BLACKLISTED) {
                Assert.fail("Expected Exception was not thrown - Incorrect BusinessException.");
            }
        } catch (Exception ex) {
            Assert.fail("Expected Exception was not thrown - Incorrect Exception.");
        }
    }

    @Test
    public void calculateRoomAndComponentChargesSaveRoomReservationBlacklistErrorTest() {
        // set mocks
        Date roomPriceDate = new Date();
        RoomReservation roomReservation = roomReservationObjectCreate(roomPriceDate);
        List<RoomRequest> roomRequestList = roomRequestListCreate();
        Room room = getRoom();
        List<AuroraPriceResponse> auroraPriceResponses = auroraPriceResponsesCreate(roomPriceDate,100);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        when(roomCacheService.getRoom(Mockito.any())).thenReturn(room);
        when(roomPriceDAOStrategyGSEImpl.getRoomPrices(Mockito.any())).thenReturn(auroraPriceResponses);
        when(componentDAOStrategyGSE.getRoomComponentAvailability(Mockito.any())).thenReturn(roomRequestList);
        EAuroraException mockException = new EAuroraException(ErrorType.System,
                ErrorCode.BlacklistReservation, "Blacklisted", "extInfo");
        RoomReservation mockUpdatedRoomReservation = roomReservationObjectCreate(roomPriceDate);
        mockUpdatedRoomReservation.setChargesAndTaxesCalc(mockRoomChargesAndTaxes(roomPriceDate));
        when(reservationDAOStrategyGSEImpl.updateRoomReservation(Mockito.any())).thenReturn(mockUpdatedRoomReservation);
        when(reservationDAOStrategyGSEImpl.saveRoomReservation(Mockito.any())).thenThrow(mockException);
        when(roomProgramCacheService.isProgramPO(Mockito.any())).thenReturn(true);

        // execute test
        try {
            gseDAOImpl.calculateRoomAndComponentCharges(roomReservation);
            Assert.fail("Expected Exception was not thrown - No Exception.");
        } catch(BusinessException bex) {
            if (bex.getErrorCode() != com.mgm.services.common.exception.ErrorCode.RESERVATION_BLACKLISTED) {
                Assert.fail("Expected Exception was not thrown - Incorrect BusinessException.");
            }
        } catch (Exception ex) {
            Assert.fail("Expected Exception was not thrown - Incorrect Exception.");
        }
    }

    private RoomReservation roomReservationObjectCreate(Date roomPriceDate) {
        RoomReservation roomReservation = new RoomReservation();
        ReservationProfile reservationProfile = new ReservationProfile();
        reservationProfile.setId(1234);
        reservationProfile.setFirstName("TestName");
        roomReservation.setProfile(reservationProfile);
        Calendar cal = Calendar.getInstance();
        cal.setTime(roomPriceDate);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        roomReservation.setCheckInDate(roomPriceDate);
        roomReservation.setCheckOutDate(cal.getTime());
        roomReservation.setPropertyId("e2704b04-d515-45b0-8afd-4fa1424ff0a8");
        roomReservation.setRoomTypeId("589b278a-6c54-41e1-a558-c4c0f83c4d71");
        roomReservation.setProgramId("dba7c831-d6cb-45fb-bef3-fcfba66d2b53");
        roomReservation.setSource("ICE");
        roomReservation.setPerpetualPricing(true);
        roomReservation.setNumAdults(2);
        roomReservation.setNumChildren(2);
        roomReservation.setNumRooms(1);
        return roomReservation;
    }

    private List<RoomRequest> roomRequestListCreate() {
        List<RoomRequest> roomRequestList = new ArrayList<>();
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setId("testComponentID");
        roomRequest.setShortDescription("testDescription");
        roomRequest.setPricingApplied("NIGHTLY");
        roomRequestList.add(roomRequest);
        return roomRequestList;
    }

    private Room getRoom() {
        Room room = new Room();
        List<RoomComponent> components = new ArrayList<>();
        RoomComponent roomComponent = new RoomComponent();
        roomComponent.setComponentType("Component");
        roomComponent.setId("testComponentID");
        roomComponent.setName("testComponentName");
        components.add(roomComponent);
        room.setComponents(components);
        room.setId("589b278a-6c54-41e1-a558-c4c0f83c4d71");
        return room;
    }

    private RoomProgram getRoomProgram() {
        RoomProgram roomProgram = new RoomProgram();
        roomProgram.setId("dba7c831-d6cb-45fb-bef3-fcfba66d2b53");
        roomProgram.setSegmentFrom(2);
        return roomProgram;
    }

    private List<AuroraPriceResponse> auroraPriceResponsesCreate(Date roomPriceDate, int price) {
        List<AuroraPriceResponse> auroraPriceResponses = new ArrayList<>();
        AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
        auroraPriceResponse.setDate(roomPriceDate);
        auroraPriceResponse.setDiscountedPrice(price);
        auroraPriceResponse.setComp(false);
        auroraPriceResponse.setResortFee(45);
        auroraPriceResponse.setBasePrice(80);
        auroraPriceResponse.setProgramId("dba7c831-d6cb-45fb-bef3-fcfba66d2b53");
        auroraPriceResponses.add(auroraPriceResponse);
        return auroraPriceResponses;
    }
}
