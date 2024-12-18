package com.mgm.services.booking.room.dao;

import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import org.springframework.http.HttpHeaders;

/**
 * DAO interface exposing dao to authorize and confirm a transaction
 * 
 * @author nitpande0
 *
 */
public interface AccertifyDAO {

    /**
     * Checks whether the given transaction is authorized
     * 
     * @param transactionRequest
     *            the initial transaction request
     * @return the authorization response
     */
    AuthorizationTransactionResponse authorize(AuthorizationTransactionRequest transactionRequest);

    /**
     * Sends a confirmation to confirm that the transaction has been completed
     *
     * @param transactionRequest
     *            the transaction request
     * @param headers
     */
    void confirm(AuthorizationTransactionRequest transactionRequest, HttpHeaders headers);

}
