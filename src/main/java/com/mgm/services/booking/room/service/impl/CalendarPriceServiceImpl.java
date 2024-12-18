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
import com.mgm.services.booking.room.model.request.CalendarPriceRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.CalendarPriceResponse;
import com.mgm.services.booking.room.service.CalendarPriceService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.AuroraPriceResponseTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.constant.ServiceCommonConstant;
import com.mgm.services.common.model.RedemptionValidationResponse;
import com.mgm.services.common.model.RedemptionValidationResponse.DatesRedemptionIsUnAvailable;

/**
 * Implementation class for exposing services to fetch calendar prices based on
 * search/input criteria.
 * 
 */
@Component
@Primary
public class CalendarPriceServiceImpl extends BasePriceServiceImpl implements CalendarPriceService {

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
    public List<CalendarPriceResponse> getCalendarPrices(CalendarPriceRequest calendarRequest) {

        // Perform validation checks if program is available
        validateProgram(programService, calendarRequest);

        // If ProgramId is available along with request to include non offer,
        // then make two calls to get the prices with and without program id and
        // combine both of
        // them. Offer price will override Non-Offer price
        if (calendarRequest.getProgramId() != null && !calendarRequest.isExcludeNonOffer()) {
            return combineCalendarPrices(calendarRequest);
        } else {
            List<CalendarPriceResponse> calendarResponseList = new ArrayList<>();
            AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraRequest(calendarRequest);
            pricingDao.getIterableCalendarPrices(request).forEach(response -> {
                CalendarPriceResponse priceResponse = AuroraPriceResponseTransformer.getCalendarPriceResponse(response,
                        calendarRequest);

                // If program is specified in the request along with
                // excludeNonOffer as true, then mark
                // all the dates without offer as sold out.
                if (calendarRequest.getProgramId() != null && calendarRequest.isExcludeNonOffer()
                        && !priceResponse.getStatus().equals(AvailabilityStatus.OFFER)) {
                    priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
                }
                calendarResponseList.add(priceResponse);
            });
            return calendarResponseList;
        }
    }

    private List<CalendarPriceResponse> combineCalendarPrices(CalendarPriceRequest calendarRequest) {
        List<CalendarPriceResponse> calendarResponseList = new ArrayList<>();

        // Create aurora pricing request without program
        CalendarPriceRequest calendarRequestWithProgramNull = new CalendarPriceRequest();
        BeanUtils.copyProperties(calendarRequest, calendarRequestWithProgramNull);
        calendarRequestWithProgramNull.setProgramId(null);
        AuroraPriceRequest requestWithProgNull = AuroraPriceRequestTransformer
                .getAuroraRequest(calendarRequestWithProgramNull);

        // Create aurora pricing request with program
        AuroraPriceRequest requestWithProgId = AuroraPriceRequestTransformer.getAuroraRequest(calendarRequest);

        // Make both the pricing calls
        overlayOfferPrices(calendarRequest, pricingDao.getCalendarPrices(requestWithProgId),
                pricingDao.getCalendarPrices(requestWithProgNull), calendarResponseList);
        return calendarResponseList;
    }

    private void overlayOfferPrices(CalendarPriceRequest calendarRequest, List<AuroraPriceResponse> programList,
            List<AuroraPriceResponse> defaultList, List<CalendarPriceResponse> calendarResponseList) {

        // If program is myvegas, get the dates unavailable dates from myvegas
        List<String> myVegasUnavailableDates = getMyVegasUnavailableDates(calendarRequest);

        for (AuroraPriceResponse respWithProgIdNull : defaultList) {
            String r1DateStr = CommonUtil.getDateStr(respWithProgIdNull.getDate(), ServiceConstant.DEFAULT_DATE_FORMAT);
            boolean isOfferAvailable = false;
            for (AuroraPriceResponse respWithProgId : programList) {
                String r2DateStr = CommonUtil.getDateStr(respWithProgId.getDate(), ServiceConstant.DEFAULT_DATE_FORMAT);
                // If price is available in both responses, then
                // take the one with offer
                if (r1DateStr.equalsIgnoreCase(r2DateStr)
                        && respWithProgId.getStatus().equals(AvailabilityStatus.AVAILABLE)) {
                    isOfferAvailable = true;
                    calendarResponseList.add(
                            AuroraPriceResponseTransformer.getCalendarPriceResponse(respWithProgId, calendarRequest));
                    break;
                }
            }
            // If the offer is available for the given date or the
            // date is unavailable for myvegas program
            // then skip this date
            if (isOfferAvailable || myVegasUnavailableDates.contains(r1DateStr)) {
                continue;
            } else {
                calendarResponseList.add(
                        AuroraPriceResponseTransformer.getCalendarPriceResponse(respWithProgIdNull, calendarRequest));
            }
        }
    }

    private List<String> getMyVegasUnavailableDates(CalendarPriceRequest calendarRequest) {
        List<String> allDates = new ArrayList<>();

        // If program is a valid myvegas program, get the unavailable dates
        // provided by myvegas
        if (calendarRequest.isValidMyVegasProgram()) {
            RedemptionValidationResponse redemptionObj = calendarRequest.getMyVegasRedemptionItems()
                    .get(calendarRequest.getProgramId());
            for (DatesRedemptionIsUnAvailable dateObj : redemptionObj.getDatesRedemptionIsUnAvailable()) {
                allDates.addAll(CommonUtil.getDatesBetweenTwoDates(dateObj.getBeginDate(), dateObj.getEndDate(),
                        ServiceCommonConstant.DATE_FORMAT_WITH_TIME_SEC_MILLISEC,
                        ServiceCommonConstant.DEFAULT_DATE_FORMAT));
            }
        }
        return allDates;
    }

}
