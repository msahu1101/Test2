package com.mgm.services.booking.room.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Utility class to verify the authorization token passed in the request for the
 * presence of valid claims.
 * 
 * @author swakulka
 *
 */

@UtilityClass
@Log4j2
public class TokenValidationUtil {

	private JwtParser parser = Jwts.parserBuilder().build();

	/**
	 * Method to retrieve parse jwt token and retrieve all claims.
	 * 
	 * @param jwt - The jwt token string
	 * @return - JWT claims
	 */
	public Optional<Claims> getClaimsFromToken(String jwt) {

		String withoutSignature = jwt.substring(0, (jwt.lastIndexOf('.') + 1));

		Claims claims = parser.parseClaimsJwt(withoutSignature).getBody();

		return Optional.ofNullable(claims);
	}

	/**
	 * Method to validate if the token contains all the scopes present in the input
	 * list.
	 * 
	 * @param jwt    - The jwt token string
	 * @param scopes - List of scopes to validate
	 * @return - true, if all scopes are present in the token, else false
	 */
	@SuppressWarnings("unchecked")
	public boolean tokenContainsAllScopes(String jwt, List<String> scopes) {

		List<Object> tokenScopes = null;

		Optional<Claims> claimsOpt = getClaimsFromToken(jwt);
		if (claimsOpt.isPresent()) {
			tokenScopes = (List<Object>) claimsOpt.get().get(ServiceConstant.IDMS_TOKEN_SCOPE_CLAIM);
		}

		if (null != tokenScopes) {
			return tokenScopes.containsAll(scopes);

		}

		return false;
	}

	/**
	 * Method to validate if the token contains any of the scopes present in the
	 * input list.
	 * 
	 * @param jwt    - The jwt token string
	 * @param scopes - List of scopes to validate
	 * @return - true, if any of the scopes is present in the token, else false
	 */
	@SuppressWarnings("unchecked")
	public boolean tokenContainAnyScopes(String jwt, List<String> scopes) {

		List<String> tokenScopes = null;

		Optional<Claims> claimsOpt = getClaimsFromToken(jwt);
		if (claimsOpt.isPresent()) {
			tokenScopes = (List<String>) claimsOpt.get().get(ServiceConstant.IDMS_TOKEN_SCOPE_CLAIM);
		}

		if (null != tokenScopes) {
			return tokenScopes.stream().anyMatch(scopes::contains);
		}

		return false;
	}

	/**
	 * Method to validate if the token contains the scope passed in the input.
	 * 
	 * @param jwt   - The jwt token string
	 * @param scope - Scope to validate
	 * @return - true, if the given scope is present in the token, else false
	 */
	@SuppressWarnings("unchecked")
	public boolean tokenContainsScope(String jwt, String scope) {

		List<String> tokenScopes = null;

		Optional<Claims> claimsOpt = getClaimsFromToken(jwt);
		if (claimsOpt.isPresent()) {
			tokenScopes = (List<String>) claimsOpt.get().get(ServiceConstant.IDMS_TOKEN_SCOPE_CLAIM);
		}

		if (null != tokenScopes) {
			return tokenScopes.stream().anyMatch(scope::equals);
		}

		return false;

	}

	/**
	 * Method to validate if the token contains the service role passed in the input.
	 *
	 * @return - true, if the given role is present in the token, else false
	 */
	@SuppressWarnings("unchecked")
	public boolean tokenContainsServiceRole(String jwt) {
		List<String> tokenType = null;
		Optional<Claims> claimsOpt = getClaimsFromToken(jwt);

		if (claimsOpt.isPresent()) {
				tokenType = (List<String>) claimsOpt.get().get(ServiceConstant.IDMS_TOKEN_GROUP_CLAIM);
		}
		if (null != tokenType) {
			if (tokenType.contains(ServiceConstant.SERVICE_TOKEN_TYPE)) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Method to validate if the token contains the scope and token type combination
	 * passed in the input.
	 * 
	 * @param jwt   - The jwt token string
	 * @param scope - Scope to validate
	 * @param type  - Token type to validate
	 * @return - true, if given scope and token type combination is present in the
	 *         token, else false
	 */
	@SuppressWarnings("unchecked")
	public boolean tokenContainsScopeAndType(String jwt, String scope, String type) {

		List<String> tokenScopes = null;
		List<String> tokenType = null;

		Optional<Claims> tokenClaims = getClaimsFromToken(jwt);

		if (tokenClaims.isPresent()) {
			tokenScopes = (List<String>) tokenClaims.get().get(ServiceConstant.IDMS_TOKEN_SCOPE_CLAIM);
			tokenType = (List<String>) tokenClaims.get().get(ServiceConstant.IDMS_TOKEN_GROUP_CLAIM);
		}

		if (null != tokenScopes && null != tokenType) {

			return (tokenScopes.stream().anyMatch(scope::contains) && tokenType.stream().anyMatch(type::contains));
		}

		return false;

	}
	
	/**
	 * Method to extract jwt token from the servlet request
	 * 
	 * @param servletRequest - The Http servlet request
	 * @return - the jwt token
	 */
	public String extractJwtToken(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader(ServiceConstant.HEADER_KEY_AUTHORIZATION);
        if(StringUtils.isEmpty(authHeader)) {
            log.warn("Received null Auth token");
            return null;
        }
        return authHeader.replace(ServiceConstant.HEADER_AUTH_BEARER, "").trim();
    }
	
    /**
     * Method to extract values for the list of claims provided from the token
     * 
     * @param jwt
     *            the jwt token
     * @param claims
     *            the list of claims
     * @return the Map of claims
     */
	public Map<String,String> getClaimsFromToken(String jwt, List<String> claims){
	    Map<String,String> tokenClaims = new HashMap<>();
	    Optional<Claims> claimsOpt = getClaimsFromToken(jwt);
        if (claimsOpt.isPresent()) {
            tokenClaims = claims.stream().filter(c -> claimsOpt.get().containsKey(c))
                    .collect(Collectors.toMap(claim -> claim, claim -> String.valueOf(claimsOpt.get().get(claim))));
        }
	    return tokenClaims;
	}
	
    /**
     * Method to check if the token is a guest token
     * 
     * @param jwt
     *            the jwt token
     * @return if the token a guest token
     */
    @SuppressWarnings("unchecked")
    public boolean isTokenAGuestToken(String jwt) {
        Optional<Claims> claimsOpt = getClaimsFromToken(jwt);
        List<String> tokenType = null;
        if (claimsOpt.isPresent()) {
            tokenType = (List<String>) claimsOpt.get().get(ServiceConstant.IDMS_TOKEN_GROUP_CLAIM);
        }
        if (null != tokenType) {
            return tokenType.contains(ServiceConstant.GUEST_TOKEN_TYPE);
        }
        return false;
    }
    
    /**
     * Method to check if the token is a service token
     * 
     * @param jwt
     *            the jwt token
     * @return if the token a service token
     */
    @SuppressWarnings("unchecked")
    public boolean isTokenAServiceToken(String jwt) {
        Optional<Claims> claimsOpt = getClaimsFromToken(jwt);
        List<String> tokenType = null;
        if (claimsOpt.isPresent()) {
            tokenType = (List<String>) claimsOpt.get().get(ServiceConstant.IDMS_TOKEN_GROUP_CLAIM);
        }
        if (null != tokenType) {
            return tokenType.contains(ServiceConstant.SERVICE_TOKEN_TYPE);
        }
        return false;
    }

}
