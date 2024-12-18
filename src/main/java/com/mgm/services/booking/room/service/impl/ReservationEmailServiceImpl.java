package com.mgm.services.booking.room.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.EmailDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.RoomContentDAO;
import com.mgm.services.booking.room.model.Email;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.BaseReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.ReservationEmailService;
import com.mgm.services.booking.room.service.cache.EmailCacheService;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.model.Message;

import freemarker.template.TemplateException;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for ReservationEmailService
 */
@Component
@Log4j2
public class ReservationEmailServiceImpl implements ReservationEmailService {

    private static final int MASK_LIMIT = 4;

    @Autowired
    private EmailCacheService emailCacheService;

    @Autowired
    private DomainProperties domainProperties;

    @Autowired
    private URLProperties urlProperties;

    @Autowired
    private RoomContentDAO roomContentDao;

    @Autowired
    private ProgramContentDAO programContentDao;

    @Autowired
    private PropertyContentCacheService propertyCacheService;

    @Autowired
    private EmailDAO emailDao;

    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    private ComponentServiceImpl componentService;

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.ReservationEmailService#
     * sendConfirmationEmail(com.mgm.services.booking.room.model.reservation.
     * RoomReservation,
     * com.mgm.services.booking.room.model.response.RoomReservationResponse)
     */
    @Override
    public void sendConfirmationEmail(RoomReservation reservation, BaseReservationResponse response,
			String emailPropertyId) {

		try {

			Payment payment = reservation.getPayments().get(0);

			Map<String, Object> actualContent = new HashMap<>();
			setCommonEmailElements(actualContent, reservation);
			actualContent.put(ServiceConstant.EMAIL_CUSTOMERCARDTYPE, payment.getChargeCardType());

			actualContent.put(ServiceConstant.EMAIL_CARDBILLING, StringUtils.EMPTY);
			actualContent.put(ServiceConstant.EMAIL_BILLING_START, StringUtils.EMPTY);
			actualContent.put(ServiceConstant.EMAIL_BILLING_END, StringUtils.EMPTY);
			if (null != payment.getChargeCardNumber()) {
				actualContent.put(ServiceConstant.EMAIL_CUSTOMERCARDNUM,
						StringUtils.right(payment.getChargeCardMaskedNumber(), MASK_LIMIT));
			}

			List<Map<String, Object>> reservations = new ArrayList<>();
			Map<String, Object> reservationContent = new HashMap<>();

			setCommonRoomContent(reservationContent, reservation, response);

			Email emailTemplate = emailCacheService.getConfirmationEmailTemplate(emailPropertyId);
			emailTemplate.setTo(reservation.getProfile().getEmailAddress1());

			setRoomMarketingContent(reservationContent, reservation);
			reservations.add(reservationContent);
			actualContent.put(ServiceConstant.EMAIL_ROOM_RESERVATIONS, reservations);
			sendEmail(emailTemplate, actualContent);

			log.info("EMail sent");
		} catch (Exception ex) {
			// Unable to send email shouldn't be hard failure
			log.error("PostReservationErrors - Unable to send email for booked reservation: ", ex);
			Message msg = new Message(ServiceConstant.MESSAGE_TYPE_WARN, "_email_failed", "Unable to send email");
			response.setMessages(Collections.singletonList(msg));
		}

	}

    private void setCommonEmailElements(Map<String, Object> actualContent, RoomReservation reservation) {

        actualContent.put(ServiceConstant.EMAIL_HOSTURL, domainProperties.getAem());
        actualContent.put(ServiceConstant.EMAIL_URI_SCHEME, "https");
        actualContent.put(ServiceConstant.EMAIL_CUSTOMERFNAME, reservation.getProfile().getFirstName());
        actualContent.put(ServiceConstant.EMAIL_PROPID, reservation.getPropertyId());
        actualContent.put(ServiceConstant.EMAIL_ROOM_CONFIRMATION, reservation.getConfirmationNumber());

        log.info("confirmation number {}", reservation.getConfirmationNumber());

    }

    private void setCommonRoomContent(Map<String, Object> reservationContent, RoomReservation reservation,
            BaseReservationResponse response) {
        reservationContent.put(ServiceConstant.EMAIL_CONFNUM, reservation.getConfirmationNumber());
        reservationContent.put(ServiceConstant.EMAIL_STAY_DURATION, response.getTripDetails().getNights());
        reservationContent.put(ServiceConstant.EMAIL_CHECKIN_DATE, reservation.getCheckInDate());
        reservationContent.put(ServiceConstant.EMAIL_CHECKOUT_DATE, reservation.getCheckOutDate());
        reservationContent.put(ServiceConstant.EMAIL_ROOM_RATE_TAX, CommonUtil.round(response.getRates().getRoomSubtotal()
                - response.getRates().getProgramDiscount() + response.getRates().getRoomChargeTax()));
        reservationContent.put(ServiceConstant.EMAIL_RESORT_FEE, CommonUtil.round(response.getRates().getResortFeeAndTax()));

        int stayDuration = response.getTripDetails().getNights();
        reservationContent.put(ServiceConstant.EMAIL_RESORT_FEE_AVG_PER_NIGHT,
                CommonUtil.round(response.getRates().getResortFee() / stayDuration));

        // populating these additional fields as per Borgata on boarding
        if (response.getRates().getOccupancyFee() != 0) {
            reservationContent.put(ServiceConstant.EMAIL_OCCUPANCY_FEE_AVG_PER_NIGHT,
                    CommonUtil.roundDown(response.getRates().getOccupancyFee() / stayDuration));
            reservationContent.put(ServiceConstant.EMAIL_OCCUPANCY_FEE_TOTAL,
                    CommonUtil.roundDown(response.getRates().getOccupancyFee()));
        }
        if (response.getRates().getTourismFeeAndTax() != 0) {
            reservationContent.put(ServiceConstant.EMAIL_TOURISM_FEE_AVG_PER_NIGHT,
                    CommonUtil.roundDown(response.getRates().getTourismFee() / stayDuration));
            reservationContent.put(ServiceConstant.EMAIL_TOURISM_FEE_TAX_TOTAL,
                    CommonUtil.roundDown(response.getRates().getTourismFeeAndTax()));
        }
        if (response.getRates().getCasinoSurchargeAndTax() != 0) {
            reservationContent.put(ServiceConstant.EMAIL_CASINO_SURCHARGE_AVG_PER_NIGHT,
                    CommonUtil.roundDown(response.getRates().getCasinoSurcharge() / stayDuration));
            reservationContent.put(ServiceConstant.EMAIL_CASINO_SURCHARGE_TAX_TOTAL,
                    CommonUtil.roundDown(response.getRates().getCasinoSurchargeAndTax()));
        }

        reservationContent.put(ServiceConstant.EMAIL_RES_TOTAL, CommonUtil.round(response.getRates().getReservationTotal()));
        reservationContent.put(ServiceConstant.EMAIL_AMT_PAID, CommonUtil.round(response.getRates().getDepositDue()));
        reservationContent.put(ServiceConstant.EMAIL_BALANCE_DUE, CommonUtil.round(response.getRates().getBalanceUponCheckIn()));

        if (response.getRates().getRoomRequestsTotal() > 0) {
            reservationContent.put(ServiceConstant.EMAIL_ROOM_REQUESTS, getRoomRequests(reservation));
            reservationContent.put(ServiceConstant.EMAIL_ROOM_REQUESTS_PRICE,
            		CommonUtil.round(response.getRates().getRoomRequestsTotal()));
        }

        final String timezone = appProperties.getTimezone(reservation.getPropertyId());
        final TimeZone tz = TimeZone.getTimeZone(timezone);
        String refundDate = CommonUtil.convertDateToString(ServiceConstant.DATE_FORMAT,
                CommonUtil.getCheckinTime(response.getFreeCancellationEndDate(), tz), tz);
        reservationContent.put(ServiceConstant.EMAIL_FREE_CANCEL_DATE, refundDate);

        ZoneId propertyZone = ZoneId.of(timezone);
        LocalDateTime propertyDate = LocalDateTime.now(propertyZone);
        // Find if the reservation is within forfeit period
        LocalDateTime forfeitDate = reservation.getDepositCalc().getForfeitDate().toInstant().atZone(propertyZone)
                .toLocalDateTime();
        if (forfeitDate.isBefore(propertyDate)) {
            log.info("Deposit Forfeit True");
            reservationContent.put(ServiceConstant.EMAIL_DEPOSIT_FORFEIT, true);
        }

        Property property = propertyCacheService.getProperty(response.getPropertyId());
        reservationContent.put(ServiceConstant.EMAIL_PROPNAME, property.getName());
        reservationContent.put(ServiceConstant.EMAIL_PHONENUM, property.getGeneralPhoneNumber());

        log.info("Property {}", property);

        reservationContent.put(ServiceConstant.EMAIL_ITINERARY_LINK, constructItineraryLink(reservation));
        reservationContent.put(ServiceConstant.EMAIL_RESERVE_PHONE, property.getReservationPhoneNumber());

    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setRoomMarketingContent(Map<String, Object> reservationContent,
            RoomReservation reservation) {

        if (StringUtils.isNotEmpty(reservation.getProgramId())) {

            Room room = roomContentDao.getRoomContent(reservation.getRoomTypeId());
            log.info("Room {}", room);

            Program program = programContentDao.getProgramContent(reservation.getPropertyId(),
                    reservation.getProgramId());
            log.info("Program {}", program);

            reservationContent.put(ServiceConstant.EMAIL_ROOMNAME, room.getName());
            reservationContent.put(ServiceConstant.EMAIL_DEFAULT_IMAGE, domainProperties.getAem() + room.getImage() + ".image.222.143.high.jpg");
            if (null != room.getAdaAttributes()) {
                reservationContent.put(ServiceConstant.EMAIL_ADA_COMPATIBLE, StringUtils.join(room.getAdaAttributes()));
            }

            reservationContent.put(ServiceConstant.EMAIL_OFFER_NAME, program.getName());
            reservationContent.put(ServiceConstant.EMAIL_OFFER_DESC, program.getShortDescription());
            reservationContent.put(ServiceConstant.EMAIL_OFFER_CODE, program.getPromoCode());
            reservationContent.put(ServiceConstant.EMAIL_PREPROMO_COPY, program.getPrepromotionalCopy());

            return reservationContent;

        } else {

            Room room = roomContentDao.getRoomContent(reservation.getRoomTypeId());

            log.info("Room {}", room);

            reservationContent.put(ServiceConstant.EMAIL_ROOMNAME, room.getName());
            reservationContent.put(ServiceConstant.EMAIL_DEFAULT_IMAGE, domainProperties.getAem() + room.getImage() + ".image.222.143.high.jpg");
            if (null != room.getAdaAttributes()) {
                reservationContent.put(ServiceConstant.EMAIL_ADA_COMPATIBLE, StringUtils.join(room.getAdaAttributes()));
            }

            return reservationContent;

        }

    }

    /**
     * Construct itinerary deeplink URL as per format defined by DMP to include
     * in the email for quick reservation lookup.
     * 
     * @param reservation
     *            Room reservation object
     * @return Returns the itinerary deep link URL to DMP
     */
    private String constructItineraryLink(RoomReservation reservation) {

        String urlParams = String.format("type=room+propertyId=%s+confirmationNumber=%s", reservation.getPropertyId(),
                reservation.getConfirmationNumber());

        String itineraryUrl = urlProperties.getItineraryDeepLink()
                .replace("{confNumber}", reservation.getConfirmationNumber())
                .replace("{firstName}", CommonUtil.urlEncode(reservation.getProfile().getFirstName()))
                .replace("{lastName}", CommonUtil.urlEncode(reservation.getProfile().getLastName()))
                .replace("{encryptedString}", CommonUtil.encryptAndCustomEncode(urlParams));

        return domainProperties.getAem() + itineraryUrl;
    }

    @SuppressWarnings("unchecked")
    private String getRoomRequests(RoomReservation reservation) {

        List<String> roomRequests = new ArrayList<>();
        reservation.getSpecialRequests().forEach(request -> {
            if (!appProperties.getBorgataSpecialRequests().contains(request)) {
                roomRequests
                        .add(componentService.getRoomComponent(reservation.getRoomTypeId(), request).getDescription());
            }
        });
        return StringUtils.join(roomRequests);
    }

    private void sendEmail(Email emailTemplate, Map<String, Object> actualContent) {
        try {
            log.info(actualContent);
            emailTemplate.setBody(CommonUtil.getFTLTransformedContent(emailTemplate.getBody(), actualContent));

        } catch (IOException | TemplateException e) {
            log.error("Error populating confirmation email: {}", e);
        }

        emailDao.sendEmail(emailTemplate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.ReservationEmailService#
     * sendCancellationEmail(com.mgm.services.booking.room.model.reservation.
     * RoomReservation,
     * com.mgm.services.booking.room.model.response.RoomReservationResponse)
     */
    @Override
    public void sendCancellationEmail(RoomReservation reservation, BaseReservationResponse response,
            String emailPropertyId) {

        Map<String, Object> actualContent = new HashMap<>();
        setCommonEmailElements(actualContent, reservation);

        actualContent.put(ServiceConstant.EMAIL_CANCEL_CONFIRMATION, reservation.getConfirmationNumber());
        actualContent.put(ServiceConstant.EMAIL_CANCEL_DATE, new Date());
        actualContent.put(ServiceConstant.EMAIL_AMT_PAID, reservation.getAmountDue());

        double refundAmount = ReservationUtil.getRefundAmount(reservation, appProperties);
        actualContent.put(ServiceConstant.EMAIL_AMT_REFUND, CommonUtil.round(refundAmount));

        Map<String, Object> reservationContent = new HashMap<>();

        setCommonRoomContent(reservationContent, reservation, response);
        reservationContent.put(ServiceConstant.EMAIL_AMT_REFUND, refundAmount);
        reservationContent.put(ServiceConstant.EMAIL_AMT_PAID, reservation.getAmountDue());

        List<Map<String, Object>> reservations = new ArrayList<>();
        
        Email emailTemplate = emailCacheService.getCancellationEmailTemplate(emailPropertyId);
        emailTemplate.setTo(reservation.getProfile().getEmailAddress1());

        setRoomMarketingContent(reservationContent, reservation);
        reservations.add(reservationContent);
        actualContent.put(ServiceConstant.EMAIL_ROOM_RESERVATIONS, reservations);
        sendEmail(emailTemplate, actualContent);
    }
}
