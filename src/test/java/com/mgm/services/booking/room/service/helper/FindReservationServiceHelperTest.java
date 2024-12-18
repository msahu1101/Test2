package com.mgm.services.booking.room.service.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.model.ocrs.GuestCountItem;
import com.mgm.services.booking.room.model.ocrs.GuestCounts;
import com.mgm.services.booking.room.model.ocrs.IndividualName;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.model.ocrs.PhoneNumber;
import com.mgm.services.booking.room.model.ocrs.PhoneNumbers;
import com.mgm.services.booking.room.model.ocrs.PostalAddress;
import com.mgm.services.booking.room.model.ocrs.PostalAddresses;
import com.mgm.services.booking.room.model.ocrs.Profile;
import com.mgm.services.booking.room.model.ocrs.ResGuest;
import com.mgm.services.booking.room.model.ocrs.ResGuests;
import com.mgm.services.booking.room.model.ocrs.ResProfile;
import com.mgm.services.booking.room.model.ocrs.ResProfiles;
import com.mgm.services.booking.room.model.ocrs.ReservationReference;
import com.mgm.services.booking.room.model.ocrs.ReservationReferences;
import com.mgm.services.booking.room.model.ocrs.RoomStay;
import com.mgm.services.booking.room.model.ocrs.RoomStays;
import com.mgm.services.booking.room.model.ocrs.SelectedMembership;
import com.mgm.services.booking.room.model.ocrs.SelectedMemberships;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.util.TokenValidationUtil;

class FindReservationServiceHelperTest {

	@Mock
	private  TokenValidationUtil tokenValidationUtil;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private FindReservationServiceHelper findReservationServiceHelper;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		HttpServletRequest request = mock(HttpServletRequest.class);
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);
	}

	@Test
	public void testIsLoyaltyReservation_WhenMlifeNumberIsPositive_ReturnsTrue() {
		RoomReservation roomReservation = new RoomReservation();
		ReservationProfile pf = new ReservationProfile();
		pf.setMlifeNo(0);
		roomReservation.setProfile(pf);
		roomReservation.getProfile().setMlifeNo(123);

		boolean result = findReservationServiceHelper.isLoyaltyReservation(roomReservation);

		assertTrue(result);
	}

	@Test
	public void testIsLoyaltyReservation_WhenMlifeNumberIsZero_ReturnsFalse() {
		RoomReservation roomReservation = new RoomReservation();
		ReservationProfile pf = new ReservationProfile();
		pf.setMlifeNo(0);
		roomReservation.setProfile(pf);
		roomReservation.getProfile().setMlifeNo(0);

		boolean result = findReservationServiceHelper.isLoyaltyReservation(roomReservation);

		assertFalse(result);
	}

	@Test
	public void testUpdateRoomReservation() {
		OcrsReservation ocrsReservation = new OcrsReservation();
		List<RoomStay> roomStayslist = new ArrayList<>();
		RoomStay roomStay = new RoomStay();
		roomStay.setRoomInventoryCode("678");
		roomStay.setReservationStatusType("Guest");
		roomStayslist.add(roomStay);

		List<GuestCountItem> guestCount = new ArrayList<>();
		GuestCountItem GuestCountItem = new GuestCountItem();
		GuestCountItem.setMfCount(5);
		GuestCountItem.setAgeQualifyingCode("ADULT");
		guestCount.add(GuestCountItem);

		GuestCounts guestCounts = new GuestCounts();
		guestCounts.setGuestCount(guestCount);

		roomStay.setGuestCounts(guestCounts);

		RoomStays roomStays = new RoomStays();
		roomStays.setRoomStay(roomStayslist);

		ocrsReservation.setReservationID("123");
		ocrsReservation.setOriginalBookingDate(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
		ocrsReservation.setRoomStays(roomStays);
		ResGuests resGuests = new ResGuests();
		List<ResGuest> resGuestlist = new ArrayList<>();
		ResGuest resGuest = new ResGuest();
		resGuest.setConfirmationID("654");
		resGuest.setReservationID("123");
		resGuest.setProfileRPHs("123");

		ReservationReferences reservationReferences = new ReservationReferences();
		List<ReservationReference> reservationReferencelist = new ArrayList<>();
		ReservationReference reservationReference = new ReservationReference();
		reservationReference.setReferenceNumber("456");
		reservationReference.setType("Friend");
		reservationReferencelist.add(reservationReference);
		reservationReferences.setReservationReference(reservationReferencelist);

		resGuest.setReservationReferences(reservationReferences);

		resGuestlist.add(resGuest);
		resGuests.setResGuest(resGuestlist);

		ResProfiles resProfiles = new ResProfiles();

		List<ResProfile> resProfilelist = new ArrayList<>();
		ResProfile resProfile = new ResProfile();
		Profile profile = new Profile();
		profile.setProfileType("guest");
		PostalAddresses postalAddresses = new PostalAddresses();
		List<PostalAddress> postalAddresslist = new ArrayList<>();
		PostalAddress postalAddress = new PostalAddress();
		postalAddress.setMfPrimaryYN("Y");
		postalAddress.setAddress1("los-angeles");
		postalAddress.setAddress2("California");
		postalAddress.setCity("Los Angeles");
		postalAddress.setCountryCode("138");
		postalAddress.setPostalCode("678545");
		postalAddress.setStateCode("45637");
		postalAddresslist.add(postalAddress);
		postalAddresses.setPostalAddress(postalAddresslist);
		profile.setPostalAddresses(postalAddresses);
		PhoneNumbers phoneNumbers = new PhoneNumbers();
		List<PhoneNumber> phoneNumberlist = new ArrayList<>();
		PhoneNumber phoneNumber = new PhoneNumber();
		phoneNumber.setPhoneNumber("9931290752");
		phoneNumber.setMfPrimaryYN("Y");
		phoneNumberlist.add(phoneNumber);
		phoneNumbers.setPhoneNumber(phoneNumberlist);
		profile.setPhoneNumbers(phoneNumbers);
		IndividualName individualName = new IndividualName();
		individualName.setNameFirst("Joe");
		individualName.setNamePrefix("Miss");
		individualName.setNameSur("Doe");
		profile.setMfResortProfileID("1");
		profile.setIndividualName(individualName);        
		resProfile.setProfile(profile);
		resProfile.setResProfileRPH(123);
		resProfilelist.add(resProfile);
		resProfiles.setResProfile(resProfilelist);
		ocrsReservation.setResProfiles(resProfiles);
		ocrsReservation.setResGuests(resGuests);
		SelectedMemberships selectedMemberships = new SelectedMemberships();
		List<SelectedMembership> selectedMembershiplist = new ArrayList<>();
		SelectedMembership selectedMembership= new SelectedMembership();
		selectedMembership.setAccountID("878");
		selectedMembership.setProgramCode("PC");
		selectedMembershiplist.add(selectedMembership);
		selectedMemberships.setSelectedMembership(selectedMembershiplist);
		ocrsReservation.setSelectedMemberships(selectedMemberships);

		RoomReservation roomReservation = new RoomReservation();
		roomReservation.setOperaConfirmationNumber("456");

		findReservationServiceHelper.updateRoomReservation(ocrsReservation, roomReservation, "456");

		assertEquals("123", roomReservation.getOperaConfirmationNumber());
	}

	@Test
	public void testIsFirstNameLastNameMatching() {
		RoomReservation roomReservation = new RoomReservation();
		ReservationProfile pf = new ReservationProfile();
		pf.setFirstName("John");
		pf.setLastName("Doe");
		roomReservation.setProfile(pf);

		boolean result = findReservationServiceHelper.isFirstNameLastNameMatching("John", "Doe", roomReservation);

		assertTrue(result);
	}
}
