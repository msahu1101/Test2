package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class PhoneNumber {
    private String phoneNumberType;
    private String phoneNumber;
    private String mfPrimaryYN;

}
