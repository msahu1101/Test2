package com.mgm.services.booking.room.exception;

import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * This class is responsible to build the error response
 * based on the input parameters.
 * @author swakulka
 *
 */
@Data
@Component
public class ErrorResponseBuilder {

	/**
	 * Method invoked to build the error response based on 
	 * error code and message passed
	 * @param code - the error code
	 * @param message - error message
	 * @return ErrorResponse error response.
	 */
	public ErrorResponse buildErrorResponse(String code, String message) {

		ErrorResponse errResponse = new ErrorResponse();
		ErrorVo vo = new ErrorVo();
		vo.setCode(code);
		vo.setMessage(message);

		errResponse.setError(vo);

		return errResponse;
	}
}
