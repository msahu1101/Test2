package com.mgm.services.booking.room.model.request;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;

import lombok.Data;

public @Data class UserProfileRequest {

	private long id;
	private int mlifeNo;
	private String title;

	@JsonFormat(
			shape = JsonFormat.Shape.STRING,
			pattern = "yyyy-MM-dd")
	private Date dateOfBirth;
	private String operaId;
	private String tier;
	@JsonFormat(
			shape = JsonFormat.Shape.STRING,
			pattern = "yyyy-MM-dd")
	private Date dateOfEnrollment;
	private String firstName;
	private String lastName;
	private String emailAddress1;
	private String emailAddress2;
	private String hgpNo;
	private String swrrNo;
	private List<ProfilePhone> phoneNumbers;
	private List<ProfileAddress> addresses;
	private String mgmId;
	private List<PartnerAccounts> partnerAccounts;
}
