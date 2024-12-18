package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.opera.OperaInfoResponse;

public interface OperaInfoDAO {

    public OperaInfoResponse getOperaInfo(String cnfNumber, String propertyCode);
}
