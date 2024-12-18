/**
 * 
 */
package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.exception.ErrorVo;

import lombok.Data;

/**
 * @author vararora
 *
 */
@JsonInclude(Include.NON_NULL)
@Data
public class ModifyRoomReservationResponse {

    private ErrorVo error;
    private RoomReservationV2Response roomReservation;
    
}
