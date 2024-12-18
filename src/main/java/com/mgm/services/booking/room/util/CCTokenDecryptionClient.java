package com.mgm.services.booking.room.util;

import java.util.Base64;

import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretPropertiesAzure;

import lombok.extern.log4j.Log4j2;

/**
 * CCTokenDecryptionClient Client for initializing the decrypter using the
 * private key
 * 
 * @author vararora
 *
 */
@Component
@ConditionalOnMissingAwsCloudEnvironment
@Log4j2
public class CCTokenDecryptionClient {

    private Decrypter decrypter;

    /**
     * Constructor which also injects the dependencies.
     * 
     * @param applicationProperties
     *            Application Properties
     * @param secretProperties
     *            Secret Properties
     * @throws Exception
     *             The Exception
     */
    public CCTokenDecryptionClient(ApplicationProperties applicationProperties, SecretPropertiesAzure secretProperties)
            throws Exception {
        long startTime = System.currentTimeMillis();
        String privateKeyBase64 = secretProperties.getSecretValue(applicationProperties.getDecryptPrivateKey());
        log.info("Time taken to fetch secret value {} ms", System.currentTimeMillis() - startTime);
        this.decrypter = new Decrypter().open(Base64.getDecoder().decode(privateKeyBase64));
    }

    /**
     * Method to decrypt the input provided
     * 
     * @param input
     *            the encrypted text
     * @return the decrypted text
     * @throws Exception
     *             the Exception
     */
    public String decrypt(String input) throws Exception {
        return this.decrypter.decrypt(input);
    }

}
