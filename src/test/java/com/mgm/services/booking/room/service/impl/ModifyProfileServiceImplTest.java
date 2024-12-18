package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.ModifyReservationDAO;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.common.model.ProfileAddress;

/**
 * Unit test class for service methods in ModifyReservationService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ModifyProfileServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private ModifyReservationDAO modifyReservationDAO;

    @Mock
    private RoomReservationResponseMapper responseMapper;

    @InjectMocks
    private ModifyReservationServiceImpl modifyReservationServiceImpl;

    @Mock
    private FindReservationDAO findReservationDao;

    @Mock
    private ReservationServiceHelper reservationServiceHelper;

    private RoomReservationV2Response getUpdateProfileResponse(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, RoomReservationV2Response.class);
    }

    /**
     * Test updateProfileInfo method in the service class for success
     */
    @Test
    public void modifyProfileSuccessTest() {

        when(responseMapper.roomReservationModelToResponse(Mockito.any()))
                .thenReturn(getUpdateProfileResponse("/modifyreservationdao-premodifycharges-response.json"));

        UpdateProfileInfoRequest updateProfileInfoRequest = new UpdateProfileInfoRequest();
        UserProfileRequest profile = new UserProfileRequest();
        profile.setFirstName("John");
        profile.setEmailAddress1("johndoe777@nomail.com");
        ProfileAddress address = new ProfileAddress();
        List<ProfileAddress> addressList = new ArrayList<>();
        address.setCity("Las Vegas");
        address.setState("NV");
        addressList.add(address);
        profile.setAddresses(addressList);
        updateProfileInfoRequest.setUserProfile(profile);

        UpdateProfileInfoResponse response = modifyReservationServiceImpl.updateProfileInfo(updateProfileInfoRequest, null);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.getRoomReservation().getPropertyId());
        assertEquals("John", response.getRoomReservation().getProfile().getFirstName());
        assertEquals("johndoe777@nomail.com", response.getRoomReservation().getProfile().getEmailAddress1());
        assertEquals("Las Vegas", response.getRoomReservation().getProfile().getAddresses().get(0).getCity());
        assertEquals("NV", response.getRoomReservation().getProfile().getAddresses().get(0).getState());
    }

    /**
     * Test updateProfileInfo method in the service class for failure
     */
    @Test
    public void modifyProfileFailureTest() {

        when(responseMapper.roomReservationModelToResponse(Mockito.any()))
                .thenReturn(getUpdateProfileResponse("/modifyreservationdao-premodifycharges-response.json"));

        UpdateProfileInfoRequest updateProfileInfoRequest = new UpdateProfileInfoRequest();
        UserProfileRequest profile = new UserProfileRequest();
        profile.setFirstName("John2");
        profile.setEmailAddress1("johndoe555@nomail.com");
        profile.setMlifeNo(76303029);
        ProfileAddress address = new ProfileAddress();
        List<ProfileAddress> addressList = new ArrayList<>();
        address.setCity("New York");
        address.setState("NY");
        addressList.add(address);
        profile.setAddresses(addressList);
        updateProfileInfoRequest.setUserProfile(profile);

        UpdateProfileInfoResponse response = modifyReservationServiceImpl.updateProfileInfo(updateProfileInfoRequest, null);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.getRoomReservation().getPropertyId());
        assertNotEquals("MlifeNo not matching",76303029, response.getRoomReservation().getProfile().getMlifeNo());
        assertNotEquals("First name not matching", "John2", response.getRoomReservation().getProfile().getFirstName());
        assertNotEquals("Email not matching", "johndoe555@nomail.com", response.getRoomReservation().getProfile().getEmailAddress1());
        assertNotEquals("City not matching", "New York", response.getRoomReservation().getProfile().getAddresses().get(0).getCity());
        assertNotEquals("State not matching", "NY", response.getRoomReservation().getProfile().getAddresses().get(0).getState());
    }

}
