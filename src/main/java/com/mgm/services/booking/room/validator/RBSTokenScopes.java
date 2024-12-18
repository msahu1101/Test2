package com.mgm.services.booking.room.validator;

public enum RBSTokenScopes {

	CREATE_RESERVATION("rooms.reservation:create"), UPDATE_RESERVATION("rooms.reservation:update"),
	GET_RESERVATION("rooms.reservation:read"), CANCEL_RESERVATION("rooms.reservation:update"),
	OVERRIDE_RESERVATION_CHARGES("rooms.reservation.charges:override"),
	GET_RESERVATION_CHARGES("rooms.reservation.charges:read"), GET_ROOM_AVAILABILITY("rooms.availability:read"),
	GET_ROOM_PROGRAMS("rooms.program:read"), GET_RESERVATION_ELEVATED("rooms.reservation:read:elevated"),
	LOYALTY_PROFILE_READ ("loyalty:profile:read"), UPDATE_RESERVATION_ELEVATED("rooms.reservation:update:elevated"),
	ALT_GET_RESERVATION("booking.room.resv:search"), ALT_UPDATE_RESERVATION("booking.room.resv:update"), OPEN_ID("openid"),
	PROFILE_READ("profile:read");
    
    private String scope;

	RBSTokenScopes(String scope) {
		this.scope = scope;
	}
	
	public String getValue() {
	    return this.scope;
	}
}
