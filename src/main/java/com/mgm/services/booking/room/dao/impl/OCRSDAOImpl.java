package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.common.exception.SystemException;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.OCRSDAO;
import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.model.ocrs.OcrsReservationList;
import com.mgm.services.booking.room.model.ocrs.Profile;
import com.mgm.services.booking.room.model.request.UpdateProfileRequest;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class OCRSDAOImpl implements OCRSDAO {

    private final URLProperties urlProperties;
    private DomainProperties domainProperties;
    private final RestTemplate client;
    private IDMSTokenDAO idmsTokenDAO;
    ApplicationProperties applicationProperties;

    protected OCRSDAOImpl(URLProperties urlProperties, RestTemplateBuilder builder, DomainProperties domainProperties,
                          ApplicationProperties applicationProperties, IDMSTokenDAO idmsTokenDAO, SecretsProperties secretsProperties) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.applicationProperties = applicationProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL());
        this.idmsTokenDAO = idmsTokenDAO;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }
/**
 * It will return primary profile of the reservation by cnfNumber
 */
    @Override
    public UserProfile getOCRSResvPrimaryProfile(HttpEntity<?> request, String cnfNumber) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("ocrsEnv", applicationProperties.getOcrsEnv());
        uriParams.put("cnfNumber", cnfNumber);
        log.info("OCRS reservation search request headers: {}", request.getHeaders());

        try {
            ResponseEntity<OcrsReservationList> response = client.exchange(
                    domainProperties.getOcrsSearchReservation() + urlProperties.getOcrsSearchReservation(), HttpMethod.GET,
                    request, OcrsReservationList.class, uriParams);

            log.info("OCRS reservation search response headers: {}", CommonUtil.convertObjectToJsonString(response.getHeaders()));
            log.info("OCRS reservation search response body: {}", CommonUtil.convertObjectToJsonString(response.getBody()));

            return getPrimaryProfile(response.getBody().get(0));
        } catch (Exception e) {
            log.error("Error while retriving reservation  from OCRS.");
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());

        }

    }

    @Override
    public OcrsReservation updateProfile(UpdateProfileRequest request) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("ocrsEnv", applicationProperties.getOcrsEnv());

        log.info("OCRS update reservation profile request: {}", CommonUtil.convertObjectToJsonString(request));

        try {
            OcrsReservation updatedReservation = client
                    .exchange(domainProperties.getOcrsPartialReservationUpdate() + urlProperties.getOcrsPartialReservationUpdate(),
                            HttpMethod.PUT, getHttpEntity(request), OcrsReservation.class, uriParams)
                    .getBody();

            log.info("OCRS update reservation profile response: {}",
                    CommonUtil.convertObjectToJsonString(updatedReservation));

            return updatedReservation;
        } catch (Exception e) {
            log.error("Error while updating reservation profile in OCRS for confirmation number {}", request.getOperaConfirmationNumber());
            log.error(e.getMessage());
        }
        return null;
    }
 
    @Override
    public OcrsReservation getOCRSReservation(String cnfNumber) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("ocrsEnv", applicationProperties.getOcrsEnv());
        uriParams.put("cnfNumber", cnfNumber);
        HttpEntity<?> request = getHttpEntity();
        log.info("OCRS reservation search request headers: {}", request.getHeaders());

        try {
            ResponseEntity<OcrsReservationList> response = client.exchange(
                    domainProperties.getOcrsSearchReservation() + urlProperties.getOcrsSearchReservation(), HttpMethod.GET,
                    request, OcrsReservationList.class, uriParams);

            log.info("OCRS reservation search response headers: {}", CommonUtil.convertObjectToJsonString(response.getHeaders()));
            log.info("OCRS reservation search response body: {}", CommonUtil.convertObjectToJsonString(response.getBody()));

            if (!response.getBody().isEmpty()) {
                Optional<OcrsReservation> tcolvOcrsResv = response.getBody().stream().
                        filter(x -> null != x.getHotelReference() && null != x.getHotelReference().getHotelCode()
                                && x.getHotelReference().getHotelCode().
                                equalsIgnoreCase(applicationProperties.getTcolvHotelCode())).findFirst();
                if (tcolvOcrsResv.isPresent()) {
                    return tcolvOcrsResv.get();
                }
            }

            return !response.getBody().isEmpty() ? response.getBody().get(0) : null;
        } catch (Exception e) {
            log.error("Error while retrieving reservation from OCRS: {}", e.getMessage());
            return null;
            //if OCRS does not return reservation then don't throw exception.
           // throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());
        }
    }

    private <T> HttpEntity<T> getHttpEntity(T request) {
        HttpHeaders headers = CommonUtil.createOcrsHeaders(idmsTokenDAO.generateToken().getAccessToken());
        return new HttpEntity<>(request, headers);
    }
    
    private <T> HttpEntity<T> getHttpEntity() {
        HttpHeaders headers = CommonUtil.createOcrsHeaders(idmsTokenDAO.generateToken().getAccessToken());
        return new HttpEntity<>(headers);
    }

    private UserProfile getPrimaryProfile(OcrsReservation ocrsReservation) {
        UserProfile userDetails = new UserProfile();
        List<String> rphs = new ArrayList<>();
        if (null != ocrsReservation.getResGuests() && null != ocrsReservation.getResGuests().getResGuest()) {
            ocrsReservation.getResGuests().getResGuest().forEach(guest -> {
                String profileRphs = StringUtils.EMPTY;
                if (guest.getReservationID().equalsIgnoreCase(ocrsReservation.getReservationID())) {
                    profileRphs = guest.getProfileRPHs();
                }
                if (StringUtils.isNotEmpty(profileRphs)) {
                    rphs.addAll(Arrays.asList(profileRphs.replaceAll("\\s", "").split(",")));
                }
            });
        }
        if (null != ocrsReservation.getResProfiles() && null != ocrsReservation.getResProfiles().getResProfile()) {
            ocrsReservation.getResProfiles().getResProfile().forEach(profile -> {
                if (profile.getProfile().getProfileType().equalsIgnoreCase("guest")
                        && rphs.contains(String.valueOf(profile.getResProfileRPH()))) {
                    Profile resProfile = profile.getProfile();
                    userDetails.setCustomerId(Long.parseLong(resProfile.getMfResortProfileID()));

                }
            });
        }

        return userDetails;
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            final String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received from OCRS: status code: {}, header: {}, body: {}",
                    httpResponse.getStatusCode().value(), httpResponse.getHeaders().toString(), response);
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, response);
            }
        }
    }

}
