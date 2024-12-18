package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.content.PackageConfig;
import com.mgm.services.booking.room.model.content.PackageConfigParam;

/**
 * DAO interface to expose services for retrieving package configurations.
 *
 */
public interface PackageConfigDAO {

    /**
     * Gets and returns PackageConfig[] for the requested key and value pair.
     * Service relies on content api end points to retrieve package configuration.
     * 
     * @param key
     *            request parameter key name
     * @param value
     *            request parameter value
     * @return array of package configurations
     */
    PackageConfig[] getPackageConfigs(PackageConfigParam key, String value);
}
