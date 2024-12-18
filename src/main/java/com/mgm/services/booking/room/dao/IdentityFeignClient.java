package com.mgm.services.booking.room.dao;

import org.springframework.web.bind.annotation.RequestBody;

import com.mgm.services.booking.room.model.request.FuzzyMatchRequest;
import com.mgm.services.booking.room.model.response.FuzzyMatchResponse;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Feign client to invoke the end points of identity's utility services.
 * 
 * @author laknaray
 *
 */
public interface IdentityFeignClient {

    @RequestLine("POST /v1/matched-names")
    @Headers({ "Content-Type: application/json", "Authorization: Bearer {accessToken}" })
    FuzzyMatchResponse performFuzzyMatch(@Param("accessToken") String accessToken,
            @RequestBody FuzzyMatchRequest fuzzyRequest);
    
}
