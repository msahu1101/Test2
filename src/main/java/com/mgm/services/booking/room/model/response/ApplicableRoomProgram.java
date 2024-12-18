package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicableRoomProgram {

    private String id;
    private String propertyId;
    private String name;
    private String category;
    private String rateCode;
    private String patronPromoId;
    private String promoCode;
    private String promo;
    private String operaBlockCode;
    private String operaBlockName;
    private String reservationMethod;
    private List<String> tags;

}
