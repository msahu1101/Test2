package com.mgm.services.booking.room.service.impl;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.IATADAO;
import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;
import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

/**
 * Service interface that exposes service for validating the iata code
 */
@Component
@Log4j2
public class IATAServiceV2Impl implements IATAV2Service {

    @Autowired
    private IATADAO iataDAO;

    @Override
    public void validateCode(String iataCode) {
        if (StringUtils.isBlank(iataCode)) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_IATA_CODE.getErrorCode()));
        }

        if(iataDAO.validateCode(iataCode)) {
            log.info("Iata code {} validated succesfully", iataCode);
            return;
        }
        throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_IATA_CODE.getErrorCode()));

    }

    @Override
    public List<OrganizationSearchV2Response> organizationSearch(OrganizationSearchV2Request organizationSearchRequest) {
        return iataDAO.organizationSearch(organizationSearchRequest);
    }
}
