package com.mgm.services.booking.room.dao.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.OktaDAO;
import com.mgm.services.booking.room.dao.OktaFeignClient;
import com.mgm.services.booking.room.model.profile.LoginCredentials;
import com.mgm.services.booking.room.model.profile.Password;
import com.mgm.services.booking.room.model.profile.Profile;
import com.mgm.services.booking.room.model.profile.RecoveryQuestion;
import com.mgm.services.booking.room.model.profile.User;
import com.mgm.services.booking.room.model.request.ActivateCustomerRequest;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.response.ActivateCustomerResponse;
import com.mgm.services.booking.room.model.response.CustomerWebInfoResponse;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation of Okta Dao methods, which interacts with Okta APIs for
 * various purposes.
 *
 */
@Component
@Log4j2
public class OktaDAOImpl implements OktaDAO {

    @Autowired
    private OktaFeignClient oktaDao;

    @Override
    public CustomerWebInfoResponse getCustomerByWebCredentials(CreateCustomerRequest createCustomerRequest) {

        CustomerWebInfoResponse webInfo = null;
        log.info("calling getUser with customerEmail {} .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. ..",
                createCustomerRequest.getCustomerEmail());
        User user = oktaDao.getUser(createCustomerRequest.getCustomerEmail());

        webInfo = new CustomerWebInfoResponse();
        webInfo.setCustomerEmail(user.getProfile().getEmail());
        webInfo.setEmailAddress(user.getProfile().getEmail());
        if (StringUtils.isNotBlank(user.getProfile().getMlifeNumber())) {
            webInfo.setMlifeNo(Integer.parseInt(user.getProfile().getMlifeNumber()));
        }
        webInfo.setActive(!(user.getStatus().contains("MFA") || "STAGED".equalsIgnoreCase(user.getStatus())));

        String secretQuestion = null;
        if (user.getCredentials() != null && user.getCredentials().getRecoveryQuestion() != null) {
            secretQuestion = user.getCredentials().getRecoveryQuestion().getQuestion();
        } else {
            secretQuestion = user.getProfile().getSecretQuestionId();
        }
        log.info("secretQuestion: {} .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. ..", secretQuestion);
        webInfo.setSecretQuestionId(NumberUtils.toInt(secretQuestion, -1));
        return webInfo;
    }

    @Override
    public void createCustomerWebCredentials(CreateCustomerRequest createCustomerRequest) {
        Profile profile = new Profile();
        profile.setFirstName(createCustomerRequest.getFirstName());
        profile.setLastName(createCustomerRequest.getLastName());
        profile.setEmail(createCustomerRequest.getCustomerEmail());
        profile.setLogin(createCustomerRequest.getCustomerEmail());
        if (createCustomerRequest.getMlifeNo() > 0) {
            profile.setMlifeNumber(createCustomerRequest.getMlifeNo().toString());
        }
        LoginCredentials cred = new LoginCredentials();
        Password pswd = new Password();
        pswd.setValue(createCustomerRequest.getPassword());
        cred.setPassword(pswd);
        RecoveryQuestion recQ = new RecoveryQuestion();
        recQ.setQuestion(String.valueOf(createCustomerRequest.getSecretQuestionId()));
        recQ.setAnswer(createCustomerRequest.getSecretAnswer());
        cred.setRecoveryQuestion(recQ);

        User user = new User();
        user.setProfile(profile);
        user.setCredentials(cred);

        oktaDao.createCustomerWebCredentials(createCustomerRequest.isActivate() ? "true" : "false", user);
    }

    @Override
    public ActivateCustomerResponse activateCustomerWebCredentials(ActivateCustomerRequest activateCustomerRequest) {
        try {
            oktaDao.activateUser(activateCustomerRequest.getCustomerEmail());
        } catch (BusinessException businessException) {
            log.error("BusinessException: {}", businessException);
            if (businessException.getErrorCode() == ErrorCode.ACCOUNT_ALREADY_ACTIVATED) {
                throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED);
            }
        }
        return null;
    }

    @Override
    public void deactivateCustomerWebCredentials(String customerEmailId) {
        oktaDao.deactivateUser(customerEmailId);
    }

    @Override
    public void deleteCustomerWebCredentials(String customerEmailId) {
        oktaDao.deleteUser(customerEmailId);
    }
}
