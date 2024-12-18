package com.mgm.services.booking.room.util.permormance;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import com.mgm.services.booking.room.model.ocrs.OcrsReservationList;
import com.mgm.services.booking.room.model.response.TokenResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.mgm.services.booking.room.model.paymentorchestration.Response;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgm.services.booking.room.model.crs.calendarsearches.SuccessfulCalendarAvailability;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSearchResponse;
import com.mgm.services.booking.room.model.crs.reservation.ReservationModifyPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.crs.reservation.ReservationSearchResPostSearchReservations;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulMultiAvailability;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulPricing;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulSingleAvailability;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforesponse.PartnerCustomerBasicInfoResponse;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchresponse.PartnerCustomerSearchResponse;
import com.mgm.services.booking.room.model.paymentorchestration.Response;
import com.mgm.services.booking.room.model.paymentorchestration.Workflow;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.model.response.PartnerAuthTokenResponse;
import com.mgm.services.booking.room.model.paymentorchestration.Workflow;
import com.mgm.services.booking.room.model.paymentorchestration.WorkflowArgs;
import com.mgm.services.booking.room.model.paymentorchestration.WorkflowArgs.OrderEnum;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RestTemplateMock extends RestTemplate {

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
		return returnMockACRSResponse(url, uriVariables, requestEntity);
		
	}
	
	@Override
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {

		return returnMockACRSResponse(url, uriVariables, null);
	}
	
	private <T> ResponseEntity<T> returnMockACRSResponse(String url, Map<String, ?> uriVariables,HttpEntity<?> requestEntity ) {
		final String crsDomain = "https://cfts.hospitality.api.amadeus.com/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}";
		InputStream input = null;
		
		//oauth token		
		if (url.contains("/security/oauth2/token")) {
			input = getClass().getResourceAsStream("/perfomance-data/oauth_token.json");
			return (ResponseEntity<T>) new ResponseEntity<ACRSAuthTokenResponse>(
					convertCrs(input, ACRSAuthTokenResponse.class), HttpStatus.OK);
		}
		if(null != requestEntity && url.contains("/mpay/v1/payment")) {
			HttpEntity<Workflow> requestBody = (HttpEntity<Workflow>)requestEntity;
			String functionName = requestBody.getBody().getDefinitions().get(0).getFunctionName().getValue();
			if(functionName.equals((OrderEnum.PPSAUTHORIZE).getValue())) {
				input = getClass().getResourceAsStream("/perfomance-data/ppsauthorize.json");
				return (ResponseEntity<T>) new ResponseEntity<Response>(
						convertCrs(input, Response.class), HttpStatus.OK);
			}
			else if(functionName.equals((OrderEnum.PPSCAPTURE).getValue())){
				input = getClass().getResourceAsStream("/perfomance-data/ppscapture.json");
				return (ResponseEntity<T>) new ResponseEntity<Response>(
						convertCrs(input, Response.class), HttpStatus.OK);
			}
			else if(functionName.equals((OrderEnum.PPSREFUND).getValue())){
				input = getClass().getResourceAsStream("/perfomance-data/ppsrefund.json");
				return (ResponseEntity<T>) new ResponseEntity<Response>(
						convertCrs(input, Response.class), HttpStatus.OK);
			}
		}
		
		// charges(create)
		if (url.contains("/hotel/offers/searches") && url.contains("{ratePlanCode}")) {
			input = getClass().getResourceAsStream("/perfomance-data/single-room-avail_offer.json");
			return (ResponseEntity<T>) new ResponseEntity<SuccessfulPricing>(convertCrs(input, SuccessfulPricing.class),
					HttpStatus.OK);
		}
		
		//multi avail
		if (url.contains("/hotel/offers/searches") && url.contains("propertyCodes=")) {
			input = getClass().getResourceAsStream("/perfomance-data/multi-room-avail.json");
			return (ResponseEntity<T>) new ResponseEntity<SuccessfulMultiAvailability>(
					convertCrs(input, SuccessfulMultiAvailability.class), HttpStatus.OK);
		}	

		// single avail
		if (url.contains("/hotel/offers/searches")) {
			input = getClass().getResourceAsStream("/perfomance-data/single-room-avail.json");
			return (ResponseEntity<T>) new ResponseEntity<SuccessfulSingleAvailability>(
					convertCrs(input, SuccessfulSingleAvailability.class), HttpStatus.OK);
		}		

		// calendar search
		else if (url.contains("/hotel/offers/calendar-searches")) {
			input = getClass().getResourceAsStream("/perfomance-data/calendar_availability.json");
			return (ResponseEntity<T>) new ResponseEntity<SuccessfulCalendarAvailability>(
					convertCrs(input, SuccessfulCalendarAvailability.class), HttpStatus.OK);
		}

		// create reservation - pending
		else if (url.equals(crsDomain.concat("/hotel/reservations/{acrsChainCode}/pending"))) {
			input = getClass().getResourceAsStream("/perfomance-data/pending_reservation.json");
			return (ResponseEntity<T>) new ResponseEntity<ReservationPendingRes>(convertCrs(input, ReservationPendingRes.class),
					HttpStatus.OK);
		}

		// find reservation
		else if (url.equals(crsDomain.concat("/hotel/reservations/{acrsChainCode}/{cfNumber}"))) {
			if(uriVariables.get("cfNumber").equals("A1B2C3")) {
				return (ResponseEntity<T>) new ResponseEntity<ReservationRetrieveResReservation>(HttpStatus.NOT_FOUND);
			}
			input = getClass().getResourceAsStream("/perfomance-data/find_reservation.json");
			return (ResponseEntity<T>) new ResponseEntity<>(
					convertCrs(input, ReservationRetrieveResReservation.class), HttpStatus.OK);
		}
		
		//search reservation
		else if (url.equals(crsDomain.concat("/hotel/reservations/{acrsChainCode}/search"))) {
			input = getClass().getResourceAsStream("/perfomance-data/search_reservation.json");
			return (ResponseEntity<T>) new ResponseEntity<>(
					convertCrs(input, ReservationSearchResPostSearchReservations.class), HttpStatus.OK);
		}

		// cancel reservation - pending
		else if (url.equals(crsDomain.concat("/hotel/reservations/{acrsChainCode}/{cfNumber}/cancel/pending"))) {
			input = getClass().getResourceAsStream("/perfomance-data/cancel_pending.json");
			return (ResponseEntity<T>) new ResponseEntity<ReservationRes>(
					convertCrs(input, ReservationRes.class), HttpStatus.OK);
		}

		// create, cancel reservation, modify reservation, modify profile -commit pending
		else if (url.equals(crsDomain.concat("/hotel/reservations/{acrsChainCode}/{confirmationNumber}/commit"))) {
			input = getClass().getResourceAsStream("/perfomance-data/comitted_pending_reservation.json");
			return (ResponseEntity<T>) new ResponseEntity<ReservationRes>(convertCrs(input, ReservationRes.class),
					HttpStatus.OK);
		}

		// pre-modify roomCharges, modify reservation, modify profile - pending
		else if (url.equals(crsDomain.concat("/hotel/reservations/{acrsChainCode}/{confirmationNumber}/pending"))) {
			input = getClass().getResourceAsStream("/perfomance-data/crs-modify-pending.json");
			return (ResponseEntity<T>) new ResponseEntity<ReservationModifyPendingRes>(
					convertCrs(input, ReservationModifyPendingRes.class), HttpStatus.OK);
		}
		
		// component availability
		else if (url.contains("/offers/searches")) {
			input = getClass().getResourceAsStream("/perfomance-data/retrieve-pricing.json");
			return (ResponseEntity<T>) new ResponseEntity<SuccessfulPricing>(convertCrs(input, SuccessfulPricing.class),
					HttpStatus.OK);
		}
		
		// enr rate plan search
		else if (url.contains("crs/search/rateplans")) {
			input = getClass().getResourceAsStream("/perfomance-data/enr_rateplan_search.json");
			return (ResponseEntity<T>) new ResponseEntity<ENRRatePlanSearchResponse[]>(
					convertCrs(input, ENRRatePlanSearchResponse[].class), HttpStatus.OK);
		}

		// organization - iata search
		else if (url.contains("hotel/organizations/{acrsChainCode}/search")) {
			input = getClass().getResourceAsStream("/perfomance-data/iata_search.json");
			return (ResponseEntity<T>) new ResponseEntity<OrganizationalSearchResponse>(
					convertCrs(input, OrganizationalSearchResponse.class), HttpStatus.OK);
		}
		else if (url.contains("/ocrs/ocrsInfo")) {
			input = getClass().getResourceAsStream("/perfomance-data/ocrs-findReservation-response.json");
			return (ResponseEntity<T>) new ResponseEntity<OcrsReservationList>(
					convertCrs(input, OcrsReservationList.class), HttpStatus.OK);
		}
		else if(url.contains("crs/reservation/retrieve") || url.contains("booking/room/search")) {
			input = getClass().getResourceAsStream("/perfomance-data/find_reservation.json");
			return (ResponseEntity<T>) new ResponseEntity<>(
					convertCrs(input, ReservationRetrieveResReservation.class), HttpStatus.OK);
		}
		// enr rate plan search
		else if (url.contains("crs/search/promochannelrateplans")) {
			input = getClass().getResourceAsStream("/perfomance-data/enr_rateplan_search.json");
			return (ResponseEntity<T>) new ResponseEntity<ENRRatePlanSearchResponse[]>(
			convertCrs(input, ENRRatePlanSearchResponse[].class), HttpStatus.OK);
		}
		else if (url.contains("crs/search/channelrateplans")) {
			input = getClass().getResourceAsStream("/perfomance-data/enr_rateplan_search.json");
			return (ResponseEntity<T>) new ResponseEntity<ENRRatePlanSearchResponse[]>(
			convertCrs(input, ENRRatePlanSearchResponse[].class), HttpStatus.OK);
		}
		/*else if(url.contains("booking/room/search")) {
			input = getClass().getResourceAsStream("/perfomance-data/ocrs_search.json");
			return (ResponseEntity<T>) new ResponseEntity<OcrsReservationList[]>(
			convertCrs(input, OcrsReservationList[].class), HttpStatus.OK);
		}*/
		
		if(url.contains("/customers/profile/search")) {
			input =  getClass().getResourceAsStream("/perfomance-data/PartnerCustomerSearchSampleResponse.json");
			return (ResponseEntity<T>) new ResponseEntity<PartnerCustomerSearchResponse>(
					convertCrs(input, PartnerCustomerSearchResponse.class), HttpStatus.OK);
		}
		if (url.contains("/ent-auth/sso/token")) {
			input = getClass().getResourceAsStream("/perfomance-data/oauth_token.json");
			return (ResponseEntity<T>) new ResponseEntity<TokenResponse>(
					convertCrs(input, TokenResponse.class), HttpStatus.OK);
		}
		if (url.contains("/queries/members/pattern1")) {
			return (ResponseEntity<T>) new ResponseEntity<PartnerAuthTokenResponse>(
					convertCrs(input, PartnerAuthTokenResponse.class), HttpStatus.OK);
		}
		if (url.contains("/customers/basicinfo")) {
			input = getClass().getResourceAsStream("/perfomance-data/PartnerCustomerBasicInfoSampleResponse.json");
			return (ResponseEntity<T>) new ResponseEntity<PartnerCustomerBasicInfoResponse>(
					convertCrs(input, PartnerCustomerBasicInfoResponse.class), HttpStatus.OK);
		}
		

		return null;

	}

	private static <T> T convertCrs(InputStream input, Class<T> target) {

		try {
			ArrayList<Module> modules = new ArrayList<>();
			final SimpleModule module = new SimpleModule();
			// support for ACRS (De)serializing.
			module.addDeserializer(org.joda.time.LocalDate.class, new LocalDateDeserializer());
			module.addSerializer(org.joda.time.LocalDate.class, new LocalDateSerializer());
			modules.add(new JavaTimeModule());
			modules.add(module);

			ObjectMapper crsMapper = new ObjectMapper();
			crsMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			crsMapper = Jackson2ObjectMapperBuilder.json().modules(modules).build();
			return crsMapper.readValue(input, target);
		} catch (IOException e) {
			log.error("Exception trying to convert file to json: ", e);
		}
		return null;
	}
}
