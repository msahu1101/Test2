package com.mgm.services.booking.room.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBIntrospector;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.JmsUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.mgm.services.booking.room.dao.MyVegasDAO;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.RedemptionValidationResponse;
import com.mgm.services.common.model.RedemptionValidationResponse.DatesRedemptionIsUnAvailable;
import com.mgmresorts.myvegas.jaxb.RedemptionConfirmationNotificationRequest;
import com.mgmresorts.myvegas.jaxb.RedemptionValidationRequest;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for validating and confirming the redemption code
 *
 */
@Component("JMSMyVegasDAOImpl")
@Primary
@Log4j2
public class JMSMyVegasDAOImpl implements MyVegasDAO {

    //@Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ApplicationProperties appProps;
    
    /** The redemption confirmation destination. */
    @Autowired
    private Destination redemptionConfirmationDestination;

    /** The redemption validation request destination. */
    @Autowired
    private Destination redemptionValidationRequestDestination;

    /** The redemption validation response destination. */
    @Autowired
    private Destination redemptionValidationResponseDestination;
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.MyVegasDAO#validateRedemptionCode(com.
     * mgm.services.booking.room.model.request.MyVegasRequest)
     */
    @Override
    public RedemptionValidationResponse validateRedemptionCode(MyVegasRequest myVegasRequest) {

        RedemptionValidationRequest request = new RedemptionValidationRequest();
        request.setRedemptionCode(myVegasRequest.getRedemptionCode().trim());

        Object object = sendAndReceive(jmsTemplate, redemptionValidationRequestDestination,
                redemptionValidationResponseDestination, request);

        com.mgmresorts.myvegas.jaxb.RedemptionValidationResponse jaxbResponse = (com.mgmresorts.myvegas.jaxb.RedemptionValidationResponse) JAXBIntrospector
                .getValue(object);

        if (null == jaxbResponse) {
            log.info("The redemption code {} could not be validated", myVegasRequest.getRedemptionCode());
            throw new BusinessException(ErrorCode.MYVEGAS_SYSTEM_ERROR);
        }

        if (jaxbResponse.getError() != null && StringUtils.isNotBlank(jaxbResponse.getError().getCode())) {
            handleMyVegasError(jaxbResponse.getError().getCode());
        } else {
            RedemptionValidationResponse response = transformResponse(jaxbResponse);

            log.info(appProps.getTestRedemptionCodes());
            log.info(request.getRedemptionCode());
            // This is just for dev to enable integration testing. Test
            // redemption codes will be set only for dev
            if (CollectionUtils.isEmpty(appProps.getTestRedemptionCodes())
                    || !appProps.getTestRedemptionCodes().contains(request.getRedemptionCode())) {
                handleStatusBasedError(response);
            }

            return response;
        }
        log.info("The redemption code {} could not be validated", myVegasRequest.getRedemptionCode());
        throw new BusinessException(ErrorCode.REDEMPTION_OFFER_NOT_AVAILABLE);
    }
    
    private RedemptionValidationResponse transformResponse(
            com.mgmresorts.myvegas.jaxb.RedemptionValidationResponse jaxbResponse) {
        RedemptionValidationResponse response = new RedemptionValidationResponse();
        BeanUtils.copyProperties(jaxbResponse, response);
        response.getCustomer().setFirstName(jaxbResponse.getCustomer().getFirstName());
        response.getCustomer().setLastName(jaxbResponse.getCustomer().getLastName());
        response.getCustomer().setEmailID(jaxbResponse.getCustomer().getEmailID());
        response.getCustomer().setMembershipID(jaxbResponse.getCustomer().getMembershipID());
        copyUnavailableDates(jaxbResponse, response);
        return response;
    }
    
    private void copyUnavailableDates(com.mgmresorts.myvegas.jaxb.RedemptionValidationResponse jaxbResponse,
            RedemptionValidationResponse response) {
        List<DatesRedemptionIsUnAvailable> datesRedemptionIsUnAvailable = new ArrayList<>();
        jaxbResponse.getDatesRedemptionIsUnAvailable().stream().forEach(date -> {
            DatesRedemptionIsUnAvailable unavailableDate = new DatesRedemptionIsUnAvailable();
            unavailableDate.setBeginDate(date.getBeginDate());
            unavailableDate.setEndDate(date.getEndDate());
            datesRedemptionIsUnAvailable.add(unavailableDate);
        });
        response.setDatesRedemptionIsUnAvailable(datesRedemptionIsUnAvailable);
    }
    
    /**
     * Converts the request object into text message and initiates synchronous
     * request/response process without separate response queue.
     * 
     * @param jmsTemplate
     *            JMS Template
     * @param requestDest
     *            Request destination
     * @param element
     *            Object to be sent
     * @return Returns parsed response object
     */
    private Object sendAndReceive(final JmsTemplate jmsTemplate, final Destination requestDest, final Object element) {
        return sendAndReceive(jmsTemplate, requestDest, null, element);
    }

    /**
     * Converts the request object into text message and initiates synchronous
     * request/response process.
     * 
     * @param jmsTemplate
     *            JMS Template
     * @param requestDest
     *            Request destination
     * @param responseDest
     *            Response destination
     * @param element
     *            Object to be sent
     * @return Returns parsed response object
     */
    private Object sendAndReceive(final JmsTemplate jmsTemplate, final Destination requestDest,
            final Destination responseDest, final Object element) {
        Object object = null;
        try {
            MessageCreator msgCrt = new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    TextMessage request = (TextMessage) jmsTemplate.getMessageConverter().toMessage(element, session);
                    log.info("Request sent to {}: {}", requestDest, request.getText());
                    return request;
                }
            };
            TextMessage response = (TextMessage) sendAndReceiveLocal(jmsTemplate, requestDest, responseDest, msgCrt);
            if (response != null) {
                log.info("Response message from destination {}: {}", requestDest,
                        response.getText().replaceAll("[\\n\\t ]", ""));
                object = jmsTemplate.getMessageConverter().fromMessage(response);
            }
        } catch (Exception e) {
            log.error("Exception: " + e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
        return object;
    }

    /**
     * Creates message producer and consumers for synchronous operations. Not
     * using jmsTemplate.send and jmsTemplate.receive as there's no safe way to
     * invoke receive before responses could be received. Moved this fully
     * custom implementation since using send and receive from jmsTemplate was
     * causing intermittent failures.
     * 
     * @param jmsTemplate
     *            JMS Template
     * @param destination
     *            Request destination
     * @param responseQueue
     *            Response destination
     * @param msgCrt
     *            Message creator
     * @return Message from response destination
     */
    private Message sendAndReceiveLocal(JmsTemplate jmsTemplate, Destination destination, Destination responseQueue,
            MessageCreator msgCrt) {
        Connection con = null;
        Session session = null;
        try {
            con = jmsTemplate.getConnectionFactory().createConnection();
            session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            con.start();
            MessageProducer producer = null;
            MessageConsumer consumer = null;
            try {
                Message requestMessage = msgCrt.createMessage(session);
                if (null == responseQueue) {
                    responseQueue = session.createTemporaryQueue();
                }
                producer = session.createProducer(destination);
                String jmsCorrelationID = UUID.randomUUID().toString();
                consumer = session.createConsumer(responseQueue, "JMSCorrelationID='" + jmsCorrelationID + "'");
                requestMessage.setJMSReplyTo(responseQueue);
                requestMessage.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
                requestMessage.setJMSCorrelationID(jmsCorrelationID);
                producer.send(requestMessage);
                return consumer.receive(jmsTemplate.getReceiveTimeout());
            } finally {
                JmsUtils.closeMessageConsumer(consumer);
                JmsUtils.closeMessageProducer(producer);
            }
        } catch (JMSException ex) {
            throw JmsUtils.convertJmsAccessException(ex);
        } finally {
            JmsUtils.closeSession(session);
            ConnectionFactoryUtils.releaseConnection(con, jmsTemplate.getConnectionFactory(), true);
        }
    }

    private void handleMyVegasError(String errorCode) {

        if (errorCode.endsWith("001")) {
            throw new BusinessException(ErrorCode.MYVEGAS_UNKNOWN_REDEMPTION_CODE);
        } else if (errorCode.endsWith("002")) {
            throw new BusinessException(ErrorCode.MYVEGAS_SYSTEM_ERROR_VIOLATIONS);
        } else if (errorCode.endsWith("003")) {
            throw new BusinessException(ErrorCode.MYVEGAS_REQUIRED_FIELDS_MISSING);
        } else if (errorCode.endsWith("004")) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_TYPE);
        } else if (errorCode.endsWith("005")) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_REDEEMED);
        } else if (errorCode.endsWith("017")) {
            throw new BusinessException(ErrorCode.MYVEGAS_INVALID_REDEMPTION_DATE);
        } else if (errorCode.endsWith("018")) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_EXPIRED);
        } else {
            throw new BusinessException(ErrorCode.MYVEGAS_SYSTEM_ERROR);
        }
    }

    private void handleStatusBasedError(RedemptionValidationResponse response) {

        log.info("Response from MyVegas: status {} and rewardType {}", response.getStatus(), response.getRewardType());

        if (!response.getRewardType().contains("Room") && !response.getRewardType().contains("Show")) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_TYPE);
        } else if ("Redeemed".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_REDEEMED);
        } else if ("Canceled".equals(response.getStatus()) || "Player_Cancel".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_CANCELLED);
        } else if ("Expired".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_EXPIRED);
        } else if ("my_vegas_exception".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_SYSTEM_ERROR);
        } else if ("rewardtype_not_avail".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_UNKNOWN_REDEMPTION_CODE);
        } else if ("Refund_Req".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_REFUND);
        } else if ("Refunded".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_REFUNDED);
        } else if ("Gifted".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_GIFTED);
        } else if ("Email_Conf_Pending".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_EMAIL_CONF);
        } else if ("On_Hold".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_HOLD);
        } else if ("Gift_On_Hold".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_GIFTHOLD);
        } else if ("Unclaimed".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.MYVEGAS_REDEMPTION_CODE_UNCLAIM);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.MyVegasDAO#confirmRedemptionCode(com.
     * mgm.services.booking.room.model.request.MyVegasRequest)
     */
    @Override
    public void confirmRedemptionCode(MyVegasRequest myVegasRequest) {

        final RedemptionConfirmationNotificationRequest rcnReq = new RedemptionConfirmationNotificationRequest();
        rcnReq.setRedemptionCode(myVegasRequest.getRedemptionCode());
        rcnReq.setReservationDate(myVegasRequest.getReservationDate());
        rcnReq.setConfirmationCode(myVegasRequest.getConfirmationNumber());
        rcnReq.setRequiredConfirmationCode(myVegasRequest.getConfirmationNumber());
        rcnReq.setCouponCode(myVegasRequest.getCouponCode());
        rcnReq.setCustomer(myVegasRequest.getCustomer());

        // spawning to async thread, so that we don't wait for it..
        CompletableFuture.runAsync(() -> {
            log.info("Sending confirmation message to myvegas for redemption code: {}",
                    myVegasRequest.getRedemptionCode());
            sendAndReceive(jmsTemplate, redemptionConfirmationDestination, rcnReq);

            log.info("Confirmation message to myvegas posted successfully for redemption code: {}",
                    myVegasRequest.getRedemptionCode());
        });

    }
    
}
