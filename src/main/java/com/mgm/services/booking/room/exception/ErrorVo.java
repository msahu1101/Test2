package com.mgm.services.booking.room.exception;

import lombok.Data;
/**
 * Error response pojo.
 * @author swakulka
 *
 */
@Data
public class ErrorVo {

	private String code;
	private String message;
}
