package com.mgm.services.booking.room.dao.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.EmailDAO;
import com.mgm.services.booking.room.model.Email;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.messages.SendEmailRequest;
import com.mgmresorts.aurora.messages.SendEmailResponse;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for EmailDAO providing sendEmail functionality.
 */
@Component("EmailDAOImpl")
@Primary
@Log4j2
public class EmailDAOImpl extends AuroraBaseDAO implements EmailDAO {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.EmailDAO#sendEmail(com.mgm.services.
     * booking.room.model.Email)
     */
    @Override
    public void sendEmail(Email email) {
        final SendEmailRequest sendEmailRequest = MessageFactory.createSendEmailRequest();

        sendEmailRequest.setFrom(email.getFrom());
        sendEmailRequest.setTo(new String[] { email.getTo() });
        sendEmailRequest.setSubject(email.getSubject());

        if (StringUtils.isNotEmpty(email.getBody())) {
            sendEmailRequest.setBody(email.getBody());
        }
        log.info("Sent the request to sendEmail as : {}", sendEmailRequest.toJsonString());
        final SendEmailResponse response = getDefaultAuroraClient().sendEmail(sendEmailRequest);
        log.info("Received the response from sendEmail as : {}", response.toJsonString());
        
    }

}
