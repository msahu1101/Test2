package com.mgm.services.booking.room.service.cache.rediscache.service.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.PIMDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.PIMPkgComponent;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.crs.searchoffers.TaxDefinition;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.service.cache.rediscache.service.PhoenixComponentsRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyContentRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyPkgComponentCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.RoomProgramRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.RoomRedisCacheService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.JSonMapper;
import com.mgm.services.booking.room.util.ReservationUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

@Component
@Log4j2
public class RedisCacheReadServiceImpl extends BaseRedisReadService implements RoomRedisCacheService, PhoenixComponentsRedisCacheService, RoomProgramRedisCacheService, PropertyContentRedisCacheService,PropertyPkgComponentCacheService {
    protected JSonMapper mapper = new JSonMapper();
    
    @Autowired
    private AcrsProperties acrsProperties;
    
    @Autowired
    private ReferenceDataDAOHelper refDataDAOHelper;

    @Autowired
    private PIMDAO pimDao;

    @Override
    public Room getRoom(String roomTypeId) {
        Room room = null;
        try {
        	if(CommonUtil.isUuid(roomTypeId)) {
        		String roomStr = getValuesByIndex(ServiceConstant.ROOM_STR, roomTypeId);
                if (StringUtils.isNotEmpty(roomStr)) {
                    room = mapper.readValue(roomStr, Room.class);
                }
            }else{
                log.info("Ignoring Redis call for ACRS RoomType {}",roomTypeId);
            }

        }catch (Exception ex){
            log.error("Error while reading from Redis {} : {}- {}",ServiceConstant.ROOM_STR,roomTypeId, ex.getMessage());
        }
        return room;
    }
        //@TODO same code for multiple properties
    @Override
    public Room getRoomByRoomCode(String roomCode) {
        Room room = null;
        try {
        	String roomListStr = getValuesByIndex(ServiceConstant.ROOMCODE_STR, roomCode);
        	if (StringUtils.isNotEmpty(roomListStr)) {
        		List<Room>  rooms = Arrays.asList(mapper.readValue(roomListStr, Room[].class));
        		if (CollectionUtils.isNotEmpty(rooms)) {
        			room = rooms.get(0);
        		}
        	}
        }catch (Exception ex){
            log.error("Error while reading from Redis {} : {}- {}",ServiceConstant.ROOM_STR,roomCode, ex.getMessage());
        }
        return room;
    }

    @Override
    public RoomComponent getComponent(String compId) {
        RoomComponent roomComponent = null;
        try {
        	if(CommonUtil.isUuid(compId)) {
        		String componentStr = getValuesByIndex(ServiceConstant.COMPONENT_STR, compId);
                if (StringUtils.isNotEmpty(componentStr)) {
                    roomComponent = mapper.readValue(componentStr, RoomComponent.class);
                }
            }else{
                log.info("Ignoring Redis call for ACRS ComponentId- {}",compId);
            }
        }catch (Exception ex){
            log.error("Error while reading from Redis {} : {}- {}",ServiceConstant.COMPONENT_STR,compId, ex.getMessage());
        }
    return roomComponent;
    }

    @Override
    public List<RoomComponent> getComponentsByExternalCode(String externalCode) {
        List<RoomComponent> components = null;
        try {
            String componentListStr = getValuesByIndex(ServiceConstant.EXTERNALCODE_STR, externalCode);
            if (StringUtils.isNotEmpty(componentListStr)) {
                components = new ArrayList<>(Arrays.asList(mapper.readValue(componentListStr, RoomComponent[].class)));
                log.info("External Code {} contains {} components in Redis", externalCode, components.size());

            }
        }catch (Exception ex){
            log.error("Error while reading from Redis {} : {}- {}",ServiceConstant.COMPONENT_STR,externalCode, ex.getMessage());
        }
        return components;
    }

    @Override
    public RoomProgram getRoomProgram(String programId) {
        RoomProgram data = null;
        try {
            if(StringUtils.isNotBlank(programId) && CommonUtil.isUuid(programId)) {
                String programStr = getValuesByIndex(ServiceConstant.PROGRAM_STR, programId);
                if (StringUtils.isNotEmpty(programStr)) {
                    data = mapper.readValue(programStr, RoomProgram.class);
                }
            }else{
                log.debug("Ignoring Redis call for ACRS RatePlan - {}",programId);
            }
        }catch (Exception ex){
            log.error("Error while reading from Redis {} : {}- {}",ServiceConstant.PROGRAM_STR,programId, ex.getMessage());
        }
        return data;
    }

    @Override
    public boolean isProgramInCache(String programId) {
        return null != getRoomProgram(programId);
    }

    /**
     * if the programId is a segmentId
     * @param programId
     *            Program Identifier
     * @return
     */
    @Override
    public boolean isSegment(String programId) {
        List<RoomProgram> programs = getProgramsBySegmentId(programId);
        return  CollectionUtils.isNotEmpty(programs) ;

    }

    @Override
    public List<RoomProgram> getProgramsBySegmentId(String segmentId) {
        List<RoomProgram> programs = getPrograms(ServiceConstant.SEGMENT_STR, segmentId);
        if(CollectionUtils.isNotEmpty(programs)) {
            log.info("ID resolves to segment {} which contains {} programs from Redis", segmentId, programs.size());
        }
        return programs;
    }

    @Override
    public List<RoomProgram> getProgramsByGroupCode(String groupCode) {
        List<RoomProgram> programs = getPrograms(ServiceConstant.GROUP_STR, groupCode);
        if(CollectionUtils.isNotEmpty(programs)) {
            log.info("ID resolves to group code {} which contains {} programs from Redis", groupCode, programs.size());
        }
        return programs;
    }

    @Override
    public List<RoomProgram> getProgramsByPromoCode(String promoCode) {
        List<RoomProgram> filteredPrograms = null;
        // remove last chars for delano/nomad
        Set<String> promoCodes = CommonUtil.promoCodes(promoCode);
        log.info("Looking up promo codes from Redis: {}", promoCodes);
        List<RoomProgram>  programList = getPrograms(ServiceConstant.PROMO_STR,new ArrayList<>(promoCodes));
        // PO and MyVegas programs shouldn't be included. For MyVegas Promo code remove the MyVegas tag check
        if(CollectionUtils.isNotEmpty(programList)) {
            log.info("Promo code from Redis resolves to {} programs in Redis", programList.size());
            if (StringUtils.isNotEmpty(promoCode) && promoCode.startsWith(ServiceConstant.ACRS_MYVEGAS_RATEPLAN))
            {
                filteredPrograms = programList.stream().filter(p -> !isProgramPO(p))
                        .collect(Collectors.toList());
            } else {
                filteredPrograms = programList.stream().filter(p -> !isProgramPO(p) &&
                                !CommonUtil.isContainMyVegasTags(p.getTags()))
                        .collect(Collectors.toList());
            }
        }
        return filteredPrograms;
    }

    @Override
    public boolean isProgramPO(String programId) {
        RoomProgram program = getRoomProgram(programId);
        return (null != program
                && (program.getCustomerRank() > 0 || program.getSegmentFrom() > 0 || program.getSegmentTo() > 0));
    }
    private boolean isProgramPO(RoomProgram program) {
        return (null != program
                && (program.getCustomerRank() > 0 || program.getSegmentFrom() > 0 || program.getSegmentTo() > 0));
    }

    @Override
    public RoomProgram getProgramByCustomerRank(int customerRank, String propertyId) {
        RoomProgram program = null;
        List<RoomProgram> programs = getPrograms(ServiceConstant.PROPERTY_RANK_STR, propertyId+":"+customerRank);
        if(CollectionUtils.isNotEmpty(programs)) {
            if (programs.size() == 1) {
                program = programs.get(0);
            } else if (programs.size() > 0) {
                program = programs.get(0);
                log.warn("More than One program for {} Rank {} in Redis", propertyId, customerRank);
            }
        }
        return program;

    }

    @Override
    public List<RoomProgram> getProgramsByPatronPromoIds(List<String> promoIds) {
        List<RoomProgram> list = null;
        if(CollectionUtils.isNotEmpty(promoIds)){
            List<String> uniqueList = promoIds.stream().collect(Collectors.toSet())
                    .stream().collect(Collectors.toList());
            list =  getPrograms(ServiceConstant.PTRN_PROMO_STR,uniqueList);
        }
        return list;
    }

    List<RoomProgram> getPrograms(String type, String keyPart){
        List<RoomProgram> programs = null;
       try {
           String programListStr = getValuesByIndex(type, keyPart);
           if (StringUtils.isNotEmpty(programListStr)) {
               RoomProgram[] programArr = mapper.readValue(programListStr, RoomProgram[].class);
               programs = new ArrayList<>(Arrays.asList(programArr));
           }

       }catch (Exception ex){
           log.error("Error while reading from Redis {} : {}- {}",type,keyPart, ex.getMessage());

       }
        return programs;
    }
   
    private List<RoomProgram> getPrograms(String type, List<String> keyPartList){
        List<RoomProgram> programs = null;
        try {
            List<String> programListObjStr = getValuesByIndex(type, keyPartList);
            if (CollectionUtils.isNotEmpty(programListObjStr)) {
                for (String programListStr : programListObjStr) {
                    if(StringUtils.isNotEmpty(programListStr)) {
                        RoomProgram[] programsArr = mapper.readValue(programListStr, RoomProgram[].class);
                        if(CollectionUtils.isEmpty(programs)){
                            programs = new ArrayList<>(Arrays.asList(programsArr));
                        }else{
                            programs.addAll(new ArrayList<>(Arrays.asList(programsArr)));
                        }

                    }
                }
            }
        }catch (Exception ex){
            log.error("Error while reading from Redis {} : {}- {}",type,keyPartList, ex.getMessage());
        }
        return programs;
    }

    @Override
    public Property getProperty(String propertyId) {
        Property property = null;
        try {
        	if(CommonUtil.isUuid(propertyId)) {
        		String propertyStr = getValuesByIndex(ServiceConstant.PROPERTY_STR, propertyId);
                if (StringUtils.isNotEmpty(propertyStr)) {
                    property = mapper.readValue(propertyStr, Property.class);
                }
            }else{
                log.info("Ignoring Redis call for ACRS PropertyId- {}",propertyId);
            }
           
       }catch (Exception ex){
           log.error("Error while reading Property from Redis {}: {}",propertyId, ex.getMessage());

       }
        return property;
    }

    @Override
    public List<Property> getPropertyByRegion(String region) {
        if(StringUtils.isBlank(region)){
            region = "ALL";
        }else{
            region = region.toUpperCase();
        }
        // LV or NONLV
        List<Property> properties = null;
       try {
           String propertyListStr = getValuesByIndex(ServiceConstant.REGION_STR, region);
           if (StringUtils.isNotEmpty(propertyListStr)) {
               properties = new ArrayList<>(Arrays.asList(mapper.readValue(propertyListStr, Property[].class)));
           }

       }catch (Exception ex){
           log.error("Error while reading properties from Redis {} : {}- {}",ServiceConstant.REGION_STR,region, ex.getMessage());

       }
       return properties;
    }

    @Override
    public List<TaxDefinition> getPropertyTaxAndFees(String propertyCode) {
        List<TaxDefinition> list = new ArrayList<>();
            String propertyListStr = getValuesByIndex(ServiceConstant.ACRS_PROPERTY_TAXFEES_STR, propertyCode);
            if (StringUtils.isNotEmpty(propertyListStr)) {
                list = new ArrayList<>(Arrays.asList(mapper.readValue(propertyListStr, TaxDefinition[].class)));
            }
            return list;
    }

    public List<String> getPkgComponentCodeByPropertyId(String propertyId) {
    	List<String> pkgComponents = null;
    	try {
            pkgComponents =  getListOfPkgComponents(propertyId);
            return ReservationUtil.componentIdsToCodes(pkgComponents);
    	} catch  (Exception ex){
    		log.error("Error while reading PkgComponents by PropertyId {}: {}",propertyId, ex.getMessage());
    		return Collections.emptyList();
    	}
    }

    public List<String> getListOfPkgComponents(String propertyId) {
        String propertyCode = refDataDAOHelper.retrieveAcrsPropertyID(propertyId);
        List<String> pkgComponents = Collections.emptyList();
        List<PIMPkgComponent> pimPkgComponents = Collections.emptyList();
        try {
           pimPkgComponents = getPkgComponentsByPropertyCode(propertyCode);
        } catch  (Exception ex){
            log.error("Error while reading from redis, hence calling PIM for PkgComponents by PropertyId {}: {}",propertyId, ex.getMessage());
            pimPkgComponents = pimDao.searchPackageComponents(propertyCode, acrsProperties.getNonRoomInventoryType());
        }
        if(CollectionUtils.isNotEmpty(pimPkgComponents)) {
            pkgComponents = pimPkgComponents.stream().map(PIMPkgComponent::getId).collect(Collectors.toList());
        }
        return pkgComponents;
    }

    public List<PIMPkgComponent> getPkgComponentsByPropertyCode(String propertyCode) {
        List<PIMPkgComponent> pimPkgComponents = null;
            String componentListStr = getValuesByIndex(ServiceConstant.PKGCOMPONENT_STR, propertyCode);
            if (StringUtils.isNotEmpty(componentListStr)) {
                pimPkgComponents = new ArrayList<>(Arrays.asList(mapper.readValue(componentListStr, PIMPkgComponent[].class)));
                log.info("Property code {} contains {} pkg components in Redis", propertyCode, pimPkgComponents.size());
                return pimPkgComponents;
            }
        return pimPkgComponents;
    }
}
