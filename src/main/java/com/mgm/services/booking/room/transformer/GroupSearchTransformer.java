package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.Room;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.crs.groupretrieve.*;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.ReservationUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class providing functions for reservation object transformations
 * required for API outputs.
 *
 */
@UtilityClass
@Log4j2
public class GroupSearchTransformer {

    public static GroupSearchReq composeGroupSearchRequest(GroupSearchV2Request groupSearchRequest,
														   PropertyConfig propertyConfig) {
        final GroupSearchReq request = new GroupSearchReq();
		GroupSearchReqData data = new GroupSearchReqData();
		request.setData(data);
        if(groupSearchRequest.getId() != null) {
			data.setGroupCode(groupSearchRequest.getId());
        }
        if(groupSearchRequest.getGroupName() != null) {
			data.setGroupName(groupSearchRequest.getGroupName());
        }

        if(groupSearchRequest.getPropertyId() != null){
			String propertyCode = getPropertyCodeConsideringPseudoProperties(groupSearchRequest.getPropertyId(),
					propertyConfig);
            List<String> propertyIdList = new ArrayList<>();
            propertyIdList.add(propertyCode);
			data.setPropertyCodes(propertyIdList);
        }
        final SearchPeriod2 searchPeriod = new SearchPeriod2();
        searchPeriod.setStart(LocalDate.parse(groupSearchRequest.getStartDate()));
        searchPeriod.setEnd(LocalDate.parse(groupSearchRequest.getEndDate()));
		data.setStayPeriod(searchPeriod);
        return request;
    }

	private static String getPropertyCodeConsideringPseudoProperties(String propertyCode, PropertyConfig propertyConfig) {
		if (null != propertyConfig) {
			Map<String, PropertyConfig.PropertyValue> propertyValuesMap = propertyConfig.getPropertyValuesMap();
			PropertyConfig.PropertyValue propertyValue = propertyValuesMap.get(propertyCode);
			if (null != propertyValue && StringUtils.isNotBlank(propertyValue.getMasterPropertyCode())) {
				return propertyValue.getMasterPropertyCode();
			}
		}
		return propertyCode;
	}

	/**
     * transform from GroupSearchResGroupBookingReservationSearch to GroupSearchResponse List
     *
     /* @param GroupSearchResGroupBookingReservationSearch
     *            response to transform
     * @return List of GroupSearchResponse
     */
    public static List<GroupSearchV2Response> transform(GroupSearchResGroupBookingReservationSearch groupSearchRS) {
        final List<GroupSearchV2Response> resp = new ArrayList<>();
		if(null != groupSearchRS.getData() && CollectionUtils.isNotEmpty(groupSearchRS.getData().getGroupReservations())) {
			List<GroupSearchV2Response> transformedResponses = groupSearchRS.getData().getGroupReservations().stream()
					.map(GroupSearchTransformer::getGroupSearchV2ResponseFromAcrsResponse)
					.collect(Collectors.toList());
			resp.addAll(transformedResponses);
		}
        return resp;
    }

	private static GroupSearchV2Response getGroupSearchV2ResponseFromAcrsResponse(GroupReservationsGroupBookingReservationSearch response) {
		final GroupSearchV2Response gsResp = new GroupSearchV2Response();

		if(null != response.getGroupIds()) {
			gsResp.setGroupCnfNumber(response.getGroupIds().getGroupCfNumber());
			gsResp.setId(ACRSConversionUtil.createGroupCodeGuid(response.getGroupIds().getGroupCode(), response.getHotel().getPropertyCode()));
		}

		gsResp.setCategory("Group");
		gsResp.setOperaBlockCode(response.getGroupIds().getGroupCode());
		gsResp.setPropertyId(response.getHotel().getPropertyCode());
		gsResp.setName(response.getGroupContract().getName());
		gsResp.setSunday(true);
		gsResp.setMonday(true);
		gsResp.setTuesday(true);
		gsResp.setWednesday(true);
		gsResp.setThursday(true);
		gsResp.setFriday(true);
		gsResp.setSaturday(true);
		gsResp.setOperaBlockName(response.getGroupContract().getName());
		gsResp.setOperaGuaranteeCode(response.getGroupContract().getPolicyTypeCode());
		gsResp.setShortDescription(response.getGroupContract().getName());
		gsResp.setPeriodStartDate(getPeriodStartDate(response.getGroupContract()));
		gsResp.setPeriodEndDate(getPeriodEndDate(response.getGroupContract()));
		// For groupCode GSE is setting TravelPeriod . So for ACRS we are settings those.
		gsResp.setTravelPeriodStart(gsResp.getPeriodStartDate());
		gsResp.setTravelPeriodEnd(gsResp.getPeriodEndDate());
		gsResp.setReservationMethod(response.getReservationMethod());
		gsResp.setRooms(composeRoom(response.getOffer()));
		gsResp.setAgentText(transformAgentText(response.getComments()));
		gsResp.setGroupCode(response.getGroupIds().getGroupCode());
		gsResp.setPublicName(response.getGroupContract().getName());
		gsResp.setIsElastic(response.getGroupContract().isIsElastic());
		gsResp.setSaleStatus(response.getGroupContract().getSaleStatus());
		gsResp.setIndustryType(response.getGroupContract().getIndustryType());
		return gsResp;
	}

	private static Date getPeriodEndDate(GroupContractAndReleaseRulesGroupBookingReservationSearch groupContract) {
		if(null != groupContract.getCorePeriod() && null != groupContract.getCorePeriod().getEnd()) {
			return ReservationUtil.convertLocalDateToDate(groupContract.getCorePeriod().getEnd());
		} else if(null != groupContract.getPeriod() && null != groupContract.getPeriod().getEnd()) {
			return ReservationUtil.convertLocalDateToDate(groupContract.getPeriod().getEnd());
		}
		return null;
	}

	private static Date getPeriodStartDate(GroupContractAndReleaseRulesGroupBookingReservationSearch groupContract) {
		if(null != groupContract.getCorePeriod() && null != groupContract.getCorePeriod().getStart()) {
			return ReservationUtil.convertLocalDateToDate(groupContract.getCorePeriod().getStart());
		} else if(null != groupContract.getPeriod() && null != groupContract.getPeriod().getStart()) {
			return ReservationUtil.convertLocalDateToDate(groupContract.getPeriod().getStart());
		}
		return null;
	}

	private static String transformAgentText(List<Comments> comments) {
		if(CollectionUtils.isEmpty(comments)){
			return null;
		}

		List<String> commentsList = comments.stream()
				//.filter() // filter by TypeOfComments?
				.flatMap(comment -> comment.getText().stream())
				.collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(commentsList)){
			return StringUtils.join(commentsList, ServiceConstant.AGENT_TEXT_NEWLINE);
		} else {
			return null;
		}
	}

    public static List<Room> composeRoom(GroupOfferSearchRes groupOfferSearchRes){
		if (null == groupOfferSearchRes || null == groupOfferSearchRes.getProductUses()) {
			return new ArrayList<>();
		}

		return groupOfferSearchRes.getProductUses().stream()
				.map(ProductUseSearchRes::getInventoryTypeCode)
				.distinct()
				.map(Room::new)
				.collect(Collectors.toList());
    }
    public static RoomProgramBasic transform(GroupSearchV2Response groupRes) {
        RoomProgramBasic res = new RoomProgramBasic();
        res.setProgramId(groupRes.getId());
        res.setPropertyId(groupRes.getPropertyId());
        res.setRatePlanCode(groupRes.getGroupCode());
        res.setTravelPeriodStart(groupRes.getPeriodStartDate());
        res.setTravelPeriodEnd(groupRes.getPeriodEndDate());
        return res;
        
    }

	public static GroupSearchV2Response pseudoPropertyGroupResponse(GroupSearchV2Response response, String propertyCode, String propertyId) {
		GroupSearchV2Response pseudoPropResp = new GroupSearchV2Response();
		pseudoPropResp.setPropertyId(propertyId);
		pseudoPropResp.setId(ACRSConversionUtil.createGroupCodeGuid(response.getGroupCode(),propertyCode));
		pseudoPropResp.setGroupCode(response.getGroupCode());
		pseudoPropResp.setPeriodEndDate(response.getPeriodEndDate());
		pseudoPropResp.setPeriodStartDate(response.getPeriodStartDate());
		return pseudoPropResp;
	}
}
