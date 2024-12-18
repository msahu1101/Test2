package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import java.util.Collections;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.RoomReservationBasicInfoRequest;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.GetRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.service.IDUtilityService;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation class for FindReservationService
 */
@Component
@Log4j2
public class FindReservationServiceImpl implements FindReservationService {
    @Nullable
    @Autowired
    private FindReservationDAO findReservationDao;

    @Autowired
    private IDUtilityService idUtilityService;

    @Autowired
    private RoomReservationResponseMapper roomReservationResponseMapper;

    @Autowired
    private FindReservationServiceHelper findReservationServiceHelper;

    @Autowired
    ApplicationProperties appProperties;

    @Autowired
    IDMSTokenDAO idmsTokenDAO;
    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    @Autowired
    private ReservationServiceHelper reservationServiceHelper;


    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.service.FindReservationService#
     * findRoomReservation(com.mgm.services.booking.room.model.request.
     * FindReservationRequest)
     */
    @Override
    public RoomReservation findRoomReservation(FindReservationRequest reservationRequest) {

        return findReservationDao.findRoomReservation(reservationRequest);
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.service.FindReservationService#findRoomReservation(com.mgm.services.booking.room.model.request.FindReservationV2Request)
     */
    @Override
    public RoomReservation findRoomReservation(FindReservationV2Request reservationRequest, boolean validate) {
        RoomReservation roomReservation = findReservationDao.findRoomReservation(reservationRequest);

        // reservation will be returned in one of the following cases, otherwise
        // exception will be thrown
        // 1. request has an elevated access (for clients like ICE)
        // 2. firstName and lastName in the request and reservation are matching (fuzzy match)
        // 3. mlife number in the JWT token and reservation are matching
        // 4. mgmId in the JWT token and Ocrs reservation are matching
        if (validate && !isFirstNameLastNameMatching(reservationRequest, roomReservation)
                && !findReservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.GET_RESERVATION_ELEVATED.getValue())
                && !isMlifeNumMatching(roomReservation) && !isMgmIdMatching(roomReservation)) {
            log.info("Does not met any critiera to return the reservation");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        return roomReservation;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.service.FindReservationService#
     * findRoomReservation(com.mgm.services.booking.room.model.request.
     * FindReservationV2Request)
     */
    @Override
    public GetRoomReservationResponse findRoomReservationResponse(FindReservationV2Request reservationRequest) {
        RoomReservation roomReservation = findRoomReservation(reservationRequest, true);

        GetRoomReservationResponse response = new GetRoomReservationResponse();
        RoomReservationV2Response roomReservationV2Response = roomReservationResponseMapper
                .roomReservationModelToResponse(roomReservation);

        // Past reservation or cancelled resv may have this as null
        if (null == roomReservationV2Response.getDepositDetails()) {
            roomReservationV2Response.setDepositDetails(new Deposit());
        }
        // Populate miscellaneous fields
        roomReservationV2Response.getDepositDetails()
                .setRefundAmount(ReservationUtil.getRefundAmount(roomReservation, appProperties));
        roomReservationV2Response.getDepositDetails()
                .setCalculatedForefeitAmount(ReservationUtil.getForfeitAmount(roomReservation, appProperties));

        //For F1 packages
        if (roomReservation.isF1Package()) {
            f1Updates(roomReservation, roomReservationV2Response,
                    reservationRequest.getMlifeNumber(), reservationRequest.getSource());
        }
        //If channel is not ICE then remove special request and room features in response
        filterOutRoomFeatureNSpecialRequests( roomReservationV2Response,reservationRequest.getSource());
        //package2.0 - Check purchaseComponent list and set nonEditable flag and isPkgComponent flag to true for package components
        if(CollectionUtils.isNotEmpty(roomReservationV2Response.getPurchasedComponents())) {
            List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                    roomReservationV2Response.getPropertyId(),
                    roomReservationV2Response.getPurchasedComponents()
            );
            roomReservationV2Response.setPurchasedComponents(updatedPurchasedComponents);
        }
        response.setRoomReservation(roomReservationV2Response);
        return response;
    }

    private void filterOutRoomFeatureNSpecialRequests( RoomReservationV2Response roomReservationV2Response, String source) {
        if(!ServiceConstant.ICE.equalsIgnoreCase(source)){
            if(CollectionUtils.isNotEmpty(roomReservationV2Response.getPurchasedComponents())) {
                List<PurchasedComponent> existingPurchasedAddons = null;
                List<String> existingAddonsIds = null;
                if(referenceDataDAOHelper.isPropertyManagedByAcrs(roomReservationV2Response.getPropertyId())){
                    //ACRS Addons component will be having ratePlanCode. Room feature and special will not be having ratePlanCode. Based on ratePlanCode we can filter
                    existingPurchasedAddons = roomReservationV2Response.getPurchasedComponents().stream().filter(p -> StringUtils.isNotBlank(p.getRatePlanCode())).collect(Collectors.toList());

                }else{
                    //for GSE
                    existingPurchasedAddons =  roomReservationV2Response.getPurchasedComponents().stream().filter( p->{
                        RoomComponent roomRequest = componentDAO.getRoomComponentById(p.getId(), roomReservationV2Response.getPropertyId());
                       if(null != roomRequest){
                            return ServiceConstant.COMPONENT_STR.equalsIgnoreCase(roomRequest.getComponentType());
                        }else{
                           return false;
                        }
                    }).collect(Collectors.toList());
                }
                existingAddonsIds = existingPurchasedAddons.stream().map(PurchasedComponent::getId).collect(Collectors.toList());
                //Update the response with only Addons
                roomReservationV2Response.setSpecialRequests(existingAddonsIds);
                roomReservationV2Response.setPurchasedComponents(existingPurchasedAddons);
            }
        }
    }

    private boolean isMlifeNumMatching(RoomReservation roomReservation) {
        return null != roomReservation && null != roomReservation.getProfile()
                && findReservationServiceHelper.isMlifeNumMatching(roomReservation.getProfile().getMlifeNo());
    }

    private boolean isFirstNameLastNameMatching(FindReservationV2Request reservationRequest,
            RoomReservation roomReservation) {
        return StringUtils.isNotEmpty(reservationRequest.getFirstName())
                && StringUtils.isNotEmpty(reservationRequest.getLastName()) && null != roomReservation
                && idUtilityService.isFirstNameLastNameMatching(roomReservation.getProfile(),
                        reservationRequest.getFirstName(), reservationRequest.getLastName());
    }

    private boolean isMgmIdMatching(RoomReservation roomReservation) {
        boolean isMatching = false;
        if (null != roomReservation && findReservationServiceHelper.isTokenHasMgmId() && StringUtils.isNotEmpty(roomReservation.getProfile()
                .getMgmId())) {
            isMatching = findReservationServiceHelper.isMgmIdMatching(roomReservation.getProfile()
                    .getMgmId());
        }
        return isMatching;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.PartyReservationService#
     * getReservationBasicInfoList(RoomReservationBasicInfoRequest)
     */
    @Override
    public ReservationsBasicInfoResponse getReservationBasicInfoList(RoomReservationBasicInfoRequest request) {
        return findReservationDao.getRoomReservationsBasicInfoList(
                RoomReservationTransformer.buildSourceRoomReservationBasicInfoRequest(request));
    }

    private void addF1CasinoDefaultComponentPrices(RoomReservationV2Response roomReservationV2Response,
                                                   List<String> ratePlanTags, String mlifeNumber,
                                                   String source) {
        String componentCode = ReservationUtil.getF1DefaultCasinoComponentCode(ratePlanTags);
        if (StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
            RoomComponent roomComponent = componentDAO.getRoomComponentByCode(roomReservationV2Response.getPropertyId(),
                    componentCode, roomReservationV2Response.getRoomTypeId(), roomReservationV2Response.getProgramId(),
                    roomReservationV2Response.getTripDetails().getCheckInDate(), roomReservationV2Response.getTripDetails().getCheckOutDate(),
                    mlifeNumber, source);
            if (null != roomComponent && org.apache.commons.lang.StringUtils.isNotEmpty(roomComponent.getId())) {
                Float updatedPrice = ReservationUtil.getRoomComponentPrice(roomComponent,
                        roomReservationV2Response.getTripDetails().getCheckInDate(), roomReservationV2Response.getTripDetails().getCheckOutDate());
                // update price in daily rates
                roomReservationV2Response.getBookings().forEach(x -> {
                    if (x.isComp()) {
                        x.setPrice(0.0);
                    }
                    BigDecimal bd = new BigDecimal(x.getPrice() + updatedPrice).setScale(2, RoundingMode.HALF_UP);
                    x.setPrice(bd.doubleValue());
                    x.setComp(false);
                    BigDecimal bd1 = new BigDecimal(x.getBasePrice() + updatedPrice).setScale(2, RoundingMode.HALF_UP);
                    x.setBasePrice(bd1.doubleValue());
                    x.setDiscounted(x.getPrice() < x.getBasePrice());
                });
                // remove the default F1 component
                roomReservationV2Response.getPurchasedComponents().removeIf(x -> x.getId().equalsIgnoreCase(roomComponent.getId()));
                roomReservationV2Response.getSpecialRequests().removeIf(x -> x.equalsIgnoreCase(roomComponent.getId()));
                ReservationUtil.updateRatesSummary(roomReservationV2Response, roomComponent, updatedPrice);
            }
        }
    }

    private void f1Updates(RoomReservation roomReservation,
                           RoomReservationV2Response roomReservationV2Response,
                           String mlifeNumber, String source) {
        roomReservationV2Response.setF1Package(true);
        roomReservationV2Response.setIsStayDateModifiable(false);
        addF1CasinoDefaultComponentPrices(roomReservationV2Response, roomReservation.getRatePlanTags(),
                mlifeNumber, source);
        ReservationUtil.purchasedComponentsF1Updates(roomReservationV2Response, appProperties,
                roomReservation.getRatePlanTags(), false);
    }
}
