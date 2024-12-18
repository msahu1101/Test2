package com.mgm.services.booking.room.dao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.mgm.services.booking.room.dao.config.PreferencesConfiguration;
import com.mgm.services.booking.room.model.request.CreateUserProfileRequest;
import com.mgm.services.booking.room.model.response.CreateUserProfileResponse;

/**
 * Preferences Dao.
 * 
 */
@FeignClient(
        value = "preferences",
        url = "${onetrust.web.url}",
        contextId = "preferencesContextId",
        configuration = PreferencesConfiguration.class)
public interface PreferencesFeignClient {

    /**
     * Call preferences user api to create the account.
     *
     * @param userIdentifier
     *              user identifier
     * @param createUserProfileRequest
     *              create user profile request object
     * @return create user profile response
     */
    @PostMapping(
            value = "/v1/user",
            consumes = "application/json",
            produces = "application/json")
    CreateUserProfileResponse createUser(@RequestHeader("userIdentifier") String userIdentifier,
            @RequestBody CreateUserProfileRequest createUserProfileRequest);
}
