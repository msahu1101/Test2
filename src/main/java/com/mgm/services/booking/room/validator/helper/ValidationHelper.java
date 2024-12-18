package com.mgm.services.booking.room.validator.helper;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.booking.room.validator.RBSTokenScopes;

/**
 * Helper class to keep supporting methods of validators.
 * 
 * @author laknaray
 *
 */
@Component
public class ValidationHelper {
    @Value("${idms.token.validation.enabled}")
    private boolean validateTokenEnabled;
    /**
     * Reads the Jwt Token from the request and check for the existence of the
     * <code>rooms.reservation:read:elevated</code>.
     * 
     * @return true, only if the Jwt Token in the request has
     *         <code>rooms.reservation:read:elevated</code> scope.
     */
    public boolean hasElevatedAccess() {
        return TokenValidationUtil.tokenContainsScope(extractJwtToken(),
                RBSTokenScopes.GET_RESERVATION_ELEVATED.getValue());
    }
    public boolean validateTokenBasedOrServiceBasedRole(){
        if(validateTokenEnabled){
            return hasElevatedAccess();
        }else{
            return hasServiceRoleAccess();
        }
    }

    /**
     * Reads the Jwt Token from the request and check for the existence of the
     * <code>service:role</code>.
     *
     * @return true, only if the Jwt Token in the request has
     *         <code>rooms.reservation:read:elevated</code> scope.
     */
    public boolean hasServiceRoleAccess() {
        return TokenValidationUtil.tokenContainsServiceRole(extractJwtToken());
    }

    /**
     * Extracts Jwt token from the request.
     * 
     * @return jwt token as string
     */
    public String extractJwtToken() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        return TokenValidationUtil.extractJwtToken(httpRequest);
    }

    /**
     * Returns whether the token is guest token or not.
     * 
     * @return true, only if the token is guest token otherwise false.
     */
    public boolean isTokenAGuestToken() {
        return TokenValidationUtil.isTokenAGuestToken(extractJwtToken());
    }

    /**
     * Reads the Jwt Token from the request and check for the existence of the
     * <code>rooms.reservation:update:elevated</code>.
     * 
     * @return true, only if the Jwt Token in the request has
     *         <code>rooms.reservation:update:elevated</code> scope.
     */
    public boolean hasElevatedAccessToUpdate() {
        return TokenValidationUtil.tokenContainsScope(extractJwtToken(),
                RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue());
    }
}
