/**
 * 
 */
package com.mgm.services.booking.room.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.WebUtils;

import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.model.BaseRequest;

import lombok.extern.log4j.Log4j2;

/**
 * Abstract class to provide common functionality across different controller
 * classes. All controller classes, which use JWB fake customerIds based on
 * cookie value should extend this class thereby leveraging some functionality.
 *
 */
@Log4j2
public class ExtendedBaseController extends BaseController {

    @Autowired
    private AuroraProperties properties;

    /**
     * Run prechecks and processes like binding validation, setting source and
     * finding customer id
     * 
     * @param source
     *            Source from header
     * @param request
     *            Base request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwbHeader
     *            enableJwbHeader from request
     */
    protected void preprocess(String source, BaseRequest request, BindingResult result,
            HttpServletRequest servletRequest, String enableJwbHeader) {

        if (null != result) {
            handleValidationErrors(result);
        }
        request.setSource(source);
        processUserInfo(request, servletRequest, enableJwbHeader);

        final Cookie cookie = WebUtils.getCookie(servletRequest, "osid");
        final String osid = (cookie == null ? "" : cookie.getValue());

        if (StringUtils.isNotEmpty(osid) && request.getCustomerId() == -1) {
            log.warn("OSID cookie is available but no customer info in session.");
        }
        log.info("Customer Details - Customer ID {}, Mlife Number {}", request.getCustomerId(),
                request.getMlifeNumber());
    }

    /**
     * Method update the request object with the customer Id to use. All GSE
     * APIs are based on Customer ID for logged-in user. If the request has
     * customer Id, session is updated with this customer id and request object
     * is updated. If the request doesn't have customer id, session is checked
     * to find the customer id and used if available. If the request and session
     * doesn't have customer id, and mlife number is available, aurora API is
     * invoked to get customer id from mlife number.
     * 
     * <p>
     * Method update the request object with the random customer Id to use, when
     * the user is not logged in and enableJwb cookie/header is true. Also, same
     * random customer Id will be set in the customer object in the session, if
     * the customer object exists.
     * </p>
     * 
     * @param request
     *            Base request object
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwbHeader
     *            enableJwbHeader from request
     */
    protected void processUserInfo(BaseRequest request, HttpServletRequest servletRequest, String enableJwbHeader) {

        if (sSession.getCustomer() != null && sSession.getCustomer().getCustomerId() > 0) {
            request.setCustomerId(sSession.getCustomer().getCustomerId());
            request.setMlifeNumber(Integer.toString(sSession.getCustomer().getMlifeNumber()));
            ThreadContext.put(MLIFENO, String.valueOf(sSession.getCustomer().getMlifeNumber()));
        } else {
            final Cookie cookie = WebUtils.getCookie(servletRequest, "enableJwb");
            final String enableJwbCookie = (cookie == null ? "" : cookie.getValue());

            // Customer id for guest customer
            long randomCustomerId = -1;
            if (sSession.getCustomer() != null) {
                sSession.getCustomer().setCustomerId(randomCustomerId);
            }
            if (StringUtils.equalsIgnoreCase(enableJwbCookie, "true")
                    || StringUtils.equalsIgnoreCase(enableJwbHeader, "true")) {
                randomCustomerId = properties.getRandomCustomerId();
                log.info("Customer Details - Randomly generated - Customer ID {}", randomCustomerId);
                log.info("Request made with JWB enabled. API: {}", servletRequest.getRequestURI());
            }
            request.setCustomerId(randomCustomerId);
        }
    }
}
