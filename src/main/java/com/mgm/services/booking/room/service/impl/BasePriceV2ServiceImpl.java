package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.BasePriceRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * Base pricing service Impl to abstract the logic common across multiple
 * pricing V2 service impl.
 */
@Log4j2
public class BasePriceV2ServiceImpl {

    @Autowired
    private RoomProgramCacheService programCacheService;

    @Autowired
    protected ReferenceDataDAOHelper referenceDataDAOHelper;
    
    @Autowired
    private ApplicationProperties appProps;

    /**
     * Performs the program validation functions like getting the program id if
     * promo code is passed, check if the user is applicable for program Id. Also,
     * this method checks for the existence of redemption code, if the program is a
     * my vegas program and requested channel is not ice.
     * 
     * @param programService
     *            Room Program service
     * @param request
     *            Availability request
     * 
     */
    public void validateProgram(RoomProgramService programService, BasePriceRequest request) {

        if (StringUtils.isEmpty(request.getPromoCode()) && StringUtils.isEmpty(request.getProgramId())) {
            return;
        }

        RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
                .getRoomProgramValidateRequest(request);

        RoomProgramValidateResponse validateResponse = programService.validateProgramV2(validateRequest);

        if (validateResponse.isEligible()) {
            request.setProgramId(validateResponse.getProgramId());
            request.setPropertyId(validateResponse.getPropertyId());
        } else {
            log.info("Program {} is not applicable for the customer {}", validateRequest.getProgramId(),
                    validateRequest.getCustomerId());
            throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
        }

        if (ACRSConversionUtil.isAcrsRatePlanGuid(request.getProgramId())) {
            final String propertyCode = ACRSConversionUtil.getPropertyCode(request.getProgramId());
            request.setPropertyId(referenceDataDAOHelper.retrieveGsePropertyID(propertyCode));
        }
        
        if (validateResponse.isMyvegas() && StringUtils.isEmpty(request.getRedemptionCode())
                && !appProps.getBypassMyvegasChannels().contains(CommonUtil.getChannelHeader())) {
            throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
        }

    }

    /**
     * Checks whether the <code>programId</code> is in the &quot;roomProgram&quot;
     * cache and also it contains myVegas tags. If so, the program is considered as
     * myVegas program.
     * 
     * @param programId
     *            program id string
     * @return true, if the programId is myvegas program otherwise false.
     */
    public boolean isMyVegasProgram(String programId) {
        boolean isMyVegasProgram = false;
        if (StringUtils.isNotEmpty(programId)) {
            RoomProgram roomProgram = programCacheService.getRoomProgram(programId);
            isMyVegasProgram = Optional.ofNullable(roomProgram).map(t -> CommonUtil.isContainMyVegasTags(t.getTags()))
                    .orElse(false);
        }
        return isMyVegasProgram;
    }

}
