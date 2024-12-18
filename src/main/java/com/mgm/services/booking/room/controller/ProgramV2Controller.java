/**
 * 
 */
package com.mgm.services.booking.room.controller;

import java.util.List;

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
import com.mgm.services.booking.room.model.request.ApplicableProgramsRequest;
import com.mgm.services.booking.room.model.request.CustomerOffersRequest;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.RoomSegmentRequest;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.validator.PerpetualProgramsRequestValidator;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.RoomProgramRequestValidator;
import com.mgm.services.booking.room.validator.RoomProgramValidateRequestValidator;
import com.mgm.services.booking.room.validator.RoomSegmentRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller to handle offers related V2 API endpoints.
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class ProgramV2Controller extends ExtendedBaseV2Controller {

    /**
     * roomProgramService
     */
    @Autowired
    private RoomProgramService roomProgramService;
    
    @Autowired
    private TokenValidator tokenValidator;
    
    private final Validator perpetualProgramsValidator = new PerpetualProgramsRequestValidator();
    
    private final Validator validatePromoValidator = new RoomProgramValidateRequestValidator();

    private final Validator roomProgramValidator = new RoomProgramRequestValidator();
    
    private final Validator roomSegmentValidator = new RoomSegmentRequestValidator();

    /**
     * Returns a list of room programs for a given request.
     * 
     * @param source                    Source header
     * @param applicableProgramsRequest Room program request
     * @param result                    Binding result
     * @param servletRequest            HttpServlet request object
     * @param enableJwb                 enableJwb header
     * @return Returns a list of room programs.
     */
    @GetMapping("/programs/applicable")
    public ApplicableProgramsResponse getApplicablePrograms(@RequestHeader String source,
            @Valid ApplicableProgramsRequest applicableProgramsRequest, BindingResult result,
            HttpServletRequest servletRequest, @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, applicableProgramsRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(applicableProgramsRequest, applicableProgramsRequest.getPropertyId());
        return roomProgramService.getApplicablePrograms(applicableProgramsRequest);
    }

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
    public CustomerOfferResponse getCustomerOffers(@RequestHeader String source,
            @Valid CustomerOffersRequest customerProgramsRequest, BindingResult result,
            HttpServletRequest servletRequest, @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, customerProgramsRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(customerProgramsRequest, customerProgramsRequest.getPropertyId());
        return roomProgramService.getCustomerOffers(customerProgramsRequest);
    }
    
    /**
     * Returns customer perpetual offer for the given request.
     * 
     * @param source                    Source header
     * @param programsRequest           customer perpetual programs request object
     * @param result                    Binding result
     * @param servletRequest            HttpServlet request object
     * @param enableJwb                 enableJwb header
     * @return Returns the perpetual programs
     */
    @GetMapping("/programs/default-perpetual")
    public List<PerpetaulProgram> getDefaultPerpetualPrograms(@RequestHeader String source,
            @Valid PerpetualProgramRequest programsRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, programsRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(programsRequest, null);
        Errors errors = new BeanPropertyBindingResult(programsRequest, "programsRequest");
        perpetualProgramsValidator.validate(programsRequest, errors);
        handleValidationErrors(errors);
        return roomProgramService.getDefaultPerpetualProgramsV2(programsRequest);
    }
    
    /**
     * Validate a room program ID or promo code to confirm if the program
     * Id/promoCode is available and eligible to be used for the user.
     * Eligibility check is done to avoid program misuse.
     * 
     * @param source                    Source header
     * @param validateRequest           validate request object
     * @param result                    Binding result
     * @param servletRequest            HttpServlet request object
     * @param enableJwb                 enableJwb header
     * @return Returns the validation response
     */
    @GetMapping("/offer/validate")
    public RoomProgramValidateResponse validateRoomOffer(@RequestHeader String source,
            @Valid RoomProgramValidateRequest validateRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, validateRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(validateRequest, validateRequest.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(validateRequest, "validateRequest");
        validatePromoValidator.validate(validateRequest, errors);
        handleValidationErrors(errors);
        return roomProgramService.validateProgramV2(validateRequest);
    }

    /**
     * Get the program/rateplan code for the given program id
     * @return the segment id
     */
    @GetMapping("/offers")
    public List<RoomOfferDetails> getRoomProgram(@RequestHeader String source,
                                           @Valid RoomProgramV2Request request, BindingResult result, HttpServletRequest servletRequest,
                                           @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, request, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(request, null);
        Errors errors = new BeanPropertyBindingResult(request, "roomProgramRequest");
        roomProgramValidator.validate(request, errors);
        handleValidationErrors(errors);
        return roomProgramService.getProgram(request);
    }
    
    /**
     * Gets participating program ids for the given segment id/code.
     *
     * @deprecated in favor of v3/segment in ProgramV3Controller.java
     *
     * @param segment
     *            Segment code or GUID
     * @return
     */
    @GetMapping("/segment/{segment}")
    @Deprecated
    public RoomSegmentResponse getRoomSegment(@RequestHeader String source, @Valid RoomSegmentRequest request,
            BindingResult result, HttpServletRequest servletRequest) {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
        preprocess(source, request, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(request, null);
        Errors errors = new BeanPropertyBindingResult(request, "roomSegmentRequest");
        roomSegmentValidator.validate(request, errors);
        handleValidationErrors(errors);

        return roomProgramService.getRoomSegment(request.getSegment(), request.getSource());
    }
}
