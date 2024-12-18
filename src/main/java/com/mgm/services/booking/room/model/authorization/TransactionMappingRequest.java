package com.mgm.services.booking.room.model.authorization;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.model.ServicesSession;

import lombok.Data;

public @Data class TransactionMappingRequest {
    private RoomReservation reservation;
    private RoomReservationResponse preresponse;
    private ServicesSession session;
    private String inAuthTransactionId;
    private String transactionId;
    private String propertyId;
    private String roomTypeId;
    private String programTypeId;
    
    

}
