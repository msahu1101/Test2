package com.mgm.services.booking.room.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class PartnerAccounts {
    private String partnerAccountNo;
    private String programCategory;
    private String programSubcategory;
    private String programDescription;
    private String membershipLevel;
    private String programCode;
}
