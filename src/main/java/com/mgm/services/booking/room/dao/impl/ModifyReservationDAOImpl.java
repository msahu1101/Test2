package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.ModifyReservationDAO;
import com.mgm.services.booking.room.dao.ModifyReservationDAOStrategy;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Log4j2
public class ModifyReservationDAOImpl extends BaseStrategyDAO implements ModifyReservationDAO {
    @Autowired
    private ModifyReservationDAOStrategyACRSImpl acrsStrategy;
    @Autowired
    private ModifyReservationDAOStrategyGSEImpl gseStrategy;
    @Autowired
    private FindReservationDAO findReservationDAO;

    @Override
    public RoomReservation preModifyReservation(PreModifyRequest preModifyRequest) {

        // Fetch Reservation to get propertyId to determine which strategy to use
        String reservationPropertyId = getPropertyIdFromReservationConfirmationNumber(preModifyRequest);

        ModifyReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(reservationPropertyId)) {
            strategy = acrsStrategy;
        }
        return preModifyReservation(preModifyRequest, strategy);
    }

    private RoomReservation preModifyReservation(PreModifyRequest preModifyRequest,
            ModifyReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("preModifyReservation", preModifyRequest.getConfirmationNumber(), strategy));
        return strategy.preModifyReservation(preModifyRequest);
    }

    @Override
    public RoomReservation modifyReservation(String source, RoomReservation reservation) {
        ModifyReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(reservation.getPropertyId())) {
            strategy = acrsStrategy;
        }
        return modifyReservation(source, reservation, strategy);
    }

    private RoomReservation modifyReservation(String source, RoomReservation reservation,
            ModifyReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("modifyReservation", reservation.getConfirmationNumber(), strategy));
        return strategy.modifyReservation(source, reservation);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ModifyReservationDAO#updateProfileInfo(com.
     * mgm. services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO)
     */
    @Override
    public RoomReservation updateProfileInfo(UpdateProfileInfoRequestDTO requestDTO) {
        ModifyReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(requestDTO.getPropertyId())) {
            strategy = acrsStrategy;
        }
        return updateProfileInfo(requestDTO, strategy);
    }

    private RoomReservation updateProfileInfo(UpdateProfileInfoRequestDTO requestDTO,
            ModifyReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("updateProfileInfo", requestDTO.getConfirmationNumber(), strategy));
        return strategy.updateProfileInfo(requestDTO);
    }

    @Override
    public RoomReservation modifyRoomReservationV2(RoomReservation reservation) {
        ModifyReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(reservation.getPropertyId())) {
            strategy = acrsStrategy;
        }
        return modifyRoomReservationV2(reservation, strategy);
    }

    private RoomReservation modifyRoomReservationV2(RoomReservation reservation,
            ModifyReservationDAOStrategy strategy) {
        log.debug(createStrategyLogEntry("modifyRoomReservationV2", reservation.getConfirmationNumber(), strategy));
        return strategy.modifyRoomReservationV2(reservation);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ModifyReservationDAO#preModifyReservation(
     * com.mgm.services.booking.room.model.request.PreModifyV2Request)
     */
    @Override
    public RoomReservation preModifyReservation(PreModifyV2Request preModifyRequest) {
        String reservationPropertyId = preModifyRequest.getPropertyId();
        if ( StringUtils.isEmpty(reservationPropertyId) ) {
            reservationPropertyId = getPropertyIdFromReservationConfirmationNumber(preModifyRequest);
        }
        ModifyReservationDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(reservationPropertyId)) {
            strategy = acrsStrategy;
        }
        return preModifyReservation(preModifyRequest, strategy);
    }

    private RoomReservation preModifyReservation(PreModifyV2Request preModifyRequest,
            ModifyReservationDAOStrategy strategy) {
        return strategy.preModifyReservation(preModifyRequest);
    }

    @Override
    public RoomReservation commitPaymentReservation(CommitPaymentDTO requestDTO) {
        return refundCommitReservation(requestDTO,acrsStrategy);
    }

    @Override
    public RoomReservation modifyPendingRoomReservationV2(RoomReservation reservation) {
        return modifyPendingRoomReservationV2(reservation, acrsStrategy);
    }
    private RoomReservation modifyPendingRoomReservationV2(RoomReservation reservation,ModifyReservationDAOStrategy strategy){
        return strategy.modifyPendingRoomReservationV2(reservation);
    }

    private RoomReservation refundCommitReservation(CommitPaymentDTO requestDTO,
                                                    ModifyReservationDAOStrategy strategy){
        return strategy.commitPaymentReservation(requestDTO);
    }

    private String createStrategyLogEntry(String method, String uniqueId, ModifyReservationDAOStrategy strategy) {
        String strategyString = (strategy instanceof ModifyReservationDAOStrategyGSEImpl) ? "GSEStrategy"
                : "ACRSStrategy";
        return "ModifyReservationDAOImpl > " + method + " | Conf#: " + uniqueId + " | " + strategyString;
    }

    private String getPropertyIdFromReservationConfirmationNumber(PreModifyRequest preModifyRequest) {
        RoomReservation fetchedReservation = findReservationDAO.findRoomReservation(transform(preModifyRequest));

        return Optional.ofNullable(fetchedReservation)
                .map(RoomReservation::getPropertyId)
                .orElse(null);
    }

    private FindReservationRequest transform(PreModifyRequest preModifyRequest) {
        FindReservationRequest findReservationRequest = new FindReservationRequest();
        findReservationRequest.setConfirmationNumber(preModifyRequest.getConfirmationNumber());
        findReservationRequest.setSource(preModifyRequest.getSource());
        findReservationRequest.setFirstName(preModifyRequest.getFirstName());
        findReservationRequest.setLastName(preModifyRequest.getLastName());

        return findReservationRequest;
    }

    private String getPropertyIdFromReservationConfirmationNumber(PreModifyV2Request preModifyV2Request) {
        RoomReservation fetchedReservation = findReservationDAO.findRoomReservation(transform(preModifyV2Request));

        return Optional.ofNullable(fetchedReservation)
                .map(RoomReservation::getPropertyId)
                .orElse(null);
    }

    private FindReservationV2Request transform(PreModifyV2Request preModifyV2Request) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(preModifyV2Request.getConfirmationNumber());
        findReservationV2Request.setSource(preModifyV2Request.getSource());
        findReservationV2Request.setCacheOnly(true);
        return findReservationV2Request;
    }
}