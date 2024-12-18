package com.mgm.services.booking.room.service.cache.rediscache.service;

import java.util.List;

public interface PropertyPkgComponentCacheService {
	
	public List<String> getPkgComponentCodeByPropertyId(String propertyId);
}
