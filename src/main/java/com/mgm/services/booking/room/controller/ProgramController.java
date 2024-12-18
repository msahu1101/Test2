/**
 * 
 */
package com.mgm.services.booking.room.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.RoomProgram;
import com.mgm.services.booking.room.model.response.RoomProgramSegmentResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to handle offers related endpoints.
 *
 */
@RestController
@RequestMapping("/v1")
@Log4j2
public class ProgramController extends ExtendedBaseController {

    @Autowired
    private RoomProgramService roomProgramService;

    /**
     * Returns a list of room offers for a transient/logged-in user.
     * 
     * @param source
     *            Source header
     * @param offersRequest
     *            Room program request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns a list of room offers for a transient/logged-in user.
     */
    @GetMapping("/offers/room")
    public List<RoomProgram> getRoomOffers(@RequestHeader String source, @Valid RoomProgramRequest offersRequest,
            BindingResult result, HttpServletRequest servletRequest, @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        preprocess(source, offersRequest, result, servletRequest, enableJwb);
        
        // temp fix until gen 3 client fixes it
        if (offersRequest.getPropertyIds().contains("mgmresorts")) {
            offersRequest.getPropertyIds().remove("mgmresorts");
        }
        
        return roomProgramService.getRoomOffers(offersRequest);
    }

    /**
     * Validate a room program ID or promo code to confirm if the program
     * Id/promoCode is available and eligible to be used for the user.
     * Eligibility check is done to avoid program misuse.
     * 
     * @param source
     *            Source header
     * @param validateRequest
     *            Room program validate request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns validate program response
     */
    @GetMapping("/room/offer/validate")
    public RoomProgramValidateResponse validateRoomOffer(@RequestHeader String source,
            @Valid RoomProgramValidateRequest validateRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        preprocess(source, validateRequest, result, servletRequest, enableJwb);

        RoomProgramValidateResponse response = roomProgramService.validateProgram(validateRequest);

        // if the program is myvegas, ensure there's an associated redemption
        // code. Otherwise, mark it in-eligible
        if (response.isMyvegas()) {
            response.setEligible(CommonUtil.isEligibleForMyVegasRedemption(sSession.getMyVegasRedemptionItems(),
                    response.getProgramId(), sSession.getCustomer()));
        }
        return response;
    }

    /**
     * Returns a list of default perpetual room offers for a logged-in user for
     * every available property. Customer ID or Mlife Number is mandatory for
     * this service.
     * 
     * @param source
     *            Source header
     * @param programsRequest
     *            Perpetual programs request
     * @param headers
     *            Request headers
     * @param result
     *            Binding result
     * @return Returns a list of default perpetual room offers.
     */
    @GetMapping("/offers/room/default-perpetual")
    public List<PerpetaulProgram> getDefaultPerpetualPrograms(@RequestHeader String source,
            @Valid PerpetualProgramRequest programsRequest, @RequestHeader Map<String, String> headers, BindingResult result) {

        preprocess(source, programsRequest, result);

        if (programsRequest.getCustomerId() == -1) {
            log.info("Headers from request: {}", headers);
            log.error("Default Perpetual Offer Endpoint: session doesn't have user info: {}", sSession);
            
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_CUSTOMER.getErrorCode()));
        }

        return roomProgramService.getDefaultPerpetualPrograms(programsRequest);
    }

    /**
     * Get the segment id for the given program id
     * 
     * @param programId
     *            the program id
     * @return the segment id
     */
    @GetMapping("/offers/room/segment/{programId}")
    public RoomProgramSegmentResponse getProgramSegment(@PathVariable String programId) {

        if (StringUtils.isBlank(programId) || !CommonUtil.isUUID(programId)) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_PROGRAM_ID.getErrorCode()));
        }

        return roomProgramService.getProgramSegment(programId);

    }
}
