package com.mgm.services.booking.room.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.model.request.PaymentTokenizeRequest;
import com.mgm.services.booking.room.service.PaymentTokenizationService;

@Component
public class PaymentTokenizationServiceImpl implements PaymentTokenizationService {

    @Autowired
    private PaymentDAO paymentDao;

    @Override
    public String tokenize(PaymentTokenizeRequest tokenizeRequest) {
        return paymentDao.tokenizeCreditCard(tokenizeRequest);
    }

}