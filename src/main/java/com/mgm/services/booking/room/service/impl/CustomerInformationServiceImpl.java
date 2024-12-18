package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.dao.CVSDao;
import com.mgm.services.booking.room.dao.ProfileServiceDAO;
import com.mgm.services.booking.room.model.request.EcidbyEmrRequest;
import com.mgm.services.booking.room.model.response.CVSResponse;
import com.mgm.services.booking.room.model.response.EcidByEmrResponse;
import com.mgm.services.booking.room.model.response.ProfileServiceResponse;
import com.mgm.services.booking.room.service.CustomerInformationService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.UserInfoDecryptionService;
import com.mgm.services.booking.room.validator.TokenValidator;

import jline.internal.Log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Primary
public class CustomerInformationServiceImpl implements CustomerInformationService {
	@Autowired
    private ProfileServiceDAO profileServiceDAO;
	@Autowired
    private CVSDao cvsDao;
    @Autowired
    private UserInfoDecryptionService userInfoService;

    @Override
    public EcidByEmrResponse getEcidByEmr(EcidbyEmrRequest emr) {
    	EcidByEmrResponse response = new EcidByEmrResponse();
    	try {
    		List<String> perpetualEligiblePropertyCodes = new ArrayList<>();
    		String mlifeNumber = userInfoService.decrypt(emr.getEmr());
    		Integer.parseInt(mlifeNumber);
    		ProfileServiceResponse profileServiceResponse = profileServiceDAO.getCustomerIdByMlifeId(mlifeNumber);
    		CVSResponse cvsResponse = cvsDao.getCustomerValues(mlifeNumber);
    		if (cvsResponse !=null) {
    			Arrays.stream(cvsResponse.getCustomer().getCustomerValues()).forEach(property -> {
    				String propertyId = property.getProperty();

    				if (propertyId != null) {
    					perpetualEligiblePropertyCodes.add(propertyId);
    				}
    			});
    			response.setPerpetualEligiblePropertyCodes(perpetualEligiblePropertyCodes.toString().substring(1, perpetualEligiblePropertyCodes.toString().length() - 1).replace(", ", ","));
    		}
    		if(profileServiceResponse != null && profileServiceResponse.getCustomer() != null) {
    			response.setEcid(userInfoService.encrypt(profileServiceResponse.getCustomer().getId()));

    		}
    	} catch (Exception ex) {
    		Log.error("Error occured while trying to retrieve ecid by emr: {}", ex.getMessage());
    	}
    	response.setEmr(emr.getEmr());

    	return response;

    }
}
