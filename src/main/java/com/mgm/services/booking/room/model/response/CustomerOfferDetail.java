package com.mgm.services.booking.room.model.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

/**
 * Offer Detail Object.
 * 
 * @author jayveera
 *
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerOfferDetail {

    private String id;
    private String propertyId;
    private String name;
    private String offerType;
    private String promoId;
    private String status;
    private String description;
    private Integer customerRank;
    private Integer segmentFrom;
    private Integer segmentTo;
    private Boolean defaultPerpetualOffer;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date endDate;

}
