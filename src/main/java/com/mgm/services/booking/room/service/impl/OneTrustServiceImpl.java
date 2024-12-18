package com.mgm.services.booking.room.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mgm.services.booking.room.dao.OneTrustDAO;
import com.mgm.services.booking.room.model.UserAttributeList;
import com.mgm.services.booking.room.service.OneTrustService;
import com.mgm.services.common.constant.ServiceCommonConstant;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.model.Customer;

import lombok.extern.log4j.Log4j2;

/**
 * The service methods provides connectivity and operations with OneTrust.
 */
@Service
@Log4j2
public class OneTrustServiceImpl implements OneTrustService {

    private static final int USER_ATTRIBUTES_SIZE = 2;

    @Autowired
    private OneTrustDAO oneTrustDAO;

    @Override
    public void createOneTrustUser(Customer customer) {
        try {
            UserAttributeList attributeList = new UserAttributeList(USER_ATTRIBUTES_SIZE)
                    .add(ServiceCommonConstant.MLIFE_ID, String.valueOf(customer.getMlifeNumber()))
                    .add(ServiceCommonConstant.GSE_ID, String.valueOf(customer.getCustomerId()));

            oneTrustDAO.createOneTrustUser(attributeList, String.valueOf(customer.getMlifeNumber()));
            log.info("OneTrust Account is created for Mlife id: {}, GSEId: {}", customer.getMlifeNumber(),
                    customer.getCustomerId());
        } catch (BusinessException e) {
            log.error("Unable to create one trust user with Mlife id: {}, GSEId: {}, Received following error : {}",
                    customer.getMlifeNumber(), customer.getCustomerId(), e);
        }
    }
}
