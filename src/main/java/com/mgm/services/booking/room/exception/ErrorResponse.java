package com.mgm.services.booking.room.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class to encapsulate the error Vo
 * 
 * @author swakulka
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ErrorResponse {

    private ErrorVo error;

}
