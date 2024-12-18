package com.mgm.services.booking.room.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.response.CalendarPriceV3Response;
import com.mgm.services.booking.room.service.CalendarPriceV3Service;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.AuroraPriceResponseTransformer;

/**
 * Implementation class for exposing services to fetch calendar prices based on
 * search/input criteria.
 * 
 */
@Component
@Primary
public class CalendarPriceV3ServiceImpl extends BasePriceV2ServiceImpl implements CalendarPriceV3Service {

    @Autowired
    private RoomPriceDAO pricingDao;

    @Autowired
    private RoomProgramDAO programDao;
    
    @Autowired
    private RoomProgramService programService;

    @Autowired
    private ServiceConversionHelper serviceConversionHelper;

    @Override
    public List<CalendarPriceV3Response> getLOSBasedCalendarPrices(CalendarPriceV3Request calendarRequest) {

        serviceConversionHelper.convertGuids(calendarRequest);

        //Set perpetual flag to true for movableink source
        if (StringUtils.isNotEmpty(calendarRequest.getSource()) &&
                calendarRequest.getSource().equalsIgnoreCase(ServiceConstant.MOVABLEINK_SOURCE)) {
            calendarRequest.setPerpetualPricing(true);
        }

        // Perform validation checks if program is available
        validateProgram(programService, calendarRequest);
        
        // if PO program is supplied for PO qualified user, drop the program
        if (calendarRequest.isPerpetualPricing() && StringUtils.isNotEmpty(calendarRequest.getProgramId())
                && programDao.isProgramPO(calendarRequest.getProgramId())) {
            calendarRequest.setProgramId(null);
        }

        List<CalendarPriceV3Response> calendarResponseList = new ArrayList<>();
        if (StringUtils.isNotEmpty(calendarRequest.getProgramId())) {
            // If a program price is requested, ignoring perpetual flag
            calendarRequest.setPerpetualPricing(false);
        }

        AuroraPriceV3Request request = AuroraPriceRequestTransformer.getAuroraRequest(calendarRequest);

        pricingDao.getLOSBasedCalendarPrices(request).forEach(response -> {
            CalendarPriceV3Response priceResponse = AuroraPriceResponseTransformer.getCalendarPriceV3Response(response,
                    calendarRequest);

            calendarResponseList.add(priceResponse);
        });
        return calendarResponseList;
    }
}
