package com.mgm.services.booking.room.dao.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.MyVegasDAO;
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.request.dto.RedemptionConfirmationRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RedemptionConfirmationRequestDTO.Address;
import com.mgm.services.booking.room.model.request.dto.RedemptionConfirmationRequestDTO.Balance;
import com.mgm.services.booking.room.model.request.dto.RedemptionConfirmationRequestDTO.Customer;
import com.mgm.services.booking.room.model.request.dto.RedemptionConfirmationRequestDTO.Phone;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.RedemptionValidationResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for validating and confirming the redemption code
 *
 */
@Component("RestMyVegasDAOImpl")
@Log4j2
public class RestMyVegasDAOImpl implements MyVegasDAO {
    
    @Autowired
    private DomainProperties domainProps;
    
    @Autowired
    private URLProperties urlProps;

    private RestTemplate client;
    private ApplicationProperties applicationProperties;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param builder
     *            Spring's auto-configured RestTemplateBuilder
     * @param applicationProperties
     *            Application Properties
     */
    public RestMyVegasDAOImpl(RestTemplateBuilder builder, ApplicationProperties applicationProperties) {
        super();
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getMyVegasConnectionTimeoutInSecs() * 1000,
                applicationProperties.getMyVegasReadTimeoutInSecs() * 1000,
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL());
        this.applicationProperties = applicationProperties;
    }

    @Override
    public RedemptionValidationResponse validateRedemptionCode(MyVegasRequest myVegasRequest) {

        String validateRedemptionURL = domainProps.getMyVegas() + urlProps.getMyVegasValidate();

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put(ServiceConstant.MYVEGAS_ENVIRONMENT_PH, applicationProperties.getMyVegasEnvironment());
        uriVariables.put(ServiceConstant.REDEMPTION_CODE_PH, myVegasRequest.getRedemptionCode());
        
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(validateRedemptionURL).build();

        try {
            HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return transformResponse(client.exchange(builder.toUriString(), HttpMethod.GET,
                    getHttpEntity(myVegasRequest, httpRequest), MyVegasResponse.class, uriVariables).getBody());
        } catch (HttpClientErrorException ex) {
            String responseString = ex.getResponseBodyAsString();
            log.error("Myvegas validate redemption code resulted in error for redemption code {} : {} : {}",
                    myVegasRequest.getRedemptionCode(), responseString, ex);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                ErrorResponse errorResponse = objectMapper.readValue(responseString, ErrorResponse.class);
                handleMyVegasError(errorResponse);
            } catch (JsonProcessingException jsonError) {
                log.error("Error Occurred while validate Redemption code error processing {} : {}",
                        myVegasRequest.getRedemptionCode(), jsonError);
            }
        }

        log.info("The redemption code {} could not be validated", myVegasRequest.getRedemptionCode());
        throw new BusinessException(ErrorCode.REDEMPTION_OFFER_NOT_AVAILABLE);
    }

    private <T> HttpEntity<T> getHttpEntity(T request, HttpServletRequest httpRequest) {
        String correlationId = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            log.info("CorrelationId was not found in header, generated Id is: {}", correlationId);
        }
        String authToken = httpRequest.getHeader(ServiceConstant.HEADER_AUTHORIZATION);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, authToken);
        headers.add(ServiceConstant.X_MGM_CORRELATION_ID, correlationId);
        headers.add(ServiceConstant.X_MGM_CHANNEL, CommonUtil.getChannelHeaderWithFallback(httpRequest));
        headers.add(ServiceConstant.X_MGM_TRANSACTION_ID, httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID));

        return new HttpEntity<>(request, headers);
    }

    private RedemptionValidationResponse transformResponse(MyVegasResponse responseDto) {
        RedemptionValidationResponse response = new RedemptionValidationResponse();
        BeanUtils.copyProperties(responseDto, response);
        return response;
    }

    private void handleMyVegasError(ErrorResponse errorResponse) {
        String errorDescription = CommonUtil.getErrorCode(errorResponse.getError().getMessage(), "\\[(.*?)\\]", 1);
        throw new BusinessException(ErrorCode.MYVEGAS_BACKEND_ERROR, errorDescription.trim());
    }

    @Override
    public void confirmRedemptionCode(MyVegasRequest myVegasRequest) {

        String confirmRedemptionURL = domainProps.getMyVegas() + urlProps.getMyVegasConfirm();

        RedemptionConfirmationRequestDTO redemptionConfirmationRequestDto = new RedemptionConfirmationRequestDTO();
        redemptionConfirmationRequestDto.setRedemptionCode(myVegasRequest.getRedemptionCode());
        redemptionConfirmationRequestDto.setReservationDate(myVegasRequest.getReservationDate());
        redemptionConfirmationRequestDto.setConfirmationNumber(myVegasRequest.getConfirmationNumber());
        redemptionConfirmationRequestDto.setRequiredConfirmationCode(myVegasRequest.getConfirmationNumber());
        redemptionConfirmationRequestDto.setCouponCode(myVegasRequest.getCouponCode());
        Customer customer = new Customer();
        BeanUtils.copyProperties(myVegasRequest.getCustomer(), customer);
        List<Address> address = CommonUtil.copyProperties(myVegasRequest.getCustomer().getAddresses(), List.class);
        List<Phone> phones = CommonUtil.copyProperties(myVegasRequest.getCustomer().getPhones(), List.class);
        Balance balances = CommonUtil.copyProperties(myVegasRequest.getCustomer().getBalances(), Balance.class);
        customer.setAddress(address);
        customer.setPhones(phones);
        customer.setBalances(balances);
        redemptionConfirmationRequestDto.setCustomer(customer);

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put(ServiceConstant.MYVEGAS_ENVIRONMENT_PH, applicationProperties.getMyVegasEnvironment());
        uriVariables.put(ServiceConstant.REDEMPTION_CODE_PH, myVegasRequest.getRedemptionCode());

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // spawning to async thread, so that we don't wait for it..
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending confirmation request to MyVegas: {}",
                        CommonUtil.convertObjectToJsonString(redemptionConfirmationRequestDto));

                client.exchange(confirmRedemptionURL, HttpMethod.POST,
                        getHttpEntity(redemptionConfirmationRequestDto, httpRequest), MyVegasResponse.class, uriVariables);
                log.info("Confirmation message to myvegas posted successfully for redemption code: {}",
                        myVegasRequest.getRedemptionCode());
            } catch (Exception e) {
                log.error("Unable to post MyVegas confirmation {}", ExceptionUtils.getStackTrace(e));
            }
        });

    }
}
