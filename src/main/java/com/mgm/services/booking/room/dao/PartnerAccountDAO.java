package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;

public interface PartnerAccountDAO {

    /**
     * @param partnerAccountRequest
     * @return
     */
    PartnerAccountsSearchV2Response searchPartnerAccount(PartnerAccountV2Request partnerAccountRequest);
}
