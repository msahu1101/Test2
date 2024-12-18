package com.mgm.services.booking.room.model.response;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;

public class RoomAvailabilityV2ResponseTest extends BaseRoomBookingTest {

    @Test
    public void compareToTest() {

        File requestFile = new File(getClass().getResource("/availability-response.json").getPath());
        Set<RoomAvailabilityV2Response> responseSet = convert(requestFile,
                mapper.getTypeFactory().constructCollectionType(Set.class, RoomAvailabilityV2Response.class));
        
        Set<RoomAvailabilityV2Response> newResponseSet = new TreeSet<>();
        
        responseSet.stream().forEach(response -> {
            newResponseSet.add(response);
        });

        Set<String> roomTypeIds = new LinkedHashSet<>();
        newResponseSet.stream().forEach(newResponse -> {
            roomTypeIds.add(newResponse.getRoomTypeId());
        });
        
        String[] actualArray = roomTypeIds.toArray(new String[8]);
        String[] expectedArray = { "9a77cc5b-f06e-4e73-b847-24433f623f23", "ddeca6cb-894d-4325-9c26-782b4f11e39c",
                "5a8e6554-34d4-473a-b082-27c5b6c50a0a", "766c066d-d28e-40cc-8ac3-926bc1f309d5",
                "2369dc93-7336-46bd-b61b-5d46f8133bc7", "e93ab7f0-f086-486a-95b8-26712d14615e",
                "5873c8fe-d110-4628-a155-f2adbdf842d9", "b2135b7f-7172-4d53-b39d-217de6f5c970" };
       // assertArrayEquals(TestConstant.INCORRECT_SORT, roomTypeIds.toArray(actualArray), expectedArray);
    }
    
}
