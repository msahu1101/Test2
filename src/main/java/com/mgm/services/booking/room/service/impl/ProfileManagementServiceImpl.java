/*
 * 
 */
package com.mgm.services.booking.room.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.CustomerDAO;
import com.mgm.services.booking.room.dao.OktaDAO;
import com.mgm.services.booking.room.model.request.ActivateCustomerRequest;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.response.ActivateCustomerResponse;
import com.mgm.services.booking.room.model.response.CreateCustomerResponse;
import com.mgm.services.booking.room.model.response.CustomerWebInfoResponse;
import com.mgm.services.booking.room.service.ProfileManagementService;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.Customer;

import lombok.extern.log4j.Log4j2;

/**
 * The Class ProfileManagementServiceImpl.
 */
@Component
@Log4j2
public class ProfileManagementServiceImpl implements ProfileManagementService {

    @Autowired
    private OktaDAO eOktaDao;

    @Autowired
    private CustomerDAO customerDao;

    /**
     * Look for the existence of online profile, if exists, check whether it is
     * active or not. If active, thrown business exception
     * <i>_account_already_exists</i> If online profile either inactive or not
     * found, look for the existence of patron profile. If patron profile(s) exists,
     * and if it is only one account and online profile is inactive, then return to
     * called method with inActiveWebProfile as true and if it is multiple patron
     * accounts, then throw business exception <i>_multiple_accounts_found</i> else
     * if patron profile does not exists, then create patron profile and active
     * online profile.
     */
    @Override
    public CreateCustomerResponse createCustomer(CreateCustomerRequest createCustomerRequest) {
        CreateCustomerResponse createCustomerResponse = new CreateCustomerResponse();
        try {
            // Check for Web profile
            final CustomerWebInfoResponse response = eOktaDao.getCustomerByWebCredentials(createCustomerRequest);
            if (response.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
            } else {
                createCustomerResponse.setInactiveWebProfile(true);
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
            }
        } catch (BusinessException businessException) {
            // Web profile not found or inactive
            if (ErrorCode.ACCOUNT_NOT_FOUND.equals(businessException.getErrorCode())
                    || (ErrorCode.ACCOUNT_NOT_ACTIVATED.equals(businessException.getErrorCode())
                            && createCustomerResponse.isInactiveWebProfile())) {
                Customer customer = null;

                // Check for patron profile
                final Customer[] customers = customerDao
                        .getCustomersByEmailAddress(createCustomerRequest.getCustomerEmail());

                if (customers != null && customers.length > 0) {

                    int validPatronProfiles = 0;
                    for (int i = 0; i < customers.length; i++) {
                        customer = customers[i];
                        if (isValidPatronProfile(createCustomerRequest, customer)) {
                            validPatronProfiles++;
                        }
                        if (validPatronProfiles > 1) {
                            break;
                        }
                    }

                    if (validPatronProfiles == 1) {
                        createCustomerResponse.setCustomer(customer);
                        if (ErrorCode.ACCOUNT_NOT_ACTIVATED.equals(businessException.getErrorCode())
                                && createCustomerResponse.isInactiveWebProfile()) {
                            return createCustomerResponse;
                        }
                        createCustomerRequest.setEnroll(Boolean.TRUE.booleanValue());
                        createCustomerRequest.setMlifeNo(customer.getMlifeNumber());
                        // Create customer active web profile
                        createCustomerRequest.setActivate(true);
                        eOktaDao.createCustomerWebCredentials(createCustomerRequest);
                        createCustomerResponse.setAccountcreated(true);
                        return createCustomerResponse;
                    } else if (validPatronProfiles > 1) {
                        throw new BusinessException(ErrorCode.MULTIPLE_ACCOUNTS_FOUND);
                    }
                }

                // Create active web profile and patron account
                createCustomerRequest.setEnroll(Boolean.TRUE.booleanValue());
                customer = customerDao.addCustomer(createCustomerRequest);
                createCustomerRequest.setMlifeNo(customer.getMlifeNumber());
                createCustomerRequest.setActivate(true);
                eOktaDao.createCustomerWebCredentials(createCustomerRequest);
                createCustomerResponse.setCustomer(customer);
                createCustomerResponse.setAccountcreated(true);
            } else if (ErrorCode.ACCOUNT_ALREADY_EXISTS.equals(businessException.getErrorCode())) {
                throw businessException;
            } else {
                throw new BusinessException(ErrorCode.SIGNUP_FAILED);
            }
        }

        return createCustomerResponse;
    }

    private boolean isValidPatronProfile(CreateCustomerRequest createCustomerRequest, Customer customer) {
        return customer.getMlifeNumber() > 0
                && StringUtils.equalsIgnoreCase(customer.getFirstName(),
                        createCustomerRequest.getFirstName())
                && StringUtils.equalsIgnoreCase(customer.getLastName(),
                        createCustomerRequest.getLastName());
    }

    @Override
    public ActivateCustomerResponse activateCustomer(ActivateCustomerRequest activateCustomerRequest) {

        ActivateCustomerResponse response = null;
        log.info("Activating Customer : {}", activateCustomerRequest.getCustomerEmail());
        response = eOktaDao.activateCustomerWebCredentials(activateCustomerRequest);
        return response;
    }

    @Override
    public void deactivateCustomer(String customerEmailId) {
        log.info("Deactivating Customer : {}", customerEmailId);
        eOktaDao.deactivateCustomerWebCredentials(customerEmailId);
    }

    @Override
    public void deleteCustomer(String customerEmailId) {
        log.info("Deleting Customer : {}", customerEmailId);
        eOktaDao.deleteCustomerWebCredentials(customerEmailId);
    }
}
