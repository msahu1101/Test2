package com.mgm.services.booking.room.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerAccountDetails {
	private String firstName;
	private String lastName;
	private List<PartnerAccounts> partnerAccounts;
}
