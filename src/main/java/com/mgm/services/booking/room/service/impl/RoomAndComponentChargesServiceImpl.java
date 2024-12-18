package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.helper.ReservationDAOHelper;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.inventory.InventoryGetRes;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mgm.services.booking.room.dao.RoomAndComponentChargesDAO;
import com.mgm.services.booking.room.mapper.CalculateRoomChargesRequestMapper;
import com.mgm.services.booking.room.mapper.CalculateRoomChargesResponseMapper;
import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.RoomAndComponentChargesService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation class to provide method for serving room and component charges
 * request.
 * 
 * @author uttam
 *
 */
@Service
@Log4j2
public class RoomAndComponentChargesServiceImpl implements RoomAndComponentChargesService {

    @Autowired
    private RoomAndComponentChargesDAO roomReservationChargesDAO;
    
    @Autowired
    private CalculateRoomChargesRequestMapper calculateRoomChargesRequestMapper;

    @Autowired
    private CalculateRoomChargesResponseMapper calculateRoomChargesResponseMapper;

    @Autowired
    private RoomProgramService programService;

    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private ServiceConversionHelper serviceConversionHelper;

    @Autowired
    private com.mgm.services.booking.room.dao.ProductInventoryDAO productInventoryDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ReservationDAOHelper reservationDAOHelper;
    
    @Autowired
    private ReservationServiceHelper reservationServiceHelper;

    @Autowired
    private CommonServiceImpl commonServiceImpl;

    @Override
    public CalculateRoomChargesResponse calculateRoomAndComponentCharges(
            CalculateRoomChargesRequest calculateRoomChargesRequest) {

        serviceConversionHelper.convertGuids(calculateRoomChargesRequest);
        boolean isF1InventoryAvailable = false;
        InventoryGetRes inventoryGetRes = new InventoryGetRes();
        String grandStandName = null;

        RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
                .getRoomProgramValidateRequest(calculateRoomChargesRequest);

        RoomProgramValidateResponse validateResponse = programService.validateProgramV2(validateRequest);

        if (validateResponse.isMyvegas() && StringUtils.isEmpty(calculateRoomChargesRequest.getRedemptionCode())
                && !appProps.getBypassMyvegasChannels().contains(CommonUtil.getChannelHeader())) {
            throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
        }

        //Invoke Inventory service for F1 packages
        if (validateResponse.isF1Package()) {
            Optional<String> productCode = appProps.getValidF1ProductCodes().stream().filter(x -> validateResponse.getRatePlanTags().contains(x)).findFirst();
            if (productCode.isPresent()) {
                String pCode = productCode.get();
                pCode = ReservationUtil.getProductCodeForF1Program(pCode, validateResponse.getRatePlanTags().stream().collect(Collectors.toList()),appProps);
                inventoryGetRes = productInventoryDAO.getInventory(pCode, false);
                int ticketCount = ReservationUtil.getF1TicketCountFromF1Tag(validateResponse.getRatePlanTags(), appProps);
                isF1InventoryAvailable = ReservationUtil.checkInventoryAvailability(inventoryGetRes, ticketCount);
                grandStandName = StringUtils.right(pCode, 5);
            }
        }

        RoomReservation roomReservation = calculateRoomChargesRequestMapper.calculateRoomChargesRequestToModel(calculateRoomChargesRequest);
        //Changes for CBSR-672 for enableJwb flow for ACRS
        if(null != calculateRoomChargesRequest.getMlifeNumber()) {
        	ReservationProfile reservationProfile = roomReservation.getProfile();
        	if(reservationProfile == null) {
        		reservationProfile = new ReservationProfile();
        	}
        	reservationProfile.setMlifeNo(Integer.parseInt(calculateRoomChargesRequest.getMlifeNumber()));
        	roomReservation.setProfile(reservationProfile);
        }
        log.info("Calling DAO to calculate room and component charges.");
        roomReservation = roomReservationChargesDAO.calculateRoomAndComponentCharges(roomReservation);

        log.info("Retrieved  room and component charges from DAO layer.");

        CalculateRoomChargesResponse response = calculateRoomChargesResponseMapper.reservationModelToCalculateRoomChargesResponseMapper(roomReservation);

        //CBSR-1657: Update Main ProgramID based on perpetual pricing request and response flag
        if (calculateRoomChargesRequest.isPerpetualPricing()
                && !response.isPerpetualPricing() && CollectionUtils.isNotEmpty(response.getBookings())) {
            String dominantProgramId = reservationDAOHelper.
                    findDominantProgram(response.getBookings());
            log.info("Dominant ProgramId in Hold Response : {}", dominantProgramId);
            if (StringUtils.isNotEmpty(dominantProgramId)) {
                response.setProgramId(dominantProgramId);
            }
        }

        //Invoke Inventory Hold service for F1 packages
        if (isF1InventoryAvailable) {
            productInventoryDAO.holdInventory(ReservationUtil.createHoldInventoryRequest(inventoryGetRes, calculateRoomChargesRequest.getHoldId()));
            response.setF1Package(true);
            addF1CasinoDefaultComponentPrices(response, validateResponse.getRatePlanTags(),
                    calculateRoomChargesRequest.getMlifeNumber(),
                    calculateRoomChargesRequest.getSource());
            addF1PublicTicketComponentPricesToRateSummary(response, validateResponse.getRatePlanTags(),
                    calculateRoomChargesRequest.getMlifeNumber(),
                    calculateRoomChargesRequest.getSource(), calculateRoomChargesRequest.getPropertyId());
            ReservationUtil.f1ResponseComponentUpdates(response, grandStandName, appProps, calculateRoomChargesRequest.getPropertyId(),
                    validateResponse.getRatePlanTags());
        } else {
            if(CollectionUtils.isNotEmpty(response.getAvailableComponents())) {
                response.getAvailableComponents().removeIf(x -> null != x.getCode() &&
                        (x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1)
                                || x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_HDN) ||
                                appProps.getTcolvF1ComponentCodes().contains(x.getCode())));
            }
        }
        //CBSR-2632 - package2.0-add specialRequest list from request to response and filter out package component from available component list
        response.setSpecialRequests(calculateRoomChargesRequest.getSpecialRequests());
        response.setAvailableComponents(filterPkgComponents(response.getAvailableComponents(), response.getPropertyId()));
        response.setPkgComponents(calculateRoomChargesRequest.getPkgComponents());
        return response;
    }

    private void addF1CasinoDefaultComponentPrices(CalculateRoomChargesResponse response,
                                                   List<String> ratePlanTags, String mlifeNumber, String source) {
        String componentCode = ReservationUtil.getF1DefaultCasinoComponentCode(ratePlanTags);
        if (StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
            RoomComponent roomComponent = componentDAO.getRoomComponentByCode(response.getPropertyId(),
                    componentCode, response.getRoomTypeId(), response.getProgramId(),
                    response.getTripDetails().getCheckInDate(), response.getTripDetails().getCheckOutDate(),
                    mlifeNumber, source);
            if (null != roomComponent && StringUtils.isNotEmpty(roomComponent.getId())) {
                Float updatedPrice = ReservationUtil.getRoomComponentPrice(roomComponent,
                        response.getTripDetails().getCheckInDate(), response.getTripDetails().getCheckOutDate());
                // update price in daily rates
                response.getBookings().forEach(x -> {
                    if (x.isComp()) {
                        x.setCustomerPrice(x.getPrice());
                        x.setPrice(0.0);
                    }
                    BigDecimal bd = new BigDecimal(x.getPrice() + updatedPrice).setScale(2, RoundingMode.HALF_UP);
                    x.setPrice(bd.doubleValue());
                    BigDecimal bd1 = new BigDecimal(x.getBasePrice() + updatedPrice).setScale(2, RoundingMode.HALF_UP);
                    x.setBasePrice(bd1.doubleValue());
                    x.setComp(false);
                    x.setDiscounted(x.getPrice() < x.getBasePrice());
                });
                // remove the default F1 component
                response.getAvailableComponents().removeIf(x -> (null != x.getId() && x.getId().equalsIgnoreCase(roomComponent.getId())));
                response.getRatesSummary().setRoomSubtotal(response.getRatesSummary().getRoomSubtotal() + roomComponent.getPrice());
                response.getRatesSummary().setDiscountedSubtotal(response.getRatesSummary().getDiscountedSubtotal() + roomComponent.getPrice());
                response.getRatesSummary().setReservationTotal(response.getRatesSummary().getReservationTotal() + roomComponent.getPrice());
                response.getRatesSummary().setDepositDue(response.getRatesSummary().getDepositDue() + roomComponent.getPrice());
                response.getRatesSummary().setTripSubtotal(response.getRatesSummary().getTripSubtotal() + roomComponent.getPrice());
            }
        }
    }

    private void addF1PublicTicketComponentPricesToRateSummary(CalculateRoomChargesResponse response, List<String> ratePlanTags,
                                                               String mlifeNumber, String source, String propertyId) {
        String componentCode = ReservationUtil.getF1DefaultPublicTicketComponentCode(ratePlanTags);
        if (StringUtils.isNotEmpty(propertyId) && StringUtils.equalsIgnoreCase(appProps.getTcolvPropertyId(), propertyId)) {
            componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(ratePlanTags));
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(componentCode)) {
            //int ticketCount = ReservationUtil.getF1TicketCountFromF1Tag(ratePlanTags, appProps);
            double ticketComponentPrice = 0.0;
            RoomComponent component = componentDAO.getRoomComponentByCode(response.getPropertyId(),
                    componentCode, response.getRoomTypeId(), response.getProgramId(),
                    response.getTripDetails().getCheckInDate(), response.getTripDetails().getCheckOutDate(),
                    mlifeNumber, source);
            if (null != component && null != component.getPrice()) {
                ticketComponentPrice += component.getPrice();
            }
        /*if (ticketCount > 1) {
            String additionalTicketComponentCode = ReservationUtil.getF1AdditionalPublicTicketComponentCode(ratePlanTags, appProps);
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(additionalTicketComponentCode)) {
                RoomComponent additionalTicketComponent = componentDAO.getRoomComponentByCode(response.getPropertyId(),
                        additionalTicketComponentCode, response.getRoomTypeId(), response.getProgramId(),
                        response.getTripDetails().getCheckInDate(), response.getTripDetails().getCheckOutDate(),
                        mlifeNumber, source);
                if (null != additionalTicketComponent && null != additionalTicketComponent.getPrice()) {
                    ticketComponentPrice += additionalTicketComponent.getPrice();
                }
            }
        }*/
            response.getRatesSummary().setTripSubtotal(response.getRatesSummary().getTripSubtotal() + ticketComponentPrice);
        }
    }
    
    private List<RoomBookingComponent> filterPkgComponents(List<RoomBookingComponent> availableComponents, String propertyId) {
		List<RoomBookingComponent> componentWithoutPkgComponents = Collections.emptyList();
        List<String> pkgComponentCodes = reservationServiceHelper.getPkgComponentCodeByPropertyId(propertyId);
		if(CollectionUtils.isNotEmpty(availableComponents)) {
			componentWithoutPkgComponents = availableComponents.stream().filter(component -> !ReservationUtil.isPkgComponent(component.getId(), pkgComponentCodes)).collect(Collectors.toList());
		}
		return componentWithoutPkgComponents;
	}
}
