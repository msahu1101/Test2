package com.mgm.services.booking.room.controller;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.service.GroupSearchV2Service;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;
import javax.servlet.http.HttpServletRequest;

/**
 * Controller to lookup room reservations based on confirmation number.
 * will remove Nullable
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class GroupSearchV2Controller extends ExtendedBaseV2Controller {
	
	@Nullable 
    @Autowired
    private GroupSearchV2Service groupSearchService;
	
	@Autowired
	private TokenValidator tokenValidator;


    /**
     * Lookup service to find group.
     * 
     * @param source
     *            Source header
     * @param groupSearchRequest
     *            GroupSearchRequest
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @return List of GroupSearchResponse
     */
    @GetMapping("/groupblocks")
    public List<GroupSearchV2Response> searchGroup(@RequestHeader String source,
            @Valid GroupSearchV2Request groupSearchRequest, BindingResult result, HttpServletRequest servletRequest)
          
          {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
    	preprocess(source, groupSearchRequest, result);
    	//CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(groupSearchRequest, groupSearchRequest.getPropertyId());
        validateDate(groupSearchRequest);

        return groupSearchService.searchGroup(groupSearchRequest);
    }

    private void validateDate(GroupSearchV2Request groupSearchRequest) {
        final String startDate = groupSearchRequest.getStartDate();
        final String endDate = groupSearchRequest.getEndDate();
        if (null != startDate && null != endDate) {
            if (CommonUtil.getDate(endDate, ServiceConstant.ISO_8601_DATE_FORMAT)
                    .before(CommonUtil.getDate(startDate, ServiceConstant.ISO_8601_DATE_FORMAT))) {
                throw new ValidationException(Collections.singletonList(ErrorCode.NO_PAST_DATES.getErrorCode()));
            }
        }
    }
}
