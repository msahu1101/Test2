package com.mgm.services.booking.room.dao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.mgm.services.booking.room.dao.config.OktaConfiguration;
import com.mgm.services.booking.room.model.profile.User;

/**
 * OKTA Dao.
 * 
 */
@FeignClient(
        value = "okta",
        url = "${okta.base.url}",
        contextId = "oktaContextId",
        configuration = OktaConfiguration.class)
public interface OktaFeignClient {

    /**
     * Calls OKTA user api to get the profile.
     *
     * @param emailId
     *            customer email id
     * @return user object
     */
    @GetMapping(
            value = "/users/{emailId}",
            consumes = "application/json",
            produces = "application/json")
    User getUser(@PathVariable("emailId") String emailId);

    /**
     * Calls OKTA user api to create the web profile.
     *
     * @param activate
     *            true to be passed if the profile need to activated
     * @param user
     *            user object
     */
    @PostMapping(
            value = "/users?activate={activate}",
            consumes = "application/json",
            produces = "application/json")
    void createCustomerWebCredentials(@PathVariable("activate") String activate, User user);

    /**
     * Calls OKTA activate user api to activate the web profile.
     *
     * @param emailId
     *            customer email id
     */
    @PostMapping(
            value = "/users/{emailId}/lifecycle/activate?sendEmail=false",
            consumes = "application/json",
            produces = "application/json")
    void activateUser(@PathVariable("emailId") String emailId);

    /**
     * Calls OKTA deactivate user api to deactivate the web profile.
     *
     * @param emailId
     *            customer email id
     */
    @PostMapping(
            value = "/users/{emailId}/lifecycle/deactivate",
            consumes = "application/json",
            produces = "application/json")
    void deactivateUser(@PathVariable("emailId") String emailId);

    /**
     * Calls OKTA delete user api to delete the web profile.
     *
     * @param emailId
     *            customer email id
     */
    @DeleteMapping(
            value = "/users/{emailId}",
            consumes = "application/json",
            produces = "application/json")
    void deleteUser(@PathVariable("emailId") String emailId);
}
