package com.mgm.services.booking.room.validator;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Token Validator class to validate if the token contains the correct scope.
 * 
 * @author swakulka
 *
 */
public interface TokenValidator {

    /**
     * Method to validate if the token contains the given scope.
     * 
     * @param jwt
     *            The JWT token
     * @param scope
     *            The scope to verify in the token
     */
    void validateToken(String jwt, String scope);

    /**
     * Method to extract token from Http Request and validate if the token
     * contains the given scope.
     * 
     * @param servletRequest
     *            The Http Servlet Request
     * @param scope
     *            The RBS Token Scope
     */
    void validateToken(HttpServletRequest servletRequest, RBSTokenScopes scope);
    
    /**
     * Method to extract token from Http Request and validate if the token
     * contains the given scope and also token is a service token.
     * 
     * @param servletRequest
     *            The Http Servlet Request
     * @param scope
     *            The RBS Token Scope
     */
    void validateServiceToken(HttpServletRequest servletRequest, RBSTokenScopes scope);
    
    /**
     * Method to extract token from Http Request and validate if the token
     * contains the given scope.
     * 
     * @param servletRequest
     *            The Http Servlet Request
     * @param scope
     *            The MyVegas Token Scope
     */
    void validateToken(HttpServletRequest servletRequest, MyVegasTokenScopes scope);

    /**
     * Method to validate if the token contains all the given scopes.
     * 
     * @param jwt
     *            The JWT token
     * @param scopes
     *            The scopes to verify in the token
     */
    void validateTokenAllScopes(String jwt, List<String> scopes);
    
    /**
     * Method to validate if the token contains any of the given scopes.
     * 
     * @param servletRequest
     *            The Http Servlet Request
     * @param scopes
     *            The scopes to verify in the token
     */
    void validateTokenAnyScopes(HttpServletRequest servletRequest, List<String> scopes);

    /**
     * Method to extract token from Http Request and validate if the token
     * contains the given scopes.
     * 
     * @param servletRequest
     *            The Http Servlet Request
     * @param scopes
     *            The MyVegas Token Scopes
     */
    void validateTokens(HttpServletRequest servletRequest, MyVegasTokenScopes[] scopes);
}
