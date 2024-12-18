package com.mgm.services.booking.room.service.cache.rediscache.service.impl;

import com.mgm.services.booking.room.dao.ENRRedisDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.RatePlanFilterRequest;
import com.mgm.services.booking.room.model.request.RedisRequest;
import com.mgm.services.booking.room.model.request.RoomProgramPromoAssociationRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.service.cache.rediscache.service.ENRRatePlanRedisService;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Log4j2
public class ENRRatePlanRedisServiceImpl implements ENRRatePlanRedisService {
	
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	@Autowired
    private ENRRedisDAO enrRedisDAO;

    @Autowired
    private ReferenceDataDAOHelper refDataDAOHelper;
    
    @Override
    public ENRRatePlanSearchResponse[] searchRatePlans(ApplicableProgramRequestDTO request,String vendorCode) {
        ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
        String propertyCode = refDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
        List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
        //step 1: Get Channel rate from redis
        ENRRatePlanSearchResponse[] channelrates = getChannelRatePlans(propertyCodes,vendorCode);
        
        //Step 2: get rate codes from channel rate response
        String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        List<String> rateCodesList = Arrays.asList(rateCodes);
        
        //step 3: get rate plan list from redis with given input
        RedisRequest redisRequst = new RedisRequest(request.getPropertyId(), null, null, propertyCode);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
        
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(request.getTravelDate(), request.getCheckInDate(), request.getCheckOutDate(), request.getBookDate(), true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
        
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by propertyId
        boolean isPromoSearch = true;
        ENRRatePlanSearchResponse[] promos = new ENRRatePlanSearchResponse[1];
        if(isPromoSearch) {
        	promos = getPromos(propertyCodes, null);
        }
        //step 7: filter promos on basis of isActive = true and status != deleted
        promos = filterPromos(promos);
        
        //Step 8: merge promo associated rate plans
        ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans, null);
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
        
        //create testing ticket for testing applicable program
        return finalRatePlans;
    }
    
    @Override
	public ENRRatePlanSearchResponse[] searchRatePlansRoomProgram(RoomProgramsRequestDTO request, String vendorCode) {
    	ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
        String propertyCode = refDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
        
        List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
        
        //step 1: Get Channel rate from redis
        ENRRatePlanSearchResponse[] channelrates = getChannelRatePlans(propertyCodes,vendorCode);
        
        //Step 2: get rate codes from channel rate response
        String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        List<String> rateCodesList = Arrays.asList(rateCodes);
        
        //step 3: get rate plan list from redis with given input
        RedisRequest redisRequst = new RedisRequest(request.getPropertyId(), null, null, propertyCode);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
        
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(null, null, null, new Date(), true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
		
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by propertyId
        boolean isPromoSearch = true;
        ENRRatePlanSearchResponse[] promos = new ENRRatePlanSearchResponse[1];
        if(isPromoSearch) {
        	promos = getPromos(propertyCodes, null);
        }
        
        //step 7: filter promos on basis of isActive = true and status != deleted
        promos = filterPromos(promos);
        
        //Step 8: merge promo associated rate plans
        ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans,null);
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
        
        return finalRatePlans;
	}
    
    @Override
	public ENRRatePlanSearchResponse[] searchRatePlanById(RoomProgramV2Request request, String vendorCode, String propertyCode, List<String> programIds) {
    	ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
    	List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
        
        //step 1: Get Channel rate from redis
        ENRRatePlanSearchResponse[] channelrates = null;
        if(StringUtils.isNotEmpty(propertyCode)) {
        	channelrates = getChannelRatePlans(propertyCodes,vendorCode);
        }
        
        //Step 2: get rate codes from channel rate response
        List<String> rateCodesList = null;
        if(ArrayUtils.isNotEmpty(channelrates)) {
        	String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        	rateCodesList = Arrays.asList(rateCodes);
        }
        
        //step 3: get rate plan list from redis with given input
        String ids = programIds.stream().collect(Collectors.joining(","));
        RedisRequest redisRequst = new RedisRequest(refDataDAOHelper.retrieveGsePropertyID(propertyCode), ids, null, null);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
        
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(null, null, null, null, true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
        
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by propertyId
        boolean isPromoSearch = request.isPromoSearch();
        
        if(isPromoSearch) {
        	ENRRatePlanSearchResponse[] promos = getPromos(propertyCodes, null);
        	
        	if(ArrayUtils.isNotEmpty(promos)) {
        		//step 7: filter promos on basis of isActive = true and status != deleted
                promos = filterPromos(promos);
                
                //Step 8: merge promo associated rate plans
                ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans, null);
        	}
        }
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
    	
    	return finalRatePlans;
	}
    
    @Override
	public ENRRatePlanSearchResponse[] searchRatePlansToValidate(RoomProgramValidateRequest request, String vendorCode,
			List<String> ratePlanIds, String propertyCode) {
    	ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
    	
    	List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
		
        //step 1: Get Channel rate from redis
        ENRRatePlanSearchResponse[] channelrates = getChannelRatePlans(propertyCodes,vendorCode);
        
        //Step 2: get rate codes from channel rate response
        String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        List<String> rateCodesList = Arrays.asList(rateCodes);
        
        //step 3: get rate plan list from redis with given input
        RedisRequest redisRequst = new RedisRequest(refDataDAOHelper.retrieveGsePropertyID(propertyCode), 
        		ratePlanIds.stream().collect(Collectors.joining(",")), null, propertyCode);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
        
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(null, null, null, null, true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
        
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by propertyId
        boolean isPromoSearch = true;
        ENRRatePlanSearchResponse[] promos = new ENRRatePlanSearchResponse[1];
        if(isPromoSearch) {
        	promos = getPromos(propertyCodes, request.getPromo());
        }
        
        //step 7: filter promos on basis of isActive = true and status != deleted
        promos = filterPromos(promos);
        
        //Step 8: merge promo associated rate plans
        ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans, null);
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
        
        return finalRatePlans;
	}
    
    @Override
	public String searchPromoByCode(String propertyId, String promoCode, String vendorCode) {
    	ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
    	
    	String propertyCode = refDataDAOHelper.retrieveAcrsPropertyID(propertyId);
    	List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
    	
        //step 1: Get Channel rate from redis
        ENRRatePlanSearchResponse[] channelrates = getChannelRatePlans(propertyCodes,vendorCode);
        
        //Step 2: get rate codes from channel rate response
        String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        List<String> rateCodesList = Arrays.asList(rateCodes);
        
        //step 3: get rate plan list from redis with given input
        RedisRequest redisRequst = new RedisRequest(propertyId, null, null, null);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
        
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(null, null, null, null, true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
        
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by propertyId
        boolean isPromoSearch = true;
        ENRRatePlanSearchResponse[] promos = new ENRRatePlanSearchResponse[1];
        if(isPromoSearch) {
        	promos = getPromos(propertyCodes, promoCode);
        }
        
        //step 7: filter promos on basis of isActive = true and status != deleted
        promos = filterPromos(promos);
        
        //Step 8: merge promo associated rate plans
        ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans,"PROMO");
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
        
        ENRRatePlanSearchResponse ratePlanRes = finalRatePlans[0];
        
		return ratePlanRes.getRatePlanId();
	}
    
    // End result may differ due to requirement at step 1
    @Override
	public ENRRatePlanSearchResponse[] searchRatePlanByCode(String ratePlanCode, String vendorCode) {
    	ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
    	
    	//step 1: Get Channel rate from redis
    	// We need to introduce channelRatePlan along with ratePlanCode. Similar implementation we have for propertyId
        ENRRatePlanSearchResponse[] channelrates = getChannelRatePlans(null,vendorCode);
        
        //Step 2: get rate codes from channel rate response
        String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        List<String> rateCodesList = Arrays.asList(rateCodes);
        
        //step 3: get rate plan list from redis with given input
        RedisRequest redisRequst = new RedisRequest(null, null, ratePlanCode, null);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
    	
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(null, null, null, null, true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
        
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by propertyId -No need of this step because we are searching rateplan by code
        boolean isPromoSearch = false;
        
        if(isPromoSearch) {
        	ENRRatePlanSearchResponse[] promos = getPromos(null, null);
        	
        	if(ArrayUtils.isNotEmpty(promos)) {
        		//step 7: filter promos on basis of isActive = true and status != deleted
                promos = filterPromos(promos);
                
                //Step 8: merge promo associated rate plans
                ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans,null);
        	}
        }
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
        
		return finalRatePlans;
	}
    
    @Override
	public ENRRatePlanSearchResponse[] searchPromoRatePlans(RoomProgramPromoAssociationRequest request,
			String vendorCode, List<String> programIds, String propertyCode, String searchType) {
    	ENRRatePlanSearchResponse[] finalRatePlans = new ENRRatePlanSearchResponse[1];
    	
        List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
        
        //step 1: Get Channel rate from redis
        ENRRatePlanSearchResponse[] channelrates = getChannelRatePlans(propertyCodes,vendorCode);
        
        //Step 2: get rate codes from channel rate response
        String [] rateCodes = getRateCodesFromChannelRates(channelrates);
        List<String> rateCodesList = Arrays.asList(rateCodes);
        
        //step 3: get rate plan list from redis with given input
        String ratePlanIdsString = programIds.stream().collect(Collectors.joining(","));
        RedisRequest redisRequst = new RedisRequest(refDataDAOHelper.retrieveGsePropertyID(propertyCode),ratePlanIdsString , null, null);
        ENRRatePlanSearchResponse[] ratePlans = getRatePlans(redisRequst);
		
        //Step 4 filter on basis of dates , rateCodes and other params - put inside rate plans
        RatePlanFilterRequest ratePlanFilterRequest = new RatePlanFilterRequest(null, null, null, null, true);
        ratePlans = filterRatePlansOnRequestParameters(ratePlans,ratePlanFilterRequest,rateCodesList);
        
        //setp 5: merge channel and rateplan response
        ratePlans = mergeChannelFeedAndRatePlans(channelrates,ratePlans);
        
        //step 6: call get Promo by promo code
        boolean isPromoSearch = true;
        
        if(isPromoSearch) {
        	ENRRatePlanSearchResponse[] promos = getPromos(propertyCodes, request.getPromo());
        	
        	if(ArrayUtils.isNotEmpty(promos)) {
        		//step 7: filter promos on basis of isActive = true and status != deleted
                promos = filterPromos(promos);
                
                //Step 8: merge promo associated rate plans
                ratePlans = mergePromoAssociatedRatePlansAndSearchRatePlan(promos,ratePlans,searchType);
        	}
        }
        
        //Step9: Update propertyId from propertyCode
        finalRatePlans = updatePropertyIdInRatePlans(ratePlans);
        
        //create testing ticket for testing applicable program
        return finalRatePlans;
	}
    
    private ENRRatePlanSearchResponse[] updatePropertyIdInRatePlans(ENRRatePlanSearchResponse[] ratePlans) {
    	List<ENRRatePlanSearchResponse> updatedRatePlans = Arrays.asList(ratePlans);
    	updatedRatePlans.forEach(x -> x.setPropertyId(refDataDAOHelper.retrieveGsePropertyID(x.getPropertyCode())));
    	
    	return updatedRatePlans.toArray(new ENRRatePlanSearchResponse[updatedRatePlans.size()]);
	}

	private ENRRatePlanSearchResponse[] filterPromos(ENRRatePlanSearchResponse[] promos) {
		
    	ENRRatePlanSearchResponse[] filteredPromos=  Stream.of(promos)
    			.filter(x -> x.getIsActive())
    			.filter(x -> (StringUtils.isEmpty(x.getStatus()) || (StringUtils.isNotEmpty(x.getStatus()) && !x.getStatus().equalsIgnoreCase("deleted"))))
    			.toArray(ENRRatePlanSearchResponse[]::new);
		return filteredPromos;
	}

	private ENRRatePlanSearchResponse[] mergePromoAssociatedRatePlansAndSearchRatePlan(
			ENRRatePlanSearchResponse[] promos, ENRRatePlanSearchResponse[] ratePlans, String searchType) {
    	final List<ENRRatePlanSearchResponse> allPromoRatePlans = new ArrayList<>();
		List<ENRRatePlanSearchResponse> ratePlanList = Arrays.asList(ratePlans);
		final Map<String, ENRRatePlanSearchResponse> ratePlanMap = ratePlanList.stream().collect(
                Collectors.toMap(x -> x.getRateCode() + x.getPropertyCode(), Function.identity(), (a1, a2) -> a1));
		for(ENRRatePlanSearchResponse promoRatePlan: promos) {
			final ENRRatePlanSearchResponse singleRatePlan = ratePlanMap.get(promoRatePlan.getRateCode() + promoRatePlan.getPropertyCode());
			if(null != singleRatePlan) {
				allPromoRatePlans.add(createPromoRatePlan(promoRatePlan, singleRatePlan));
			}
		}
		
		// As search type not defined, default search type = ALL considered. Hence add existing rateplans
		if(StringUtils.isEmpty(searchType)) {
			allPromoRatePlans.addAll(ratePlanMap.values());
		}
		return  allPromoRatePlans.toArray(new ENRRatePlanSearchResponse[allPromoRatePlans.size()]);
	}

	private ENRRatePlanSearchResponse createPromoRatePlan(ENRRatePlanSearchResponse promoRatePlan,
			ENRRatePlanSearchResponse singleRatePlan) {
		final ENRRatePlanSearchResponse clonedRatePlan = (null != singleRatePlan) ? singleRatePlan.toBuilder().build() : new ENRRatePlanSearchResponse();
		clonedRatePlan.setPromo(promoRatePlan.getPromo());
		
		final Boolean isActive =promoRatePlan.getIsActive();
		if(null != isActive) {
			clonedRatePlan.setIsActive(isActive);
		}
		final String bookingStartDate = promoRatePlan.getBookingStartDate();
		if(StringUtils.isNotEmpty(bookingStartDate)) {
			clonedRatePlan.setBookingStartDate(bookingStartDate);
		}
		final String bookingEndDate = promoRatePlan.getBookingEndDate();
		if(StringUtils.isNotEmpty(bookingEndDate)) {
			clonedRatePlan.setBookingEndDate(bookingEndDate);
		}
		final String travelStartDate = promoRatePlan.getTravelStartDate();
		if(StringUtils.isNotEmpty(travelStartDate)) {
			clonedRatePlan.setTravelStartDate(travelStartDate);
		}
		final String travelEndDate = promoRatePlan.getTravelEndDate();
		if(StringUtils.isNotEmpty(travelEndDate)) {
			clonedRatePlan.setTravelEndDate(travelEndDate);
		}
		final String description = promoRatePlan.getDescription();
		if(StringUtils.isNotEmpty(description)) {
			clonedRatePlan.setDescription(description);
		}
		
		final String ratePlanName = promoRatePlan.getName();
		if(StringUtils.isNotEmpty(ratePlanName)) {
			clonedRatePlan.setName(ratePlanName);
		}

		
		return clonedRatePlan;
	}

	private ENRRatePlanSearchResponse[] getPromos(List<String> propertyCodes, String promo ) {
    	ENRRatePlanSearchResponse[] channelRates = new ENRRatePlanSearchResponse[1];
    	if(StringUtils.isNotEmpty(promo)) {
    		channelRates =  enrRedisDAO.getPromoByCode(promo);
    	}
    	
    	if(StringUtils.isEmpty(promo) && CollectionUtils.isNotEmpty(propertyCodes)) {
    		channelRates =  enrRedisDAO.getPromoByPropertyId(propertyCodes);
    	}
    	return channelRates;
	}

	private ENRRatePlanSearchResponse[] mergeChannelFeedAndRatePlans(ENRRatePlanSearchResponse[] channelrates,
			ENRRatePlanSearchResponse[] ratePlans) {
    	List<ENRRatePlanSearchResponse> ratePlanDataList = Arrays.asList(ratePlans);
    	 final Map<String, ENRRatePlanSearchResponse> ratePlanMap = ratePlanDataList.stream().collect(
                 Collectors.toMap(x -> x.getRateCode() + x.getPropertyCode(), Function.identity(), (a1, a2) -> a1));

         final List<ENRRatePlanSearchResponse> commonRatePlanList = new ArrayList<>();
         List<ENRRatePlanSearchResponse> channelDataRates = Arrays.asList(channelrates);
         if (CollectionUtils.isNotEmpty(channelDataRates)) {
             for (ENRRatePlanSearchResponse channelFeed : channelDataRates) {

                     final String channelPropertyCode = channelFeed.getPropertyCode();
                     
                         final String ratePlanCode = channelFeed.getRateCode();
                         final ENRRatePlanSearchResponse ratePlan = ratePlanMap.get(ratePlanCode + channelPropertyCode);
                         if (null != ratePlan) {
                             commonRatePlanList.add(ratePlan);
                             overrideRatePlanData(channelFeed, ratePlan);
                         }
             }
         } else {
             // In case its a request with empty channel then return all the ratePlans
             commonRatePlanList.addAll(ratePlanMap.values());
         }

         return commonRatePlanList.toArray(new ENRRatePlanSearchResponse[commonRatePlanList.size()]);
		
	}

	private void overrideRatePlanData(ENRRatePlanSearchResponse channelFeed, ENRRatePlanSearchResponse ratePlan) {
		try {
			ratePlan.setIsActive(channelFeed.getIsActive());
			ratePlan.setIsPublic(channelFeed.getIsPublic());
			ratePlan.setRateCode(channelFeed.getRateCode());
			final String ratePlanName = channelFeed.getName();
			if(StringUtils.isNotEmpty(ratePlanName)) {
				ratePlan.setName(ratePlanName);
			}
			final String longDescription = channelFeed.getLongDescription();
			if(StringUtils.isNotEmpty(longDescription)) {
				ratePlan.setLongDescription(longDescription);
			}
			final String shortDescription = channelFeed.getDescription();
			if(StringUtils.isNotEmpty(shortDescription)) {
				ratePlan.setDescription(shortDescription);
			}
			final String bookingMessage = channelFeed.getBookingMessage();
			if(StringUtils.isNotEmpty(bookingMessage)) {
				ratePlan.setBookingMessage(bookingMessage);
			}
			final String bookingStartDate =channelFeed.getBookingStartDate();
			if(StringUtils.isNotEmpty(bookingStartDate)) {
				ratePlan.setBookingStartDate(bookingStartDate);
			}
			final String bookingEndDate = channelFeed.getBookingEndDate();
			if(StringUtils.isNotEmpty(bookingEndDate)) {
				ratePlan.setBookingEndDate(bookingEndDate);
			}
			ratePlan.setSequenceNo(channelFeed.getSequenceNo());
			ratePlan.setRoomTypeCodes(channelFeed.getRoomTypeCodes());
		} catch (Exception e) {
			log.warn("Exception while overriding ratePlan with channel feed");
		}
		
	}

	private ENRRatePlanSearchResponse[] filterRatePlansOnRequestParameters(ENRRatePlanSearchResponse[] ratePlans, RatePlanFilterRequest ratePlanFilterRequest, List<String> rateCodesList) {
    	List<ENRRatePlanSearchResponse> ratePlansList = Arrays.asList(ratePlans);
    	Date travelDate =ratePlanFilterRequest.getTravelDate();
    	Date checkinDate = ratePlanFilterRequest.getCheckInDate();
    	Date checkOutDate = ratePlanFilterRequest.getCheckOutDate();
    	final Date fixCheckinDate;
    	
    	//Filter on basis of rate codes receied from channel feed
    	if(CollectionUtils.isNotEmpty(rateCodesList)) {
    		ratePlansList = ratePlansList.stream().filter(x -> rateCodesList.contains(x.getRateCode())).collect(Collectors.toList());
    	}
    	
    	// if travelDate is not null and (checkinDate is null or travelDate is before checkinDate) then checkinDate = travelDate
    	//Filter by travelStart Date
    	if (null != travelDate && (checkinDate== null || travelDate.before(checkinDate))) {
            // If travel date is before than than check-in date then consider travel date to validate instead
    		fixCheckinDate = travelDate;
    	}else {
    		fixCheckinDate = checkinDate;
    	}
    	
    	if(null != fixCheckinDate) {
    		ratePlansList = ratePlansList.stream()
        			.filter(x -> StringUtils.isEmpty(x.getTravelStartDate()) || 
        					(StringUtils.isNotEmpty(x.getTravelStartDate()) && validDates(x.getTravelStartDate(),dateToString(fixCheckinDate))))
        			.collect(Collectors.toList());
    	}
    	
    	
    	//filter by travleEndDate
    	if(null != checkOutDate) {
    		ratePlansList = ratePlansList.stream()
    				.filter(x -> StringUtils.isEmpty(x.getTravelEndDate()) || 
    						(StringUtils.isNotEmpty(x.getTravelEndDate()) && validDates(dateToString(checkOutDate), x.getTravelEndDate())))
    				.collect(Collectors.toList());
    	}
    	
    	// filter by triplength
    	if(null != checkinDate &&  null != checkOutDate) {
    		if(!validDates(dateToString(checkinDate), dateToString(checkOutDate))) {
    			log.warn("TravelStartDate can not be before than travelEndDate");
    			throw new ValidationException(ErrorCode.INVALID_DATES, null);
    		}
    		
    		long tripLength = geTripLength(dateToString(checkinDate) ,dateToString(checkOutDate));
    		ratePlansList = ratePlansList.stream()
    				.filter(x -> StringUtils.isEmpty(x.getMaxLos()) ||  (StringUtils.isNotEmpty(x.getMaxLos()) && Long.parseLong(x.getMaxLos()) >= tripLength))
    				.filter(x -> StringUtils.isEmpty(x.getMinLos()) || (StringUtils.isNotEmpty(x.getMinLos()) && Long.parseLong(x.getMinLos()) <= tripLength))
    				.collect(Collectors.toList()); 
    	}
    	//Filter by isActive - note isActive is not getting in redis response
    	//ratePlansList = ratePlansList.stream().filter(x -> x.getIsActive()).collect(Collectors.toList());
    	
    	//filter by status
    	ratePlansList = ratePlansList.stream().filter(x ->(x.getStatus().equalsIgnoreCase(null) || StringUtils.isEmpty(x.getStatus()) || !x.getStatus().equalsIgnoreCase("Deleted"))).collect(Collectors.toList());
    	
    	//filter by bookDate - redis response BookingStartDate <= requestBookDate and BookingEndDate >= requestBookDate
    	if(null != ratePlanFilterRequest.getBookDate()) {
    		ratePlansList = ratePlansList.stream()
        			.filter(x -> (StringUtils.isEmpty(x.getBookingStartDate()) || (StringUtils.isNotEmpty(x.getBookingStartDate()) && validDates(x.getBookingStartDate(), dateToString(ratePlanFilterRequest.getBookDate()))))
        					&& (StringUtils.isEmpty(x.getBookingEndDate()) || ( StringUtils.isNotEmpty(x.getBookingEndDate()) && validDates(dateToString(ratePlanFilterRequest.getBookDate()), x.getBookingEndDate())))).collect(Collectors.toList());
    	}
    	
    	return ratePlansList.toArray(new ENRRatePlanSearchResponse[ratePlansList.size()]);
    			
	}
    
    private long geTripLength(String travelStartDate, String travelEndDate) {
        LocalDate startTravelDate = LocalDate.parse(travelStartDate, DateTimeFormatter.ofPattern("yyyy-MM-d"));
        LocalDate endTravelDate = LocalDate.parse(travelEndDate, DateTimeFormatter.ofPattern("yyyy-MM-d"));
        return ChronoUnit.DAYS.between(startTravelDate, endTravelDate);
    }

    private String dateToString(Date date) {
    	return dateFormat.format(date);
    }
    
	private boolean validDates(String startDate, String endDate) {
        LocalDate startDateLocal = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDateLocal = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return startDateLocal.isBefore(endDateLocal) || startDateLocal.equals(endDateLocal);
    }

	private String[] getRateCodesFromChannelRates(ENRRatePlanSearchResponse[] channelrates) {
    	List<ENRRatePlanSearchResponse> channelRatePlanList = Arrays.asList(channelrates);
    	Set<String> rateCodes = new HashSet<>();
    	rateCodes.addAll(channelRatePlanList.stream().map(x -> x.getRateCode()).collect(Collectors.toSet()));
        return rateCodes.toArray(new String[0]);
	}

	private ENRRatePlanSearchResponse[] getChannelRatePlans(List<String> propertyCodes, String channel) {
    	ENRRatePlanSearchResponse[] channelRates = new ENRRatePlanSearchResponse[1];
    	if(CollectionUtils.isNotEmpty(propertyCodes) && StringUtils.isNotBlank(channel)) {
    		channelRates =  enrRedisDAO.getRatePlanByPropertyChannel(propertyCodes,channel);
    	}
    	if(StringUtils.isNotEmpty(channel) && CollectionUtils.isEmpty(propertyCodes)) {
    		channelRates = enrRedisDAO.getRatePlanByChannel(channel);
    	}
    	return channelRates;
    }
    
	private ENRRatePlanSearchResponse[] getRatePlans(RedisRequest redisRequst) {
		ENRRatePlanSearchResponse[] ratePlans = new ENRRatePlanSearchResponse[1];
		
		//Get rateplanIds into list if present. rateplanIds will be in string comma seperated
		List<String> ratePlanIds = null;
		if(StringUtils.isNotEmpty(redisRequst.getRatePlanId())) {
			ratePlanIds = Arrays.asList(redisRequst.getRatePlanId().split(","));
		}
		
		List<String> ratePlanCodesList = null;
		if(StringUtils.isNotEmpty(redisRequst.getRateCode())) {
			ratePlanCodesList = Arrays.asList(redisRequst.getRateCode());
		}
		String propertyCode = refDataDAOHelper.retrieveAcrsPropertyID(redisRequst.getPropertyId());
        List<String> propertyCodes = new ArrayList<>();
        propertyCodes.add(propertyCode);
		if (CollectionUtils.isNotEmpty(ratePlanIds)) {
			for (String singleRatePlan : ratePlanIds) {
				ratePlans = enrRedisDAO.getRatePlanById(singleRatePlan);
			}
		}
		if (CollectionUtils.isEmpty(ratePlanIds) && CollectionUtils.isNotEmpty(propertyCodes)) {
			ratePlans = enrRedisDAO.getRatePlansByProperty(propertyCodes);
		}
		if (CollectionUtils.isNotEmpty(ratePlanCodesList)) {
			ratePlans = enrRedisDAO.getRatePlanByCode(ratePlanCodesList);
		}
		return ratePlans;

		// addFilter for travel start and travel end and booking start booking end,
		// status, trip lenght
	}

}
