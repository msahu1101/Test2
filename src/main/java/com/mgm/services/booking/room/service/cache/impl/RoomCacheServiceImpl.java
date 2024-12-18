package com.mgm.services.booking.room.service.cache.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.service.cache.rediscache.service.RoomRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.impl.RedisCacheReadServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PhoenixRoomDAO;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.CacheProperties;
import com.mgm.services.booking.room.service.cache.RoomCacheService;

import lombok.extern.log4j.Log4j2;
import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

/**
 * Implementation class for room cache service to cache room and room components
 * information from phoenix.
 */
@Component
@Log4j2
public class RoomCacheServiceImpl extends AbstractCacheService implements RoomCacheService {

    private static final String CACHE_NAME = "room";

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private AuroraProperties auroraProperties;

    @Autowired
    private PhoenixRoomDAO roomDao;

    @Autowired
    private RoomRedisCacheService redisCacheReadService;

    @Override
    public Room getRoom(String roomTypeId) {


        if(cacheProperties.isRedisEnabled()){
           return Optional.ofNullable(redisCacheReadService.getRoom(roomTypeId))
                   .orElseGet(()->(Room) getCachedObject(roomTypeId));
        }else {
            return (Room) getCachedObject(roomTypeId);
        }
    }

    @Override
    public Room getRoomByRoomCode(String roomCode) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadService.getRoomByRoomCode(roomCode))
                    .orElseGet(()-> getEhRoomByRoomCode(roomCode));
        }else {
            return getEhRoomByRoomCode(roomCode);
        }
    }
    private Room getEhRoomByRoomCode(String roomCode) {
        final Cache cache = getCache();
        final Attribute<String> operaRoomCodeAttr = cache.getSearchAttribute("operaRoomCode");
        final Results results = cache.createQuery().addCriteria(operaRoomCodeAttr.eq(roomCode)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        Room room = null;
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            List<Result> resultList = resultsValue.get().all();
            room = (Room) resultList.get(0).getValue();
            log.info("operaRoomCode {} resolves to room which id {}", roomCode, room.getId());
            if(cacheProperties.isRedisEnabled()){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.ROOMCODE_STR +":"+roomCode);
            }
        }
        return room;
    }

    @Override
    protected Map<Object, Object> fetchData(String key) {
        Map<Object, Object> roomMap = new HashMap<>();
        try {
            // using some block statements since this is background process
            // anyway
            log.info("[EhCacheLogger]|'{}' Cache -> Fetching Rooms", getCacheName());
            List<Room> roomList = roomDao.getRoomsByProperty(key);
            log.info("[EhCacheLogger]|'{}' Cache -> Successfully fetched Rooms", getCacheName());

            for (Room room : roomList) {
                if (room.isActiveFlag()) {
                    roomMap.put(room.getId(), room);
                }
            }

        } catch (Exception e) {
            failureCount++;
            log.error("[EhCacheLogger]|'{}' Cache -> Exception while retrieving rooms from phoenix", getCacheName(),
                    e);
        }
        return roomMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getCacheName()
     */
    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getRefreshPeriodInSeconds()
     */
    @Override
    protected long getRefreshPeriodInSeconds() {
        return cacheProperties.getRoomRefreshFreqInSecs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getRetryAttempts()
     */
    @Override
    protected int getRetryAttempts() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getKeys()
     */
    @Override
    protected List<String> getKeys() {
        return auroraProperties.getPropertyIds();
    }

}
