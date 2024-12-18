package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mgm.services.booking.room.model.PartnerAccountDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerAccountsSearchV2Response {
	private PartnerAccountDetails partnerAccountDetails;
}
