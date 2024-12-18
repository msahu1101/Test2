package com.mgm.services.booking.room.service.cache.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.Email;

import lombok.experimental.UtilityClass;

/**
 * Email cache service helper class for common methods.
 *
 */
@UtilityClass
public class EmailCacheServiceHelper {

    /**
     * Populates the email map with the content.
     *
     * @param key
     *              email key
     * @param emailMap
     *              email map
     * @param emailContent
     *              email content string
     * @throws JsonProcessingException
     *              throw json processing exception
     * @throws UnsupportedEncodingException
     *              throw unsupported encoding exception
     */
    public static void populateEmailMap(String key, Map<Object, Object> emailMap, String emailContent)
            throws JsonProcessingException, UnsupportedEncodingException {

        ObjectMapper mapper = new ObjectMapper();

        Email email = mapper.readValue(emailContent, Email.class);

        email.setFrom(URLDecoder.decode(email.getFrom(), ServiceConstant.UTF_8));
        email.setBody(URLDecoder.decode(email.getBody(), ServiceConstant.UTF_8));
        email.setSubject(URLDecoder.decode(email.getSubject(), ServiceConstant.UTF_8));

        emailMap.put(key, email);
    }

}
