package com.mgm.services.booking.room.dao.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.OneTrustDAO;
import com.mgm.services.booking.room.dao.PreferencesFeignClient;
import com.mgm.services.booking.room.model.UserAttribute;
import com.mgm.services.booking.room.model.UserAttributeList;
import com.mgm.services.booking.room.model.request.CreateUserProfileRequest;
import com.mgm.services.booking.room.model.response.CreateUserProfileResponse;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * The DAO class for One Trust Integration.
 */
@Component
@Log4j2
public class OneTrustDAOImpl implements OneTrustDAO {

    @Autowired
    private PreferencesFeignClient preferencesClient;

    @Override
    public void createOneTrustUser(UserAttributeList userAttributes, String userIdentifier) {
        log.debug("Creating One Trust Account for User identifier: {} Attributes: {}", userIdentifier, userAttributes);
        CreateUserProfileRequest createUserProfileRequest = new CreateUserProfileRequest();
        createUserProfileRequest.setUserAttributes(userAttributes.getUserAttributes());
        CreateUserProfileResponse createUserProfileResponse = preferencesClient.createUser(userIdentifier,
                createUserProfileRequest);
        log.debug("Recieved Response : Status: {} Attributes{}", createUserProfileResponse.getType(),
                createUserProfileResponse.getPayload());
        List<UserAttribute> attributeList = createUserProfileResponse.getPayload().getUserAttributes();
        boolean isSuccess = StringUtils.equalsIgnoreCase("SUCCESS", createUserProfileResponse.getType());
        if (attributeList == null || !isSuccess) {
            throw new BusinessException(ErrorCode.PREFERENCES_ACCOUNT_NOT_CREATED);
        }
    }
}
