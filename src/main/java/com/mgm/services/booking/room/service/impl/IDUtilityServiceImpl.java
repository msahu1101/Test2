package com.mgm.services.booking.room.service.impl;

import java.util.Collections;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.IdentityFeignClient;
import com.mgm.services.booking.room.error.FeignErrorDecoder;
import com.mgm.services.booking.room.model.request.FuzzyMatchRequest;
import com.mgm.services.booking.room.model.request.FuzzyNamesRequest;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.response.FuzzyMatchResponse;
import com.mgm.services.booking.room.service.IDUtilityService;
import com.mgm.services.booking.room.util.CommonUtil;

import feign.Feign;
import feign.Logger.Level;
import feign.Request.Options;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.httpclient.ApacheHttpClient;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class IDUtilityServiceImpl implements IDUtilityService {

    @Autowired
    private IDMSTokenDAO idmsTokenDAO;

    private IdentityFeignClient identityClient;
    private String idUtilityServiceUrl;
    private int connectionTimeout;
    private int readTimeout;
    private boolean enableFuzzyNameMatch;

    public IDUtilityServiceImpl(@Value("${identity.utilityService}") String idUtilityServiceUrl,
            @Value("${feign.client.config.default.connect-timeout}") int connectionTimeout,
            @Value("${feign.client.config.default.read-timeout}") int readTimeout,
            @Value("${application.enableFuzzyNameMatch}") boolean enableFuzzyNameMatch) {
        this.idUtilityServiceUrl = idUtilityServiceUrl;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.enableFuzzyNameMatch = enableFuzzyNameMatch;
        initializeIdentityClient();
    }

    @Override
    public boolean isFirstNameLastNameMatching(ReservationProfile profile,
            String firstName, String lastName) {
        
        boolean isMatching = false;

        if (StringUtils.isEmpty(firstName) || StringUtils.isEmpty(lastName)) {
            log.info("Either of first name or last name is not available");
            return false;
        }

        if (enableFuzzyNameMatch) {
            final String accessToken = idmsTokenDAO.generateToken().getAccessToken();
            
            FuzzyMatchResponse response = identityClient.performFuzzyMatch(accessToken,
                    transform(profile, firstName, lastName));
            if (response != null && CollectionUtils.isNotEmpty(response.getNames())) {
                isMatching = response.getNames()
                        .get(0)
                        .isMatch();
            }
        } else {
            isMatching = StringUtils.equalsIgnoreCase(CommonUtil.removeNonAlphaChars(firstName),
                    CommonUtil.removeNonAlphaChars(profile.getFirstName()))
                    && StringUtils.equalsIgnoreCase(CommonUtil.removeNonAlphaChars(lastName),
                            CommonUtil.removeNonAlphaChars(profile.getLastName()));
        }
        
        if (!isMatching) {
            log.info("Either or both first and last name didn't match with name on the reservation");
        }

        return isMatching;
    }

    private FuzzyMatchRequest transform(ReservationProfile profile, String firstName, String lastName) {
        FuzzyMatchRequest fuzzyMatchRequest = new FuzzyMatchRequest();
        fuzzyMatchRequest.setFirstName(profile.getFirstName());
        fuzzyMatchRequest.setLastName(profile.getLastName());
        FuzzyNamesRequest names = new FuzzyNamesRequest();
        names.setId(UUID.randomUUID().toString());
        names.setFirstName(firstName);
        names.setLastName(lastName);
        fuzzyMatchRequest.setNames(Collections.singletonList(names));
        return fuzzyMatchRequest;
    }

    private void initializeIdentityClient() {
        if (identityClient == null) {
            log.info("identityClient client is null, initializing it now");

            Options options = new Options(connectionTimeout, readTimeout, true);

            identityClient = Feign.builder().client(new ApacheHttpClient()).encoder(new GsonEncoder())
                    .decoder(new GsonDecoder()).options(options).errorDecoder(new FeignErrorDecoder())
                    .logLevel(Level.FULL).target(IdentityFeignClient.class, idUtilityServiceUrl);
        }
    }

}
