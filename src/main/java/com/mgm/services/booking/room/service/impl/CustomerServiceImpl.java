package com.mgm.services.booking.room.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.CustomerDAO;
import com.mgm.services.booking.room.service.CustomerService;
import com.mgm.services.common.model.Customer;

/**
 * Implementation class for customer related services.
 * will remove Nullable
 * 
 */
@Component
public class CustomerServiceImpl implements CustomerService {
	@Nullable
    @Autowired
    private CustomerDAO customerDao;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.CustomerService#getCustomer(java.
     * lang.String)
     */
    @Override
    public Customer getCustomer(String mlifeNumber) {
        return customerDao.getCustomer(mlifeNumber);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.CustomerService#getCustomerById(
     * long)
     */
    @Override
    public Customer getCustomerById(long id) {
        return customerDao.getCustomerById(id);
    }

}
