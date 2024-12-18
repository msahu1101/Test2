package com.mgm.services.booking.room.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.CalendarPriceV2Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.CalendarPriceV2Response;
import com.mgm.services.booking.room.service.CalendarPriceV2Service;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.AuroraPriceResponseTransformer;
import com.mgm.services.booking.room.util.CommonUtil;

/**
 * Implementation class for exposing services to fetch calendar prices based on
 * search/input criteria.
 * 
 */
@Component
@Primary
public class CalendarPriceV2ServiceImpl extends BasePriceV2ServiceImpl implements CalendarPriceV2Service {

    @Autowired
    private RoomPriceDAO pricingDao;

    @Autowired
    private RoomProgramService programService;

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.CalendarPriceService#
     * getCalendarPrices(com.mgm.services.booking.room.model.request.
     * CalendarPriceRequest)
     */
    @Override
    public List<CalendarPriceV2Response> getCalendarPrices(CalendarPriceV2Request calendarRequest) {

        // Perform validation checks if program is available
        validateProgram(programService, calendarRequest);

        // If ProgramId is available along with request to include non offer,
        // then make two calls to get the prices with and without program id and
        // combine both of
        // them. Offer price will override Non-Offer price
        if (calendarRequest.getProgramId() != null && !calendarRequest.isExcludeNonOffer()) {
            return combineCalendarPrices(calendarRequest);
        } else {
            List<CalendarPriceV2Response> calendarResponseList = new ArrayList<>();
            AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraRequest(calendarRequest);

            pricingDao.getIterableCalendarPricesV2(request).forEach(response -> {
                CalendarPriceV2Response priceResponse = AuroraPriceResponseTransformer
                        .getCalendarPriceV2Response(response, calendarRequest);

                // If program is specified in the request along with excludeNonOffer as true,
                // then mark all the dates without offer as sold out.
                if (calendarRequest.getProgramId() != null && calendarRequest.isExcludeNonOffer()
                        && !priceResponse.getStatus().equals(AvailabilityStatus.OFFER)) {
                    priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
                }
                calendarResponseList.add(priceResponse);
            });
            return calendarResponseList;
        }
    }

    private List<CalendarPriceV2Response> combineCalendarPrices(CalendarPriceV2Request calendarRequest) {
        List<CalendarPriceV2Response> calendarResponseList = new ArrayList<>();

        // Create aurora pricing request without program
        CalendarPriceV2Request calendarRequestWithProgramNull = new CalendarPriceV2Request();
        BeanUtils.copyProperties(calendarRequest, calendarRequestWithProgramNull);
        calendarRequestWithProgramNull.setProgramId(null);
        AuroraPriceRequest requestWithProgNull = AuroraPriceRequestTransformer
                .getAuroraRequest(calendarRequestWithProgramNull);

        // Create aurora pricing request with program
        AuroraPriceRequest requestWithProgId = AuroraPriceRequestTransformer.getAuroraRequest(calendarRequest);

        // Make both the pricing calls
        overlayOfferPrices(calendarRequest, pricingDao.getCalendarPricesV2(requestWithProgId),
                pricingDao.getCalendarPricesV2(requestWithProgNull), calendarResponseList);
        return calendarResponseList;
    }

    public void overlayOfferPrices(CalendarPriceV2Request calendarRequest, List<AuroraPriceResponse> programList,
            List<AuroraPriceResponse> defaultList, List<CalendarPriceV2Response> calendarResponseList) {

        // iterate through best available prices and overlay offer prices when applicable
        for (AuroraPriceResponse respWithProgIdNull : defaultList) {
            String nonOfferDateStr = CommonUtil.getDateStr(respWithProgIdNull.getDate(), ServiceConstant.DEFAULT_DATE_FORMAT);
            boolean isOfferAvailable = false;
            
            for (AuroraPriceResponse respWithProgId : programList) {
                String offerDateStr = CommonUtil.getDateStr(respWithProgId.getDate(),
                        ServiceConstant.DEFAULT_DATE_FORMAT);
                // If price is available in both responses, then
                // take the one with offer
                if (nonOfferDateStr.equalsIgnoreCase(offerDateStr)
                        && respWithProgId.getStatus().equals(AvailabilityStatus.AVAILABLE)) {
                    isOfferAvailable = true;
                    calendarResponseList.add(
                            AuroraPriceResponseTransformer.getCalendarPriceV2Response(respWithProgId, calendarRequest));
                    break;
                }
            }
            // If the offer is not available for the given date then add this date with
            // default pricing.
            if (!isOfferAvailable) {
                calendarResponseList.add(
                        AuroraPriceResponseTransformer.getCalendarPriceV2Response(respWithProgIdNull, calendarRequest));
            }
        }
    }

}
