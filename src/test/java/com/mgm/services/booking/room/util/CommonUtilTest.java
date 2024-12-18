package com.mgm.services.booking.room.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.RoomCartItem;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.model.CartItem;
import com.mgm.services.common.model.ServicesSession;

public class CommonUtilTest extends BaseRoomBookingTest {

    @Test
    public void testGetRoomCartItems() {
        // Cart item for mgmgrand property
        RoomCartItem item1 = new RoomCartItem();
        RoomReservation reservation1 = new RoomReservation();
        reservation1.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        item1.setReservation(reservation1);

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(item1);

        assertEquals("All cart items should be returned when source is empty", 1,
                CommonUtil.getRoomCartItems(cartItems, null).size());
        assertEquals("Only mgmgrand cart items should be returned when source is UUID for mgmgrand", 1,
                CommonUtil.getRoomCartItems(cartItems, "66964e2b-2550-4476-84c3-1a4c0c5c067f").size());
        assertEquals("No items should be returned since there's no cart items for bellagio", 0,
                CommonUtil.getRoomCartItems(cartItems, "44e610ab-c209-4232-8bb4-51f7b9b13a75").size());
        assertEquals("All cart items should be returned when source is non-uuid", 1,
                CommonUtil.getRoomCartItems(cartItems, "mgmri").size());

    }


    @Test
    public void testIsEligibleForAccountCreation() {
        // Cart item for mgmgrand property in non JWB
        RoomCartItem item1 = new RoomCartItem();
        RoomReservation reservation1 = new RoomReservation();
        reservation1.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        item1.setReservation(reservation1);

        // Cart item for mirage property in JWB
        RoomCartItem item2 = new RoomCartItem();
        RoomReservation reservation2 = new RoomReservation();
        reservation2.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
        item2.setReservation(reservation2);
        item2.setPromotedMlifePrice(true);

        List<RoomCartItem> cartItems = new ArrayList<>();
        cartItems.add(item1);
        cartItems.add(item2);

        ServicesSession sSession = new ServicesSession();
        sSession.getCartItems().add(item1);
        sSession.getCartItems().add(item2);

        assertFalse("false should be returned when none of the RoomCartItems are added by opting for JWB",
                CommonUtil.isEligibleForAccountCreation(sSession, "66964e2b-2550-4476-84c3-1a4c0c5c067f"));

        assertTrue("true should be returned when at least one of the RoomCartItem is added by opting for JWB",
                CommonUtil.isEligibleForAccountCreation(sSession, "mgmri"));
    }
    
    @Test
    public void testRemoveNonAlphaChars() {

        String text = "Jo-hn Smith`1,./;',./~!@#$%^&*()_+{}:<>?|";

        assertEquals("Text shouldn't contain non-alpha chars", "JohnSmith", CommonUtil.removeNonAlphaChars(text));
    }
}
