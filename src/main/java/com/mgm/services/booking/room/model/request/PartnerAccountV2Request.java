package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.NotEmpty;

import com.mgm.services.common.model.BaseRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class PartnerAccountV2Request extends BaseRequest{

	private String firstName;
	
	private String lastName;
	
	private String emailAddress;
	
	private String partnerAccountNo;
	
	@NotEmpty(message = "_invalid_program_code_")
	private String programCode;
}
