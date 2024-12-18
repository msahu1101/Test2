package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.FindReservationDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.dto.SourceRoomReservationBasicInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.reservation.ShareWithType;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.BaseAcrsTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation class which integrates with Okta to validate client
 * credentials and returns token response which includes access_token
 *
 */
@Log4j2

@Component
public class FindReservationDAOStrategyACRSImpl extends BaseReservationDao implements FindReservationDAOStrategy {
	static final Integer RESERVATION_NOT_FOUND_ACRS_WARNING_CODE = 284;

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
    public FindReservationDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                              ApplicationProperties applicationProperties, AcrsProperties acrsProperties,
											  RestTemplateBuilder builder, ReferenceDataDAOHelper referenceDataDAOHelper,
											  ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl) throws SSLException {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties, CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeoutACRS(),
				applicationProperties.getReadTimeOutACRS(),
				applicationProperties.getSocketTimeOutACRS(),
				1,
				applicationProperties.getCrsRestTTL()), referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.FindReservationDAO#findRoomReservation(
     * com.mgm.services.booking.room.model.request.FindReservationRequest)
     */
    @Override
    public RoomReservation findRoomReservation(FindReservationRequest reservationRequest) {

       log.info("Incoming Request for findRoomReservation: {}", CommonUtil.convertObjectToJsonString(reservationRequest));


        RoomReservation roomReservation = RoomReservationTransformer.transform(retrieveReservationByCnfNumber(reservationRequest.getConfirmationNumber(),reservationRequest.getSource()), acrsProperties);
        referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
        return roomReservation;
    }

    @Override
    public RoomReservation findRoomReservation(FindReservationV2Request reservationRequest) {

         log.info("Incoming Request for findRoomReservationV2: {}", CommonUtil.convertObjectToJsonString(reservationRequest));

		ReservationRetrieveResReservation retrieveResvRes ;
		retrieveResvRes = retrieveReservationByCnfNumber(reservationRequest.getConfirmationNumber(),reservationRequest.getSource());
		RoomReservation roomReservation = RoomReservationTransformer.transform(retrieveResvRes, acrsProperties);
		if(!ServiceConstant.ICE.equalsIgnoreCase(reservationRequest.getSource()) &&
				ReservationUtil.isBlackListed(acrsProperties.getWhiteListMarketCodeList(), roomReservation.getMarkets()) ){
			throw new BusinessException(ErrorCode.RESERVATION_BLACKLISTED);
		}
		//for shared reservation
		final HotelReservationRetrieveResReservation hotelReservation = retrieveResvRes.getData().getHotelReservation();
		final Optional<String> linkId = getSharedLinkId(hotelReservation.getLinks());

		if (linkId.isPresent()) {
			final List<String> reservationIds = getSharedReservationConfirmationNumbers(
					hotelReservation.getReservationIds().getCfNumber(), linkId.get(), reservationRequest.getSource());
			// 1st resv id will be primary as per the sorting
			roomReservation.setPrimarySharerConfirmationNumber(reservationIds.get(0));

			final List<String> shareWiths = reservationIds.stream().filter(x -> !StringUtils.equals(roomReservation.getConfirmationNumber(), x)).collect(Collectors.toList());
			roomReservation.setShareWiths(shareWiths.toArray(new String[0]));
			roomReservation.setShareWithType(ShareWithType.Full);
			roomReservation.setShareId(linkId.get());
		}
        //CBSR-934
        SegmentRes segmentRes = hotelReservation.getSegments();
		//get the SL room segment
		SegmentResItem segment = BaseAcrsTransformer.getMainSegment(segmentRes);
        roomReservation.setOperaState(BaseAcrsTransformer.acrsResPmsStatusToReservationOperaState(acrsProperties.getSegmentPmsStateOperaStateMap(), segment.getSegmentPmsStatus(), hotelReservation.getStatus()));
        referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
        roomReservation.setOrigin(referenceDataDAOHelper.getChannelName(roomReservation.getOrigin()));
        // convert acrs to refData guid
        invokeRefDataRoutingInfo(roomReservation.getRoutingInstructions(), roomReservation.getPropertyId(), false);

        // convert id to code for alerts , traces and spl req from refData.
        setReferenceData(roomReservation);
        //CQ-16654 ICE needs reservation level program id when Promo code is present, so get the program id 
        //from ENR Rateplan search API if the program id is not present at the reservation level.
        if(StringUtils.isNotEmpty(roomReservation.getPromo()) && StringUtils.isEmpty(roomReservation.getProgramId())){
        	String programId = getProgramIdByPromo(roomReservation.getPropertyId(), roomReservation.getPromo());
        	roomReservation.setProgramId(programId);
       }
        //ICE needs reservation level groupId if groupCode is there in the reservation.
		// In migrated reservation with groupCode, ACRS doesn't have customData.
		// for this we have to set reservation level group as programId
		if(roomReservation.getIsGroupCode() && StringUtils.isEmpty(roomReservation.getProgramId())){
			if (CollectionUtils.isNotEmpty(roomReservation.getBookings())){
				roomReservation.setProgramId(roomReservation.getBookings().get(0).getProgramId());
			}
		}
        return roomReservation;
    }
	@Override
    public ReservationsBasicInfoResponse getRoomReservationsBasicInfoList(
            SourceRoomReservationBasicInfoRequestDTO request) {

    	final ReservationRetrieveResReservation retrieveReservationByConfirmationNumber = retrieveReservationByCnfNumber(request.getConfirmationNumber(),request.getSource());
    	final RoomReservation fetchedRoomReservation = RoomReservationTransformer.transform(retrieveReservationByConfirmationNumber, acrsProperties);

		//for shared reservation
		final HotelReservationRetrieveResReservation hotelReservation = retrieveReservationByConfirmationNumber.getData().getHotelReservation();
		final Optional<String> linkId = getSharedLinkId(hotelReservation.getLinks());
		final String shareLinkId = linkId.orElse(null);

        //Build ReservationSearchReq from RoomReservation
        ReservationSearchReq reservationSearchRequest = RoomReservationTransformer.buildSearchRequestfromRoomReservation(fetchedRoomReservation.getExtConfirmationNumber(), 
        		shareLinkId);
        //Retrieve ReservationSearchRes and transform to ReservationsBasicInfoResponse
        ReservationSearchResPostSearchReservations reservationSearchRes = searchReservationsByReservationSearchReq(reservationSearchRequest,request.getSource());
        return RoomReservationTransformer.transform(reservationSearchRes.getData().getHotelReservations(), shareLinkId);
    }

    @Override
	public String searchRoomReservationByExternalConfirmationNo(FindReservationV2Request reservationRequest) {
		String confirmationNumber = reservationRequest.getConfirmationNumber();
		ReservationSearchReq reservationSearchRequest =
				RoomReservationTransformer.buildSearchRequestWithOperaConfirmationNumber(confirmationNumber);
		ReservationSearchResPostSearchReservations hotelReservations =
				searchReservationsByReservationSearchReq(reservationSearchRequest, reservationRequest.getSource());

		if (null == hotelReservations || null == hotelReservations.getData() || !validateSearchRoomReservationWarnings(hotelReservations.getWarnings())) {
			log.warn("Unable to find reservation via search reservation api with confirmationNumber `{}`", confirmationNumber);
			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}
		return hotelReservations.getData().getHotelReservations().get(0).getReservationIds().getCfNumber();
	}

	/**
	 * Evaluate warning codes in the inputted parameter and return false if at least 1 of them is fatal.
	 *
	 * @param warnings
	 * List of Warning objects as collected in the ReservationSearchResPostSearchReservations response we receive
	 * from invoking ACRS' searchReservations API
	 *
	 * @return
	 * Returns false if at least 1 warning contained in the warnings parameter has a code which we consider fatal for
	 * searchReservations response.
	 *
	 * Returns true in all other cases.
	 */
	private boolean validateSearchRoomReservationWarnings(List<Warning> warnings) {
		// if no warning return true
		if (CollectionUtils.isEmpty(warnings)) {
			return true;
		}
		// If we receive a warning with code = RESERVATION_NOT_FOUND_ACRS_CODE, then we need to return false as that is
		// a fatal warning for reservation not found
		return warnings.stream()
				.map(Warning::getCode)
				.noneMatch(RESERVATION_NOT_FOUND_ACRS_WARNING_CODE::equals);
	}

	static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
			LocalDateTime stop = LocalDateTime.now();
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
			LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
			long duration = ChronoUnit.MILLIS.between(start, stop);
			try {
				ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
			} catch (Exception e) {
				log.warn("Caught exception during Setting of Custom Dimensions: ", e);
			} finally {
				log.debug("Custom Dimensions updated after ACRS call");
			}

            //Map ACRS error codes to Exception Types and ErrorCode Enum values
            if ( httpResponse.getStatusCode().value() >= 500 ) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }else if(response.contains("<_invalid_cardInfo>")) {
            	throw new BusinessException(ErrorCode.INVALID_CARDINFO);
            } else if (response.contains("14021")) {
                throw new BusinessException(ErrorCode.RESERVATION_BLACKLISTED);
            } else if (response.contains("<BookingNotFound>")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }
        }
    }
}
