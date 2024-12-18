package com.mgm.services.booking.room.controller;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.CustomerOffersV3Request;
import com.mgm.services.booking.room.model.request.RoomSegmentRequest;
import com.mgm.services.booking.room.model.response.CustomerOfferV3Response;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.RoomSegmentRequestV3Validator;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller to handle offers related V3 API endpoints.
 *
 */
@RestController
@RequestMapping("/v3")
@V2Controller
public class ProgramV3Controller extends ExtendedBaseV2Controller {
    
    @Autowired
    private TokenValidator tokenValidator;
    
    @Autowired
    private RoomProgramService roomProgramService;
    
    private final Validator roomSegmentValidator = new RoomSegmentRequestV3Validator();

    /**
     * Returns customer room offers for the given request.
     * 
     * @param source                  Source header
     * @param customerProgramsRequest customer programs request object
     * @param result                  Binding result
     * @param servletRequest          HttpServlet request object
     * @param enableJwb               enableJwb header
     * @return Returns the customer offers
     */
    @GetMapping("/customer/offers")
    public CustomerOfferV3Response getCustomerOffers(@RequestHeader String source, @RequestHeader String channel,
            @Valid CustomerOffersV3Request customerProgramsRequest, BindingResult result,
            HttpServletRequest servletRequest) {
    	tokenValidator.validateTokenAnyScopes(servletRequest, Arrays.asList(
    			RBSTokenScopes.GET_ROOM_PROGRAMS.getValue(), RBSTokenScopes.OPEN_ID.getValue()));
        preprocess(source, customerProgramsRequest, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(customerProgramsRequest, customerProgramsRequest.getPropertyId());
        customerProgramsRequest.setChannel(channel);
        return roomProgramService.getCustomerOffers(customerProgramsRequest);
    }
    
    /**
     * Gets participating program ids for a given segment or by resolving
     * segment from given program id.
     * 
     * @param source
     *            Calling source
     * @param request
     *            Request data
     * @param result
     *            Binding result
     * @param servletRequest
     *            Http servlet request
     * @return
     */
    @GetMapping("/segment")
    public RoomSegmentResponse getRoomSegment(@RequestHeader String source, @Valid RoomSegmentRequest request,
            BindingResult result, HttpServletRequest servletRequest) {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, request, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(request,null);
        Errors errors = new BeanPropertyBindingResult(request, "roomSegmentRequest");
        roomSegmentValidator.validate(request, errors);
        handleValidationErrors(errors);

        return roomProgramService.getRoomSegment(request.getSegment(), request.getProgramId(), request.getSource());
    }
}
