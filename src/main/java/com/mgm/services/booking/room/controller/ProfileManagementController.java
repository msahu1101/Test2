package com.mgm.services.booking.room.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.service.ProfileManagementService;
import com.mgm.services.common.controller.BaseController;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to handle room booking confirmation service for checkout.
 *
 */
@RestController
@RequestMapping("/v1/profile")
@Log4j2
public class ProfileManagementController extends BaseController {

    @Autowired
    private ProfileManagementService profileService;

    /**
     * Deactivates the customer web account.
     *
     * @param customerEmailId
     *              customer email id
     */
    @PostMapping("/deactivate/{customerEmailId}")
    public void deactivateCustomer(@PathVariable String customerEmailId) {
        log.info("Invoking user deactivation process for {} in integration test execution", customerEmailId);
        profileService.deactivateCustomer(customerEmailId);
    }

    /**
     * Deletes the customer web account.
     *
     * @param customerEmailId
     *              customer email id
     */
    @DeleteMapping("/delete/{customerEmailId}")
    public void deleteCustomer(@PathVariable String customerEmailId) {
        log.info("Invoking user delete process for {} in integration test execution", customerEmailId);
        profileService.deleteCustomer(customerEmailId);
    }
}
