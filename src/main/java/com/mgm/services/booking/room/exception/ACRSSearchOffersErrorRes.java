package com.mgm.services.booking.room.exception;

import lombok.Data;

@Data
public class ACRSSearchOffersErrorRes {
	private ACRSSearchOffersErrorDetails error;
	private ACRSSearchOffersErrorData data;
}
