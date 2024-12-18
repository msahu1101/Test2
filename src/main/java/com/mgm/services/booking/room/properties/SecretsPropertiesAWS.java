package com.mgm.services.booking.room.properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.context.annotation.ConditionalOnAwsCloudEnvironment;
import org.springframework.stereotype.Component;

import com.mgm.services.common.aws.AWSSecretsManagerUtil;

/**
 * Spring helper component for one-time initialization setup with AWS secret
 * manager util. This makes it easier for injection into code service or dao
 * classes.
 * 
 */
@Component
@ConditionalOnAwsCloudEnvironment
public class SecretsPropertiesAWS implements SecretsProperties{

    @Autowired
    private ApplicationProperties appProperties;

    @Value("${aws.profile}")
    private String awsProfile;

    private AWSSecretsManagerUtil awsSecretsManagerUtil;

    @PostConstruct
    private void postConstruct() {
        awsSecretsManagerUtil = new AWSSecretsManagerUtil(appProperties.getAwsSecretsRegion(),
                appProperties.getSecretName(), awsProfile);
    }

    /**
     * Gets secret values from AWS Secrets Manager utility
     * 
     * @param keyName
     *            Secret Key Name
     * @return Returns secret value as retrieved from utility
     */
    public String getSecretValue(String keyName) {
        return awsSecretsManagerUtil.getSecretValue(keyName);
    }

    /**
     * Gets secret values from AWS Secrets Manager utility
     * 
     * @param keyName
     *            Secret Key Name
     * @return Returns secret value as retrieved from utility
     */
    public String getLatestSecretValue(String keyName) {
        return awsSecretsManagerUtil.getLatestSecretValue(keyName);
    }

}
