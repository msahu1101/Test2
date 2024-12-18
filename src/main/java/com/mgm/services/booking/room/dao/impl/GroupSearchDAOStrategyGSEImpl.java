package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.GroupSearchDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.Room;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomProgramsTransformer;
import com.mgmresorts.aurora.messages.GetApplicableProgramsResponse;
import com.mgmresorts.aurora.service.EAuroraException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation class for RoomProgramDAO providing functionality to provide room
 * program related functionalities.
 *
 */
@Component
@Log4j2
public class GroupSearchDAOStrategyGSEImpl extends AuroraBaseDAO implements GroupSearchDAOStrategy {

    @Autowired
    private RoomProgramCacheService roomProgramCacheService;

    @Override
    @LogExecutionTime
    public List<GroupSearchV2Response> searchGroup(GroupSearchV2Request request) {
        try {
            final GetApplicableProgramsResponse response = getAuroraClient(request.getSource()).getApplicablePrograms(
                    RoomProgramsTransformer.buildAuroraRequestForGetApplicableProgramsRequest(request));
            if (response != null) {
                log.info("Received the response from getApplicablePrograms as : {}", response.toJsonString());
                return constructGroupSearchResponse(response);
            }
        } catch (EAuroraException ex) {
            log.error("Exception trying to retrieve applicable programs : ", ex);
            handleAuroraError(ex);
        }
        return new ArrayList<>();
    }

    private List<GroupSearchV2Response> constructGroupSearchResponse(GetApplicableProgramsResponse gseResponse) {
        final List<GroupSearchV2Response> allPrograms = new ArrayList<>();
        for (String programId : gseResponse.getProgramIds()) {
            final Optional<RoomProgram> cacheProgram = Optional.ofNullable(getRoomProgramCacheService().getRoomProgram(programId));
            if (cacheProgram.isPresent()) {
                final RoomProgram roomProgram = cacheProgram.get();
                if (ServiceConstant.GROUP_PROGRAM_CATEGORY.equalsIgnoreCase(roomProgram.getCategory())) {
                    allPrograms.add(RoomProgramsTransformer.buildGroupSearchResponse(roomProgram));
                }
            }
        }
        return allPrograms;
    }

    public RoomProgramCacheService getRoomProgramCacheService() {
        return this.roomProgramCacheService;
    }
}
