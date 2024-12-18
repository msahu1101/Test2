package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.dao.impl.AuroraBaseDAO;
import com.mgm.services.booking.room.model.HoldReservationBasicInfo;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.CommonService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.cache.impl.PropertyContentCacheServiceImpl;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.common.BookingLimitBasicBooking;
import com.mgmresorts.aurora.common.BookingLimitBasicReservation;
import com.mgmresorts.aurora.common.TripParams;
import com.mgmresorts.aurora.messages.IsBookingLimitAppliedRequest;
import com.mgmresorts.aurora.messages.IsBookingLimitAppliedResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.messages.MessageHeader;
import com.mgmresorts.aurora.service.EAuroraException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CommonServiceImpl extends AuroraBaseDAO implements CommonService {
    @Autowired
    private RoomProgramCacheService roomProgramCacheService;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Autowired
    private  ReferenceDataDAOHelper referenceDataDAOHelper;
    @Autowired
    private SecretsProperties secretProperties;
    @Autowired
    private PropertyContentCacheServiceImpl propertyContentCacheService;
    @Override
    public void checkBookingLimitApplied(RoomReservation roomReservation) {
        String checkBookingLimitStr = secretProperties.getSecretValue(String.format("rbs-checkbookinglimit-%s", applicationProperties.getRbsEnv()));
        boolean checkBookingLimit = StringUtils.isNotBlank(checkBookingLimitStr) && Boolean.parseBoolean(checkBookingLimitStr);
        if (checkBookingLimit) {
            boolean isPoFlow = isPOFlow(roomReservation);
            if (isPoFlow) {
                List<Property> properties = propertyContentCacheService.getPropertyByRegion("LV");
                Optional<String> isLVProperty = properties.stream().map(Property::getId).filter(pid -> roomReservation.getPropertyId().equalsIgnoreCase(pid)).findAny();
                if(isLVProperty.isPresent()) {
                    if (null != roomReservation.getProfile()) {
                        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                                .getRequest();
                        String correlationId = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
                        if (StringUtils.isBlank(correlationId)) {
                            correlationId = UUID.randomUUID().toString();
                            log.info("CorrelationId was not found in header, generated Id is: {}", correlationId);
                        }
                        List<HoldReservationBasicInfo> availableItemsInCart = roomReservation.getItemsInCart();
                        HoldReservationBasicInfo bookingLimitReservation = createBookingLimitReservation(roomReservation);
                        BookingLimitBasicReservation bookingLimitReservationGse = convertToBookingLimitBasicReservation(bookingLimitReservation);
                        BookingLimitBasicReservation[] availableItemsInCartGse = convertToBookingLimitBasicReservations(availableItemsInCart);
                        IsBookingLimitAppliedRequest request = MessageFactory.createIsBookingLimitAppliedRequest();
                        request.setMlifeNo(roomReservation.getProfile().getMlifeNo());
                        request.setCustomerId(roomReservation.getProfile().getId());
                        request.setBookingLimitReservation(bookingLimitReservationGse);
                        if (!CommonUtil.isUuid(roomReservation.getPropertyId())) {
                            String propertyGuid = referenceDataDAOHelper.retrieveGsePropertyID(roomReservation.getPropertyId());
                            request.setPropertyId(propertyGuid);
                        } else {
                            request.setPropertyId(roomReservation.getPropertyId());
                        }
                        request.setCartReservations(availableItemsInCartGse);
                        request.setExcludeModifyConfNo(roomReservation.getConfirmationNumber());
                        TripParams tripParams = new TripParams();
                        tripParams.setArrivalDate(roomReservation.getCheckInDate());
                        tripParams.setDepartureDate(roomReservation.getCheckOutDate());
                        tripParams.setNumAdults(roomReservation.getNumAdults() > 0 ? roomReservation.getNumAdults() : 1);
                        request.setTripParams(tripParams);
                        MessageHeader header = new MessageHeader();
                        header.setTransactionId(correlationId);
                        request.setHeader(header);
                        boolean isBookingLimitApplied = false;
                        try {
                            log.info("Sent the request to isBookingLimitApplied as : {}",
                                    request.toJsonString());
                            IsBookingLimitAppliedResponse isBookingLimitAppliedRes = getAuroraClient(roomReservation.getPropertyId())
                                    .isBookingLimitApplied(request);
                            if (null != isBookingLimitAppliedRes) {
                                log.info("Received Response from isBookingLimitApplied : {}",
                                        isBookingLimitAppliedRes.toJsonString());
                            }
                            isBookingLimitApplied = null != isBookingLimitAppliedRes && isBookingLimitAppliedRes.getIsApplicable();
                        } catch (EAuroraException exception) {
                            log.error("Error while calling isBookingLimitApplied for {} - {}", request.getMlifeNo(), exception.getMessage());
                        }
                        if (isBookingLimitApplied) {
                            log.error("Hold/Charge call got failed Due to Booking limit applied");
                            throw new BusinessException(ErrorCode.BOOKING_LIMIT_APPLIED, "Perpetual offer has already been used for these dates.");
                        }
                    } else {
                        throw new BusinessException(ErrorCode.INVALID_CUSTOMER, "Profile info is missing during check booking limit");
                    }
                }
            }
        }
    }
    private BookingLimitBasicReservation[] convertToBookingLimitBasicReservations(List<HoldReservationBasicInfo> holdReservationBasicInfos) {
        List<BookingLimitBasicReservation> list = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(holdReservationBasicInfos)){
            list = holdReservationBasicInfos.stream().map(this::convertToBookingLimitBasicReservation)
                    .collect(Collectors.toList());
        }
        BookingLimitBasicReservation[] bookingLimitBasicReservationArray = new BookingLimitBasicReservation[list.size()];
        list.toArray(bookingLimitBasicReservationArray);
        return bookingLimitBasicReservationArray;
    }

    private BookingLimitBasicReservation convertToBookingLimitBasicReservation(HoldReservationBasicInfo holdReservationBasicInfo) {
        BookingLimitBasicReservation reservation = new BookingLimitBasicReservation();
        reservation.setBookings(convertToBookings(holdReservationBasicInfo.getBookings()));
        reservation.setPropertyId(holdReservationBasicInfo.getPropertyId());
        reservation.setNumberOfRoom(holdReservationBasicInfo.getTripDetails().getNumRooms());
        return reservation;
    }

    private BookingLimitBasicBooking[] convertToBookings(List<RoomPrice> bookings) {
        List<BookingLimitBasicBooking> list = bookings.stream().map(this::convertToBasicBooking).collect(Collectors.toList());
        BookingLimitBasicBooking[] bookingLimitBasicBookingArray = new BookingLimitBasicBooking[list.size()];
        list.toArray(bookingLimitBasicBookingArray);
        return bookingLimitBasicBookingArray;
    }
    private BookingLimitBasicBooking convertToBasicBooking(RoomPrice roomPrice) {
        BookingLimitBasicBooking booking = new BookingLimitBasicBooking();
        booking.setDate(roomPrice.getDate());
        booking.setProgramId(roomPrice.getProgramId());
        booking.setPricingRuleId(roomPrice.getPricingRuleId());
        if(!CommonUtil.isUuid(roomPrice.getProgramId())){
            String ratePlanCode = ACRSConversionUtil.getRatePlanCode(roomPrice.getProgramId());
            if(null == ratePlanCode){
                ratePlanCode = roomPrice.getProgramId();
            }
            if(ratePlanCode.startsWith(ServiceConstant.CASH)){
                booking.setIsCash(true);
            }
            if(ratePlanCode.startsWith(ServiceConstant.COMP)){
                booking.setIsComp(true);
            }
        }else{
            booking.setIsComp(roomPrice.isComp());
        }
        return booking;
    }
    private HoldReservationBasicInfo createBookingLimitReservation(RoomReservation roomReservation) {
        HoldReservationBasicInfo basicInfo = new HoldReservationBasicInfo();
        basicInfo.setPropertyId(roomReservation.getPropertyId());
        basicInfo.setBookings(roomReservation.getBookings());
        TripDetailsRequest tripDetails = new TripDetailsRequest();
        tripDetails.setCheckInDate(roomReservation.getCheckInDate());
        tripDetails.setCheckOutDate(roomReservation.getCheckOutDate());
        tripDetails.setNumRooms(roomReservation.getNumRooms());
        basicInfo.setTripDetails(tripDetails);
        basicInfo.setMlifeNumber(roomReservation.getProfile().getMlifeNo());
        basicInfo.setCustomerId(roomReservation.getProfile().getId());
        return  basicInfo;
    }

    private boolean isPOFlow(RoomReservation roomReservation) {
        return ( null != roomReservation.getProfile() &&  roomReservation.getProfile().getMlifeNo() > 0 && isPOReservation(roomReservation));
    }

    private boolean isPOReservation(RoomReservation roomReservation) {
        if (CollectionUtils.isNotEmpty(roomReservation.getBookings())) {
            return roomReservation.getBookings().stream().map(RoomPrice::getProgramId).filter(this::isPOProgram).findAny().isPresent();
        } else {
            return isPOProgram(roomReservation.getProgramId());
        }
    }
    private boolean isPOProgram(String programId){
        if(CommonUtil.isUuid(programId)){
            return roomProgramCacheService.isProgramPO(programId);
        }
        else{
            return isACRSPOProgram(programId);
        }
    }
    private boolean isACRSPOProgram(String programId){
        String programCode = ACRSConversionUtil.getRatePlanCode(programId);
        if(programCode.startsWith(ServiceConstant.COMP) ||  programCode.startsWith(ServiceConstant.CASH)){
            return true;
        }
        else
            return false;
    }
}
