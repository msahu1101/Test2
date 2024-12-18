package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.RoomAndComponentChargesDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulPricing;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.reservation.PkgComponent;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.impl.CommonServiceImpl;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomAndComponentChargesTranformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * DAO implementation class to provide methods for calculating room and
 * component charges
 *
 * @author uttam
 */
@Log4j2
@Component
public class RoomAndComponentChargesDAOStrategyACRSImpl extends BaseReservationDao
        implements RoomAndComponentChargesDAOStrategy {
        @Autowired
        private CommonServiceImpl commonService;

    protected RoomAndComponentChargesDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties, ApplicationProperties applicationProperties,
                                                         AcrsProperties acrsProperties, RestTemplateBuilder builder,
                                                         ReferenceDataDAOHelper referenceDataDAOHelper,
                                                         ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl,
                                                         RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl) {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties, CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
                        applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                        applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                        applicationProperties.getConnectionTimeoutACRS(),
                        applicationProperties.getReadTimeOutACRS(),
                        applicationProperties.getSocketTimeOutACRS(),
                        1,
                        applicationProperties.getCrsRestTTL()), referenceDataDAOHelper,
                acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }

    /**
     * This method will return room and component charges
     */
    @Override
    public RoomReservation calculateRoomAndComponentCharges(RoomReservation roomReservation) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for calculateRoomAndComponentCharges: {}", CommonUtil.convertObjectToJsonString(roomReservation));
        }
        // Reset the PO flag if its Promo or not PO program
        //since hold scenario does not have any bookings and dominant program concept so created reset logic separately
        //moving reset logic before retrieve call since request may have po flag as true for non-po program.
        //in tht case we should not send po flag to retrieve price API
        resetPerpetualPricingForHold(roomReservation);
        RoomReservation charges = getRoomAndComponentCharges(roomReservation, roomReservation.isPerpetualPricing());
        if(null != charges) {
            //CBSR-1910 Remove non-public components from WEB hold API
            if (CollectionUtils.isNotEmpty(acrsProperties.getSuppresWebComponentPatterns()) ) {
                charges.setAvailableComponents(filterNonPublicComponents(charges.getAvailableComponents()));
            }

            if (StringUtils.isNotEmpty(roomReservation.getPromo())
                    && !StringUtils.equalsIgnoreCase(roomReservation.getPromo(), charges.getPromo())) {
                throw new BusinessException(ErrorCode.UNABLE_TO_PRICE);
            }
        }

        final String acrsPropertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(roomReservation.getPropertyId());
        String computedBaseRatePlan = acrsProperties.getBaseRatePlan(acrsPropertyCode.toUpperCase());
         List<RoomPrice> barPricingList = null;
        if(!roomReservation.isPerpetualPricing()) {
            String programId = referenceDataDAOHelper.retrieveRatePlanDetail(roomReservation.getPropertyId(),
                    computedBaseRatePlan);
            // to calculate discounted price
            AuroraPriceRequest singleAvailBarRequest =
                    AuroraPriceRequestTransformer.getAuroraPriceV2RequestForCharges(roomReservation, programId);
            try {
                barPricingList = getRoomPricesFromSingleAvail(singleAvailBarRequest, roomReservation.getCheckInDate(), roomReservation.getCheckOutDate(), roomReservation.getRoomTypeId());
            } catch (BusinessException be) {
                log.warn("Error wile getting barPricingList");
            }
        }
        boolean isGroupCodeId = ACRSConversionUtil.isAcrsGroupCodeGuid(roomReservation.getProgramId());
        if (null != roomReservation.getProfile() && roomReservation.getProfile().getMlifeNo() > 0 && roomReservation.isPerpetualPricing()) {

            AuroraPriceRequest singleAvailRequest = AuroraPriceRequestTransformer.getAuroraPriceV2RequestForCharges(roomReservation);
            List<RoomPrice> newRoomBookings = getRoomPricesFromSingleAvail(singleAvailRequest, roomReservation.getCheckInDate(), roomReservation.getCheckOutDate(), roomReservation.getRoomTypeId());
            roomReservation.setBookings(newRoomBookings);
            //Check if booking limit is applied
            commonService.checkBookingLimitApplied(roomReservation);
            RoomReservation pendingResv = makePendingRoomReservation(createPendingReservationReq(roomReservation),
                    roomReservation.getPropertyId(), roomReservation.getAgentInfo(), roomReservation.getRrUpSell(), roomReservation.getSource());
            // Pricing mode
            pendingResv.setPerpetualPricing(false);
            pendingResv.setProgramId(referenceDataDAOHelper
                    .retrieveRatePlanDetail(roomReservation.getPropertyId(), roomReservation.getProgramId()));
            if (ACRSConversionUtil.isPORatePlan(roomReservation.getProgramId())) {
                pendingResv.setPerpetualPricing(true);
                pendingResv.setPricingMode(PricingModes.PERPETUAL);
            } else if (computedBaseRatePlan.equals(roomReservation.getProgramId())) {
                pendingResv.setPricingMode(PricingModes.BEST_AVAILABLE);
            } else {
                pendingResv.setPricingMode(PricingModes.PROGRAM);
            }
            if(null != charges) {
                pendingResv.setAvailableComponents(charges.getAvailableComponents());
            }
            //CBSR-2393 - ACRS properties showing 0 base price on Review Page for COMP rooms
            updateBARBasePrice(newRoomBookings, pendingResv.getBookings());
            return pendingResv;
        } else if (isGroupCodeId) {
            //CBSR-739
            // transient hold with group code
            roomReservation.setIsGroupCode(true);
            RoomReservation roomReservationResponse = getChargesFromSingleAvailability(roomReservation);
            if (null != charges){
                roomReservationResponse.setAvailableComponents(charges.getAvailableComponents());
            }
            referenceDataDAOHelper.updateAcrsBookingsReferences(roomReservationResponse);
            updateDiscountedPrice(barPricingList,roomReservationResponse.getBookings());
            overrideShowsComponentData(roomReservation,charges.getAvailableComponents());
            updateComponentTaxNchargesResponse(roomReservationResponse, roomReservation);
            return roomReservationResponse;
        }else {
            if(null != charges) {
                // Pricing mode
                if (computedBaseRatePlan.equals(roomReservation.getProgramId())) {
                    charges.setPricingMode(PricingModes.BEST_AVAILABLE);
                } else {
                    charges.setPricingMode(PricingModes.PROGRAM);
                }
                charges.setProfile(roomReservation.getProfile());
                updateDiscountedPrice(barPricingList, charges.getBookings());
            }else{
                throw new BusinessException(ErrorCode.UNABLE_TO_PRICE);
            }
            overrideShowsComponentData(roomReservation,charges.getAvailableComponents());
            updateComponentTaxNchargesResponse(charges, roomReservation);
            return charges;
        }
    }
    private void overrideShowsComponentData(RoomReservation roomReservation, List<RoomBookingComponent> availableComponents){
        List<PkgComponent> pkgComponents = roomReservation.getPkgComponents();
        if(CollectionUtils.isNotEmpty(pkgComponents)) {
            // if availableComponents is null make it []
            pkgComponents.stream()
                    .filter(x -> ServiceConstant.SHOW_PKG_TYPE.equalsIgnoreCase(x.getType()))
                    .forEach(pkgComponent -> {
                        // update start and end in roomReservation based on pkgComponents
                        roomReservation.setCheckInDate(pkgComponent.getStart());
                        roomReservation.setCheckOutDate(ReservationUtil.convertLocalDateToDate(ReservationUtil.convertDateToLocalDate(pkgComponent.getEnd()).plusDays(1)));// Do + 1 for this
                        SuccessfulPricing searchOffersResponse = getAcrsDataForRoomAndComponentCharge(roomReservation, roomReservation.isPerpetualPricing());
                        // get the availableComponents for the event Date
                        RoomReservation eventDateData = RoomAndComponentChargesTranformer.getRoomAndComponentChargesResponse(searchOffersResponse
                                , roomReservation.getNumRooms(), acrsProperties);
                        // get particular show event data
                        Optional<RoomBookingComponent> showComponentEventDetails = eventDateData.getAvailableComponents().stream().filter(x -> x.getId().equalsIgnoreCase(pkgComponent.getId())).findFirst();
                        //if present override/add show component in searchOffersResponse.getAdditionalOffers
                        //else throw exception
                        if(showComponentEventDetails.isPresent()) {
                            overrideOrAddInExistingAvailableComponent(availableComponents, showComponentEventDetails.get());
                        }else {
                           throw  new BusinessException(ErrorCode.UNABLE_TO_PRICE, "PKG component is not returned by ACRS for given date:".concat(pkgComponent.getId()));
                        }

                    });
        }
    }

    private void overrideOrAddInExistingAvailableComponent(List<RoomBookingComponent> availableComponents, RoomBookingComponent showEvent) {
        Optional<RoomBookingComponent> showComponentInAvail = availableComponents.stream().filter(availComponent -> availComponent.getId().equalsIgnoreCase(showEvent.getId())).findAny();
        if(showComponentInAvail.isPresent()){
            availableComponents.forEach(availableComponent->{
                if(availableComponent.getId().equalsIgnoreCase(showEvent.getId())){
                    availableComponent.setPrices(showEvent.getPrices());
                    availableComponent.setDepositAmount(showEvent.getDepositAmount());
                    availableComponent.setDepositAmount(showEvent.getDepositAmount());
                    availableComponent.setTripPrice(showEvent.getTripPrice());
                    availableComponent.setTripTax(showEvent.getTripTax());
                    availableComponent.setIsDespsitRequired(showEvent.getIsDespsitRequired());
                    availableComponent.setPrice(showEvent.getPrice());
                }
            });
        }else{
            availableComponents.add(showEvent);
        }
    }

    private List<RoomBookingComponent> filterNonPublicComponents(
			List<RoomBookingComponent> roomBookingComponents) {
		List<RoomBookingComponent> componentsWithoutNonPublic= Collections.emptyList();
		componentsWithoutNonPublic= roomBookingComponents.stream().filter(component ->
                !ReservationUtil.isSuppressWebComponent(component.getId(), acrsProperties)).collect(Collectors.toList());
		return componentsWithoutNonPublic;
	}

    private void updateDiscountedPrice(List<RoomPrice> barPricingList, List<RoomPrice> bookings) {
        if(CollectionUtils.isNotEmpty(barPricingList)){
            barPricingList.forEach(barRoomPrice ->{
                Optional<RoomPrice> booking = bookings.stream().filter(b -> b.getDate().equals(barRoomPrice.getDate())).findFirst();
                if(booking.isPresent()){
                    booking.get().setBasePrice(barRoomPrice.getPrice());
                }
            });
        }
    }

    private void updateBARBasePrice(List<RoomPrice> barPricingList, List<RoomPrice> bookings) {
        if(CollectionUtils.isNotEmpty(barPricingList)){
            barPricingList.forEach(barRoomPrice ->{
                Optional<RoomPrice> booking = bookings.stream().filter(b -> b.getDate().equals(barRoomPrice.getDate())).findFirst();
                if(booking.isPresent()){
                    booking.get().setBasePrice(barRoomPrice.getBasePrice());
                }
            });
        }
    }

    private void updateComponentTaxNchargesResponse(RoomReservation roomResvResponse, RoomReservation roomResvRequest) {
        // Add component charges in response. As a part of package2.0, hold api will contains pkg components
        // specialRequest ids in request. So in response we are sending those pkg components special request ids
        // along with their charges
        if (null != roomResvResponse.getChargesAndTaxesCalc()) {
            addComponentTaxAndCharges(roomResvResponse.getChargesAndTaxesCalc(), roomResvResponse.getAvailableComponents(),
                    roomResvRequest.getSpecialRequests());
        }
        // add component deposit
        if (null != roomResvResponse.getDepositCalc()) {
            double totalDepositRequired = roomResvResponse.getDepositCalc().getAmount() + getComponentDeposit(roomResvResponse.getAvailableComponents(),
                    roomResvRequest.getSpecialRequests());
            roomResvResponse.getDepositCalc().setAmount(totalDepositRequired);
        }
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
            try {
                LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
                long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
                ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
                log.info("Custom Dimensions updated after ACRS call");
            } catch (Exception e) {
                // Do nothing
            }
            //TODO handle errors
            ACRSErrorDetails acrsError = ACRSErrorUtil.getACRSErrorDetailsFromACRSErrorRes(response);
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else if (response.contains("<UnableToPriceTrip>")) {
                throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
            } else if (response.contains("<BookingNotFound>")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, acrsError.getTitle());
            }

        }
    }

    private List<RoomPrice> getRoomPricesFromSingleAvail(AuroraPriceRequest singleAvailRequest, Date checkInDate, Date checkOutDate, String roomTypeId) {
        AuroraPricesResponse pricingResponse = getRoomPricesV2(singleAvailRequest);

        long noOfDays = TimeUnit.DAYS.convert((checkOutDate.getTime() - checkInDate.getTime()),
                TimeUnit.MILLISECONDS);

        List<RoomPrice> roomBookings = new ArrayList<>();
        for (int i = 0; i < noOfDays; i++) {
            Date specificDate = Date.from(checkInDate.toInstant().plus(i, ChronoUnit.DAYS));
            Optional<AuroraPriceResponse> auroraPricesRoomDateSpecific = pricingResponse.getAuroraPrices().stream()
                    .filter(x -> DateUtils.isSameDay(specificDate, x.getDate()) && roomTypeId.equals(x.getRoomTypeId())).findFirst();
            if (auroraPricesRoomDateSpecific.isPresent()) {
                roomBookings.add(prepareBookingFromRoomPrice(auroraPricesRoomDateSpecific.get()));
            } else {
                throw new BusinessException(ErrorCode.UNABLE_TO_PRICE);
            }
        }
        return roomBookings;
    }
}
