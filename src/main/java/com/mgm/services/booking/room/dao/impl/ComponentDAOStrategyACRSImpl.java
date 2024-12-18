package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.RoomComponentRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomComponentResponseTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation class for ComponentDAO providing functionality to provide room
 * component related functionalities.
 */
@Component
@Log4j2
public class ComponentDAOStrategyACRSImpl extends BaseReservationDao implements ComponentDAOStrategy {

	/**
	 * Constructor which also injects all the dependencies. Using constructor based
	 * injection since spring's auto-configured WebClient. Builder is not
	 * thread-safe and need to get a new instance for each injection point.
	 *
	 * @param urlProperties         URL Properties
	 * @param domainProperties      Domain Properties
	 * @param applicationProperties Application Properties
	 * @param builder               Spring's auto-configured rest template builder
	 * @throws SSLException Throws SSL Exception
	 */
	@Autowired
	protected ComponentDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
			ApplicationProperties applicationProperties, AcrsProperties acrsProperties, RestTemplateBuilder builder,
			ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl)
			throws SSLException {
		super(urlProperties, domainProperties, applicationProperties, acrsProperties,
				CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
						applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
						applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
						applicationProperties.getConnectionTimeoutACRS(),
						applicationProperties.getReadTimeOutACRS(),
						applicationProperties.getSocketTimeOutACRS(),
						1,
						applicationProperties.getCrsRestTTL()),
				referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
		this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgm.services.booking.room.dao.ComponentDAO#
	 * getRoomComponentAvailability(com.mgm.services.booking.room.model.request.
	 * RoomComponentRequest)
	 */
	@Override
	public List<RoomRequest> getRoomComponentAvailability(RoomComponentRequest request) {


		log.info("Incoming Request for Room Component Availability: {}",
					CommonUtil.convertObjectToJsonString(request));

		final String acrsPropertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());
		final String acrsInventoryCode = referenceDataDAOHelper.retrieveRoomTypeDetail(acrsPropertyCode,
				request.getRoomTypeId());
		
        String computedBaseRatePlan = acrsProperties.getSearchOfferRatePlan(acrsPropertyCode.toUpperCase());
		
		String acrsProgramId;
		if (null != request.getProgramId() && !ACRSConversionUtil.isAcrsGroupCodeGuid(request.getProgramId())) {
			try {
				acrsProgramId = referenceDataDAOHelper.retrieveRatePlanDetail(acrsPropertyCode, request.getProgramId());
				if(ACRSConversionUtil.isPORatePlan(acrsProgramId)){
					acrsProgramId = computedBaseRatePlan;
				}
			} catch (Exception e) {
				acrsProgramId = computedBaseRatePlan;
			}
		} else {
			acrsProgramId = computedBaseRatePlan;
		}

		BodyParameterPricing bodyParameterPricing = new BodyParameterPricing();
		DataRqPricing dataRqPricing = new DataRqPricing();
		List<RequestedProductPricing> requestedProductPricingList = new ArrayList<>();
		RequestedProductPricing requestedProductPricing = new RequestedProductPricing();

		requestedProductPricing.setInventoryTypeCode(acrsInventoryCode);

		String tripLength = String.valueOf(
				TimeUnit.DAYS.convert((request.getTravelEndDate().getTime() - request.getTravelStartDate().getTime()),
						TimeUnit.MILLISECONDS));

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String startDate = simpleDateFormat.format(request.getTravelStartDate());
		requestedProductPricingList.add(requestedProductPricing);
		dataRqPricing.setProducts(requestedProductPricingList);
		
		OptionsPricing optionsPricing = new OptionsPricing();
		
		RequestedDescriptionPricing requestedDescriptionPricing = new RequestedDescriptionPricing();
		requestedDescriptionPricing.setPackageDescFlag(true);
		//Long Description
		requestedDescriptionPricing.setProductLongDescFlag(true);
		requestedDescriptionPricing.setRatePlanLongDescFlag(true);
		requestedDescriptionPricing.setTaxLongDescFlag(true);
		
		optionsPricing.setDescription(requestedDescriptionPricing);
		if (StringUtils.isNotBlank(request.getMlifeNumber()) ) {
			Loyalty loyalty = new Loyalty();
			loyalty.setLoyaltyId(request.getMlifeNumber());
			optionsPricing.setLoyalty(loyalty);
		}
		dataRqPricing.setOptions(optionsPricing);
		
		bodyParameterPricing.setData(dataRqPricing);

		try {
			SuccessfulPricing searchOffersResponse = acrsSearchOffers(bodyParameterPricing,
					acrsPropertyCode, acrsProgramId, startDate, tripLength, request.getSource(),
					request.isPerpetualPricing());
			List<RoomRequest> components = RoomComponentResponseTransformer.getRoomRequestComponentResponse(searchOffersResponse,
					request.getPropertyId(), acrsPropertyCode);
			if(!ServiceConstant.ICE.equalsIgnoreCase(request.getSource()) && null!= acrsProperties.getSuppresWebComponentPatterns() && !acrsProperties.getSuppresWebComponentPatterns().isEmpty()) {
				return filterNonPublicComponents(components);
			}else{
				return components;
			}

		} catch (BusinessException e) {
			//TODO: As per Web Team request, sending empty for failures
			return Collections.emptyList();
		}
	}
	private List<RoomRequest> filterNonPublicComponents(
			List<RoomRequest> roomBookingComponents) {
		List<RoomRequest> componentsWithoutNonPublic= Collections.emptyList();
		componentsWithoutNonPublic= roomBookingComponents.stream().filter(component ->
				!ReservationUtil.isSuppressWebComponent(component.getId(), acrsProperties)).collect(Collectors.toList());
		return componentsWithoutNonPublic;
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
			// TODO: Error Handling for Component error code
			ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
			try {
				LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
				long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
				ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
				log.info("Custom Dimensions updated after ACRS call");
			} catch (Exception e) {
				// Do nothing
			}
			ACRSErrorDetails acrsError = ACRSErrorUtil.getACRSErrorDetailsFromACRSErrorRes(response);
			if (httpResponse.getStatusCode().value() >= 500) {
				throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			} else if (httpResponse.getStatusCode().value() == 404 && response.contains("50006")) {
				throw new BusinessException(ErrorCode.INVALID_PROGRAM_ID);
			} else if (httpResponse.getStatusCode().value() == 404
					&& (response.contains("50018") || response.contains("50019") || response.contains("50012"))) {
				throw new BusinessException(ErrorCode.INVALID_DATES);
			} else if (httpResponse.getStatusCode().value() == 404 && response.contains("50020")) {
				throw new BusinessException(ErrorCode.INVALID_CHANNEL_HEADER);
			} else if (httpResponse.getStatusCode().value() == 404 && response.contains("50010")) {
				throw new BusinessException(ErrorCode.INVALID_PROPERTY);
			} else if (httpResponse.getStatusCode().value() == 404 && response.contains("50002")) {
				throw new BusinessException(ErrorCode.INVALID_ROOMTYPE);
			} else {
				throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, acrsError.getTitle());
			}
		}
	}

    @Override
    public RoomRequest getRoomComponentById(String componentId, String roomTypeId) {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public RoomComponent getRoomComponentByCode(String propertyId, String code, String roomTypeId,
												String ratePlanId, Date checkInDate, Date checkOutDate,
												String mlifeNumber, String source) {
		RoomComponentRequest componentRequest = RoomComponentRequestTransformer
				.getRoomComponentRequest(propertyId, roomTypeId, ratePlanId, checkInDate, checkOutDate,
						mlifeNumber, source);
		List<RoomRequest> roomRequestList = getRoomComponentAvailability(componentRequest);
		if (CollectionUtils.isNotEmpty(roomRequestList)) {
			Optional<RoomRequest> roomRequestOptional = roomRequestList.stream().filter(x ->
					x.getCode().equalsIgnoreCase(code)).findFirst();
			if (roomRequestOptional.isPresent()) {
				return RoomComponentResponseTransformer.
						getRoomComponentResponse(roomRequestOptional.get());
			}
		}
		return new RoomComponent();
	}

	@Override
	public RoomComponent getRoomComponentById(String componentId) {
		// TODO Auto-generated method stub
		return new RoomComponent();
	}
}
