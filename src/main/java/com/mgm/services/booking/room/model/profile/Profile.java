package com.mgm.services.booking.room.model.profile;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import lombok.Data;

@JsonInclude(Include.NON_EMPTY)
public @Data class Profile implements Serializable {

    private static final long serialVersionUID = 7896781435879051618L;
    
    private String uid;
    private String login;
    private String email;
    private String secondEmail;
    private String title;
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private String mlifeNumber;
    private String secretQuestionAnswerHash;
    private String secretQuestionId;
    private String oldPasswordHash;
    private List<ProfilePhone> phoneNumbers;
    private List<ProfileAddress> addresses;
    private List<PartnerAccounts> partnerAccounts;
}
