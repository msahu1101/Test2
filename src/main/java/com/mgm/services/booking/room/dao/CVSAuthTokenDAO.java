package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.response.TokenResponse;

public interface CVSAuthTokenDAO {
	
	public TokenResponse generateToken(boolean forceStale);

}
