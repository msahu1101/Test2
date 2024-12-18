package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.properties.AcrsProperties;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseStrategyDAO {

    @Autowired
    protected AcrsProperties acrsProperties;
    @Autowired
    protected ReferenceDataDAOHelper referenceDataDAOHelper;

    protected boolean isPropertyManagedByAcrs(String propertyId) {
        return referenceDataDAOHelper.isPropertyManagedByAcrs(propertyId);
    }

    protected boolean isAcrsEnabled() {
        return referenceDataDAOHelper.isAcrsEnabled();
    }


}
