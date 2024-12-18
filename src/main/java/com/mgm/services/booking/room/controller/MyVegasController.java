package com.mgm.services.booking.room.controller;

import javax.validation.Valid;
import javax.xml.bind.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.service.MyVegasService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.validator.MyVegasRequestValidator;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.RedemptionValidationResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to validate and confirm the myvegas redemption code.
 *
 */
@RestController
@RequestMapping("/v1")
@Log4j2
public class MyVegasController extends BaseController {

    private static final String TOKEN_PATTERN = "_";
    private static final int TOKEN_SIZE = 2;

    @Autowired
    private MyVegasService myVegasService;

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
     * @return Returns session in header
     * @throws ValidationException
     *             if any
     */
    @GetMapping("/myvegas/{redemptionCode}/validate")
    public MyVegasResponse validateRedemptionCode(@PathVariable String redemptionCode, @RequestHeader String source,
            @Valid MyVegasRequest myVegasRequest) {
        if (myVegasRequest == null) {
            myVegasRequest = new MyVegasRequest();
        }
        preprocess(source, myVegasRequest, null);

        myVegasRequest.setRedemptionCode(redemptionCode);
        log.info("MyVegas Redemption request:{}", myVegasRequest);

        // Validate and report errors
        final Errors errors = new BeanPropertyBindingResult(myVegasRequest, "myVegasRequest");
        validator.validate(myVegasRequest, errors);
        handleValidationErrors(errors);

        long customerId = myVegasRequest.getCustomerId();

        RedemptionValidationResponse response = myVegasService.validateRedemptionCode(myVegasRequest);
        if (customerId > 0 && !CommonUtil.isMatchingMyVegasProfile(sSession.getCustomer(), response.getCustomer())) {
            throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
        }
        MyVegasResponse myVegasResponse = convertFromAuroraRedemptionResponse(response);

        sSession.getMyVegasRedemptionItems().put(myVegasResponse.getProgramId(), response);
        return myVegasResponse;

    }

    private MyVegasResponse convertFromAuroraRedemptionResponse(RedemptionValidationResponse response) {

        MyVegasResponse myVegasResponse = new MyVegasResponse();
        myVegasResponse.setStatus(response.getStatus());
        if (response.getCouponCode() != null && response.getCouponCode().split(TOKEN_PATTERN).length == TOKEN_SIZE) {
            String[] tokens = response.getCouponCode().split(TOKEN_PATTERN);
            myVegasResponse.setProgramId(tokens[1]);
            myVegasResponse.setPropertyId(tokens[0]);
        }
        myVegasResponse.setRewardType(response.getRewardType());

        return myVegasResponse;
    }

    /**
     * Service to handle POST request of confirming a redemption code.
     * 
     * @param redemptionCode
     *            redemption code
     * @param skipMyVegasConfirm
     *            whether to skip sending the confirmation
     * @return Returns session in header
     * @throws ValidationException
     *             if any
     */
    @PostMapping("/myvegas/{redemptionCode}/confirm")
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void confirmRedemptionCode(@PathVariable String redemptionCode, @RequestHeader(
            defaultValue = "false") String skipMyVegasConfirm) {

        if (StringUtils.isNotEmpty(skipMyVegasConfirm) && skipMyVegasConfirm.equalsIgnoreCase(ServiceConstant.TRUE)) {
            log.info("Skipping sending the confirmation for the redemption code for MyVegas program");
            return;
        }

        MyVegasRequest myVegasRequest = new MyVegasRequest();
        myVegasRequest.setRedemptionCode(redemptionCode);
        log.debug("MyVegas Confirmation request:{}", myVegasRequest);

        // Validate and report errors
        final Errors errors = new BeanPropertyBindingResult(myVegasRequest, "myVegasRequest");
        validator.validate(myVegasRequest, errors);
        handleValidationErrors(errors);

        myVegasService.confirmRedemptionCode(myVegasRequest);

    }

}
