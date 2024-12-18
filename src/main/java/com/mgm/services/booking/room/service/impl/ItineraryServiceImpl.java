package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.ItineraryServiceRequest.Itinerary;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.mgm.services.booking.room.dao.ItineraryDAO;
import com.mgm.services.booking.room.model.request.ItineraryServiceRequest;
import com.mgm.services.booking.room.model.request.RoomReservationBasic;
import com.mgm.services.booking.room.model.request.TripParams;
import com.mgm.services.booking.room.model.response.ItineraryResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service interface exposing service to authorize and confirm a transaction
 * 
 * @author vararora
 *
 */
@Component
@Log4j2
public class ItineraryServiceImpl implements ItineraryService {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static boolean itineraryServiceEnabled;

    private static boolean transformUpdate = false;

    public static boolean isItineraryServiceEnabled() {
        return itineraryServiceEnabled;
    }

    @Value("${itineraryService.enabled}")
    public void setItineraryServiceEnabled(boolean itineraryServiceEnabled) {
        ItineraryServiceImpl.itineraryServiceEnabled = itineraryServiceEnabled;
    }

    @Autowired
    private ItineraryDAO itineraryDao;
    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    @Autowired
    private RoomReservationResponseMapper responseMapper;

    /**
     * Transform the room request for Update/Create Itinerary into a format which can be passed to the
     * Authorization service
     *
     * @param roomReservationResponse
     *            the room reservation response
     * @return the transformed response
     */
    private ItineraryServiceRequest transform(RoomReservationV2Response roomReservationResponse, boolean transformUpdate) {

        int numAdults = 0;
        int numChildren = 0;
        int numRooms = 0;
        String checkinDate = "";
        String checkoutDate = "";

        if (roomReservationResponse.getTripDetails() != null) {
            numAdults = roomReservationResponse.getTripDetails().getNumAdults();
            numChildren = roomReservationResponse.getTripDetails().getNumChildren();
            numRooms = roomReservationResponse.getTripDetails().getNumRooms();
            checkinDate = CommonUtil.getDateStr(roomReservationResponse.getTripDetails().getCheckInDate(), DATE_FORMAT);
            checkoutDate = CommonUtil.getDateStr(roomReservationResponse.getTripDetails().getCheckOutDate(),
                    DATE_FORMAT);
        }

        ItineraryServiceRequest request = new ItineraryServiceRequest();
        Itinerary itinerary = new Itinerary();
        if (transformUpdate != true) {
            itinerary.setItineraryName(ServiceConstant.ITINERARY_FOR_PARTYRESERVATION);
        }

        //CBSR-2289 : Update the customer ID to null if room reservation response customer ID is 0 or -1 else keep it as it is
        if (roomReservationResponse.getCustomerId() == 0 || roomReservationResponse.getCustomerId() == -1) {
            itinerary.setCustomerId(null);
        } else {
            itinerary.setCustomerId(String.valueOf(roomReservationResponse.getCustomerId()));
        }

        TripParams tripParams = new TripParams();
        tripParams.setNumAdults(numAdults);
        tripParams.setNumChildren(numChildren);
        tripParams.setArrivalDate(checkinDate);
        tripParams.setDepartureDate(checkoutDate);
        itinerary.setTripParams(tripParams);

        if (transformUpdate == true) {
            RoomReservationBasic roomReservationBasic = new RoomReservationBasic();
            roomReservationBasic.setNumAdults(numAdults);
            roomReservationBasic.setNumChildren(numChildren);
            roomReservationBasic.setNumRooms(numRooms);
            roomReservationBasic.setCheckInDate(checkinDate);
            roomReservationBasic.setCheckOutDate(checkoutDate);
            roomReservationBasic.setConfirmationNumber(roomReservationResponse.getConfirmationNumber());
            roomReservationBasic.setPropertyId(roomReservationResponse.getPropertyId());
            roomReservationBasic.setRoomTypeId(roomReservationResponse.getRoomTypeId());
            if (roomReservationResponse.getState() != null) {
                roomReservationBasic.setState(roomReservationResponse.getState().toString().toUpperCase());
            }
            roomReservationBasic.setNrgStatus(roomReservationResponse.isNrgStatus());
            itinerary.setRoomReservationBasic(roomReservationBasic);
        }
        request.setItinerary(itinerary);

        return request;
    }


    public void updateCustomerItinerary(RoomReservationV2Response roomReservationResponse) {
        String itineraryId = "";
        transformUpdate = true;
        try {
            ItineraryServiceRequest itineraryRequest = transform(roomReservationResponse, transformUpdate);
            itineraryId = roomReservationResponse.getItineraryId();
            itineraryDao.updateCustomerItinerary(itineraryId, itineraryRequest);
        } catch (HttpClientErrorException ex) {
            // Unable to send itinerary request, logging 4xx failures separately
            log.error("Recieved error response while calling Itinerary service for ItineraryId({}): {}", itineraryId,
                    ex.getResponseBodyAsString());
            log.error("Unable to send request to Itinerary service for ItineraryId({}): ", itineraryId, ex);
        } catch (Exception ex) {
            // Unable to send itinerary request, shouldn't be hard failure
            log.error("Unable to send request to Itinerary service for ItineraryId({}): ", itineraryId, ex);
        }
    }

    public String createCustomerItinerary(RoomReservationV2Response roomReservationResponse) {
        String itineraryId = "";
        transformUpdate = false;
        try {
            ItineraryServiceRequest itineraryRequest = transform(roomReservationResponse, transformUpdate);
            itineraryId = itineraryDao.createCustomerItinerary(itineraryRequest);
        } catch (HttpClientErrorException ex) {
            // Unable to send itinerary request, logging 4xx failures separately
            log.error("Recieved error response while calling create Itinerary for ItineraryId({}): {}", itineraryId,
                    ex.getResponseBodyAsString());
            log.error("Unable to send request to create Itinerary for ItineraryId({}): ", itineraryId, ex);
        } catch (Exception ex) {
            // Unable to send itinerary request, shouldn't be hard failure
            log.error("Unable to send request to create Itinerary for ItineraryId({}): ", itineraryId, ex);
        }
        return itineraryId;
    }

    @Override
    public ItineraryResponse getCustomerItineraryByConfirmationNumber(String confirmationNumber) {
        return itineraryDao.retreiveCustomerItineraryDetailsByConfirmationNumber(confirmationNumber);
    }
    @Override
    public void createOrUpdateCustomerItinerary(RoomReservationV2Response primaryResv,List<RoomReservation> sharedReservationResponses){
        // For GSE property itinerary sync is handled by GSE.
        //We have to take care only for ACRS
        if (referenceDataDAOHelper.isPropertyManagedByAcrs(primaryResv.getPropertyId())) {
            // update primary
            updateCustomerItinerary(primaryResv);
            //create and update Itinerary for shareWith Reservations
            if (CollectionUtils.isNotEmpty(sharedReservationResponses)) {
                List<RoomReservationV2Response> sharedReservationResponseV2s = sharedReservationResponses.stream().map(sharedReservation -> responseMapper.roomReservationModelToResponse(sharedReservation)).collect(Collectors.toList());
                //create and update Itinerary for shareWith Reservations
                createUpdateCustomerItineraryForSharedResv(sharedReservationResponseV2s);
            }
        }
    }


    private void createUpdateCustomerItineraryForSharedResv(List<RoomReservationV2Response> sharedReservations) {
        if (CollectionUtils.isNotEmpty(sharedReservations)) {
            try {
                sharedReservations.stream()
                        .forEach(roomReservationResponse -> {
                            ItineraryResponse itineraryDetails = getCustomerItineraryByConfirmationNumber(roomReservationResponse.getConfirmationNumber());
                            String itineraryId = null != itineraryDetails ? itineraryDetails.getItinerary().getItineraryId()
                                    : ServiceConstant.EMPTY_STRING;
                            // Only primary reservation itineraryId will be there into the request.
                            //From ACRS we will not be getting itineraryId.
                            // For existing shared reservation existing itineraryId will be used for modify.
                            // for new shared withs reservation new itineraryId is to be created.
                            if (StringUtils.isNotEmpty(itineraryId)) {
                                roomReservationResponse
                                        .setItineraryId(itineraryId);
                            } else {
                                roomReservationResponse
                                        .setItineraryId(createCustomerItinerary(roomReservationResponse));
                            }
                        });
                sharedReservations.stream().forEach(
                      roomReservationResponse -> updateCustomerItinerary(roomReservationResponse));
            } catch (HttpClientErrorException ex){
                log.error("Received error response while calling  Itinerary : {}",
                        ex.getResponseBodyAsString());
                log.error("Unable to send request to Itinerary : ",  ex);
            }catch (Exception e) {
                log.error("Unable to send request to Itinerary : ",  e);
            }
        }
    }
}
