package com.mgm.services.booking.room.model.loyalty;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.Data;

import java.util.Date;

@Data
public class CustomerPromotion {

    private String promoId;
    private String name;
    private String status;
    private String publicDescription;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date endDate;
    private String propertyId;
    private SiteInfo siteInfo;
}
