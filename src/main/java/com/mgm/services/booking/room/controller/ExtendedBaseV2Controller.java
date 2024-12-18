/**
 * 
 */
package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.service.UserInfoDecryptionService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.WebUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.BaseCommonUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.log4j.Log4j2;

/**
 * Abstract class to provide common functionality across different controller
 * classes. All controller classes, which use JWB fake customerIds based on
 * cookie value should extend this class thereby leveraging some functionality.
 *
 */
@Log4j2
public class ExtendedBaseV2Controller extends BaseController {

    @Autowired
    private AuroraProperties properties;
    
    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private UserInfoDecryptionService userInfoDecryptionService;

    /**
     * Run prechecks and processes like binding validation, setting source and
     * finding customer id
     * 
     * @param source
     *              Source from header
     * @param request
     *              Base request
     * @param result
     *              Binding result
     * @param servletRequest
     *              HttpServlet request object
     * @param enableJwbHeader
     *              enableJwbHeader from request
     */
    protected void preprocess(String source, BaseRequest request, BindingResult result,
            HttpServletRequest servletRequest, String enableJwbHeader) {
        String derivedSource = StringUtils.isNotBlank(source) ? source
                : servletRequest.getHeader(ServiceConstant.X_MGM_SOURCE);
        log.info("API called with source: {}, request: {}, enableJwb: {}", derivedSource,
                BaseCommonUtil.objectToJson(request), enableJwbHeader);
        
        if (null != result) {
            handleValidationErrors(result);
        }

        processTokenClaims(request, servletRequest);
        if (null != request) {
            request.setSource(derivedSource);
            extractUserInfo(request);
            processUserInfo(request, servletRequest, enableJwbHeader);

            log.debug("Customer Details - Customer ID {}, Mlife Number {}", request.getCustomerId(),
                    request.getMlifeNumber());
        }

    }

    protected void extractUserInfo(BaseRequest request) {
    	try {
    		if(StringUtils.isNotEmpty(request.getEcid())) {
    			long customerId = Long.parseLong(
    					userInfoDecryptionService.decrypt(request.getEcid().replace(' ', '+').replaceFirst("^@","")));
    			request.setCustomerId(customerId);
    		}
    		if(StringUtils.isNotEmpty(request.getEmr())) {
    			String mlifeNumber = userInfoDecryptionService.decrypt(request.getEmr().replaceFirst("^@",""));
    			Integer.parseInt(mlifeNumber);
    			request.setMlifeNumber(mlifeNumber);
    		}
    	} catch (Exception e) {
    		log.error("Error occured while setting MlifeNumber and CustomerId from Request Information: {}", e.getMessage());
    	}
    }

    /**
     * Extracts the jwt token from the request and pulls the claims
     * <ul>
     * <li>mlife</li>
     * <li>com.mgm.gse.id</li>
     * <li>com.mgm.loyalty.tier</li>
     * <li>com.mgm.loyalty.perpetual_eligible</li>
     * </ul>
     * and sets them in the request, if none of them are null.
     * 
     * @param request
     *            BaseRequest object
     * @param servletRequest
     *            HttpServletRequest object
     */
    @SuppressWarnings("unchecked")
	private void processTokenClaims(BaseRequest request, HttpServletRequest servletRequest) {
        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        List<String> perpetualEligiblePropertyCodes = new ArrayList<String>();
        if (null != appProps && appProps.isEnableJwtLogging()) {
            log.info("Authorization: {}", token);
        }
        
        if (StringUtils.isNotEmpty(token) && TokenValidationUtil.isTokenAGuestToken(token)) {
            log.debug("Token present is a guest token, proceeding with populating customer details");
            Map<String, String> tokenClaims = TokenValidationUtil.getClaimsFromToken(token,
                    Arrays.asList(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, ServiceConstant.IDMS_TOKEN_CUSTOMER_ID_CLAIM,
                            ServiceConstant.IDMS_TOKEN_CUSTOMER_TIER_CLAIM, ServiceConstant.IDMS_TOKEN_PERPETUAL_CLAIM,
                            ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
            log.info("Received values from guest token: {}", tokenClaims);

            if (tokenClaims.containsKey(ServiceConstant.IDMS_TOKEN_CUSTOMER_ID_CLAIM)) {
                request.setCustomerId(Long.valueOf(tokenClaims.get(ServiceConstant.IDMS_TOKEN_CUSTOMER_ID_CLAIM)));
            }

            request.setMlifeNumber(tokenClaims.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
            request.setCustomerTier(tokenClaims.get(ServiceConstant.IDMS_TOKEN_CUSTOMER_TIER_CLAIM));

            if (tokenClaims.containsKey(ServiceConstant.IDMS_TOKEN_PERPETUAL_CLAIM)) {
                request.setPerpetualPricing(
                        Boolean.valueOf(tokenClaims.get(ServiceConstant.IDMS_TOKEN_PERPETUAL_CLAIM)));
            } 
            
            //CBSR-1452 Extract the perpetual Eligible Property Ids to be used for ACRS from the JWT Token
            Optional<Claims> claimsOpt = TokenValidationUtil.getClaimsFromToken(token);
            if (claimsOpt.isPresent()) {
            	perpetualEligiblePropertyCodes = (List<String>) claimsOpt.get()
            	.get(ServiceConstant.IDMS_TOKEN_PERPETUAL_ELIGIBLE_PROPERTY_IDS_CLAIM);
            }
        }
        if(request.getPerpetualEligiblePropertyCodes() != null && CollectionUtils.isEmpty(perpetualEligiblePropertyCodes)){
            perpetualEligiblePropertyCodes = Arrays.stream(request.getPerpetualEligiblePropertyCodes().split(",")).collect(Collectors.toList());
        }
    	if(CollectionUtils.isNotEmpty(perpetualEligiblePropertyCodes) && null != appProps) {
            // for pseudo property there will be multiple property for same operaCode.
    	    List<String> propertyIdsList = perpetualEligiblePropertyCodes.stream()
                    .map(propertyCode -> appProps.getPropertyIdsFromHotelCode(propertyCode))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
    		request.setPerpetualEligiblePropertyIds(propertyIdsList);
    	}
    }

    /**
     * Method update the request object with the customer Id to use. All GSE APIs
     * are based on Customer ID for logged-in user. If the request has customer Id,
     * session is updated with this customer id and request object is updated. If
     * the request doesn't have customer id, session is checked to find the customer
     * id and used if available. If the request and session doesn't have customer
     * id, and mlife number is available, aurora API is invoked to get customer id
     * from mlife number.
     * 
     * <p>
     * Method update the request object with the random customer Id to use, when the
     * user is not logged in and enableJwb cookie/header is true. Also, same random
     * customer Id will be set in the customer object in the session, if the
     * customer object exists.
     * </p>
     * 
     * @param request
     *              Base request object
     * @param servletRequest
     *              HttpServlet request object
     * @param enableJwbHeader
     *              enableJwbHeader from request
     */
    protected void processUserInfo(BaseRequest request, HttpServletRequest servletRequest, String enableJwbHeader) {

        String mlifeNumber = request.getMlifeNumber();
        long customerId = request.getCustomerId() != 0 ? request.getCustomerId() : -1;
        if (StringUtils.isNotBlank(mlifeNumber) || customerId > 0) {
            request.setCustomerId(customerId);
            ThreadContext.put(MLIFENO, StringUtils.trimToEmpty(mlifeNumber) + "-" + customerId);
        } else {
            final Cookie cookie = WebUtils.getCookie(servletRequest, "enableJwb");
            final String enableJwbCookie = (cookie == null ? "" : cookie.getValue());

            // Customer id for guest customer
            long randomCustomerId = -1;
            if (StringUtils.equalsIgnoreCase(enableJwbCookie, "true")
                    || StringUtils.equalsIgnoreCase(enableJwbHeader, "true")) {
                AuroraProperties.JWBCustomer randomJWBCustomer = properties.getRandomJWBCustomer();
                randomCustomerId = randomJWBCustomer.getCustId();
                mlifeNumber = randomJWBCustomer.getMlifeNo();
                log.info("Customer Details - Randomly generated - Customer ID {} and Mlife No {}", randomCustomerId, mlifeNumber);
                log.info("Request made with JWB enabled. API: {}", servletRequest.getRequestURI());
            }
            request.setCustomerId(randomCustomerId);
            request.setMlifeNumber(mlifeNumber);
        }
    }
    
    /**
     * Method to set the perpetual flag based on com.mgm.loyalty.perpetual_eligible_properties JWT Claim.
     * If Property Id is present in request, perpetual eligible flag will be set to true if the requested property id is present in the JWT claim.
     * If there is no property id, the perpetual flag will be set to true if the JWT claim has at least 1 perpetual eligible property.
     * @param request
     * @param propertyId
     */
    protected void preProcessPerpetualPricing(BaseRequest request, String propertyId) {
    	if(CollectionUtils.isNotEmpty(request.getPerpetualEligiblePropertyIds())) {
    		request.setPerpetualPricing(CommonUtil.isPerpetualPricingEligible(request.getPerpetualEligiblePropertyIds(), propertyId));
    	}
    	
    }
}
