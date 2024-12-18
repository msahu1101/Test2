package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.EcidbyEmrRequest;
import com.mgm.services.booking.room.model.response.EcidByEmrResponse;

public interface CustomerInformationService {
    /**
     * Confirm room reservation with source system and return updated room
     * reservation object.
     *
     * @param emr EcidbyEmrRequest object
     * @return Updated room reservation object with confirmation number
     */
    EcidByEmrResponse getEcidByEmr(EcidbyEmrRequest emr);
}
