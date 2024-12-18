package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.PIMPkgComponent;

import java.util.List;

public interface PIMDAO {

    /**
     * @param propertyCode
     * @return
     */
    List<PIMPkgComponent> searchPackageComponents(String propertyCode, String type);
}
