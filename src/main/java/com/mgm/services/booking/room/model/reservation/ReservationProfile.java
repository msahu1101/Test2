package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;

import lombok.Data;

public @Data class ReservationProfile implements Serializable {

    private static final long serialVersionUID = 6259401509405053407L;
    
    private long id;
    private int mlifeNo;
    private String mgmId;
    private String title;

    @JsonFormat( shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date dateOfBirth;
    private boolean modifiable;
    private long createdAt;
    private long updatedAt;
    private long refreshedAt;
    private boolean stale;
    private long externalSyncAt;
    private boolean caslOptin;
    private String operaId;
    private String lmsId;
    private String tier;
    private String firstName;
    private String lastName;
    private String emailAddress1;
    private String emailAddress2;
    private String hgpNo;
    private String swrrNo;
    private List<ProfilePhone> phoneNumbers;
    private List<ProfileAddress> addresses;
    private List<PartnerAccounts> partnerAccounts;
}
