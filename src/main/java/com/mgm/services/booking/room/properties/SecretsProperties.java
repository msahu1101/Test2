package com.mgm.services.booking.room.properties;

/**
 * Spring helper component for one-time initialization setup with AWS secret
 * manager util. This makes it easier for injection into code service or dao
 * classes.
 * 
 */
public interface SecretsProperties {

    /**
     * Gets secret values from Azure key vault
     * 
     * @param keyName Secret Key Name
     * @param Key Vault ClientId
     * @param Key Vault ClientKey
     * @param Key Vault Url
     * @return Returns secret value as retrieved from key vault
     */
    String getSecretValue(String keyName);

    /**
     * Gets secret values from AWS Secrets Manager utility
     * 
     * @param keyName
     *            Secret Key Name
     * @return Returns secret value as retrieved from utility
     */
    String getLatestSecretValue(String keyName);

}
