/**
 * 
 */
package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.Payload;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class CreateUserProfileResponse {
    private String type;
    private Payload payload;
}
