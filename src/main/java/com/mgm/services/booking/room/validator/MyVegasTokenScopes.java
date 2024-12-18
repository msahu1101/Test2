package com.mgm.services.booking.room.validator;

public enum MyVegasTokenScopes {

	VALIDATE_CODE("myvegas.code:read"), CONFIRM_CODE("myvegas.code:update");
    
    private String scope;

	MyVegasTokenScopes(String scope) {
		this.scope = scope;
	}
	
	public String getValue() {
	    return this.scope;
	}
}
