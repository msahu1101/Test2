package com.mgm.services.booking.room.model.request;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class RatePlanFilterRequest {

	private Date travelDate;
	private Date checkInDate;
	private Date checkOutDate;
	private Date bookDate;
	private boolean isActive;
}
