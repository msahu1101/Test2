package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.booking.room.model.response.PartnerConfigResponse;

/**
 * Interface containing all the partner related services
 */
public interface PartnerService {

    /**
     * Returns a list of partner names and codes
     * @return response object containing partner details
     */
    PartnerConfigResponse getPartnerConfig();

	PartnerAccountsSearchV2Response searchPartnerAccount(PartnerAccountV2Request partnerAccountRequest);
}
