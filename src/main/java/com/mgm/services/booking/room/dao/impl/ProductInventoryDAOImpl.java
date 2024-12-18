package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.ProductInventoryDAO;
import com.mgm.services.booking.room.model.inventory.*;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Log4j2
public class ProductInventoryDAOImpl implements ProductInventoryDAO {
    private final URLProperties urlProperties;
    private DomainProperties domainProperties;
    private final RestTemplate client;
    private IDMSTokenDAO idmsTokenDAO;
    ApplicationProperties applicationProperties;

    protected ProductInventoryDAOImpl(URLProperties urlProperties, RestTemplate restTemplate,
                                      ApplicationProperties applicationProperties, DomainProperties domainProperties, IDMSTokenDAO idmsTokenDAO) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        this.idmsTokenDAO = idmsTokenDAO;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.applicationProperties = applicationProperties;
    }
    
    @Override
    public InventoryGetRes getInventory(String productCode, boolean cacheOnly) {
        Map<String, String> uriParams = createUriParams();
        uriParams.put("productCode", productCode);
        uriParams.put("cacheOnly", String.valueOf(cacheOnly));
        HttpEntity<?> request = getHttpEntity(null, true);
        log.debug("Get Inventory service request headers: {}", request.getHeaders());
        try {
            ResponseEntity<InventoryGetRes> inventoryAvailabilityResponse = client.exchange(
                    domainProperties.getInventoryService() + urlProperties.getGetInventory(), HttpMethod.GET,
                    request, InventoryGetRes.class, uriParams);
            if (log.isDebugEnabled()) {
                log.debug("Received headers from Get Inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryAvailabilityResponse.getHeaders()));
                log.debug("Received response from Get Inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryAvailabilityResponse.getBody()));
            }
            return inventoryAvailabilityResponse.getBody();
        } catch (Exception ex) {
            log.error("Error while invoking inventory availability service: {}", ex.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
    }

    @Override
    public void holdInventory(HoldInventoryReq request) {
        HttpEntity<HoldInventoryReq> httpRequest = getHttpEntity(request, false);
        log.debug("Hold inventory service request headers: {}", httpRequest.getHeaders());
        try {
            ResponseEntity<HoldInventoryRes> inventoryHoldResponse = client.exchange(
                    domainProperties.getInventoryService() + urlProperties.getHoldInventory(), HttpMethod.POST,
                    httpRequest, HoldInventoryRes.class, createUriParams());
            if (log.isDebugEnabled()) {
                log.debug("Received headers from Hold inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryHoldResponse.getHeaders()));
                log.debug("Received response from Hold inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryHoldResponse.getBody()));
            }
        } catch (Exception ex) {
            log.error("Error while invoking inventory hold service: {}", ex.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
    }

    @Override
    public void releaseInventory(ReleaseInventoryReq request) {
        HttpEntity<ReleaseInventoryReq> httpRequest = getHttpEntity(request, false);
        log.debug("Release inventory service request headers: {}", httpRequest.getHeaders());
        try {
            ResponseEntity<Void> inventoryReleaseResponse = client.exchange(
                    domainProperties.getInventoryService() + urlProperties.getReleaseInventory(), HttpMethod.PUT,
                    httpRequest, Void.class, createUriParams());
            if (log.isDebugEnabled()) {
                log.debug("Received headers from Release inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryReleaseResponse.getHeaders()));
            }
        } catch (BusinessException ex) {
            log.error("Business Error while invoking inventory release service: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        } catch (Exception ex) {
            log.error("Error while invoking inventory release service: {}", ex.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
    }

    @Override
    public String commitInventory(CommitInventoryReq request) {
        HttpEntity<CommitInventoryReq> httpRequest = getHttpEntity(request, false);
        log.debug("Commit inventory service request headers: {}", httpRequest.getHeaders());
        try {
            ResponseEntity<Void> inventoryCommitResponse = client.exchange(
                    domainProperties.getInventoryService() + urlProperties.getCommitInventory(), HttpMethod.POST,
                    httpRequest, Void.class, createUriParams());
            if (log.isDebugEnabled()) {
                log.debug("Received headers from commit inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryCommitResponse.getHeaders()));
            }
            return "Success";
        } catch (Exception ex) {
            log.error("Error while invoking inventory commit service: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public void rollBackInventory(RollbackInventoryReq request) {
        HttpEntity<RollbackInventoryReq> httpRequest = getHttpEntity(request, false);
        log.debug("Commit inventory service request headers: {}", httpRequest.getHeaders());
        try {
            ResponseEntity<Void> inventoryRollbackResponse = client.exchange(
                    domainProperties.getInventoryService() + urlProperties.getRollbackInventory(), HttpMethod.PUT,
                    httpRequest, Void.class, createUriParams());
            if (log.isDebugEnabled()) {
                log.debug("Received headers from commit inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryRollbackResponse.getHeaders()));
            }

        } catch (Exception ex) {
            log.error("Error while invoking inventory rollback service: {}", ex.getMessage());
            log.error("Rollback Inventory call failed even after successful cancellation for confirmation number : {}", request.getConfirmationNumber());
            log.error("Failed Rollback Inventory Request: {}", CommonUtil.convertObjectToJsonString(request));
        }
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received from  Inventory service API : header: {} body: {}", httpResponse.getHeaders().toString(), response);
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else {
                if (httpResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
                } else if (response.contains(ServiceConstant.INVENTORY_SERVICE_INVALID_HOLDID_ERROR)) {
                	//Requested by carts to return Reservation Not found to match with ACRS flow.
                	log.warn("Error Received From Inventory Services: {}", response);
                    throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,response);
                }
            }
        }
    }

    private <T> HttpEntity<T> getHttpEntity(T request, boolean noRequest) {
        if (noRequest) {
            return new HttpEntity<>(CommonUtil.createProductInventoryHeaders(idmsTokenDAO.generateToken().getAccessToken()));
        } else {
            return new HttpEntity<>(request, CommonUtil.createProductInventoryHeaders(idmsTokenDAO.generateToken().getAccessToken()));
        }
    }

    private Map<String, String> createUriParams()  {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        return uriParams;
    }

    @Override
    public BookedItemList getInventoryStatus(String confirmationNumber, String holdId) {
        Map<String, String> uriParams = createUriParams();
        String inventoryStatusUrl;
        if (StringUtils.isNotEmpty(confirmationNumber)) {
            uriParams.put("confirmationNumber", confirmationNumber);
            inventoryStatusUrl = urlProperties.getStatusInventory();
        } else {
            uriParams.put("holdId", holdId);
            inventoryStatusUrl = urlProperties.getStatusInventoryHoldId();
        }
        HttpEntity<?> request = getHttpEntity(null, true);
        log.debug("Status Inventory service request headers: {}", request.getHeaders());
        try {
            ResponseEntity<BookedItemList> inventoryStatusResponse = client.exchange(
                    domainProperties.getInventoryService() + inventoryStatusUrl, HttpMethod.GET,
                    request, BookedItemList.class, uriParams);
            if (log.isDebugEnabled()) {
                log.debug("Received headers from Status Inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryStatusResponse.getHeaders()));
                log.debug("Received response from Status Inventory service API: {}",
                        CommonUtil.convertObjectToJsonString(inventoryStatusResponse.getBody()));
            }
            return inventoryStatusResponse.getBody();
        } catch (Exception ex) {
            log.error("Error while invoking inventory status service: {}", ex.getMessage());
            return null;
        }
    }
}
