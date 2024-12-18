package com.mgm.services.booking.room.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.mgm.services.booking.room.model.request.PackageComponentRequest;
import com.mgm.services.booking.room.model.request.PackageComponentRequestV1;
import com.mgm.services.booking.room.model.response.PackageComponentResponse;
import com.mgm.services.booking.room.model.response.PackageComponentResponseV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.RoomComponentV2RequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * This class contains the services responsible for returning the room related
 * details like available component etc.
 * 
 * @author laknaray
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class RoomDetailsController extends ExtendedBaseV2Controller {

    private final Validator validator = new RoomComponentV2RequestValidator();

    @Autowired
    private ComponentService componentService;

    @Autowired
    private TokenValidator tokenValidator;

    /**
     * Returns the list of available components for a given roomTypeId, trip details
     * and propertyId.
     * 
     * @param source
     *            source header string
     * @param componentRequest
     *            RoomComponentV2Request object
     * @param result
     *            BindingResult object
     * @param servletRequest
     *            HttpServletRequest object
     * @return list of RoomComponents available
     */
    @GetMapping("/availability/components")
    public List<RoomComponent> getAvailableRoomComponents(@RequestHeader String source,
            @Valid RoomComponentV2Request componentRequest, BindingResult result, HttpServletRequest servletRequest) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, componentRequest, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(componentRequest, componentRequest.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(componentRequest, "roomComponentV2Request");
        validator.validate(componentRequest, errors);
        handleValidationErrors(errors);
        return componentService.getAvailableRoomComponents(componentRequest);
    }

    @PostMapping("/availability/packageComponents")
    public List<PackageComponentResponseV1> getPackageComponents(@RequestHeader String source,
                                                                    @RequestBody PackageComponentRequestV1 pkgComponentRequest,
                                                                    BindingResult result, HttpServletRequest servletRequest){
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, pkgComponentRequest, result, servletRequest , null);
        return componentService.getAvailablePackageComponentsV1(pkgComponentRequest);
    }

    @PostMapping("/availability/package2Components")
    public List<PackageComponentResponse> getPackage2Components(@RequestHeader String source,
                                                               @RequestBody PackageComponentRequest pkgComponentRequest,
                                                               BindingResult result, HttpServletRequest servletRequest){
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, pkgComponentRequest, result, servletRequest , null);
        return componentService.getAvailablePackageComponents(pkgComponentRequest);
    }
}
