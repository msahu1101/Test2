package com.mgm.services.booking.room.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.model.request.EditReservationRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.properties.ApplicationProperties;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create and return PreModifyRequestTransformer from
 * PreModifyRequest object.
 *
 */
@UtilityClass
public class EditReservationRequestTransformer {

    /**
     * Create and return EditReservationRequest from PreModifyRequest object.
     * 
     * @param preModifyRequest PreModifyRequest object
     * @return Returns EditReservationRequest object
     */
    public static EditReservationRequest getEditReservationRequest(PreModifyRequest preModifyRequest) {
        final EditReservationRequest request = new EditReservationRequest();
        request.setConfirmationNumber(preModifyRequest.getConfirmationNumber());
        request.setCheckInDate(preModifyRequest.getTripDetails().getCheckInDate());
        request.setCheckOutDate(preModifyRequest.getTripDetails().getCheckOutDate());

        if (preModifyRequest.getRoomRequests().isEmpty()) {
            request.setRemoveAllComponents(true);
            request.setComponentIds(new String[] { "" });
        } else {
            List<String> componentIds = new ArrayList<>();
            preModifyRequest.getRoomRequests().forEach(roomRequest -> {
                if (roomRequest.isSelected()) {
                    componentIds.add(roomRequest.getId());
                }
            });
            request.setComponentIds(componentIds.toArray(new String[componentIds.size()]));
        }
        return request;
    }

    /**
     * Create and return EditReservationRequest from PreModifyRequest object.
     * 
     * @param preModifyRequest
     *            Preview request
     * @param appProps
     *            Application properties
     * @return Edit reservation request
     */
    public static EditReservationRequest getEditReservationRequest(PreModifyV2Request preModifyRequest,
            ApplicationProperties appProps) {
        final EditReservationRequest request = new EditReservationRequest();
        request.setConfirmationNumber(preModifyRequest.getConfirmationNumber());
        request.setCheckInDate(preModifyRequest.getTripDetails()
                .getCheckInDate());
        request.setCheckOutDate(preModifyRequest.getTripDetails()
                .getCheckOutDate());

        // Set room requests related to borgata tax elements
        if (preModifyRequest.getPropertyId()
                .equals(appProps.getBorgataPropertyId())) {

            List<String> roomRequests = preModifyRequest.getRoomRequests();
            roomRequests.addAll(appProps.getBorgataSpecialRequests());

            // remove possible duplicates
            preModifyRequest.setRoomRequests(roomRequests.stream()
                    .distinct()
                    .collect(Collectors.toList()));

        }

        if (preModifyRequest.getRoomRequests()
                .isEmpty()) {
            request.setRemoveAllComponents(true);
            request.setComponentIds(new String[] { "" });
        } else {
            request.setComponentIds(preModifyRequest.getRoomRequests()
                    .toArray(new String[preModifyRequest.getRoomRequests()
                            .size()]));
        }
        return request;
    }

}
