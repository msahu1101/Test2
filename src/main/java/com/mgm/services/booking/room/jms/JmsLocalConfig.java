package com.mgm.services.booking.room.jms;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.NamingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MarshallingMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.JMSProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.tibco.tibjms.TibjmsTopic;

import lombok.extern.log4j.Log4j2;

/**
 * Spring boot configuration class for JMS settings required for MyVegas flows.
 *
 */
@Configuration
@Log4j2
public class JmsLocalConfig {

    @Autowired
    private JMSProperties tbmProps;

    /** The redemption confirmation destination. */
    @Value("${myvegas.topic.redemption.confirmation.destination}")
    private String redemptionConfirmationDestination;

    /** The redemption validation request destination. */
    @Value("${myvegas.topic.redemption.validation.request.destination}")
    private String redemptionValidationRequestDestination;

    /** The redemption validation response destination. */
    @Value("${myvegas.topic.redemption.validation.response.destination}")
    private String redemptionValidationResponseDestination;

    @Autowired
    private SecretsProperties secretsProperties;

    /**
     * Return the connection factory using JNDI settings
     * 
     * @return the connection factory
     */
    //@Bean
    /*public ConnectionFactory jmsConnectionFactory() {
        try {
            log.debug("Retrieving JMS queue with JNDI name: " + tbmProps.getJndiName());
            JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
            jndiObjectFactoryBean.setJndiName(tbmProps.getJndiName());
            jndiObjectFactoryBean.setJndiEnvironment(getEnvProperties());
            jndiObjectFactoryBean.afterPropertiesSet();
            return (ConnectionFactory) jndiObjectFactoryBean.getObject();
        } catch (NamingException ex) {
            log.debug("Error while retrieving JMS queue with JNDI name: [" + tbmProps.getJndiName() + "]");
            throw new SystemException(ErrorCode.SYSTEM_ERROR, ex);
        }
    }*/

    private Properties getEnvProperties() {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, tbmProps.getInitialContextFactory());
        env.put(Context.PROVIDER_URL, tbmProps.getServerUrl());
        env.put(Context.SECURITY_PRINCIPAL, secretsProperties.getSecretValue(ServiceConstant.SERVER_USERNAME));
        env.put(Context.SECURITY_CREDENTIALS, secretsProperties.getSecretValue(ServiceConstant.SERVER_USERPWD));
        return env;
    }

    /**
     * Returns the user credentials connection factory
     * 
     * @return the factory object
     */
    /*@Bean
    public UserCredentialsConnectionFactoryAdapter userCredentialsConnectionFactoryAdapter() {
        UserCredentialsConnectionFactoryAdapter userCredentialsConnectionFactoryAdapter = new UserCredentialsConnectionFactoryAdapter();
        userCredentialsConnectionFactoryAdapter.setUsername(secretsProperties.getSecretValue(ServiceConstant.JMS_USERNAME));
        userCredentialsConnectionFactoryAdapter.setPassword(secretsProperties.getSecretValue(ServiceConstant.JMS_USERPWD));
        userCredentialsConnectionFactoryAdapter.setTargetConnectionFactory(jmsConnectionFactory());
        return userCredentialsConnectionFactoryAdapter;
    }*/

    /**
     * Returns the JMS template
     * 
     * @return the jms template
     */
    /*@Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(userCredentialsConnectionFactoryAdapter());
        jmsTemplate.setReceiveTimeout(tbmProps.getTopicReadTimeOut());
        jmsTemplate.setMessageConverter(jacksonJmsMessageConverter());
        return jmsTemplate;
    }*/

    /**
     * Returns the confirmation topic
     * 
     * @return the confirmation topic
     */
    @Bean
    public Destination redemptionConfirmationDestination() {
        return new TibjmsTopic(redemptionConfirmationDestination);
    }

    /**
     * Returns the validation request topic
     * 
     * @return the topic
     */
    @Bean
    public Destination redemptionValidationRequestDestination() {
        return new TibjmsTopic(redemptionValidationRequestDestination);
    }

    /**
     * Returns the validation response topic
     * 
     * @return the topic
     */
    @Bean
    public Destination redemptionValidationResponseDestination() {
        return new TibjmsTopic(redemptionValidationResponseDestination);
    }

    /**
     * Returns the message convertor
     * 
     * @return the message convertor
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.mgmresorts.myvegas.jaxb");
        MarshallingMessageConverter converter = new MarshallingMessageConverter(marshaller, marshaller);
        converter.setTargetType(MessageType.TEXT);
        return converter;
    }

}
