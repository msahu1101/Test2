package com.mgm.services.booking.room.service.cache.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.service.cache.rediscache.service.RoomProgramRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.impl.RedisCacheReadServiceImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PhoenixRoomProgramDAO;
import com.mgm.services.booking.room.dao.PhoenixRoomSegmentDAO;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.phoenix.RoomSegment;
import com.mgm.services.booking.room.properties.CacheProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;
import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

/**
 * Implementation class for room program cache service to cache room programs
 * from phoenix. Room programs information is updated with segment information
 * by mashing up segment information.
 */
@Component
@Log4j2
public class RoomProgramCacheServiceImpl extends AbstractCacheService implements RoomProgramCacheService {

    private static final String CACHE_NAME = "roomProgram";

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private PhoenixRoomProgramDAO roomProgramDao;

    @Autowired
    private PhoenixRoomSegmentDAO roomSegmentDao;

    @Autowired
    private RoomProgramRedisCacheService redisCacheReadService;

    @Override
    public RoomProgram getRoomProgram(String programId) {
            if(cacheProperties.isRedisEnabled()){
               return Optional.ofNullable(redisCacheReadService.getRoomProgram(programId))
                       .orElseGet(() ->(RoomProgram) getCachedObject(programId));
            }else{
                return (RoomProgram) getCachedObject(programId);
            }
    }

    @Override
    public boolean isProgramInCache(String programId) {
        return null != getRoomProgram(programId);
    }
    
    @Override
    public boolean isProgramPO(String programId) {
        RoomProgram program = getRoomProgram(programId);
        return (null != program
                && (program.getCustomerRank() > 0 || program.getSegmentFrom() > 0 || program.getSegmentTo() > 0));
    }
    @Override
    public RoomProgram getProgramByCustomerRank(int customerRank, String propertyId) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadService.getProgramByCustomerRank(customerRank,propertyId))
                    .orElseGet(()-> getEhProgramByCustomerRank(customerRank,propertyId));
        }else{
            return  getEhProgramByCustomerRank(customerRank,propertyId);
        }
    }
    private RoomProgram getEhProgramByCustomerRank(int customerRank, String propertyId) {

        final Cache cache = getCache();
        final Attribute<String> propertyAtt = cache.getSearchAttribute("propertyId");
        final Attribute<Integer> rankAtt = cache.getSearchAttribute("customerRank");
        final Results results = cache.createQuery().addCriteria(propertyAtt.eq(propertyId))
                .addCriteria(rankAtt.eq(customerRank)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);

        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.debug("Customer rank {} for property {} resolves to {} programs", customerRank, propertyId,
                    resultsValue.get().size());
            if(cacheProperties.isRedisEnabled()){
                log.info("Data found in ehCache but not in Redis for - {}",propertyId+":"+customerRank);
            }
            return (RoomProgram) resultsValue.get().all().get(0).getValue();

        }
        return null;
    }
    
    @Override
    public List<RoomProgram> getProgramsBySegmentId(String segmentId) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadService.getProgramsBySegmentId(segmentId))
                    .orElseGet(()-> getEhProgramsBySegmentId(segmentId));
        }else{
            return getEhProgramsBySegmentId(segmentId);
        }
    }
    private List<RoomProgram> getEhProgramsBySegmentId(String segmentId) {
        final Cache cache = getCache();
        final Attribute<String> segmentAtt = cache.getSearchAttribute("segmentId");
        final Results results = cache.createQuery().addCriteria(segmentAtt.eq(segmentId)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        List<RoomProgram> programsList = new ArrayList<>();
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.info("ID resolves to segment {} which contains {} programs", segmentId, resultsValue.get().size());
            if(cacheProperties.isRedisEnabled()){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.SEGMENT_STR +":"+segmentId);
            }
            List<Result> resultList = resultsValue.get().all();

            for (Result result : resultList) {
                programsList.add((RoomProgram) result.getValue());
            }

        }
        return programsList;
    }

    @Override
    public List<RoomProgram> getProgramsByGroupCode(String groupCode) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadService.getProgramsByGroupCode(groupCode))
                    .orElseGet(()-> getEhProgramsByGroupCode(groupCode));
        }else{
            return getEhProgramsByGroupCode(groupCode);
        }
    }
    private List<RoomProgram> getEhProgramsByGroupCode(String groupCode) {
        final Cache cache = getCache();
        final Attribute<String> groupAtt = cache.getSearchAttribute("operaBlockCode");
        final Results results = cache.createQuery().addCriteria(groupAtt.eq(groupCode)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        List<RoomProgram> programsList = new ArrayList<>();
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.debug("ID resolves to group code {} which contains {} programs", groupCode, resultsValue.get().size());
            if(cacheProperties.isRedisEnabled()){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.GROUP_STR +":"+groupCode);
            }
            List<Result> resultList = resultsValue.get().all();

            for (Result result : resultList) {
                programsList.add((RoomProgram) result.getValue());
            }

        }
        return programsList;
    }
    
    @Override
    public List<RoomProgram> getProgramsByPromoCode(String promoCode) {
        if (cacheProperties.isRedisEnabled()) {
            return Optional.ofNullable(redisCacheReadService.getProgramsByPromoCode(promoCode))
                    .orElseGet(() -> getEhProgramsByPromoCode(promoCode));
        } else {
            return getEhProgramsByPromoCode(promoCode);
        }
    }

    private List<RoomProgram> getEhProgramsByPromoCode(String promoCode) {

        final Cache cache = getCache();
        final Attribute<String> promoAtt = cache.getSearchAttribute("promoCode");
        // remove last chars for delano/nomad
        Set<String> promoCodes = CommonUtil.promoCodes(promoCode);

        log.info("Looking up promo codes: {}", promoCodes);

        final Results results = cache.createQuery().addCriteria(promoAtt.in(promoCodes)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        List<RoomProgram> programsList = new ArrayList<>();
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.info("Promo code resolves to {} programs", resultsValue.get().size());
            List<Result> resultList = resultsValue.get().all();

            for (Result result : resultList) {
                RoomProgram program = (RoomProgram) result.getValue();
                // PO and MyVegas programs shouldn't be included. For MyVegas Promo code remove the MyVegas tag check
                if (StringUtils.isNotEmpty(promoCode) && promoCode.startsWith(ServiceConstant.ACRS_MYVEGAS_RATEPLAN)) {
                    if (!isProgramPO(program.getId())) {
                        programsList.add(program);
                    }
                } else {
                    if (!isProgramPO(program.getId()) && !CommonUtil.isContainMyVegasTags(program.getTags())) {
                        programsList.add(program);
                    }
                }
            }
            if(cacheProperties.isRedisEnabled() && CollectionUtils.isNotEmpty(programsList)){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.PROMO_STR +":"+promoCodes);
            }

        } else {
            log.info("Promo code didn't resolve to any programs");
        }
        return programsList;
    }

    @Override
    public boolean isSegment(String programId){
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadService.isSegment(programId))
                    .orElseGet(()-> isEhSegment(programId));
        }else{
            return isEhSegment(programId);
        }
    }
    private boolean isEhSegment(String programId) {

        final Cache cache = getCache();
        final Attribute<String> segmentAtt = cache.getSearchAttribute("segmentId");
        final Results results = cache.createQuery().addCriteria(segmentAtt.eq(programId)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.info("ID resolves to segment which contains {} programs", resultsValue.get().size());
            if(cacheProperties.isRedisEnabled()){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.SEGMENT_STR +":"+programId);
            }
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * fetchData(java.lang.String)
     */
    @Override
    protected Map<Object, Object> fetchData(String key) {
        Map<Object, Object> roomProgramMap = new HashMap<>();
        try {
            // using some block statements since this is background process
            // anyway
            log.info("[EhCacheLogger]|'{}' Cache -> Fetching Room programs", getCacheName());
            List<RoomProgram> programList = roomProgramDao.getRoomPrograms();
            log.info("[EhCacheLogger]|'{}' Cache -> Successfully fetched Room programs", getCacheName());

            log.info("[EhCacheLogger]|'{}' Cache -> Fetching Room segments", getCacheName());
            List<RoomSegment> segmentList = roomSegmentDao.getRoomSegments();
            log.info("[EhCacheLogger]|'{}' Cache -> Successfully fetched Room segments", getCacheName());

            roomProgramMap = mergeAndPrepareCacheObject(programList, segmentList);
        } catch (Exception e) {
            failureCount++;
            log.error("[EhCacheLogger]|'{}' Cache -> Exception while retrieving phoenix Room Programs from phoenix",
                    getCacheName(), e);
        }
        return roomProgramMap;
    }

    /**
     * Enhances the room programs with segment id based on segment info
     * retrieved separately.
     * 
     * @param programList
     *            List of room programs
     * @param segmentList
     *            List of segments
     * @return Map of room programs with segment info added
     */
    private Map<Object, Object> mergeAndPrepareCacheObject(List<RoomProgram> programList,
            List<RoomSegment> segmentList) {

        // Update program data with segment Id, if mapping is available
        Map<Object, Object> roomProgramMap = new HashMap<>();

        for (RoomProgram program : programList) {
            if (program.isActiveFlag()) {
                roomProgramMap.put(program.getId(), program);
            }
        }

        for (RoomSegment segment : segmentList) {
            for (String programId : segment.getPrograms()) {
                if (roomProgramMap.containsKey(programId)) {
                    RoomProgram program = (RoomProgram) roomProgramMap.get(programId);
                    program.setSegmentId(segment.getId());
                }
            }
        }

        return roomProgramMap;
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
        return cacheProperties.getRoomProgramRefreshFreqInSecs();
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
        final List<String> keys = new ArrayList<>();
        keys.add("static");
        // Just using a static key list for non-key dependent cache services
        return keys;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.cache.RoomProgramCacheService#
     * getPromoCodeByProgramId(java.lang.String)
     */
    @Override
    public String getPromoCodeByProgramId(String programId) {

        RoomProgram program = getRoomProgram(programId);
        int characterToReplace;

        if (null == program) {
            // Backward compatibility for GSE segment GUID passed as programId
            // Will be removed once client teams have moved to segment code
            // approach

            List<RoomProgram> programs = getProgramsBySegmentId(programId);

            if (!programs.isEmpty()) {
                program = programs.get(0);
            }
        }

        if (null != program) {
            String promoCode = program.getPromoCode();
            if (StringUtils.isEmpty(promoCode) || StringUtils.isEmpty(program.getPropertyId())) {
                return promoCode;
            }
            // If delano, remove D as second last char
            // If Nomad, remove N as second last char
            characterToReplace = promoCode.length() - 1;
            // TMLIFE requires special handling to remove last character
            if (promoCode.startsWith("TMLIFE")) {
            	characterToReplace = promoCode.length();
            }
            if (program.getPropertyId().equals("8bf670c2-3e89-412b-9372-6c87a215e442")) {
                promoCode = CommonUtil.removeChar(promoCode, 'D', characterToReplace);
            } else if (program.getPropertyId().equals("2159252c-60d3-47db-bbae-b1db6bb15072")) {
                promoCode = CommonUtil.removeChar(promoCode, 'N', characterToReplace);
            }
            return promoCode;
        }

        return StringUtils.EMPTY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.cache.RoomProgramCacheService#
     * getProgramsByPatronPromoIds(java.util.List)
     */
    @Override
    public List<RoomProgram> getProgramsByPatronPromoIds(List<String> promoIds){
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(getRedisProgramsByPatronPromoIds(promoIds))
                    .orElseGet(()-> getEhProgramsByPatronPromoIds(promoIds));
        }else{
            return getEhProgramsByPatronPromoIds(promoIds);
        }
    }
    private List<RoomProgram> getRedisProgramsByPatronPromoIds(List<String> promoIds){
        List<RoomProgram> programsList = null;
        List<RoomProgram> programs = redisCacheReadService.getProgramsByPatronPromoIds(promoIds);
        if(CollectionUtils.isNotEmpty(programs)){
            programsList = new ArrayList<>();
            log.info("Patron promoIds resolves to {} programs from Redis", programs.size());
            for (RoomProgram program:programs) {
                boolean programValid = isProgramValidForBooking(program);
                log.info("Program ID: {}, Promo Code: {}, Property ID: {}, Valid for Booking: {} in Redis", program.getId(),
                        program.getPromoCode(), program.getPropertyId(), programValid);
                program.setPromoCode(CommonUtil.normalizePromoCode(program.getPromoCode(), program.getPropertyId()));

                if (programValid) {
                    programsList.add(program);
                }
            }
        }else{
            log.info("Patron promoIds didn't resolve to any programs in Redis");
        }
        return programsList;
    }
    private List<RoomProgram> getEhProgramsByPatronPromoIds(List<String> promoIds) {

        final Cache cache = getCache();
        final Attribute<String> promoAtt = cache.getSearchAttribute("patronPromoId");
        final Results results = cache.createQuery().addCriteria(promoAtt.in(promoIds)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        List<RoomProgram> programsList = new ArrayList<>();
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.info("Patron promoIds resolves to {} programs", resultsValue.get().size());
            List<Result> resultList = resultsValue.get().all();

            for (Result result : resultList) {
                RoomProgram program = (RoomProgram) result.getValue();
                boolean programValid = isProgramValidForBooking(program);
                log.debug("Program ID: {}, Promo Code: {}, Property ID: {}, Valid for Booking: {}", program.getId(),
                        program.getPromoCode(), program.getPropertyId(), programValid);
                program.setPromoCode(CommonUtil.normalizePromoCode(program.getPromoCode(), program.getPropertyId()));
                
                if (programValid) {
                    programsList.add(program);
                }
            }
            if(cacheProperties.isRedisEnabled() && CollectionUtils.isNotEmpty(programsList)){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.PTRN_PROMO_STR +":"+promoIds);
            }

        } else {
            log.info("Patron promoIds didn't resolve to any programs");
        }

        return programsList;
    }
    
    private boolean isProgramValidForBooking(RoomProgram program) {

        LocalDate currentDate = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return (program.isActiveFlag() && null != program.getBookBy() && program.getBookBy()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .isAfter(currentDate) && program.isBookableOnline());
    }

}
