package com.mgm.services.booking.room.service.cache.rediscache.service;

import java.util.List;
import java.util.Map;

import com.mgm.services.booking.room.model.request.RoomProgramPromoAssociationRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;

public interface ENRRatePlanRedisService {

    ENRRatePlanSearchResponse[] searchRatePlans(ApplicableProgramRequestDTO request, String vendorCode);

	ENRRatePlanSearchResponse[] searchRatePlansRoomProgram(RoomProgramsRequestDTO request, String vendorCode);

	ENRRatePlanSearchResponse[] searchRatePlanById(RoomProgramV2Request request, String vendorCode, String propertyCode, List<String> programIds);

	ENRRatePlanSearchResponse[] searchRatePlansToValidate(RoomProgramValidateRequest request, String vendorCode,
			List<String> ratePlanIds, String propertyCode);

	String searchPromoByCode(String propertyId, String promoCode, String vendorCode);

	ENRRatePlanSearchResponse[] searchRatePlanByCode(String ratePlanCode, String vendorCode);

	ENRRatePlanSearchResponse[] searchPromoRatePlans(RoomProgramPromoAssociationRequest request, String vendorCode,
			List<String> programIds, String propertyCode, String searchType);
	
	
}
