package com.mgm.services.booking.room.validator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.common.exception.AuthorizationException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * This class provides methods to validate the JWT token against various claims
 * such as scope, groups etc.
 * 
 * @author swakulka
 *
 */
@Component
@Log4j2
public class TokenValidatorImpl implements TokenValidator {

    @Value("${idms.token.validation.enabled}")
    private boolean validateTokenEnabled;

    @Override
    public void validateToken(String jwt, String scope) {

        log.debug("Validating jwt token for the presence of given scope {}", scope);
        if (!TokenValidationUtil.tokenContainsScope(jwt, scope)) {

            log.warn("Scope - {} - not found in jwt token!!", scope);
            throw new AuthorizationException(ErrorCode.IDMS_TOKEN_REQUIRED_SCOPE_NOT_PRESENT);
        }
    }

    @Override
    public void validateToken(HttpServletRequest servletRequest, RBSTokenScopes scope) {
        if (validateTokenEnabled) {
            String jwt = TokenValidationUtil.extractJwtToken(servletRequest);
            validateToken(jwt, scope.getValue());
            doAdditionalValidationForGuestToken(jwt);
        } else {
            log.debug("Token validation was skipped for scope: {}", scope.toString());
        }
    }
    
    @Override
    public void validateServiceToken(HttpServletRequest servletRequest, RBSTokenScopes scope) {
        if (validateTokenEnabled) {
            String jwt = TokenValidationUtil.extractJwtToken(servletRequest);
            validateToken(jwt, scope.getValue());
            if(!TokenValidationUtil.isTokenAServiceToken(jwt)) {
                log.warn("JWT provided is not a service token!!", scope);
                throw new AuthorizationException(ErrorCode.TOKEN_NOT_SUPPORTED);
            }
        } else {
            log.debug("Token validation was skipped for scope: {}", scope.toString());
        }
    }

    @Override
    public void validateToken(HttpServletRequest servletRequest, MyVegasTokenScopes scope) {
        if (validateTokenEnabled) {
            String jwt = TokenValidationUtil.extractJwtToken(servletRequest);
            validateToken(jwt, scope.getValue());
        } else {
            log.debug("Token validation was skipped for scope: {}", scope.toString());
        }
    }

    @Override
    public void validateTokenAllScopes(String jwt, List<String> scopes) {

        log.debug("Validating jwt token for the presence of given scopes {}", scopes);
        if (!TokenValidationUtil.tokenContainsAllScopes(jwt, scopes)) {

            log.warn("One or more scope(s) - {} - not found in jwt token!!", scopes);
            throw new AuthorizationException(ErrorCode.IDMS_TOKEN_REQUIRED_SCOPE_NOT_PRESENT);
        }
    }
    
    @Override
    public void validateTokenAnyScopes(HttpServletRequest servletRequest, List<String> scopes) {

       if(validateTokenEnabled) {
           log.debug("Validating jwt token for the presence of given scopes {}", scopes);
           String jwt = TokenValidationUtil.extractJwtToken(servletRequest);
           if (!TokenValidationUtil.tokenContainAnyScopes(jwt, scopes)) {

               log.warn("None of the scope(s) - {} - found in jwt token!!", scopes);
               throw new AuthorizationException(ErrorCode.IDMS_TOKEN_REQUIRED_SCOPE_NOT_PRESENT);
           }
           doAdditionalValidationForGuestToken(jwt);
       }
    }

    @Override
    public void validateTokens(HttpServletRequest servletRequest, MyVegasTokenScopes[] myVegasTokenscopes) {
        List<String> scopes = Arrays.stream(myVegasTokenscopes).map(MyVegasTokenScopes::getValue).collect(Collectors.toList());
        if (validateTokenEnabled) {
            String jwt = TokenValidationUtil.extractJwtToken(servletRequest);
            validateTokenAllScopes(jwt, scopes);
        } else {
            log.debug("Token validation was skipped for scope: {}", scopes);
        }
    }

    private void doAdditionalValidationForGuestToken(String jwt) {
        // Check if the token is guest token
        if (TokenValidationUtil.isTokenAGuestToken(jwt)) {
            // retrieve the required claims from the token
            Map<String, String> tokenClaims = TokenValidationUtil.getClaimsFromToken(jwt,
                    Arrays.asList(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, ServiceConstant.IDMS_TOKEN_CUSTOMER_ID_CLAIM,
                            ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
            // isCustomerIdBlank will be true, if either GSE customer id is not present in
            // the tokenClaims or it's value is blank (i.e., null or empty)
            boolean isCustomerIdBlank = !tokenClaims.containsKey(ServiceConstant.IDMS_TOKEN_CUSTOMER_ID_CLAIM)
                    || StringUtils.isBlank(tokenClaims.get(ServiceConstant.IDMS_TOKEN_CUSTOMER_ID_CLAIM));
            // isMlifeBlank will be true, if either mlife number is not present in the
            // tokenClaims or it's value is blank (i.e., null or empty)
            boolean isMlifeBlank = !tokenClaims.containsKey(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM)
                    || StringUtils.isBlank(tokenClaims.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
            if (isCustomerIdBlank && isMlifeBlank) {
                return;
            }
            if (isCustomerIdBlank) {
                log.info("GSE Customer Id is not available for mlife {} and mgmId {}",
                        tokenClaims.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM),
                        tokenClaims.get(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
                throw new AuthorizationException(ErrorCode.IDMS_TOKEN_NO_GSE_CUSTOMER_ID_CLAIM);
            }
        }
    }
}
