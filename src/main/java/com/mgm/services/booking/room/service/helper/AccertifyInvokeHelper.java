package com.mgm.services.booking.room.service.helper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.SecretsProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.model.authorization.TransactionMappingRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.AccertifyService;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Class to handle creation of request to AFS and interact with it's services.
 * 
 * @author laknaray
 *
 */
@Component
@Log4j2
public class AccertifyInvokeHelper {

    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    private AccertifyService transactionService;
    @Autowired
    private SecretsProperties secretProperties;
    @Value("${afsCheck.enabled}")
    private boolean afsCheckEnabled;

    /**
     * Determines whether to perform AFS check or not depending on the channel flag, 
     * billing information and enabled flag.
     * 
     * @param reservationRequest
     *            reservation request
     * @return true, if the flag is true and billing information is present in the
     *         request otherwise false.
     */
    public boolean performAFSCheck(RoomReservationRequest reservationRequest) {
        return !appProperties.getBypassAfsChannels().contains(this.getChannelHeader())
                && CollectionUtils.isNotEmpty(reservationRequest.getBilling()) && afsCheckEnabled;
    }
    
    public boolean performAFSCheck(RoomReservation roomReservation) {
        return !appProperties.getBypassAfsChannels().contains(this.getChannelHeader())
                && validateCreditCardCharges(roomReservation) && afsCheckEnabled;
    }


    public boolean isEnableZeroAmountAuth(){
        boolean enableZeroAmountAuth = false;
        String enableZeroAmountAuthStr = secretProperties.getSecretValue(appProperties.getEnableZeroAmountAuthKey());
        if(StringUtils.isNotBlank(enableZeroAmountAuthStr)){
            enableZeroAmountAuth = Boolean.parseBoolean(enableZeroAmountAuthStr);
        }
        return enableZeroAmountAuth;
    }

    public boolean isEnableZeroAmountAuthTCOLVCreate() {
        boolean enableZeroAmountAuthTCOLVCreate = false;
        String enableZeroAmountCreateFlag = secretProperties.
                getSecretValue(appProperties.getEnableZeroAmountAuthKeyTCOLVCreate());
        if (StringUtils.isNotEmpty(enableZeroAmountCreateFlag) &&
                StringUtils.isNotBlank(enableZeroAmountCreateFlag)) {
            enableZeroAmountAuthTCOLVCreate = Boolean.parseBoolean(enableZeroAmountCreateFlag);
        }
        return enableZeroAmountAuthTCOLVCreate;
    }

    public boolean isEnableZeroAmountAuthTCOLVModify() {
        boolean enableZeroAmountAuthTCOLVModify = false;
        String enableZeroAmountModFlag = secretProperties.
                getSecretValue(appProperties.getEnableZeroAmountAuthKeyTCOLVModify());
        if (StringUtils.isNotEmpty(enableZeroAmountModFlag) &&
                StringUtils.isNotBlank(enableZeroAmountModFlag)) {
            enableZeroAmountAuthTCOLVModify = Boolean.parseBoolean(enableZeroAmountModFlag);
        }
        return enableZeroAmountAuthTCOLVModify;
    }
    /**
     * Check to see if the credit card charges section is valid
     * 
     * If the total amount of charge on the credit card is 0, this is going to return
     * false and will skip the AFS check on that credit card
     * @param roomReservation reservation
     * @return validation of the charges
     */
    private boolean validateCreditCardCharges(RoomReservation roomReservation) {
        boolean enableZeroAmountAuth = isEnableZeroAmountAuth();
        if(CollectionUtils.isEmpty(roomReservation.getCreditCardCharges())) {
    		return false;
    	}else if(enableZeroAmountAuth){
            return true;
        }
    	
    	double charges = roomReservation.getCreditCardCharges().stream().mapToDouble(o -> o.getAmount()).sum();
    	
    	return 0 != BigDecimal.ZERO.compareTo(BigDecimal.valueOf(charges).setScale(2, BigDecimal.ROUND_HALF_UP));	
    }

    /**
     * Creates AuthorizationTransactionRequest object from room reservation request.
     * 
     * @param roomReservation
     *            room reservation object
     * @param inAuthTransactionId
     *            in auth transactionId string
     * @return AuthorizationTransactionRequest object
     */
    public AuthorizationTransactionRequest createAuthorizeRequest(RoomReservation roomReservation,
            String inAuthTransactionId) {
        TransactionMappingRequest req = new TransactionMappingRequest();
        req.setPreresponse(RoomReservationTransformer.transform(roomReservation, appProperties));
        req.setReservation(roomReservation);
        req.setInAuthTransactionId(inAuthTransactionId);
        req.setTransactionId(ThreadContext.get("CLIENT_TRANSACTIONID"));
        return transactionService.transform(req);
    }

    /**
     * Calls <i>authorize</i> service of accertify and returns the authorization
     * received from it.
     * 
     * @param authorizeRequest
     *            AuthorizationTransactionRequest object
     * @return return the authorized value from the AFS response
     */
    public boolean authorize(AuthorizationTransactionRequest authorizeRequest) {
        try {
            log.debug("Request to Accertify: {}", authorizeRequest);
            AuthorizationTransactionResponse txResponse = transactionService.authorize(authorizeRequest);
            log.info("reserveRoomWithRedemption::Response from Accertify:{}", txResponse);
            return txResponse.isAuthorized();
        } catch (Exception ex) {
            log.error("Error from anti-fraud service: ", ex);
            return true;
            //throw new BusinessException(ErrorCode.AFS_FAILURE);
        }
    }


    public void confirmAsyncCall(AuthorizationTransactionRequest authorizeRequest, String confirmationNumber){
        HttpHeaders headers = new HttpHeaders();
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN))) {
            headers.set(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN));
        }
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_USER_AGENT))) {
            headers.set(ServiceConstant.HEADER_USER_AGENT, ThreadContext.get(ServiceConstant.HEADER_USER_AGENT));
        }
        Map.Entry<String, String> clientRef = CommonUtil.getClientRefForPayment();
        headers.set(clientRef.getKey(),clientRef.getValue());
        CommonUtil.setPaymentSourceAndChannelHeaders(headers);

        if(null != authorizeRequest) {
            String confNumber = !StringUtils.isBlank(confirmationNumber) ? confirmationNumber : ServiceConstant.CONFIRMATION_PENDING;
            CompletableFuture.runAsync(() -> confirm(authorizeRequest, confNumber , headers));
        }
    }


    /**
     * Calls <i>confirm</i> service of accertify with the given confirmation
     * number(s).
     * 
     * @param authorizeRequest
     *            AuthorizationTransactionRequest object
     * @param confirmationNumbers
     *            list of confirmationNumbers with comma delimiter
     */
    public void confirm(AuthorizationTransactionRequest authorizeRequest, String confirmationNumbers, HttpHeaders headers) {
        if(StringUtils.isNotBlank(confirmationNumbers)) {
            authorizeRequest.getTransaction().setConfirmationNumbers(confirmationNumbers);
            String[] confirmationNumbersArray = confirmationNumbers.split(",");
            for (int i = 0; i < confirmationNumbersArray.length; i++) {
                authorizeRequest.getTransaction().getProducts().getRooms().get(i)
                        .setConfirmationNumber(confirmationNumbersArray[i]);
            }
        }
        transactionService.confirm(authorizeRequest, headers);
    }

    public String getChannelHeader() {
        return CommonUtil.getChannelHeaderWithFallback(
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest());
    }

}
