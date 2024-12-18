package com.mgm.services.booking.room.properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.azure.keyvault.AzureGenericKeyVaultUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Utility to access keyValut to access secret.
 * 
 * @author jayveera
 *
 */
@Component
@ConditionalOnMissingAwsCloudEnvironment
@Log4j2
public class SecretPropertiesAzure implements SecretsProperties {
    
    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    private AcrsProperties acrsProperties;
    
    private AzureGenericKeyVaultUtil azureKeyVaultUtil;

    @Value("${azure.profile}")
    private String azureProfile;
    
    
    @PostConstruct
    private void postConstruct() {
        azureKeyVaultUtil = new AzureGenericKeyVaultUtil(azureProfile, appProperties.getSubscription(),
                appProperties.getResourceGroup(), appProperties.getResourceName(), ServiceConstant.KEYVAULT_RBS_KEY, appProperties.getSecretKeyVaultUrl());
    }

    /**
     * Gets secret values from AWS Secrets Manager utility
     * 
     * @param keyName
     *            Secret Key Name
     * @return Returns secret value as retrieved from utility
     */
    public String getSecretValue(String keyName) {
        return azureKeyVaultUtil.getSecretValue(keyName);
    }

    /**
     * Gets secret values from AWS Secrets Manager utility
     * 
     * @param keyName
     *            Secret Key Name
     * @return Returns secret value as retrieved from utility
     */
    public String getLatestSecretValue(String keyName) {
        return azureKeyVaultUtil.getLatestSecretValue(keyName);
    }
    
    @Scheduled(cron = "0 */15 * * * *")
    public void scheduleTaskUsingCronExpression() {

        log.info("Refreshing key vault secrets every 15 mins");
        azureKeyVaultUtil.getAllLatestSecrets();
        // Refresh individual secrets as well outside of main secrets json
        getLatestSecretValue(
                String.format(appProperties.getRtcEmailsOnboardedListSecretKey(), appProperties.getRbsEnv()));
        getLatestSecretValue(acrsProperties.getAcrsPropertyListSecretKey());
        getLatestSecretValue(appProperties.getEnableZeroAmountAuthKey());
        getLatestSecretValue(appProperties.getEnableZeroAmountAuthKeyTCOLVCreate());
        getLatestSecretValue(appProperties.getEnableZeroAmountAuthKeyTCOLVModify());
    }
}
