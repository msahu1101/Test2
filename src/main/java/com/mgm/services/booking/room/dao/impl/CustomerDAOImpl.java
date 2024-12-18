/**
 * 
 */
package com.mgm.services.booking.room.dao.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.CustomerDAO;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.request.ProfileRequest;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.transformer.CustomerProfileTransformer;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.common.CustomerAddress;
import com.mgmresorts.aurora.common.CustomerPhoneNumber;
import com.mgmresorts.aurora.common.CustomerProfile;
import com.mgmresorts.aurora.messages.AddCustomerRequest;
import com.mgmresorts.aurora.messages.AddCustomerResponse;
import com.mgmresorts.aurora.messages.CustomerSearchFilter;
import com.mgmresorts.aurora.messages.CustomerSearchKey;
import com.mgmresorts.aurora.messages.GetCustomerByIdRequest;
import com.mgmresorts.aurora.messages.GetCustomerByIdResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.messages.SearchCustomerRequest;
import com.mgmresorts.aurora.messages.SearchCustomerResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class to get customer related information by invoking aurora
 * APIs
 */
@Component
@Log4j2
public class CustomerDAOImpl extends AuroraBaseDAO implements CustomerDAO {

    @Autowired
    private ApplicationProperties appProperties;

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.CustomerDAO#getCustomer(java.lang.
     * String)
     */
    @Override
    public Customer getCustomer(String mlifeNumber) {

        SearchCustomerRequest searchCustomerRequest = MessageFactory.createSearchCustomerRequest();

        CustomerSearchKey customerSearchKey = CustomerSearchKey.create();
        customerSearchKey.setMlifeNo(Integer.parseInt(mlifeNumber));

        searchCustomerRequest.setKey(customerSearchKey);

        log.info("Sent the request to searchCustomer as : {}", searchCustomerRequest.toJsonString());
        final SearchCustomerResponse searchCustomerResponse = getDefaultAuroraClient()
                .searchCustomer(searchCustomerRequest);

        log.info("Received the response from searchCustomer as : {}", searchCustomerResponse.toJsonString());
        if (ArrayUtils.isNotEmpty(searchCustomerResponse.getCustomers())) {
            CustomerProfile customerProfile = searchCustomerResponse.getCustomers()[0];

            return convert(customerProfile);
        }

        return null;
    }

    /**
     * Convert aurora customer profile object to application customer profile.
     * 
     * @param customerProfile
     *            Aurora customer profile
     * @return Customer local instance
     */
    private Customer convert(CustomerProfile customerProfile) {

        Customer customer = new Customer();
        customer.setCustomerId(customerProfile.getId());
        customer.setMlifeNumber(customerProfile.getMlifeNo());
        customer.setFirstName(customerProfile.getFirstName());
        customer.setLastName(customerProfile.getLastName());
        customer.setDateOfBirth(customerProfile.getDateOfBirth());
        customer.setDateOfEnroll(customerProfile.getDateOfEnrollment());
        customer.setTier(customerProfile.getTier());
        customer.setEmailAddress(customerProfile.getEmailAddress1());

        ProfilePhone profilePhone = new ProfilePhone();
        for (CustomerPhoneNumber phone : customerProfile.getPhoneNumbers()) {
            if (phone.hasNumber()) {
                profilePhone.setNumber(phone.getNumber());
                profilePhone.setType(phone.getType().name());
            }
        }
        List<ProfilePhone> phoneNumbers = new ArrayList<>();
        phoneNumbers.add(profilePhone);
        customer.setPhoneNumbers(phoneNumbers);

        ProfileAddress address = new ProfileAddress();
        for (CustomerAddress customerAddress : customerProfile.getAddresses()) {
            if (customerAddress.getPreferred()) {
                address.setPreferred(true);
                address.setType(customerAddress.getType().name());
                address.setStreet1(customerAddress.getStreet1());
                address.setStreet2(customerAddress.getStreet2());
                address.setCity(customerAddress.getCity());
                address.setState(customerAddress.getState());
                address.setCountry(customerAddress.getCountry());
                address.setPostalCode(customerAddress.getPostalCode());
            }
        }
        List<ProfileAddress> addresses = new ArrayList<>();
        addresses.add(address);
        customer.setAddresses(addresses);

        return customer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.CustomerDAO#getCustomerById(long)
     */
    @Override
    public Customer getCustomerById(long id) {

        GetCustomerByIdRequest request = MessageFactory.createGetCustomerByIdRequest();
        request.setCustomerId(id);

        log.info("Sent the request to getCustomerById as : {}", request.toJsonString());
        final GetCustomerByIdResponse response = getDefaultAuroraClient().getCustomerById(request);

        log.info("Received the response from getCustomerById as : {}", response.toJsonString());

        return convert(response.getCustomer());
    }

    @Override
    public Customer[] getCustomersByEmailAddress(String emailAddress) {
        SearchCustomerRequest searchCustomerRequest = MessageFactory.createSearchCustomerRequest();

        CustomerSearchKey customerSearchKey = CustomerSearchKey.create();
        customerSearchKey.setEmailAddress(emailAddress);

        searchCustomerRequest.setKey(customerSearchKey);

        final CustomerSearchFilter customerSearchFilter = new CustomerSearchFilter();
        customerSearchFilter.setReturnCustomerValue(true);
        searchCustomerRequest.setFilter(customerSearchFilter);

        log.info("Sent the request to searchCustomer as : {}", searchCustomerRequest.toJsonString());
        final SearchCustomerResponse searchCustomerResponse = getDefaultAuroraClient()
                .searchCustomer(searchCustomerRequest);

        log.info("Received the response from searchCustomer as : {}", searchCustomerResponse.toJsonString());
        Customer[] customers = null;
        if (ArrayUtils.isNotEmpty(searchCustomerResponse.getCustomers())) {
            CustomerProfile[] customerProfiles = searchCustomerResponse.getCustomers();

            if (null != customerProfiles && customerProfiles.length > 0) {
                customers = new Customer[customerProfiles.length];
                int i = 0;
                for (final CustomerProfile profile : customerProfiles) {
                    customers[i] = convert(profile);
                    i++;
                }
            }
        }
        return customers;
    }

    @Override
    public Customer addCustomer(ProfileRequest profileRequest) {
        CreateCustomerRequest createCustomerRequest = null;
        if (profileRequest instanceof CreateCustomerRequest) {
            createCustomerRequest = (CreateCustomerRequest) profileRequest;
        } else {
            createCustomerRequest = new CreateCustomerRequest();
        }
        if (createCustomerRequest.isEnroll()
                && (!checkMinAgeRequirement(DateUtil.toDate(createCustomerRequest.getDateOfBirth()), appProperties.getMinimumAge()))) {
            log.error("minimum age requirement did not met");
            throw new BusinessException(ErrorCode.INVALID_TYPE);
        }

        AddCustomerRequest addCustomerRequest = MessageFactory.createAddCustomerRequest();
        Customer customer = null;
        CustomerProfile customerProfile = CustomerProfileTransformer.convert(createCustomerRequest);
        if (customerProfile.getId() == -1) {
            customerProfile.setId(0);
        }
        addCustomerRequest.setCustomer(customerProfile);
        addCustomerRequest.setEnroll(createCustomerRequest.isEnroll());
        log.info("addCustomer Request : {}", addCustomerRequest.toJsonString());
        final AddCustomerResponse addCustomerResponse = getAuroraClient(createCustomerRequest.getSource()).addCustomer(addCustomerRequest);

        if (null != addCustomerResponse) {
            log.info("addCustomer Response : {}", addCustomerResponse.toJsonString());
            if (null != addCustomerResponse.getCustomer()) {
                customer = convert(addCustomerResponse.getCustomer());
            }
        }

        return customer;
    }

    /**
     * Check min age requirement.
     *
     * @param dateOfBirth
     *            the date of birth
     * @param minAgeRequired
     *            the min age required
     * @return true, if successful
     */
    private boolean checkMinAgeRequirement(Date dateOfBirth, int minAgeRequired) {
        if (null == dateOfBirth) {
            return false;
        }
        Calendar cdate = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.setTimeInMillis(dateOfBirth.getTime());

        cdate.add(Calendar.YEAR, -minAgeRequired);

        return cdate.getTime().after(dob.getTime());
    }
}
