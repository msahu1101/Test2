package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.ComponentDAOStrategy;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Log4j2
public class ComponentDAOImpl extends BaseStrategyDAO implements ComponentDAO {
    @Autowired ComponentDAOStrategyACRSImpl acrsStrategy;
    @Autowired ComponentDAOStrategyGSEImpl gseStrategy;

    @Override public List<RoomRequest> getRoomComponentAvailability(RoomComponentRequest request) {
        ComponentDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(request.getPropertyId()) ){
            strategy = acrsStrategy;
        }
        return getRoomComponentAvailability(request, strategy);
    }

    private List<RoomRequest> getRoomComponentAvailability(RoomComponentRequest request, ComponentDAOStrategy strategy){
        String uniqueId = "RT:" + request.getRoomTypeId() + ">SD:" + request.getTravelStartDate().toString() + ">ED:" + request.getTravelEndDate();
        log.debug(createStrategyLogEntry("getRoomComponentAvailability", uniqueId, strategy));
        return strategy.getRoomComponentAvailability(request);
    }

    private String createStrategyLogEntry(String method, String uniqueId, ComponentDAOStrategy strategy) {
        String strategyString = (strategy instanceof ComponentDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "ComponentDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.ComponentDAO#getRoomComponentById(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public RoomRequest getRoomComponentById(String componentId, String roomTypeId, String propertyId) {
        ComponentDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(propertyId) ){
            strategy = acrsStrategy;
        }
        return strategy.getRoomComponentById(componentId, roomTypeId);
    }

    @Override
    public RoomComponent getRoomComponentByCode(String propertyId, String code, String roomTypeId,
                                                String ratePlanId, Date checkInDate, Date checkOutDate,
                                                String mlifeNumber, String source) {
        ComponentDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(propertyId)) {
            strategy = acrsStrategy;
        }
        return strategy.getRoomComponentByCode(propertyId, code, roomTypeId,
                ratePlanId, checkInDate, checkOutDate, mlifeNumber, source);
    }

    @Override
    public RoomComponent getRoomComponentById(String componentId, String propertyId) {
        ComponentDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(propertyId)) {
            strategy = acrsStrategy;
        }
        return strategy.getRoomComponentById(componentId);
    }
}
