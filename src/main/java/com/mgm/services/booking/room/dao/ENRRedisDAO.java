package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;

public interface ENRRedisDAO {
    public ENRRatePlanSearchResponse[] getRatePlansByProperty(List<String> propertyCode);
    public ENRRatePlanSearchResponse[]  getRatePlanByCode(List<String>  ratePlanCodes);
    public ENRRatePlanSearchResponse[] getRatePlanByChannel(String channel);
	public ENRRatePlanSearchResponse[] getRatePlanByPropertyChannel(List<String> propertyCode, String channel);
	public ENRRatePlanSearchResponse[] getPromoByPropertyId(List<String> propertyCodes);
	public ENRRatePlanSearchResponse[] getPromoByCode(String promo);
	public ENRRatePlanSearchResponse[] getRatePlanById(String singleRatePlan);
}
