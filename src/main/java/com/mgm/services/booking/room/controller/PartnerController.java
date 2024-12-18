package com.mgm.services.booking.room.controller;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.booking.room.model.response.PartnerConfigResponse;
import com.mgm.services.booking.room.service.PartnerService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/v2")
@V2Controller
public class PartnerController extends ExtendedBaseV2Controller{

    @Autowired
    private PartnerService partnerService;
    

    /**
     * Returns the list of partner names and codes that are stored in the config file
     * @return response with names and codes
     */
    @GetMapping("/partner/programs")
    public PartnerConfigResponse getPartnerPrograms() {
        return partnerService.getPartnerConfig();

    }
    
    @GetMapping("/partner/accounts")
	public PartnerAccountsSearchV2Response searchPartnerAccount(@RequestHeader String source,
			@Valid PartnerAccountV2Request partnerAccountRequest, BindingResult result, HttpServletRequest servletRequest) {
		
		preprocess(source, partnerAccountRequest, result, servletRequest, null);
		Errors errors = new BeanPropertyBindingResult(partnerAccountRequest, "partnerAccountRequest");
        handleValidationErrors(errors);
		
		return partnerService.searchPartnerAccount(partnerAccountRequest);
	}
}
