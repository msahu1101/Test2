package com.mgm.services.booking.room.service.cache.rediscache.service;

import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.crs.searchoffers.TaxDefinition;

import java.util.List;

/**
 * Service interface exposing services for property content cache services.
 *
 */
public interface PropertyContentRedisCacheService {

    /**
     * Get property marketing information from the cache based on property id.
     * 
     * @param propertyId
     *            Property Identifier
     * @return Property marketing cache information
     */
    Property getProperty(String propertyId);

    /**
     * Get properties based on region.
     * 
     * @param region
     *            Possible values are LV, NONLV
     * @return Returns properties based on region
     */
    List<Property> getPropertyByRegion(String region);

    List<TaxDefinition> getPropertyTaxAndFees(String propertyCode);
}
