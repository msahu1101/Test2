package com.mgm.services.booking.room.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.service.MyVegasService;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.booking.room.validator.MyVegasRequestValidator;
import com.mgm.services.booking.room.validator.MyVegasTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to validate and confirm the myvegas redemption code.
 * 
 * @author vararora
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
@Log4j2
@Deprecated
public class MyVegasV2Controller extends ExtendedBaseV2Controller {

    @Autowired
    private MyVegasService myVegasService;
    
    @Autowired
    private TokenValidator tokenValidator;

    private final Validator validator = new MyVegasRequestValidator();

    /**
     * Service to handle GET request of validating a redemption code.
     * 
     * @param source
     *            Source header
     * @param redemptionCode
     *            redemption code
     * @param myVegasRequest
     *            request body
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @return Returns session in header
     * @throws ValidationException
     *             if any
     */
    @GetMapping("/myvegas/{redemptionCode}/validate")
    public MyVegasResponse validateRedemptionCode(@PathVariable String redemptionCode, @RequestHeader String source,
            @Valid MyVegasRequest myVegasRequest, BindingResult result, HttpServletRequest servletRequest) {
        tokenValidator.validateToken(servletRequest, MyVegasTokenScopes.VALIDATE_CODE);
        if (myVegasRequest == null) {
            myVegasRequest = new MyVegasRequest();
        }
        preprocess(source, myVegasRequest, result, servletRequest, "false");

        myVegasRequest.setRedemptionCode(redemptionCode);
        log.info("MyVegas Redemption request:{}", myVegasRequest);

        // Validate and report errors
        final Errors errors = new BeanPropertyBindingResult(myVegasRequest, "myVegasRequest");
        validator.validate(myVegasRequest, errors);
        handleValidationErrors(errors);
        
        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return myVegasService.validateRedemptionCodeV2(myVegasRequest, token);
    }

    /**
     * Service to handle POST request of confirming a redemption code.
     * 
     * @param redemptionCode
     *            redemption code
     * @param source
     *            Source header
     * @param skipMyVegasConfirm
     *            whether to skip sending the confirmation
     * @param myVegasRequest
     *            request body
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @return Returns session in header
     * @throws ValidationException
     *             if any
     */
    @PostMapping("/myvegas/{redemptionCode}/confirm")
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void confirmRedemptionCode(@PathVariable String redemptionCode, @RequestHeader String source, @RequestHeader(
            defaultValue = "false") String skipMyVegasConfirm, @Valid @RequestBody MyVegasRequest myVegasRequest,
            BindingResult result, HttpServletRequest servletRequest) {

        tokenValidator.validateToken(servletRequest, MyVegasTokenScopes.CONFIRM_CODE);
        if (StringUtils.isNotEmpty(skipMyVegasConfirm) && skipMyVegasConfirm.equalsIgnoreCase(ServiceConstant.TRUE)) {
            log.info("Skipping sending the confirmation for the redemption code for MyVegas program");
            return;
        }

        myVegasRequest.setRedemptionCode(redemptionCode);
        log.debug("MyVegas Confirmation request:{}", myVegasRequest);

        // Validate and report errors
        final Errors errors = new BeanPropertyBindingResult(myVegasRequest, "myVegasRequest");
        validator.validate(myVegasRequest, errors);
        handleValidationErrors(errors);

        myVegasService.confirmRedemptionCodeV2(myVegasRequest);

    }

}
