package com.mgm.services.booking.room.transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.PartnerAccountDetails;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforequest.PartnerCustomerBasicInfoRequest;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforesponse.PartnerCustomerBasicInfoResponse;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforesponse.PartnerCustomerBasicInfoResponseCustomerInfo;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequest;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteria;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaAddressCriteriaPostalCode;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaCustomerTypeCriteria;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteria;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaAddress;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaEmailAddresses;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteria;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteriaNames;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequestProfileSearchCriteriaProfileStatusCriteria;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.microsoft.azure.management.trafficmanager.ProfileStatus;
import org.apache.commons.lang3.StringUtils;

public class PartnerAccountSearchTransformer {
	
	private PartnerAccountSearchTransformer() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	public static PartnerCustomerSearchRequest composeProfileSearchRequest(PartnerAccountV2Request partnerAccountRequest) {
		final PartnerCustomerSearchRequest req = new PartnerCustomerSearchRequest();
		
		PartnerCustomerSearchRequestProfileSearchCriteria profileSearchCriteria = new PartnerCustomerSearchRequestProfileSearchCriteria();
		
		PartnerCustomerSearchRequestProfileSearchCriteriaCustomerTypeCriteria customerTypeCriteria = new PartnerCustomerSearchRequestProfileSearchCriteriaCustomerTypeCriteria();
		List<String> customerTypes = Arrays.asList("NON-LOYALTY","LOYALTY");
		customerTypeCriteria.setCustomerTypes(customerTypes);
		profileSearchCriteria.setCustomerTypeCriteria(customerTypeCriteria);
		
		PartnerCustomerSearchRequestProfileSearchCriteriaProfileStatusCriteria profileStatusCriteria = new PartnerCustomerSearchRequestProfileSearchCriteriaProfileStatusCriteria();
		List<String> profileStatusTypes = Arrays.asList("ACTIVE");
		profileStatusCriteria.setProfileStatusTypes(profileStatusTypes);
		List<String> profileStatusCodes = Arrays.asList("U","B");
		profileStatusCriteria.setProfileStatusCodes(profileStatusCodes);
		profileSearchCriteria.setProfileStatusCriteria(profileStatusCriteria);
		
		PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteria emailCriteria = new PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteria();
		List<PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaEmailAddresses> emailAddresses = new ArrayList<>();
		PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaEmailAddresses emailAddress = new PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaEmailAddresses();
		PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaAddress address = new PartnerCustomerSearchRequestProfileSearchCriteriaEmailCriteriaAddress();
		address.setValue(partnerAccountRequest.getEmailAddress());
		emailAddress.setAddress(address);
		emailAddresses.add(emailAddress);
		emailCriteria.setEmailAddresses(emailAddresses);
		emailCriteria.setSearchHistory(true);
		profileSearchCriteria.setEmailCriteria(emailCriteria);

		if(StringUtils.isNotEmpty(partnerAccountRequest.getLastName())) {
			PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteriaNames name = new PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteriaNames();
			PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteria nameCriteria = new PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteria();
			List<PartnerCustomerSearchRequestProfileSearchCriteriaNameCriteriaNames> names = new ArrayList<>();

			PartnerCustomerSearchRequestProfileSearchCriteriaAddressCriteriaPostalCode lastName = new PartnerCustomerSearchRequestProfileSearchCriteriaAddressCriteriaPostalCode();
			lastName.setValue(partnerAccountRequest.getLastName());
			lastName.setSearchType(Arrays.asList("exact-match"));
			name.setLastName(lastName);
			names.add(name);
			nameCriteria.setNames(names);
			profileSearchCriteria.setNameCriteria(nameCriteria);
		}

		req.setLimit(100);
		req.setProfileSearchCriteria(profileSearchCriteria);
		return req;
	}
	
	public static PartnerCustomerBasicInfoRequest composeBasicMemberDataRequest(String partnerAccountNumber, String partnerSearchMGMId) {
		final PartnerCustomerBasicInfoRequest req = new PartnerCustomerBasicInfoRequest();
		req.setId(partnerSearchMGMId);
		req.setValue(partnerAccountNumber);
		return req;
	}
	
	public static PartnerAccountsSearchV2Response composePartnerAccountResponse(PartnerCustomerBasicInfoResponse memberData,
			PartnerCustomerBasicInfoRequest memberReqData, PartnerAccountV2Request baseRequest, Map<String, String> membershipLevel) {
		final PartnerAccountsSearchV2Response response = new PartnerAccountsSearchV2Response();
		
		
		PartnerAccountDetails accountDetails = new PartnerAccountDetails();
		accountDetails.setFirstName(memberData.getCustomerInfo().getName().getGivenName());
		accountDetails.setLastName(memberData.getCustomerInfo().getName().getSurname());
		
		List<PartnerAccounts> partnerAccounts = new ArrayList<>();
		PartnerAccounts account = composeAccountDataResponse(memberData,baseRequest,memberReqData.getValue());
		account.setMembershipLevel(membershipLevel.get(account.getProgramSubcategory()));
		partnerAccounts.add(account);
		accountDetails.setPartnerAccounts(partnerAccounts);
		response.setPartnerAccountDetails(accountDetails);
		
		return response;
	}
	public static PartnerAccounts composeAccountDataResponse(PartnerCustomerBasicInfoResponse memberData,
			PartnerAccountV2Request baseRequest, String accountNumber) {
		PartnerAccounts account = new PartnerAccounts();
		account.setPartnerAccountNo(accountNumber);
		PartnerCustomerBasicInfoResponseCustomerInfo basicInfo = memberData.getCustomerInfo();
		if(null != basicInfo && null != basicInfo.getEliteLevel()) {
			account.setProgramCategory(memberData.getCustomerInfo().getEliteLevel().getId());
			account.setProgramSubcategory(memberData.getCustomerInfo().getEliteLevel().getType());
			account.setProgramDescription(memberData.getCustomerInfo().getEliteLevel().getDescription());
		}else{
			throw new BusinessException(ErrorCode.INVALID_CUSTOMER,"Customer  not having any EliteLevel info from partner service");
		}
		account.setProgramCode(baseRequest.getProgramCode());
		return account;
	}
	public static PartnerAccountDetails mapBasicParnerAccountDetails(
			PartnerCustomerBasicInfoResponse memberDataRes) {
		PartnerAccountDetails basicDetails = new PartnerAccountDetails();
		basicDetails.setFirstName(memberDataRes.getCustomerInfo().getName().getGivenName());
		basicDetails.setLastName(memberDataRes.getCustomerInfo().getName().getSurname());
		return basicDetails;
	}
	
}
