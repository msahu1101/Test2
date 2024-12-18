package com.mgm.services.booking.room.properties;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

/**
 * Configuration class to read properties from application.properties file with
 * "aurora" prefix
 *
 */
@Component
@ConfigurationProperties(
        prefix = "aurora")
public @Data class AuroraProperties {
    private String url;
    private String appName;
    private String appPartitions;
    private String securityEnabled;
    private String publicKey;
    private List<AuroraCredential> channelCredentials;
    private int responseTimeout;
    private List<String> propertyIds;
    private List<String> emailPropertyIds;
    private List<JWBCustomer> sapphireJWBCustomer;
    private List<String> signupEmailPropertyIds;

    private String userKey;
    private String passwordKey;

    @Autowired
    private SecretsProperties secretsProperties;

    @PostConstruct
    private void populateUrlFromSecretStore() {
        url = url.replace("<aurora-user>", secretsProperties.getSecretValue(userKey))
                .replace("<aurora-pwd>", secretsProperties.getSecretValue(passwordKey));
    }

    /**
     * Method to return a random customerId from the list of
     * <i>sapphireJWBCustomer</i> configured in the property file.
     * 
     * @return random customerId as String
     */
    public Long getRandomCustomerId() {
        return getRandomJWBCustomer().getCustId();
    }

    public JWBCustomer getRandomJWBCustomer() {
        return sapphireJWBCustomer.get(ThreadLocalRandom.current().nextInt(0, sapphireJWBCustomer.size()));
    }

    /**
     * Class for aurora credential properties
     */
    public static @Data class AuroraCredential {
        private String key;
        private String name;
        private String code;

    }

    public static @Data class JWBCustomer {
        private Long custId;
        private String mlifeNo;

    }

}
