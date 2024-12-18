package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;

/**
 * Service class to find groups
 */
public interface GroupSearchV2Service {

    /**
     * Lookup service to find group.
     * 
     * @param groupSearchRequest
     *            GroupSearchRequest
     * @return List of GroupSearchResponse
     */
    List<GroupSearchV2Response> searchGroup(GroupSearchV2Request groupSearchRequest);
}
