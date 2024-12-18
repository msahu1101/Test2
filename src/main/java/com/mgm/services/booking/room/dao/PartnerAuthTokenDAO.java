package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.response.TokenResponse;

public interface PartnerAuthTokenDAO {
	
	public TokenResponse generateAuthToken();

}
