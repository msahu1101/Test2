package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.authorization.TransactionMappingRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import org.springframework.http.HttpHeaders;

/**
 * Service interface exposing service to authorize and confirm a transaction
 * 
 * @author nitpande0
 *
 */
public interface AccertifyService {

    /**
     * Checks whether the given transaction is authorized
     * 
     * @param req
     *            the initial transaction request
     * @return the authorization response
     */
    AuthorizationTransactionResponse authorize(AuthorizationTransactionRequest req);

    /**
     * Sends a confirmation to confirm that the transaction has been completed
     * 
     * @param transactionRequest
     *            the transaction request
     */
    void confirm(AuthorizationTransactionRequest transactionRequest, HttpHeaders headers);

    /**
     * Method to transform the room booking data into Accertify request format
     * @param req the request
     * @return the transformed object
     */
    AuthorizationTransactionRequest transform(TransactionMappingRequest req);

}
