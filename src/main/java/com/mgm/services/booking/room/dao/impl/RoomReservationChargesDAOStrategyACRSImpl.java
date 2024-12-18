package com.mgm.services.booking.room.dao.impl;


import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import com.mgm.services.booking.room.constant.ServiceConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.RoomReservationChargesDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.crs.reservation.ReservationPendingReq;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

import lombok.extern.log4j.Log4j2;

/**
 * DAO implementation class to provide methods for calculating room charges
 * 
 * @author swakulka
 *
 */
@Log4j2
@Component
public class RoomReservationChargesDAOStrategyACRSImpl extends BaseReservationDao implements RoomReservationChargesDAOStrategy {
    @Autowired
    private ModifyReservationDAOStrategyACRSImpl acrsStrategyModify;
    
    @Autowired
    private ReservationDAOStrategyACRSImpl reservationDAOStrategyACRSImpl;
    
    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     *
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param applicationProperties
     *            Application Properties
     * @param builder
     *            Spring's auto-configured rest template builder
     * @throws SSLException
     *             Throws SSL Exception
     */
    @Autowired
    protected RoomReservationChargesDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
            AcrsProperties acrsProperties, RestTemplateBuilder builder, ReferenceDataDAOHelper referenceDataDAOHelper,
            ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, ApplicationProperties applicationProperties,
            RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl) {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties,
                CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                        applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                        applicationProperties.getConnectionTimeoutACRS(),
                        applicationProperties.getReadTimeOutACRS(),
                        applicationProperties.getSocketTimeOutACRS(),
                        1,
                        applicationProperties.getCrsRestTTL()),
                referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }
    
    /**
     * Implementation method to calculate room charges for given request. The method
     * invokes A pricing call to ACRS if no confirmation number in the roomReservation param
     * and invokes modify pending/ignore if there is a confirmation number in order to
     * calculate charges and populate the relevant fields in the RoomReservation object.
     * 
     * @param roomReservation - RoomReservation object
     * @return RoomReservation model
     */
    @Override
    public RoomReservation calculateRoomReservationCharges(RoomReservation roomReservation) {

        log.info("Incoming Request for calculateRoomReservationCharges: {}", CommonUtil.convertObjectToJsonString(roomReservation));
        int qty = roomReservation.getNumRooms();
        // Reset PO flag if promo booking or request does not have any PO rate plan.
        resetPerpetualPricing(roomReservation);
        boolean isPOFlow = roomReservation.isPerpetualPricing();
        boolean isGroupCode = ACRSConversionUtil.isAcrsGroupCodeGuid(roomReservation.getProgramId());
        if (null != roomReservation.getConfirmationNumber()) {
            RoomReservation reservation = acrsStrategyModify.preModifyCharges(acrsStrategyModify.populatePreModifyRequest(roomReservation));
            
            reservation.setCustomerId(roomReservation.getCustomerId());
            reservation.setPerpetualPricing(roomReservation.isPerpetualPricing());
            if(roomReservation.getProfile() != null) {
                reservation.getProfile().setId(roomReservation.getProfile().getId());
                reservation.getProfile().setMlifeNo(roomReservation.getProfile().getMlifeNo());
            }
            reservation.setSpecialRequests(updateResvSpecialReq(reservation));
            reservation.setCreditCardCharges(roomReservation.getCreditCardCharges());
            return reservation;
		} else {
			if (roomReservation.getBookings() != null && roomReservation.getBookings().stream()
					.anyMatch(booking -> booking.getOverridePrice() > -1.0)) {
				return setChargesFromCreatePendingIgnore(roomReservation);
			}
			//for transient flow  call only retrieve pricing
			RoomReservation offerCharges = null;
            boolean isGroupCodeWithAddonComponents = CollectionUtils.isNotEmpty(roomReservation.getSpecialRequests()) && CollectionUtils.isNotEmpty(roomReservation.getSpecialRequests()
                    .stream().filter(addOns->addOns.contains(ServiceConstant.COMPONENT_STR)).collect(Collectors.toList()));
            boolean isGroupCodeWithAddons = isGroupCode && isGroupCodeWithAddonComponents;
            if (!isGroupCode || isGroupCodeWithAddons) {
                offerCharges = getRoomAndComponentCharges(roomReservation,isPOFlow);
                if(null != offerCharges) {
                    offerCharges.setSpecialRequests(roomReservation.getSpecialRequests());
                    if (null != offerCharges.getChargesAndTaxesCalc()) {
                        addComponentTaxAndCharges(offerCharges.getChargesAndTaxesCalc(), offerCharges.getAvailableComponents(),
                                roomReservation.getSpecialRequests());
                    }
                    // add component deposit
                    if (null != offerCharges.getDepositCalc()) {
                        double totalDepositRequired = offerCharges.getDepositCalc().getAmount() + (qty*getComponentDeposit(
                                offerCharges.getAvailableComponents(), roomReservation.getSpecialRequests()));
                        offerCharges.getDepositCalc().setAmount(totalDepositRequired);
                    }
                    // set creditCardCharges to what was inputted
                    offerCharges.setCreditCardCharges(roomReservation.getCreditCardCharges());
                }
            }

            // Fix for CQ-14507:
            // for single avail for PO return BAR price.
            // Ice call charges with PO=false. if PO=false then it will not call
            // getChargesFromSingleAvailability()
            // and resulting price will be different.
            // So if there is mlife in req then do single avail with PO=true as
            // ICE does in previous call.

            if (null != roomReservation.getProfile() && roomReservation.getProfile().getMlifeNo() > 0) {
                // Todo: For each Mlife, ICE doesnot sent PO flag = true in single avail. So we have to handle it differently
				//roomReservation.setPerpetualPricing(true);
            }
            
            //for PO merge single avail with retrieve pricing API response.
			if(roomReservation.isPerpetualPricing() || isGroupCode) {
			    RoomReservation availabilityCharges = getChargesFromSingleAvailability(roomReservation);
			    //merge with offerCharges components
				if (null != offerCharges) {
					availabilityCharges.setAvailableComponents(offerCharges.getAvailableComponents());
                    if (CollectionUtils.isNotEmpty(roomReservation.getSpecialRequests())
                            && CollectionUtils.isNotEmpty(offerCharges.getAvailableComponents())) {
                        //Moving addComponentTaxAndCharges method in BaseReservationDao to support CBSR-2632 code changes
                    	addComponentTaxAndCharges(availabilityCharges.getChargesAndTaxesCalc(),
                                offerCharges.getAvailableComponents(), roomReservation.getSpecialRequests());
                        // add component deposit. Moving getComponentDeposit method in BaseReservationDao to support CBSR-2632 code changes.
                        double totalDepositRequired = availabilityCharges.getDepositCalc().getAmount() + (qty*getComponentDeposit(
                                offerCharges.getAvailableComponents(), roomReservation.getSpecialRequests()));
                        availabilityCharges.getDepositCalc().setAmount(totalDepositRequired);
                    }
				}
			    availabilityCharges.setSpecialRequests(roomReservation.getSpecialRequests());

			    return availabilityCharges;
			}
            if(null == offerCharges){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }else{
                return offerCharges;
            }

		}
    }
    
    private List<String> updateResvSpecialReq(RoomReservation roomReservation) {
    	List<String> roomReservationSplReqNRoomFeatureIds = null;
    	if(CollectionUtils.isNotEmpty(roomReservation.getPurchasedComponents())) {
    		roomReservationSplReqNRoomFeatureIds = roomReservation.getPurchasedComponents().stream().map(PurchasedComponent::getId).collect(Collectors.toList());
    	}
    	return roomReservationSplReqNRoomFeatureIds;
	}

    private RoomReservation setChargesFromCreatePendingIgnore(RoomReservation roomReservation) {

    	ReservationPendingReq acrsRequest = createPendingReservationReq(roomReservation);
    	acrsRequest.getData().getHotelReservation().getSegments().get(0).setFormOfPayment(null);
    	RoomReservation roomReservationPending = reservationDAOStrategyACRSImpl.makePendingRoomReservation(acrsRequest, roomReservation.getPropertyId(), roomReservation.getAgentInfo(),
    			roomReservation.getRrUpSell(),roomReservation.getSource());    	
    	reservationDAOStrategyACRSImpl.ignorePendingRoomReservation(roomReservationPending.getConfirmationNumber(), roomReservation.getPropertyId(), roomReservation.getSource(), roomReservation.isPerpetualPricing());
    	roomReservationPending.setConfirmationNumber(null);
    	copyValuesFromRequestToResponse(roomReservation, roomReservationPending);
    	return roomReservationPending;
	}
    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
            try {
                LocalDateTime end = LocalDateTime.now();
                LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
                long duration = ChronoUnit.MILLIS.between(start, end);
                ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
                log.info("Custom Dimensions updated after ACRS call");
            } catch (Exception e) {
                // Do nothing
            }

            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            // TODO handle errors
            ACRSErrorDetails acrsError = ACRSErrorUtil.getACRSErrorDetailsFromACRSErrorRes(response);
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else if (response.contains("<BookingNotFound>")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION,acrsError.getTitle());
            }
        }
    }
}
