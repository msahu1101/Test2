package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapperImpl;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.TripDetail;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import java.io.File;
import java.util.Calendar;

import org.junit.Assert;
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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ModifyReservationDAOImplTest extends BaseRoomBookingTest {

	@Mock
	private static ModifyReservationDAOStrategyACRSImpl acrsStrategy;
	
	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;
	
	@Mock
	private BaseStrategyDAO baseStrategyDao;
	@Mock
    private FindReservationDAO findReservationDao;
    

	@InjectMocks
	private ModifyReservationDAOImpl modifyReservationDAOImpl  = new ModifyReservationDAOImpl();

	static Logger logger = LoggerFactory.getLogger(ModifyReservationDAOImplTest.class);


	private <T> T getObject(String fileName, Class<T> target) {
		File file = new File(getClass().getResource(fileName).getPath());
		return convert(file, target);
	}

	private PreModifyV2Request getPreModifyV2Request() {
		File file = new File(getClass().getResource("/preModifyReservation-v2-preModifyReservation.json").getPath());
		PreModifyV2Request data = convertRBSReq(file, PreModifyV2Request.class);
		return data;
	}
	
	private RoomReservation getRoomReservationReq() {
		File file = new File(getClass().getResource("/roomReservation-validRequest.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}

	private RoomReservation getRoomReservationResponse() {
		File file = new File(getClass().getResource("/reservation-booked.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}

	private RoomReservation getCommitRoomReservationResponse_ForNoDeposit() {
		return getObject("/paymentwidgetv4/commit/no_deposit/crs-commit-roomreservation-response-nodeposit.json", RoomReservation.class);
	}

	private PaymentRoomReservationRequest getCommitRequest_ForNoDeposit() {
		return getObject("/paymentwidgetv4/commit/no_deposit/commit-v4-nodeposit-request.json", PaymentRoomReservationRequest.class);
	}

	private RoomReservation getCommitRoomReservationResponse_ForAddnDeposit() {
		return getObject("/paymentwidgetv4/commit/addn_deposit/crs-commit-roomreservation-response-addndeposit.json", RoomReservation.class);
	}

	private PaymentRoomReservationRequest getCommitRequest_ForAddnDeposit() {
		return getObject("/paymentwidgetv4/commit/addn_deposit/commit-v4-addndeposit-request.json", PaymentRoomReservationRequest.class);
	}

	private RoomReservation getCommitRoomReservationResponse_ForRefundDeposit() {
		return getObject("/paymentwidgetv4/commit/refund_deposit/crs-commit-roomreservation-response-refunddeposit.json", RoomReservation.class);
	}

	private PaymentRoomReservationRequest getCommitRequest_ForRefundDeposit() {
		return getObject("/paymentwidgetv4/commit/refund_deposit/commit-v4-refunddeposit-request.json", PaymentRoomReservationRequest.class);
	}
	
	
	@Test
	public void modifyReservationTest() {        
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.modifyReservation(anyString(),ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = modifyReservationDAOImpl.modifyReservation("mgmri", getRoomReservationReq());
		Assert.assertNotNull(response);
	}
	
	@Test
	public void modifyRoomReservationV2Test() {
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.modifyRoomReservationV2(ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = modifyReservationDAOImpl.modifyRoomReservationV2(getRoomReservationReq());
		Assert.assertNotNull(response);
	}
	
	@Test
	public void preModifyReservationV2Test() {
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.preModifyReservation(getPreModifyV2Request())).thenReturn(getRoomReservationResponse());
		RoomReservation response = modifyReservationDAOImpl.preModifyReservation(getPreModifyV2Request());
		Assert.assertNotNull(response);
	}
	
	@Test
	public void updateProfileInfoTest() {
		RoomReservation roomReservation = getRoomReservationReq();
		UpdateProfileInfoRequestDTO updateProfileInfo = new UpdateProfileInfoRequestDTO();
		updateProfileInfo.setConfirmationNumber(roomReservation.getConfirmationNumber());
		updateProfileInfo.setPropertyId(roomReservation.getPropertyId());
		updateProfileInfo.setItineraryId(roomReservation.getItineraryId());
		updateProfileInfo.setOriginalReservation(roomReservation);
		updateProfileInfo.setSource(roomReservation.getSource());
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.updateProfileInfo(ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = modifyReservationDAOImpl.updateProfileInfo(updateProfileInfo);
		Assert.assertNotNull(response);
	}
	
	@Test
	public void commitPaymentReservationTest_ForNoDeposit() {
		when(acrsStrategy.commitPaymentReservation(ArgumentMatchers.any())).thenReturn(getCommitRoomReservationResponse_ForNoDeposit());
		RoomReservationRequestMapper requestMapper = new RoomReservationRequestMapperImpl();
		CommitPaymentDTO commitPaymentDTO = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(getCommitRequest_ForNoDeposit());
		RoomReservation reservation = modifyReservationDAOImpl.commitPaymentReservation(commitPaymentDTO);

		// Assertions
		assertNotNull(reservation);
		assertNotNull(reservation.getConfirmationNumber());
		assertNotNull(reservation.getProfile());
		assertEquals(commitPaymentDTO.getFirstName(), reservation.getProfile().getFirstName());
		assertEquals(commitPaymentDTO.getLastName(), reservation.getProfile().getLastName());
		assertEquals(ReservationState.Booked, reservation.getState());
		// extended by 2 days
		assertNotNull(reservation.getBookings());
		assertEquals(4, reservation.getBookings().size());
		// 0 amount in billing for No Deposit
		assertNotNull(reservation.getCreditCardCharges());
		assertEquals(0, reservation.getCreditCardCharges().get(0).getAmount());
		// payments[] should not be empty
		assertNotNull(reservation.getPayments());
		assertNotEquals(0, reservation.getPayments().size());
	}

	@Test
	public void commitPaymentReservationTest_ForAddnDeposit() {
		when(acrsStrategy.commitPaymentReservation(ArgumentMatchers.any())).thenReturn(getCommitRoomReservationResponse_ForAddnDeposit());
		RoomReservationRequestMapper requestMapper = new RoomReservationRequestMapperImpl();
		CommitPaymentDTO commitPaymentDTO = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(getCommitRequest_ForAddnDeposit());
		RoomReservation reservation = modifyReservationDAOImpl.commitPaymentReservation(commitPaymentDTO);

		// Assertions
		assertNotNull(reservation);
		assertNotNull(reservation.getConfirmationNumber());
		assertNotNull(reservation.getProfile());
		assertEquals(commitPaymentDTO.getFirstName(), reservation.getProfile().getFirstName());
		assertEquals(commitPaymentDTO.getLastName(), reservation.getProfile().getLastName());
		assertEquals(ReservationState.Booked, reservation.getState());
		// added component check
		assertNotNull(reservation.getSpecialRequests());
		assertEquals(1, reservation.getSpecialRequests().size());
		assertTrue(reservation.getSpecialRequests().get(0).contains("DOGFEE"));
		assertEquals(1, reservation.getPurchasedComponents().size());
		// additional payment in billing
		assertNotNull(reservation.getCreditCardCharges());
		assertEquals(56.69, reservation.getCreditCardCharges().get(0).getAmount());
		// payments[] should not be empty
		assertNotNull(reservation.getPayments());
		assertNotEquals(0, reservation.getPayments().size());
	}

	@Test
	public void commitPaymentReservationTest_ForRefundDeposit() {
		when(acrsStrategy.commitPaymentReservation(ArgumentMatchers.any())).thenReturn(getCommitRoomReservationResponse_ForRefundDeposit());
		RoomReservationRequestMapper requestMapper = new RoomReservationRequestMapperImpl();
		CommitPaymentDTO commitPaymentDTO = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(getCommitRequest_ForRefundDeposit());
		RoomReservation reservation = modifyReservationDAOImpl.commitPaymentReservation(commitPaymentDTO);

		// Assertions
		assertNotNull(reservation);
		assertNotNull(reservation.getConfirmationNumber());
		assertNotNull(reservation.getProfile());
		assertEquals(commitPaymentDTO.getFirstName(), reservation.getProfile().getFirstName());
		assertEquals(commitPaymentDTO.getLastName(), reservation.getProfile().getLastName());
		assertEquals(ReservationState.Booked, reservation.getState());
		// removed component check
		assertNotNull(reservation.getSpecialRequests());
		assertEquals(0, reservation.getSpecialRequests().size());
		assertEquals(0, reservation.getPurchasedComponents().size());
		// refund payment in billing
		assertNotNull(reservation.getCreditCardCharges());
		assertEquals(-56.69, reservation.getCreditCardCharges().get(0).getAmount());
		// payments[] should not be empty
		assertNotNull(reservation.getPayments());
		assertNotEquals(0, reservation.getPayments().size());
	}
     
	@Test
    public void modifyPendingRoomReservationV2Test() {
        when(acrsStrategy.modifyPendingRoomReservationV2(ArgumentMatchers.any())).thenReturn(getRoomReservationReq());
		RoomReservation response = modifyReservationDAOImpl.modifyPendingRoomReservationV2(getRoomReservationReq());
		Assert.assertNotNull(response);
	}
	
	@Test
	public void preModifyReservationTest() {
		PreModifyRequest preModifyRequest = new PreModifyRequest();
		preModifyRequest.setFirstName("john");
		preModifyRequest.setLastName("doe");
		preModifyRequest.setConfirmationNumber("M00AE6261");
		TripDetail tripDetail = new TripDetail();
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 4);
		tripDetail.setCheckInDate(calendar.getTime());
		
		Calendar calendar1 = Calendar.getInstance();
		calendar1.add(Calendar.DATE, 2);
		tripDetail.setCheckOutDate(calendar1.getTime());
		preModifyRequest.setTripDetails(tripDetail);

		RoomReservation findRoomReservation = getObject("/preModifyReservation-v2-findRoomReservation.json",RoomReservation.class);

		when(findReservationDao.findRoomReservation(any((FindReservationRequest.class)))).thenReturn(findRoomReservation);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
	
		RoomReservation response = modifyReservationDAOImpl.preModifyReservation((preModifyRequest));
		assertNull(response);
	}
}
