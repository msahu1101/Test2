package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.GroupSearchDAO;
import com.mgm.services.booking.room.dao.GroupSearchDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation class for calling ACRS Soap XML end point
 */
@Component
@Log4j2
public class GroupSearchDAOImpl extends BaseStrategyDAO implements GroupSearchDAO {

    @Autowired
    private GroupSearchDAOStrategyACRSImpl acrsStrategy;
    @Autowired
    private GroupSearchDAOStrategyGSEImpl gseStrategy;

    @Override
    @LogExecutionTime
    public List<GroupSearchV2Response> searchGroup(GroupSearchV2Request request) {
        GroupSearchDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(request.getPropertyId())) {
            strategy = acrsStrategy;
        }
        final String uniqueId = "PID:" + request.getPropertyId() + "TD:" + request.getStartDate() + ">ED:" + request.getEndDate() + ">ID:" + request.getId();
        log.debug(createStrategyLogEntry("groupSearchRequest", uniqueId, strategy));
        return strategy.searchGroup(request);
    }

    private String createStrategyLogEntry(String method, String uniqueId, GroupSearchDAOStrategy strategy) {
        String strategyString = (strategy instanceof GroupSearchDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "GroupSearchDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }

}
