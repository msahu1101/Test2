package com.mgm.services.booking.room.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class ACRSErrorUtil {
    public static ACRSErrorDetails getACRSErrorDetailsFromACRSErrorRes(String errorRes) {
        Gson g = new Gson();
        ACRSErrorDetails error = new ACRSErrorDetails();
        try {
            ACRSErrorRes aCRSErrorRes = g.fromJson(errorRes, ACRSErrorRes.class);
            if (null != aCRSErrorRes.getError()) {
                error.setCode(aCRSErrorRes.getError().getCode());
                error.setTitle(aCRSErrorRes.getError().getTitle());
                error.setStatus(aCRSErrorRes.getError().getStatus());
            } else {
                error.setTitle(errorRes);
            }
            return error;
        } catch (JsonSyntaxException ex) {
            error.setTitle(errorRes);
            return error;
        }
    }

    public static ACRSSearchOffersErrorRes parseACRSSearchOffersErrorDetails(String errorResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(ReservationUtil.getJavaTimeModuleISO());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ACRSSearchOffersErrorRes response;
        try {
            response = objectMapper.readValue(errorResponse,
                    ACRSSearchOffersErrorRes.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse ACRS SearchOffers Error Details from: {}", errorResponse);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
        return response;
    }
}
