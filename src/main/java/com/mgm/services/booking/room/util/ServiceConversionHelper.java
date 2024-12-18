package com.mgm.services.booking.room.util;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Log4j2
public class ServiceConversionHelper {

    private static final String GET_PROPERTY_METHOD = "getPropertyId";

    private static final String GET_PROGRAM_METHOD = "getProgramId";
    private static final String SET_PROGRAM_METHOD = "setProgramId";

    private static final String GET_PROGRAMS_METHOD = "getProgramIds";
    private static final String SET_PROGRAMS_METHOD = "setProgramIds";

    private static final String GET_ROOM_METHOD = "getRoomTypeId";
    private static final String SET_ROOM_METHOD = "setRoomTypeId";

    private static final String GET_ROOMS_METHOD = "getRoomTypeIds";
    private static final String SET_ROOMS_METHOD = "setRoomTypeIds";

    @Autowired
    protected AcrsProperties acrsProperties;

    @Autowired
    private RoomProgramCacheService programCache;

    @Autowired
    private RoomCacheService roomCache;

    @Autowired
    private ReferenceDataDAOHelper refDataHelper;

    public void convertGuids(Object o) {

        final String propertyId = getValue(o, GET_PROPERTY_METHOD, String.class);
        if (refDataHelper.isPropertyManagedByAcrs(propertyId)) {

            final String propertyCode = refDataHelper.retrieveAcrsPropertyID(propertyId);

            // Convert ProgramId
            overrideProgramId(o, propertyCode);

            // Convert ProgramsId (plural)
            overrideProgramIds(o, propertyCode);

            // Convert RoomId
            overrideRoomId(o, propertyCode);

            // Convert RoomId Array
            overrideRoomIds(o, propertyCode);

            // Convert Reservation Guids
            overrideReservationGuids(o, propertyCode);
        }

    }

    private void overrideProgramIds(Object o, String propertyCode) {
        final List<String> programIds = getValue(o, GET_PROGRAMS_METHOD, List.class);
        if (CollectionUtils.isNotEmpty(programIds)){
            List<String> crsProgramIds = programIds.stream()
                    .map(programId -> overrideProgramGuid(propertyCode, programId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            setValue(o, SET_PROGRAMS_METHOD, crsProgramIds, List.class);
        }
    }

    private String overrideProgramGuid(String propertyCode, String programId) {
        if(StringUtils.isNotEmpty(programId) && StringUtils.isNotEmpty(propertyCode) && CommonUtil.isUuid(programId)) {
            // programId is uuid so convert to Rate Plan code if possible
            return getRatePlanId(propertyCode, programId);
        }
        // if not UUID, return unchanged value.
        return programId;
    }

    private void overrideRoomIds(Object o, String propertyCode) {
        final List<String> roomIds = getValue(o, GET_ROOMS_METHOD, List.class);
        if (CollectionUtils.isNotEmpty(roomIds)) {
            final List<String> roomACrsIds = new ArrayList<>();
            for (String singleRoomId : roomIds) {
                if (StringUtils.isNotEmpty(singleRoomId) && CommonUtil.isUuid(singleRoomId)) {

                    // Get Cached Program
                    final Room room = roomCache.getRoom(singleRoomId);
                    if (null != room) {
                        roomACrsIds.add(ACRSConversionUtil.createRoomCodeGuid(room.getOperaRoomCode(), propertyCode));
                    }

                } else {

                    roomACrsIds.add(singleRoomId);
                }
            }

            //Sets Room Ids
            setValue(o, SET_ROOMS_METHOD, roomACrsIds, List.class);
        }
    }

    private void overrideRoomId(Object o, String propertyCode) {
        final String roomId = getValue(o, GET_ROOM_METHOD, String.class);
        if (StringUtils.isNotEmpty(roomId) && CommonUtil.isUuid(roomId)) {

            // Get Cached Program
            final Room room = roomCache.getRoom(roomId);
            if (null != room) {
                final String roomACrsId = ACRSConversionUtil.createRoomCodeGuid(room.getOperaRoomCode(), propertyCode);

                setValue(o, SET_ROOM_METHOD, roomACrsId, String.class);
            }
        }
    }

    private void overrideProgramId(Object o, String propertyCode) {
        final String programId = getValue(o, GET_PROGRAM_METHOD, String.class);
        final String ratePlanId = getRatePlanId(propertyCode, programId);
        if (StringUtils.isNotEmpty(ratePlanId)) {
            setValue(o, SET_PROGRAM_METHOD, ratePlanId, String.class);
        }
    }

    private void overrideReservationGuids(Object o, String propertyCode) {
        if (RoomReservationRequest.class.isInstance(o)) {

            final RoomReservationRequest request = (RoomReservationRequest) o;
            final List<RoomPrice> bookings = request.getBookings();
            if (CollectionUtils.isNotEmpty(bookings)) {
                for (RoomPrice booking : bookings) {

                    // Booking ProgramId
                    String programId = booking.getProgramId();
                    String ratePlanId = getRatePlanId(propertyCode, programId);
                    if (StringUtils.isNotEmpty(ratePlanId)) {
                        booking.setProgramId(ratePlanId);
                    }

                    // Booking Override ProgramId
                    programId = booking.getOverrideProgramId();
                    ratePlanId = getRatePlanId(propertyCode, programId);
                    if (StringUtils.isNotEmpty(ratePlanId)) {
                        booking.setOverrideProgramId(ratePlanId);
                    }
                }
            }
        }
    }

    private String getRatePlanId(String propertyCode, String programId) {
        if (StringUtils.isNotEmpty(programId) && CommonUtil.isUuid(programId)) {
            final RoomProgram program = programCache.getRoomProgram(programId);
            if (null != program) {
                String computedBaseRatePlan = acrsProperties.getBaseRatePlan(propertyCode.toUpperCase());
            
                final String promoCode = program.getPromoCode();
                final String ratePlanCode = StringUtils.isNotEmpty(promoCode) ? promoCode : computedBaseRatePlan;
                return ACRSConversionUtil.createRatePlanCodeGuid(ratePlanCode, propertyCode);
            }
        }
        return null;
    }

    private static <T> void setValue(Object o, String methodName, T value, Class c) {
        try {
            final Method method = o.getClass().getMethod(methodName, c);
            method.invoke(o, value);
        } catch (NoSuchMethodException e) {
            log.debug("Method {} not found in {}", methodName, o.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Unable to invoke method {} in {} | Exception : {}",
                    methodName, o.getClass().getSimpleName(), ExceptionUtils.getStackTrace(e));
        }
    }

    private static <T> T getValue(Object o, String methodName, Class<T> valueClass) {
        try {
            final Method method = o.getClass().getMethod(methodName);
            return (T) method.invoke(o);
        } catch (NoSuchMethodException e) {
            log.debug("Method {} not found in {}", methodName, o.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Unable to invoke method {} in {} | Exception : {}",
                    methodName, o.getClass().getSimpleName(), ExceptionUtils.getStackTrace(e));
        }
        return null;
    }


}
