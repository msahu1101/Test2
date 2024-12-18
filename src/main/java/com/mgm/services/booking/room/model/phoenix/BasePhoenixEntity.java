
package com.mgm.services.booking.room.model.phoenix;

import lombok.Data;

/**
 * Base Phoenix entity class
 * @author nitpande0
 *
 */
public @Data class BasePhoenixEntity {

    private String id;
    private String name;
    private String shortDescription;
    private boolean activeFlag;
    private boolean viewOnline;
    private String propertyId;
    private boolean viewOnProperty;
    private boolean bookableOnline;
    private boolean bookableByProperty;

}
