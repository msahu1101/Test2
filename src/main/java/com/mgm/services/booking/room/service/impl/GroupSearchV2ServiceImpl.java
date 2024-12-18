package com.mgm.services.booking.room.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.GroupSearchDAO;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.service.GroupSearchV2Service;

/**
 * Service interface that exposes service for validating the iata code
 */
@Component
public class GroupSearchV2ServiceImpl implements GroupSearchV2Service {

    @Autowired
    private GroupSearchDAO groupSearchDAO;

    @Override
    public List<GroupSearchV2Response> searchGroup(GroupSearchV2Request groupSearchRequest) {        
        return groupSearchDAO.searchGroup(groupSearchRequest);
    }   
}
