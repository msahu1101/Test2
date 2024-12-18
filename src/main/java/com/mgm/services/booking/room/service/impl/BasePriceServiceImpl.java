package com.mgm.services.booking.room.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.BasePriceRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * Base pricing service Impl to abstract the logic common across multiple
 * pricing service impl.
 */
@Log4j2
public class BasePriceServiceImpl {
    @Autowired
    private RoomProgramCacheService programCacheService;

    /**
     * Performs the program validation functions like getting the program id if
     * promo code is passed, check if the user is applicable for program Id.
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

        if (StringUtils.isNotEmpty(request.getPromoCode())) {

            String programId = programService.getProgramByPromoCode(request.getPromoCode(), request.getPropertyId());

            if (StringUtils.isEmpty(programId)) {
                validateRequest.setProgramId(StringUtils.EMPTY);
            } else {
                validateRequest.setProgramId(programId);
                request.setProgramId(programId);
            }

        } else if (StringUtils.isNotEmpty(request.getProgramId())) {
            validateRequest.setProgramId(request.getProgramId());
        }

        if (StringUtils.isEmpty(validateRequest.getProgramId())) {
            throw new BusinessException(ErrorCode.OFFER_NOT_AVAILABLE);
        } else {
            if (!programService.isProgramApplicable(validateRequest)) {
                log.info("Program {} is not applicable for the customer {}", validateRequest.getProgramId(),
                        validateRequest.getCustomerId());
                request.setProgramId(null);
            }

            if (StringUtils.isEmpty(request.getProgramId())) {
                throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
            }
        }
        RoomProgram roomProgram = programCacheService.getRoomProgram(request.getProgramId());
        if (null != roomProgram) {
            request.setPropertyId(roomProgram.getPropertyId());
        }
        request.setValidMyVegasProgram(CommonUtil.isEligibleForMyVegasRedemption(request.getMyVegasRedemptionItems(),
                roomProgram, request.getCustomer()));
    }
}
