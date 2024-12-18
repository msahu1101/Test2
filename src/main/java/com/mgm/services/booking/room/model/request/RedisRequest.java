package com.mgm.services.booking.room.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @Builder @NoArgsConstructor
public class RedisRequest {

	private String propertyId;
	private String ratePlanId;
	private String rateCode;
	private String acrsPropertyCode;
}
