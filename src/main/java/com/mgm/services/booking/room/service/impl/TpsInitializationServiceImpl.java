/**
 * 
 */
package com.mgm.services.booking.room.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.impl.AuroraBaseDAO;
import com.mgm.services.booking.room.service.TpsInitializationService;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * @author laknaray
 *
 */
@Component
@Log4j2
public class TpsInitializationServiceImpl implements TpsInitializationService {

    @Autowired
    private AuroraBaseDAO auroraDao;

    @Override
    public String reinitializeConnections() {
        String result = "TPS connection re-initialization ";
        try {
            auroraDao.reinitializeAuroraConnections();
            result += "is successful";
        } catch (EAuroraException e) {
            log.error("Exception occured while creating Aurora connection", e);
            result += "is failed due to " + e.getMessage();
        }
        log.info(result);
        return result;
    }

}
