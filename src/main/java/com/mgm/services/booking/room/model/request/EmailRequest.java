package com.mgm.services.booking.room.model.request;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Notification Service Email Request
 * 
 * @author vararora
 *
 */
@Builder
public @Data class EmailRequest {

    private List<String> recipientEmailAddressList;
    private String source;
    private String emailSubject;
    private String emailBody;

}
