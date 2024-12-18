/**
 * 
 */
package com.mgm.services.booking.room.model.request;

import java.util.List;

import com.mgm.services.booking.room.model.UserAttribute;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class CreateUserProfileRequest {

    private List<UserAttribute> userAttributes;
    private String defaultPreferences;
}
