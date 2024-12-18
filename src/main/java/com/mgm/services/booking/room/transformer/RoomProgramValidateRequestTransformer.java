package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.BasePriceRequest;
import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.request.ResortPriceRequest;
import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Transformer class to create RoomProgramValidateRequestTransformer from
 * different source objects.
 * 
 */
@UtilityClass
public class RoomProgramValidateRequestTransformer {

    /**
     * Creates RoomProgramValidateRequest from RoomAvailabilityRequest object.
     * 
     * @param request
     *            RoomAvailabilityRequest object
     * @return Returns RoomProgramValidateRequest from RoomAvailabilityRequest
     *         object.
     */
    public static RoomProgramValidateRequest getRoomProgramValidateRequest(BasePriceRequest request) {
        final RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setSource(request.getSource());
        validateRequest.setCustomerId(request.getCustomerId());
        validateRequest.setPropertyId(request.getPropertyId());
        validateRequest.setProgramId(request.getProgramId());
        validateRequest.setPromo(request.getPromo());
        validateRequest.setPromoCode(request.getPromoCode());
        validateRequest.setRedemptionCode(request.getRedemptionCode());
        validateRequest.setMlifeNumber(request.getMlifeNumber());
        validateRequest.setModifyFlow(request.isModifyFlow());
        return validateRequest;
    }

    /**
     * Creates RoomProgramValidateRequest from ResortPriceRequest object.
     * 
     * @param request
     *            ResortPriceRequest object
     * @param program
     *            RoomProgram object
     * @return Returns RoomProgramValidateRequest from ResortPriceRequest
     *         object.
     */
    public static RoomProgramValidateRequest getRoomProgramValidateRequest(ResortPriceRequest request,
            RoomProgram program) {
        final RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setSource(request.getSource());
        validateRequest.setCustomerId(request.getCustomerId());
        validateRequest.setPropertyId(program.getPropertyId());
        validateRequest.setProgramId(program.getId());
        return validateRequest;
    }

    /**
     * Creates RoomProgramValidateRequest from ResortPriceRequest object.
     * 
     * @param request
     *            ResortPriceRequest object
     * @param program
     *            RoomProgram object
     * @return Returns RoomProgramValidateRequest from ResortPriceRequest
     *         object.
     */
    public static RoomProgramValidateRequest getRoomProgramValidateRequest(ResortPriceV2Request request,
            RoomProgram program) {
        final RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setSource(request.getSource());
        validateRequest.setCustomerId(request.getCustomerId());
        validateRequest.setPropertyId(program.getPropertyId());
        validateRequest.setProgramId(program.getId());
        validateRequest.setRedemptionCode(request.getRedemptionCode());
        return validateRequest;
    }

    /**
     * Creates RoomProgramValidateRequest from CalculateRoomChargesRequest object.
     * 
     * @param request
     *            CalculateRoomChargesRequest object
     * @return Returns RoomProgramValidateRequest from CalculateRoomChargesRequest
     *         object.
     */
    public static RoomProgramValidateRequest getRoomProgramValidateRequest(CalculateRoomChargesRequest request) {
        final RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setSource(request.getSource());
        validateRequest.setCustomerId(request.getCustomerId());
        validateRequest.setPropertyId(request.getPropertyId());
        validateRequest.setProgramId(request.getProgramId());
        validateRequest.setPromo(request.getPromo());
        validateRequest.setRedemptionCode(request.getRedemptionCode());
        return validateRequest;
    }

    /**
     * Creates RoomProgramValidateRequest from RoomReservation object.
     *
     * @param request
     *            RoomReservation object
     * @return Returns RoomProgramValidateRequest from RoomReservation
     *         object.
     */
    public static RoomProgramValidateRequest getRoomProgramValidateRequest(RoomReservation request, String programId) {
        final RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        if (null != request) {
            validateRequest.setSource(request.getSource());
            validateRequest.setCustomerId(request.getCustomerId());
            validateRequest.setPropertyId(request.getPropertyId());
            if (StringUtils.isEmpty(programId)) {
                validateRequest.setProgramId(request.getProgramId());
            } else {
                validateRequest.setProgramId(programId);
            }
        }
        return validateRequest;
    }

}
