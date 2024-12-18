package com.mgm.services.booking.room.dao;

import java.util.Map;

import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;

public interface ACRSOAuthTokenDAO {
	
	public Map<String, ACRSAuthTokenResponse> generateToken();

}
