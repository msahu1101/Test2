package com.mgm.services.booking.room.controller;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.EcidbyEmrRequest;
import com.mgm.services.booking.room.model.response.EcidByEmrResponse;
import com.mgm.services.booking.room.service.CustomerInformationService;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/v3")
@V2Controller
public class CustomerV3Controller extends ExtendedBaseV2Controller{
    private final CustomerInformationService customerInformationService;
    private final TokenValidator tokenValidator;

    @Autowired
    public CustomerV3Controller(CustomerInformationService customerInformationService,TokenValidator tokenValidator ){
        this.customerInformationService = customerInformationService;
        this.tokenValidator = tokenValidator;
    }


    @GetMapping("/customer/ecidbyemr")
    public EcidByEmrResponse GetEcidByEmr (@RequestHeader String source,
                                           @Valid EcidbyEmrRequest request, BindingResult result, HttpServletRequest servletRequest,
                                           @RequestHeader(
                                                  defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        //preprocess(source, emr, result, servletRequest, enableJwb);
        if(request == null || StringUtils.isEmpty(request.getEmr())){
            throw new IllegalArgumentException(ErrorCode.INVALID_CUSTOMER.getErrorCode());
        }
        return customerInformationService.getEcidByEmr(request);
    }
}
