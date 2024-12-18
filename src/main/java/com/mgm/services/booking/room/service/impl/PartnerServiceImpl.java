package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.dao.PartnerAccountDAO;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.booking.room.model.response.PartnerConfigResponse;
import com.mgm.services.booking.room.service.PartnerService;
import com.mgm.services.booking.room.util.PartnerProgramConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@Primary
public class PartnerServiceImpl implements PartnerService {
    @Autowired
    private PartnerProgramConfig partnerProgramConfig;
    
    @Autowired
	private PartnerAccountDAO partnerAccountDao;

    @Override
    public PartnerConfigResponse getPartnerConfig() {
        PartnerConfigResponse response = new PartnerConfigResponse();
        response.setPartnerProgramValues(partnerProgramConfig.getPartnerProgramValues());
        return response ;
    }
	
	@Override
	public PartnerAccountsSearchV2Response searchPartnerAccount(PartnerAccountV2Request partnerAccountRequest) {
		return partnerAccountDao.searchPartnerAccount(partnerAccountRequest);
	}
}
