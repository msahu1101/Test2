package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.CancelReservationDAO;
import com.mgm.services.booking.room.dao.CancelReservationDAOStrategy;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
@Log4j2
public class CancelReservationDAOImpl extends BaseStrategyDAO implements CancelReservationDAO {
    private final CancelReservationDAOStrategyACRSImpl acrsStrategy;
    private final CancelReservationDAOStrategyGSEImpl gseStrategy;
    private final FindReservationDAO findReservationDAO;

    @Autowired
    public CancelReservationDAOImpl(CancelReservationDAOStrategyACRSImpl acrsStrategy,
                                    CancelReservationDAOStrategyGSEImpl gseStrategy,
                                    FindReservationDAO findReservationDAO,
                                    AcrsProperties acrsProperties,
                                    ReferenceDataDAOHelper referenceDataDAOHelper) {
        this.acrsStrategy = acrsStrategy;
        this.gseStrategy = gseStrategy;
        this.findReservationDAO = findReservationDAO;
        this.acrsProperties = acrsProperties;
        this.referenceDataDAOHelper = referenceDataDAOHelper;
    }

    @Override public RoomReservation cancelReservation(CancelRequest request) {
        CancelReservationDAOStrategy strategy = gseStrategy;

        // We don't have propertyId in CancelRequest, so extra step to fetch reservation is required.
        // TODO: Note that we can remove this step once GSE is retired and we are only using acrsStrategy
        String reservationPropertyId = getPropertyIdFromReservationConfirmationNumber(request);

        if( isPropertyManagedByAcrs(reservationPropertyId) ) {
            strategy = acrsStrategy;
        }
        return cancelReservation(request, strategy, reservationPropertyId);
    }

    private RoomReservation cancelReservation(CancelRequest request, CancelReservationDAOStrategy strategy, String propertyId){
        log.debug(createStrategyLogEntry("cancelReservation", request.getConfirmationNumber(), strategy));
        return strategy.cancelReservation(request, propertyId);
    }

    @Override
    public RoomReservation cancelReservation(CancelV2Request request) {
        CancelReservationDAOStrategy strategy = gseStrategy;

        // If propertyId is specified in the request then skip fetching reservation from backend
        String reservationPropertyId = request.getPropertyId();
        if ( StringUtils.isEmpty(reservationPropertyId) ) {
            reservationPropertyId = getPropertyIdFromReservationConfirmationNumber(request);
            request.setPropertyId(reservationPropertyId);
        }

        if( isPropertyManagedByAcrs(reservationPropertyId) ) {
            strategy = acrsStrategy;
        }
        return cancelReservation(request, strategy);
    }

    /**
     * This will be used for payment widget flow  for ACRS only
     * @param request
     * @return
     */
    @Override
    public RoomReservation cancelPreviewReservation(CancelV2Request request) {
        RoomReservation existingResv = request.getExistingReservation();
        // cancel pending
        try {
            return acrsStrategy.cancelPendingRoomReservation(request);
        } catch (ParseException  e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR,e);

        } catch (BusinessException ex){
            throw ex;
        }finally {
            //ignore cancel
            acrsStrategy.ignorePendingRoomReservation(request.getConfirmationNumber(),
                    existingResv.getPropertyId(),
                    request.getSource(),
                    existingResv.isPerpetualPricing());
        }

    }

    @Override
    public RoomReservation cancelCommitReservation(CancelV2Request request) {
        return acrsStrategy.cancelReservationV4(request);
    }

    private RoomReservation cancelReservation(CancelV2Request request, CancelReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("cancelReservationV2", request.getConfirmationNumber(), strategy));
        //V2 call
        return strategy.cancelReservation(request);
    }

    private String createStrategyLogEntry(String method, String uniqueId, CancelReservationDAOStrategy strategy) {
        String strategyString = (strategy instanceof CancelReservationDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "CancelReservationDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }

    @Override
    public boolean ignoreReservation(ReleaseV2Request request) {
        CancelReservationDAOStrategy strategy = gseStrategy;
        
        if( isPropertyManagedByAcrs(request.getPropertyId()) ) {
            strategy = acrsStrategy;
        }
        return ignoreReservation(request, strategy);
    }

    private boolean ignoreReservation(ReleaseV2Request request, CancelReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("cancelReservationV2", request.getConfirmationNumber(), strategy));        
        return strategy.ignoreReservation(request);
    }

    protected String getPropertyIdFromReservationConfirmationNumber(CancelRequest cancelRequest) {
        RoomReservation fetchedReservation = findReservationDAO.findRoomReservation(transform(cancelRequest));

        return (null == fetchedReservation) ? null : fetchedReservation.getPropertyId();
    }

    private FindReservationRequest transform(CancelRequest cancelRequest) {
        FindReservationRequest findReservationRequest = new FindReservationRequest();
        findReservationRequest.setConfirmationNumber(cancelRequest.getConfirmationNumber());
        findReservationRequest.setSource(cancelRequest.getSource());
        findReservationRequest.setFirstName(cancelRequest.getFirstName());
        findReservationRequest.setLastName(cancelRequest.getLastName());
        return findReservationRequest;
    }

    protected String getPropertyIdFromReservationConfirmationNumber(CancelV2Request cancelV2Request) {
        RoomReservation fetchedReservation = findReservationDAO.findRoomReservation(transform(cancelV2Request));

        return (null == fetchedReservation) ? null : fetchedReservation.getPropertyId();
    }

    private FindReservationV2Request transform(CancelV2Request cancelV2Request) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(cancelV2Request.getConfirmationNumber());
        findReservationV2Request.setSource(cancelV2Request.getSource());
        findReservationV2Request.setCacheOnly(true);
        return findReservationV2Request;
    }
}
