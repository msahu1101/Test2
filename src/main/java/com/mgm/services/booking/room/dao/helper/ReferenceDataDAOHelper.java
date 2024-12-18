/**
 * Helper class to keep the supporting methods of ReservationDAO class.
 */
package com.mgm.services.booking.room.dao.helper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mgm.services.booking.room.model.Room;
import com.mgm.services.booking.room.model.request.ModificationChangesRequest;
import com.mgm.services.booking.room.properties.AcrsProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.properties.SecretsProperties;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.RequestSourceConfig;
import com.mgm.services.booking.room.util.RequestSourceConfig.SourceDetails;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

import lombok.extern.log4j.Log4j2;

/**
 * Helper class to keep the supporting methods of ReferenceDataDAOHelper class.
 *
 */
@Component
@Log4j2
public class ReferenceDataDAOHelper {

    @Autowired
    private PropertyConfig propertyConfig;
    
    @Autowired
    private RequestSourceConfig requestSourceConfig;
	
	@Autowired
    private SecretsProperties secretProperties;

    @Autowired
    private AcrsProperties acrsProperties;

	public void updateAcrsReferencesFromGse(RoomReservation roomReservation) {
		roomReservation.setPropertyId(retrieveAcrsPropertyID(roomReservation.getPropertyId()));
		roomReservation.setRoomTypeId(
				retrieveRoomTypeDetail(roomReservation.getPropertyId(), roomReservation.getRoomTypeId()));
		if (roomReservation.getProgramId() != null) {
			roomReservation.setProgramId(
					retrieveRatePlanDetail(roomReservation.getPropertyId(), roomReservation.getProgramId()));
		}
		roomReservation.getBookings().forEach(booking -> {
			booking.setProgramId(retrieveRatePlanDetail(roomReservation.getPropertyId(), booking.getProgramId()));
			if (null != booking.getOverrideProgramId()) {
				booking.setOverrideProgramId(
						retrieveRatePlanDetail(roomReservation.getPropertyId(), booking.getOverrideProgramId()));
			}
		});
	}

    public ModificationChangesRequest updateAcrsReferencesFromGse(ModificationChangesRequest modificationChangesRequest){
        ModificationChangesRequest response = modificationChangesRequest;
        String propertyId = modificationChangesRequest.getPropertyId();

        // Update programIds in booking objects
        if (CollectionUtils.isNotEmpty(modificationChangesRequest.getBookings())) {
            response.getBookings().forEach(booking -> {
                // programId field
                String bookingProgramId = booking.getProgramId();
                if (ACRSConversionUtil.isAcrsGroupCodeGuid(bookingProgramId)) {
                    booking.setProgramId(createFormatedGroupCode(propertyId, bookingProgramId));
                    response.setGroupCode(true);
                } else {
                    booking.setProgramId(retrieveRatePlanDetail(propertyId, bookingProgramId));
                }
                // overrideProgramId field
                if (StringUtils.isNotEmpty(booking.getOverrideProgramId())) {
                    booking.setOverrideProgramId(retrieveRatePlanDetail(propertyId, booking.getOverrideProgramId()));
                }
            });
        }

        // update roomTypeId
        if (null != modificationChangesRequest.getRoomTypeId()) {
            response.setRoomTypeId(retrieveRoomTypeDetail(propertyId, modificationChangesRequest.getRoomTypeId()));
        }

        // update programId
        String programId = modificationChangesRequest.getProgramId();
        if (null != programId) {
            if (ACRSConversionUtil.isAcrsGroupCodeGuid(programId)) {
                response.setProgramId(createFormatedGroupCode(propertyId, programId));
                response.setGroupCode(true);
            } else {
                response.setProgramId(retrieveRatePlanDetail(propertyId, programId));
            }
        }
        return response;
    }

    public void updateAcrsReferencesToGse(RoomReservation roomReservation) {
	    if(StringUtils.isNotEmpty(roomReservation.getPropertyId())) {
            roomReservation.setPropertyId(retrieveGsePropertyID(roomReservation.getPropertyId()));
            if(StringUtils.isNotEmpty(roomReservation.getRoomTypeId())) {
                roomReservation.setRoomTypeId(
                        retrieveRoomTypeDetail(roomReservation.getPropertyId(), roomReservation.getRoomTypeId()));
            }
        }

        if (roomReservation.getProgramId() != null) {
            if (roomReservation.getIsGroupCode()) {
                roomReservation.setProgramId(
                        createFormatedGroupCode(roomReservation.getPropertyId(), roomReservation.getProgramId()));
            } else {
                roomReservation.setProgramId(
                        retrieveRatePlanDetail(roomReservation.getPropertyId(), roomReservation.getProgramId()));
            }
        }
        if (CollectionUtils.isNotEmpty(roomReservation.getBookings())) {
            updateAcrsBookingsReferences(roomReservation);
        }

    }
    
    public void updateAcrsBookingsReferences(RoomReservation roomReservation) {
        roomReservation.getBookings().forEach(booking -> {
            if (roomReservation.getIsGroupCode()) {
                booking.setProgramId(createFormatedGroupCode(roomReservation.getPropertyId(), booking.getProgramId()));
            } else {
                booking.setProgramId(retrieveRatePlanDetail(roomReservation.getPropertyId(), booking.getProgramId()));
            }
            if (null != booking.getOverrideProgramId()) {
                booking.setOverrideProgramId(
                        retrieveRatePlanDetail(roomReservation.getPropertyId(), booking.getOverrideProgramId()));
            }

        });
    }

    public void updateAcrsReferencesToGse(List<AuroraPriceResponse> response, boolean isGroupCode) {
        response.stream().forEach(pricingResponse -> mapAcrsDetailsToGse(pricingResponse, isGroupCode));
    }

    private void mapAcrsDetailsToGse(AuroraPriceResponse response, boolean isGroupCode) {
        if (response.getRoomTypeId() != null) {
            response.setRoomTypeId(retrieveRoomTypeDetail(response.getPropertyId(), response.getRoomTypeId()));
        }
        if (response.getProgramId() != null) {
            if (isGroupCode) {
                response.setProgramId(createFormatedGroupCode(response.getPropertyId(), response.getProgramId()));
            } else {
                response.setProgramId(retrieveRatePlanDetail(response.getPropertyId(), response.getProgramId()));
            }
        }
        if (response.getPropertyId() != null) {
            response.setPropertyId(retrieveGsePropertyID(response.getPropertyId()));
        }
    }
    
    public void updateAcrsReferencesToGseV3(List<AuroraPriceV3Response> response, String propertyId) {
        response.stream().forEach(pricingResponse -> mapAcrsDetailsToGseV3(pricingResponse, propertyId));
    }

	private void mapAcrsDetailsToGseV3(AuroraPriceV3Response response, String propertyId) {

		if (response.getRoomTypeId() != null) {
			response.setRoomTypeId(retrieveRoomTypeDetail(propertyId, response.getRoomTypeId()));
		}
		if(response.getTripDetails() != null) {
			response.getTripDetails().forEach(
				tripDetail -> tripDetail.setProgramId(retrieveRatePlanDetail(propertyId, tripDetail.getProgramId())));
		}
	}

    /**
     * retrieve Acrs PropertyID
     * 
     * @param propertyId
     *            Gse property Id input
     * @return Acrs PropertyID
     */
    public String retrieveAcrsPropertyID(String propertyId) {
        if (propertyConfig.getPropertyValuesMap().get(propertyId) != null) {
            return propertyConfig.getPropertyValuesMap().get(propertyId).getAcrsPropertyIds().get(0);
        } else {
            log.error("Unable to find ACRS Property ID from PropertyValuesMap for property: " + propertyId);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
    }

    /**
     * retrieve GSE PropertyID
     * 
     * @param propertyId
     *            ACRS property Id input
     * @return GSE PropertyID
     */
    public String retrieveGsePropertyID(String propertyId) {
        if (propertyConfig.getPropertyValuesMap().get(propertyId) != null) {
            return propertyConfig.getPropertyValuesMap().get(propertyId).getGsePropertyIds().get(0);
        } else {
            log.error("Unable to find GSE Property ID from PropertyValuesMap for property: " + propertyId);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
    }

    /**
     * retrieve MerchantID
     * 
     * @param id
     *            propertyId
     * @return MerchantID
     */
    public String retrieveMerchantID(String id) {
        final String merchantID = propertyConfig.getPropertyValuesMap().get(id) != null
                ? propertyConfig.getPropertyValuesMap().get(id).getGseMerchantID()
                : null;
        if (merchantID != null) {
            return merchantID;
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "No Merchant id found for property id: " + id);
        }
    }

    public int getPatronSiteId(String propertyId) {
        final PropertyConfig.PropertyValue propertyValue = propertyConfig.getPropertyValuesMap().get(propertyId);
        final Integer patronSiteId = propertyValue != null ? propertyValue.getPatronSiteId(): null;
        if (patronSiteId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "No PatronSite id found for property id: " + propertyId);
        }
        return patronSiteId;
    }

    /**
     * retrieve RoomType Detail based on property and Acrs Room Type or Gse Guid
     * 
     * @param propertyId
     *            property id input.
     * @param roomTypeInput
     *            Acrs Room Type or Gse Guid
     * @return Acrs Room Type or Gse Guid
     */
    public String retrieveRoomTypeDetail(String propertyId, String roomTypeInput) {
    	
		String propertyCode = propertyId;
		if (CommonUtil.isUuid(propertyId)) {
			propertyCode = retrieveAcrsPropertyID(propertyId);
		}

		if (CommonUtil.isUuid(roomTypeInput)) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "roomTypeId - GUIDs are not supported: " + roomTypeInput);

		}
		return ACRSConversionUtil.isAcrsRoomCodeGuid(roomTypeInput) ? ACRSConversionUtil.getRoomCode(roomTypeInput)
				: ACRSConversionUtil.createRoomCodeGuid(roomTypeInput, propertyCode);
    }

    /**
     * This method retrieve RatePlan Detail based on property and Acrs rate plan or Gse
     * program id Guid
     * @param propertyId
     * @param ratePlanInput
     * @return
     */
    public String retrieveRatePlanDetail(String propertyId, String ratePlanInput) {
        return retrieveRatePlanDetail(propertyId, ratePlanInput, true);
    }

    /**
     * This method retrieve RatePlan Detail based on property and Acrs rate plan or Gse
     * program id Guid
     * 
     * @param propertyId
     *            property id input.
     * @param ratePlanInput
     *            Acrs rate plan or Gse program id Guid
     * @return Acrs rate plan or Gse program id Guid
     */
	public String retrieveRatePlanDetail(String propertyId, String ratePlanInput, boolean doErrorOut) {

		String propertyCode = propertyId;
		if (CommonUtil.isUuid(propertyId)) {
			propertyCode = retrieveAcrsPropertyID(propertyId);
		}

		if (CommonUtil.isUuid(ratePlanInput)) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "programId - GUIDs are not supported: " + ratePlanInput);
		}
		return ACRSConversionUtil.isAcrsRatePlanGuid(ratePlanInput) ? ACRSConversionUtil.getRatePlanCode(ratePlanInput)
				: ACRSConversionUtil.createRatePlanCodeGuid(ratePlanInput, propertyCode);

	}

	public String createFormatedGroupCode(String propertyId, String groupInput) {
	    String propertyCode = propertyId;
	    if (CommonUtil.isUuid(propertyId)) {
	        propertyCode = retrieveAcrsPropertyID(propertyId);
	    }

	    if (CommonUtil.isUuid(groupInput)) {
	        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Group programId - GUIDs are not supported: " + groupInput);
	    }
	    return ACRSConversionUtil.isAcrsGroupCodeGuid(groupInput) ? ACRSConversionUtil.getGroupCode(groupInput)
	            : ACRSConversionUtil.createGroupCodeGuid(groupInput, propertyCode);

	}
	
    public void updateGroupAcrsReferencesToGse(List<GroupSearchV2Response> groupSearchV2ResponseList) {
        groupSearchV2ResponseList.forEach(groupSearchV2Response -> {
           String propertyCode = groupSearchV2Response.getPropertyId();
            final List<Room> rooms = groupSearchV2Response.getRooms();
            if (CollectionUtils.isNotEmpty(rooms)) {
                rooms.forEach(room -> {
                    if (room.getId() != null) {
                        room.setId(retrieveRoomTypeDetail(propertyCode, room.getId()));
                    }
                });
            }
            groupSearchV2Response.setPropertyId(retrieveGsePropertyID(propertyCode));
        });
    }

    public String getAcrsVendor(String source) {
        return getAcrsVendor(source, false);
    }

    public String getAcrsVendor(String source, boolean allowInvalidSource) {
        source = source != null ? source.toLowerCase() : null;
        final SourceDetails sourceDetails = requestSourceConfig.getRequestSourcesMap().get(source);
        if (null != sourceDetails) {
            return sourceDetails.getAcrsVendor();
        }

        if (!allowInvalidSource) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, ServiceConstant.INVALID_SOURCE);
        }
        return null;
    }
    
    public SourceDetails getRequestSource(String source) {
        SourceDetails sourceDetails = requestSourceConfig.getRequestSourcesMap().get(source);
        if (null == sourceDetails) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, ServiceConstant.INVALID_SOURCE);
        }
        return sourceDetails;
    }
    
	public String getChannelName(String acrsSubChannel) {
		Optional<SourceDetails> sourceDetails = requestSourceConfig.getRequestSourcesMap().values().stream()
				.filter(source -> source.getAcrsSubChannel().equalsIgnoreCase(acrsSubChannel)).findFirst();
		if (sourceDetails.isPresent()) {
			return sourceDetails.get().getChannelName();
		}
		return "channel";
	}
    public boolean isPETDisabled(){
        String petDisabledStr = secretProperties.getSecretValue(acrsProperties.getPetDisabledKey());
        if(StringUtils.isNotBlank(petDisabledStr)){
            return Boolean.parseBoolean(petDisabledStr);
        }else{
            return false;
        }
    }
	
	public boolean isPropertyManagedByAcrs(String propertyId) {
        if (StringUtils.isNotEmpty(propertyId)) {
            String propertyCode = propertyId;
            if (CommonUtil.isUuid(propertyId) && null != propertyConfig.getPropertyValuesMap()
                    && null != propertyConfig.getPropertyValuesMap().get(propertyId)) {
                List<String> acrsPropertyDetails = propertyConfig.getPropertyValuesMap().get(propertyId).getAcrsPropertyIds();
                if (CollectionUtils.isNotEmpty(acrsPropertyDetails)) {
                    propertyCode = acrsPropertyDetails.get(0);
                }
            }
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(propertyCode)) {
                String acrsEnabledProperties = secretProperties.getSecretValue(acrsProperties.getAcrsPropertyListSecretKey());
                if (StringUtils.isNotEmpty(acrsEnabledProperties)) {
                    return StringUtils.contains(acrsEnabledProperties, propertyCode);
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean isAcrsEnabled() {
        String acrsEnabledProperties = secretProperties.getSecretValue(acrsProperties.getAcrsPropertyListSecretKey());
         if (org.apache.commons.lang3.StringUtils.isNotBlank(acrsEnabledProperties)) {
             return org.apache.commons.lang3.StringUtils.isNotEmpty(acrsEnabledProperties);
         } else {
             return false;
         }
    }

    public String getPseudoPropertyCode(String propertyId) {
        if (null != propertyConfig) {
            Map<String, PropertyConfig.PropertyValue> propertyValuesMap = propertyConfig.getPropertyValuesMap();
            PropertyConfig.PropertyValue propertyValue = propertyValuesMap.get("MASTER-"+propertyId);
            if (null != propertyValue && null != propertyValue.getAcrsPropertyIds() &&
                    StringUtils.isNotBlank(propertyValue.getAcrsPropertyIds().get(0))) {
                return propertyValue.getAcrsPropertyIds().get(0);
            }
        }
        return null;
    }
}
