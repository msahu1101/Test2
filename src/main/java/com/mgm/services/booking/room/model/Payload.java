/**
 * 
 */
package com.mgm.services.booking.room.model;

import java.util.List;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class Payload {
    private String userIdentifier;
    private List<UserAttribute> userAttributes;
}
