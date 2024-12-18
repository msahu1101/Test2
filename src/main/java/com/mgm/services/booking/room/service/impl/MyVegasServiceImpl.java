package com.mgm.services.booking.room.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.MyVegasDAO;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.service.MyVegasService;
import com.mgm.services.booking.room.service.helper.MyVegasServiceHelper;
import com.mgm.services.common.model.RedemptionValidationResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Service interface that exposes service for validating and confirming the
 * redemption code
 */
@Component
@Log4j2
public class MyVegasServiceImpl implements MyVegasService {

    @Autowired
    private Map<String, MyVegasDAO> myVegasDAOMap;
    
    @Autowired
    private MyVegasServiceHelper myVegasServiceHelper;

    @Value("${myVegas.restClient.enabled}")
    private boolean isRestClientEnabled;
    
    private static final String JMS_IMPL = "JMSMyVegasDAOImpl";
    private static final String REST_IMPL = "RestMyVegasDAOImpl";

    @Override
    public RedemptionValidationResponse validateRedemptionCode(MyVegasRequest myVegasRequest) {
        return myVegasDAOMap.get(JMS_IMPL).validateRedemptionCode(myVegasRequest);
    }

    @Override
    public void confirmRedemptionCode(MyVegasRequest myVegasRequest) {

        try {
            myVegasDAOMap.get(JMS_IMPL).confirmRedemptionCode(myVegasRequest);
        } catch (Exception ex) {
            // Unable to send myvegas confirmation shouldn't be hard failure
            log.error("PostReservationErrors - Unable to confirm redemption code with myvegas: ", ex);
        }

    }

    @Override
    public MyVegasResponse validateRedemptionCodeV2(MyVegasRequest myVegasRequest, String token) {
        log.info("Making {} call to validate redemption code {}", isRestClientEnabled ? "Rest" : "JMS",
                myVegasRequest.getRedemptionCode());

        RedemptionValidationResponse redemptionDetails = myVegasDAOMap.get(isRestClientEnabled ? REST_IMPL : JMS_IMPL)
                .validateRedemptionCode(myVegasRequest);
        return myVegasServiceHelper.convertFromAuroraRedemptionResponse(redemptionDetails);
    }

    @Override
    public void confirmRedemptionCodeV2(MyVegasRequest myVegasRequest) {
        try {
            log.info("Making {} call for redemption confirmation for code {}", isRestClientEnabled ? "Rest" : "JMS",
                    myVegasRequest.getRedemptionCode());
            myVegasDAOMap.get(isRestClientEnabled ? REST_IMPL : JMS_IMPL).confirmRedemptionCode(myVegasRequest);

        } catch (Exception ex) {
            // Unable to send myvegas confirmation shouldn't be hard failure
            log.error("PostReservationErrors - Unable to confirm redemption code with myvegas: ", ex);
        }
    }
}
