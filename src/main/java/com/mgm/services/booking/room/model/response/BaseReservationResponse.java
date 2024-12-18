package com.mgm.services.booking.room.model.response;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.Message;
import com.mgm.services.booking.room.model.PaymentBasic;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.TripDetail;
import com.mgm.services.booking.room.model.reservation.ReservationState;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Base Reservation response class
 * @author nitpande0
 *
 */
public @Data class BaseReservationResponse {

    private String itemId;
    private String propertyId;
    private TripDetail tripDetails;
    @JsonInclude(Include.NON_EMPTY)
    private String confirmationNumber;
    @JsonInclude(Include.NON_EMPTY)
    private Date bookDate;
    @JsonInclude(Include.NON_EMPTY)
    private ReservationState state;
    private RatesSummary rates;
    @JsonInclude(Include.NON_EMPTY)
    private PaymentBasic payment;
    @JsonInclude(Include.NON_EMPTY)
    @JsonFormat(
            pattern = ServiceConstant.DATE_FORMAT_WITH_TIME)
    private Date freeCancellationEndDate;
    @Getter(onMethod = @__(@JsonIgnore))
    @Setter
    private List<Message> messages;
}
