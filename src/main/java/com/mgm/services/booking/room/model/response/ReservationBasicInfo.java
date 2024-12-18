package com.mgm.services.booking.room.model.response;

import lombok.Data;

public @Data class ReservationBasicInfo {
    private String operaConfNo;
    private String confNo;
    private String externalConfNo;
    private String partyConfNo;
    private String operaPartyCode;
    private String guestName;
    private String guestFName;
    private String guestLname;
    private String roomTypeCode;
    private String roomCount;
    private int adultCount;
    private int childCount;
    private String checkInDate;
    private String checkOutDate;
    private String resvNameId;
    private String[] reservationTypes;
    private String primarySharerConfNo;
    private String status;
    private String resType;
}
