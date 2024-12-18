package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.ComponentDAOStrategy;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.cache.PhoenixComponentsCacheService;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgmresorts.aurora.messages.GetRoomComponentAvailabilityRequest;
import com.mgmresorts.aurora.messages.GetRoomComponentAvailabilityResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implementation class for ComponentDAO providing functionality to provide room
 * component related functionalities.
 *
 */
@Component
@Log4j2
public class ComponentDAOStrategyGSEImpl extends AuroraBaseDAO implements ComponentDAOStrategy {

    private static final String COMPONENT_TYPE_SPECIAL_REQUEST = "SPECIAL_REQUEST";
    private static final String COMPONENT_TYPE_ROOM_FEATURE = "ROOM_FEATURE";
    private static final String NIGHTLY = "NIGHTLY";

    @Autowired
    private RoomCacheService roomCacheService;
    
    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private PhoenixComponentsCacheService phoenixComponentsCacheService;

    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.dao.ComponentDAO#
     * getRoomComponentAvailability(com.mgm.services.booking.room.model.request.
     * RoomComponentRequest)
     */
    @Override
    public List<RoomRequest> getRoomComponentAvailability(RoomComponentRequest request) {

        Room room = roomCacheService.getRoom(request.getRoomTypeId());
        List<RoomRequest> roomRequests = new ArrayList<>();
        if (room != null) {
            List<RoomComponent> components = room.getComponents();
            List<String> componentIds = new ArrayList<>();
            Map<String, RoomComponent> componentMap = new HashMap<>();

            components.forEach(component -> {
                if (component.isActiveFlag() && !COMPONENT_TYPE_SPECIAL_REQUEST.equals(component.getComponentType())
                        && !COMPONENT_TYPE_ROOM_FEATURE.equals(component.getComponentType())) {
                    componentIds.add(component.getId());
                    componentMap.put(component.getId(), component);
                }

            });
            request.setComponentIds(componentIds);
            
            // Removing borgata specific special requests
            if (request.getPropertyId().equals(appProps.getBorgataPropertyId())) {

                appProps.getBorgataSpecialRequests().forEach(componentIds::remove);

            }
            
            if (componentIds.isEmpty()) {
                log.info("No components of type COMPONENT to fetch availability");
            } else {
                GetRoomComponentAvailabilityRequest componentRequest = MessageFactory
                        .createGetRoomComponentAvailabilityRequest();
                componentRequest.setPropertyId(request.getPropertyId());
                componentRequest
                        .setComponentIds(request.getComponentIds().toArray(new String[request.getComponentIds().size()]));
                componentRequest.setTravelStartDate(request.getTravelStartDate());
                componentRequest.setTravelEndDate(request.getTravelEndDate());

                log.info("Sent the request to getComponentsAvailability as : {}", componentRequest.toJsonString());

                final GetRoomComponentAvailabilityResponse response = getAuroraClient(request.getSource())
                        .getRoomComponentAvailability(componentRequest);

                log.info("Received the response from getComponentsAvailability as : {}", response.toJsonString());

                if (null != response.getComponentIds()) {
                    for (String compId : response.getComponentIds()) {
                        RoomRequest roomRequest = new RoomRequest();
                        RoomComponent component = componentMap.get(compId);
                        roomRequest.setId(compId);
                        roomRequest.setCode(component.getName());
                        roomRequest.setPrice(null != component.getPrice() ? component.getPrice() : 0);
                        roomRequest.setActive(component.isActiveFlag());
                        roomRequest.setNightlyCharge(component.getPricingApplied().equals(NIGHTLY));
                        roomRequest.setDescription(component.getDescription());
                        roomRequest.setShortDescription(component.getShortDescription());
                        
                        // removing html tags coming from phoenix
                        if(StringUtils.isNotEmpty(component.getLearnMoreDescription())) {
                            roomRequest.setLongDescription(component.getLearnMoreDescription().replaceAll("\\<.*?\\>", ""));
                        }
                        
                        roomRequest.setPricingApplied(component.getPricingApplied());
                        roomRequest.setTaxRate(null != component.getTaxRate() ? component.getTaxRate() : 0);
                        roomRequests.add(roomRequest);
                    }
                }
            }
        }
        return roomRequests;
    }

    @Override
    public RoomRequest getRoomComponentById(String componentId, String roomTypeId) {
        Room room = roomCacheService.getRoom(roomTypeId);

        if (room != null) {
            List<RoomComponent> components = room.getComponents();

            Optional<RoomComponent> componentOpt = components.stream()
                    .filter(c -> c.getId()
                            .equalsIgnoreCase(componentId))
                    .findFirst();

            if (componentOpt.isPresent()) {
                RoomComponent c = componentOpt.get();

                RoomRequest roomRequest = new RoomRequest();
                roomRequest.setId(c.getId());
                roomRequest.setCode(c.getName());
                roomRequest.setActive(c.isActiveFlag());
                roomRequest.setDescription(c.getDescription());
                //CBSR-1680: if ShortDescription null or empty then set it from Description
                Optional<String> shortDescription = Optional.ofNullable(c.getShortDescription());
                if (shortDescription.isPresent()) {
                    roomRequest.setShortDescription(c.getShortDescription());
                } else {
                    roomRequest.setShortDescription(c.getDescription());
                }
                // removing html tags coming from phoenix
                if (StringUtils.isNotEmpty(c.getLearnMoreDescription())) {
                    roomRequest.setLongDescription(c.getLearnMoreDescription()
                            .replaceAll("\\<.*?\\>", ""));
                }
                roomRequest.setPricingApplied(c.getPricingApplied());
                roomRequest.setPrice(null != c.getPrice() ? c.getPrice() : 0);
                roomRequest.setTaxRate(null != c.getTaxRate() ? c.getTaxRate() : 0);

                return roomRequest;
            }

        }
        return null;
    }

    @Override
    public RoomComponent getRoomComponentByCode(String propertyId, String code, String roomTypeId,
                                                String ratePlanId, Date checkInDate, Date checkOutDate,
                                                String mlifeNumber, String source) {
        List<RoomComponent> roomComponentList = phoenixComponentsCacheService.getComponentsByExternalCode(code);
        RoomComponent roomComponent = new RoomComponent();
        if (CollectionUtils.isNotEmpty(roomComponentList)) {
            Optional<RoomComponent> rComp = roomComponentList.stream().filter(x -> x.isActiveFlag() && x.getPropertyId().equalsIgnoreCase(propertyId)).findFirst();
            if (rComp.isPresent()) {
                roomComponent = rComp.get();
            }
        }
        return roomComponent;
    }

    @Override
    public RoomComponent getRoomComponentById(String componentId) {
        return phoenixComponentsCacheService.getComponent(componentId);
    }
}
