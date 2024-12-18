package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.PaymentTokenizeRequest;

/**
 * Service interface to provide service for tokenization integration
 *
 */
public interface PaymentTokenizationService {

    /**
     * Service method to get token for the credit card info supplied.
     * 
     * @param tokenizeRequest
     *            Tokenize request with credit card info
     * @return Returns token
     */
    String tokenize(PaymentTokenizeRequest tokenizeRequest);

}
