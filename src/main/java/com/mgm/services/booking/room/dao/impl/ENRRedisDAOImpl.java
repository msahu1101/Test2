package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ENRRedisDAO;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.service.cache.rediscache.service.impl.BaseRedisReadService;
import com.mgm.services.booking.room.util.JSonMapper;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


@Component
@Log4j2
public class ENRRedisDAOImpl extends BaseRedisReadService implements ENRRedisDAO {

	protected JSonMapper mapper = new JSonMapper();

	@Override
	public ENRRatePlanSearchResponse[] getRatePlansByProperty(List<String> propertyCodes) {
		return getENRRatePlansFromRedis(ServiceConstant.ENR_RATE_PLAN_BY_PROPERTY, propertyCodes);

	}

	private ENRRatePlanSearchResponse[] getENRRatePlansFromRedis(String type, List<String> keyPartList) {
		List<ENRRatePlanSearchResponse> enrRatePlanList = null;

		List<String> programListObjStr = getValuesByIndex(type, keyPartList);
		if (CollectionUtils.isNotEmpty(programListObjStr)) {
			for (String programListStr : programListObjStr) {
				if (StringUtils.isNotEmpty(programListStr)) {
					ENRRatePlanSearchResponse[] programsArr = mapper.readValue(programListStr,
							ENRRatePlanSearchResponse[].class);
					if (CollectionUtils.isEmpty(enrRatePlanList)) {
						enrRatePlanList = new ArrayList<>(Arrays.asList(programsArr));
					} else {
						enrRatePlanList.addAll(new ArrayList<>(Arrays.asList(programsArr)));
					}

				}
			}
		}
		return enrRatePlanList.toArray(new ENRRatePlanSearchResponse[enrRatePlanList.size()]);
	}
	
	private ENRRatePlanSearchResponse[] getENRRatePlansFromRedis(String type, String channel) {
		List<ENRRatePlanSearchResponse> enrRatePlanList = null;

		String programListObjStr = getValuesByIndex(type, channel);

		if (StringUtils.isNotEmpty(programListObjStr)) {
			ENRRatePlanSearchResponse programsArr = mapper.readValue(programListObjStr,
					ENRRatePlanSearchResponse.class);
			if (CollectionUtils.isEmpty(enrRatePlanList)) {
				enrRatePlanList = new ArrayList<>(Arrays.asList(programsArr));
			}
		}

		return enrRatePlanList.toArray(new ENRRatePlanSearchResponse[enrRatePlanList.size()]);
	}

	@Override
	public ENRRatePlanSearchResponse[] getRatePlanById(String ratePlanId) {
		//return getValuesByIndex(ServiceConstant.ENR_RATE_PLAN_BY_ID, ratePlanIds);
		return getENRRatePlansFromRedis(ServiceConstant.ENR_RATE_PLAN_BY_ID, ratePlanId);
	}

	@Override
	public ENRRatePlanSearchResponse[] getRatePlanByCode(List<String> ratePlanCodes) {
		return getENRRatePlansFromRedis(ServiceConstant.ENR_RATE_PLAN_BY_CODE, ratePlanCodes);
	}

	@Override
	public ENRRatePlanSearchResponse[] getRatePlanByChannel(String channel) {
		return getENRRatePlansFromRedis(ServiceConstant.ENR_RATE_PLAN_BY_CHANNEL, Arrays.asList(channel));
	}

	@Override
	public ENRRatePlanSearchResponse[] getRatePlanByPropertyChannel(List<String> propertyCodes, String channel) {
		return getENRRatePlansFromRedis(ServiceConstant.ENR_RATE_PLAN_BY_PROPERTY_CHANNEL.concat(channel), propertyCodes);
	}

	@Override
	public ENRRatePlanSearchResponse[] getPromoByPropertyId(List<String> propertyCodes) {
		return getENRRatePlansFromRedis(ServiceConstant.ENR_PROMO_BY_PROPERTY, propertyCodes);
	}

	@Override
	public ENRRatePlanSearchResponse[] getPromoByCode(String promo) {
		// TODO Auto-generated method stub
		return getENRRatePlansFromRedis(ServiceConstant.ENR_PROMO_BY_CODE, Arrays.asList(promo));
	}
}
