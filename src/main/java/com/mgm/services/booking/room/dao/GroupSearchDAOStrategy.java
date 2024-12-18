package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;

import java.util.List;

/**
 * DAO interface to expose services to fetch available programs for a room
 *
 */
public interface GroupSearchDAOStrategy {

    /**
     * This method searches the applicable Group Programs for a customer
     * @param request
     * @return
     */
    List<GroupSearchV2Response> searchGroup(GroupSearchV2Request request);
}
