package com.mgm.services.booking.room.model.content;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public @Data class Property {

    private String id;
    private String name;
    private String reservationPhoneNumber;
    private String generalPhoneNumber;
    private String region;
    private String corporateSortOrder;

    @JsonProperty("phoneNumber")
    private void unpackNameFromNestedObject(Map<String, String> phoneNumber) {
        reservationPhoneNumber = phoneNumber.get("reservationsNumber");
        generalPhoneNumber = phoneNumber.get("generalNumber");
    }
}
