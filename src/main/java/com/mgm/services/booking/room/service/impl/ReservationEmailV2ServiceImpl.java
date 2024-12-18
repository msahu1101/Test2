package com.mgm.services.booking.room.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.cache.EmailCacheService;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;

import freemarker.template.TemplateException;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ReservationEmailV2ServiceImpl implements ReservationEmailV2Service {

	private static final int MASK_LIMIT = 4;

	@Autowired
	private EmailCacheService emailCacheService;

	@Autowired
	private DomainProperties domainProperties;

	@Autowired
	private ComponentServiceImpl componentService;

	@Autowired
	private ApplicationProperties appProperties;

	@Autowired
	private PropertyContentCacheService propertyCacheService;

	@Autowired
	private URLProperties urlProperties;

	@Autowired
	private RoomContentDAO roomContentDao;

	@Autowired
	private ProgramContentDAO programContentDao;

    @Autowired
    private Map<String, EmailDAO> emailDAOMap;
    
    @Value("${email.notificationService.enabled}")
    private boolean isNSForEmailEnabled;

    private static final String EMAIL_IMPL = "EmailDAOImpl";
    private static final String EMAIL_NS_IMPL = "EmailDAONSImpl";

	@Override
    public void sendConfirmationEmail(RoomReservation reservation, RoomReservationV2Response response, boolean isHdePackageReservation) {

        // spawning to async thread, so that we don't wait for it..
        CompletableFuture.runAsync(() -> {

            log.info("Sending confirmation email for reservation: {}", response.getConfirmationNumber());

            sendConfirmationEmailInternal(reservation, response, isHdePackageReservation);

            log.info("Confirmation email successfully sent reservation: {}", response.getConfirmationNumber());
        });
    }
	
    public void sendConfirmationEmailInternal(RoomReservation reservation, RoomReservationV2Response response, boolean isHDEPackageReservation) {

        try {
        	Email emailTemplate = new Email();
        	boolean isHDETemplateFound = false;
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
            if(isHDEPackageReservation) {
            	emailTemplate = emailCacheService.getHDEPackageConfirmationEmailTemplate(response.getPropertyId());
            	if(null!=emailTemplate) {
            		log.debug("Found HDE Package Email Template for property id: {}", response.getPropertyId());
            		isHDETemplateFound = true;
            	} else {
            		log.warn("HDE Package Email Template Not Found for property id: {}, Sending Regular Confirmation Email", response.getPropertyId());
            	}
            }
            if(!isHDETemplateFound) {
            	emailTemplate = emailCacheService.getConfirmationEmailTemplate(response.getPropertyId());
            }
            emailTemplate.setTo(reservation.getProfile().getEmailAddress1());
            log.debug("Found confirmation email template from the cache for property id:{}",
                    reservation.getPropertyId());

            setRoomMarketingContent(reservationContent, response);
            reservations.add(reservationContent);
            actualContent.put(ServiceConstant.EMAIL_ROOM_RESERVATIONS, reservations);
            sendEmail(emailTemplate, actualContent);

            log.info("Email sent");
        } catch (Exception ex) {
            // Unable to send email shouldn't be hard failure
            log.error("SendEmailFailure - Unable to send email for booked reservation: {}, itinerary: {} and exception: {}",
                    reservation.getItineraryId(), reservation.getItineraryId(), ExceptionUtils.getStackTrace(ex));
        }
    }

	@SuppressWarnings("unchecked")
	private Map<String, Object> setRoomMarketingContent(Map<String, Object> reservationContent,
			RoomReservationV2Response reservation) {

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

	private void setCommonEmailElements(Map<String, Object> actualContent, RoomReservation reservation) {

		actualContent.put(ServiceConstant.EMAIL_HOSTURL, domainProperties.getAem());
		actualContent.put(ServiceConstant.EMAIL_URI_SCHEME, "https");
		actualContent.put(ServiceConstant.EMAIL_CUSTOMERFNAME, reservation.getProfile().getFirstName());
		actualContent.put(ServiceConstant.EMAIL_PROPID, reservation.getPropertyId());
		actualContent.put(ServiceConstant.EMAIL_ROOM_CONFIRMATION, reservation.getConfirmationNumber());

		log.info("confirmation number {}", reservation.getConfirmationNumber());

	}

	private void setCommonRoomContent(Map<String, Object> reservationContent, RoomReservation reservation,
			RoomReservationV2Response response) {
		reservationContent.put(ServiceConstant.EMAIL_CONFNUM, reservation.getConfirmationNumber());
		reservationContent.put(ServiceConstant.EMAIL_STAY_DURATION, response.getBookings().size());
		reservationContent.put(ServiceConstant.EMAIL_CHECKIN_DATE, reservation.getCheckInDate());
		reservationContent.put(ServiceConstant.EMAIL_CHECKOUT_DATE, reservation.getCheckOutDate());
		reservationContent.put(ServiceConstant.EMAIL_ROOM_RATE_TAX, round(response.getRatesSummary().getRoomSubtotal()
				- response.getRatesSummary().getProgramDiscount() + response.getRatesSummary().getRoomChargeTax()));
		reservationContent.put(ServiceConstant.EMAIL_RESORT_FEE,
				round(response.getRatesSummary().getResortFeeAndTax()));

        int stayDuration = response.getBookings().size();
        reservationContent.put(ServiceConstant.EMAIL_RESORT_FEE_AVG_PER_NIGHT,
                round(response.getRatesSummary().getResortFee() / stayDuration));

        // populating these additional fields as per Borgata on boarding
        if (response.getRatesSummary().getOccupancyFee() != 0) {
            reservationContent.put(ServiceConstant.EMAIL_OCCUPANCY_FEE_AVG_PER_NIGHT,
                    round(response.getRatesSummary().getOccupancyFee() / stayDuration));
            reservationContent.put(ServiceConstant.EMAIL_OCCUPANCY_FEE_TOTAL,
                    round(response.getRatesSummary().getOccupancyFee()));
        }
        if (response.getRatesSummary().getTourismFeeAndTax() != 0) {
            reservationContent.put(ServiceConstant.EMAIL_TOURISM_FEE_AVG_PER_NIGHT,
                    round(response.getRatesSummary().getTourismFee() / stayDuration));
            reservationContent.put(ServiceConstant.EMAIL_TOURISM_FEE_TAX_TOTAL,
                    round(response.getRatesSummary().getTourismFeeAndTax()));
        }
		if (response.getRatesSummary().getCasinoSurchargeAndTax() != 0) {
			reservationContent.put(ServiceConstant.EMAIL_CASINO_SURCHARGE_AVG_PER_NIGHT,
					CommonUtil.roundDown(response.getRatesSummary().getCasinoSurcharge() / stayDuration));
			reservationContent.put(ServiceConstant.EMAIL_CASINO_SURCHARGE_TAX_TOTAL,
					CommonUtil.roundDown(response.getRatesSummary().getCasinoSurchargeAndTax()));
		}

		reservationContent.put(ServiceConstant.EMAIL_RES_TOTAL,
				round(response.getRatesSummary().getReservationTotal()));
		reservationContent.put(ServiceConstant.EMAIL_AMT_PAID, round(response.getRatesSummary().getDepositDue()));
		reservationContent.put(ServiceConstant.EMAIL_BALANCE_DUE,
				round(response.getRatesSummary().getBalanceUponCheckIn()));

		if (response.getRatesSummary().getRoomRequestsTotal() > 0) {
			reservationContent.put(ServiceConstant.EMAIL_ROOM_REQUESTS, getRoomRequests(response));
			reservationContent.put(ServiceConstant.EMAIL_ROOM_REQUESTS_PRICE,
					round(response.getRatesSummary().getRoomRequestsTotal()));
		}

		final String timezone = appProperties.getTimezone(reservation.getPropertyId());
		final TimeZone tz = TimeZone.getTimeZone(timezone);
		String refundDate = CommonUtil.convertDateToString(ServiceConstant.DATE_FORMAT,
				CommonUtil.getCheckinTime(reservation.getDepositCalc().getForfeitDate(), tz), tz);

		reservationContent.put(ServiceConstant.EMAIL_FREE_CANCEL_DATE, refundDate);
		if (response.isDepositForfeit()) {
			log.info("Deposit Forfeit True");
			reservationContent.put(ServiceConstant.EMAIL_DEPOSIT_FORFEIT, true);
		}

		Property property = propertyCacheService.getProperty(reservation.getPropertyId());
		reservationContent.put(ServiceConstant.EMAIL_PROPNAME, property.getName());
		reservationContent.put(ServiceConstant.EMAIL_PHONENUM, property.getGeneralPhoneNumber());

		log.info("Property {}", property);

		reservationContent.put(ServiceConstant.EMAIL_ITINERARY_LINK, constructItineraryLink(response));
		reservationContent.put(ServiceConstant.EMAIL_RESERVE_PHONE, property.getReservationPhoneNumber());

	}

	@SuppressWarnings("unchecked")
    private String getRoomRequests(RoomReservationV2Response reservation) {

        List<String> roomRequests = new ArrayList<>();
        reservation.getSpecialRequests().forEach(request -> {
            if (!appProperties.getBorgataSpecialRequests().contains(request)) {
                roomRequests
                        .add(componentService.getRoomComponent(reservation.getRoomTypeId(), request).getDescription());
            }
        });
        return StringUtils.join(roomRequests);
    }

	/**
	 * Construct itinerary deeplink URL as per format defined by DMP to include in
	 * the email for quick reservation lookup.
	 * 
	 * @param reservation Room reservation object
	 * 
	 * @return Returns the itinerary deep link URL to DMP
	 */
    private String constructItineraryLink(RoomReservationV2Response reservation) {

        String urlParams = String.format("type=room+propertyId=%s+confirmationNumber=%s", reservation.getPropertyId(),
                reservation.getConfirmationNumber());

        String itineraryUrl = urlProperties.getItineraryDeepLink()
                .replace("{confNumber}", reservation.getConfirmationNumber())
                .replace("{firstName}", CommonUtil.urlEncode(reservation.getProfile().getFirstName()))
                .replace("{lastName}", CommonUtil.urlEncode(reservation.getProfile().getLastName()))
                .replace("{encryptedString}", CommonUtil.encryptAndCustomEncode(urlParams));

        return domainProperties.getAem() + itineraryUrl;
    }

	private String round(double number) {
		return CommonUtil.round(number);
	}

	private void sendEmail(Email emailTemplate, Map<String, Object> actualContent) {
		try {
			log.info(actualContent);
			emailTemplate.setBody(CommonUtil.getFTLTransformedContent(emailTemplate.getBody(), actualContent));

		} catch (IOException | TemplateException e) {
			log.error("Error populating confirmation email: {}", e);
		}
        emailDAOMap.get(isNSForEmailEnabled ? EMAIL_NS_IMPL : EMAIL_IMPL).sendEmail(emailTemplate);
	}

	@Override
	public void sendCancellationEmail(RoomReservation reservation, RoomReservationV2Response response) {

		try {
			Map<String, Object> actualContent = new HashMap<>();
			setCommonEmailElements(actualContent, reservation);
	
			actualContent.put(ServiceConstant.EMAIL_CANCEL_CONFIRMATION, reservation.getConfirmationNumber());
			actualContent.put(ServiceConstant.EMAIL_CANCEL_DATE, new Date());
			actualContent.put(ServiceConstant.EMAIL_AMT_PAID, reservation.getAmountDue());
	
			double refundAmount = ReservationUtil.getRefundAmount(reservation, appProperties);
			actualContent.put(ServiceConstant.EMAIL_AMT_REFUND, round(refundAmount));
	
			Map<String, Object> reservationContent = new HashMap<>();
	
			setCommonRoomContent(reservationContent, reservation, response);
			reservationContent.put(ServiceConstant.EMAIL_AMT_REFUND, refundAmount);
			reservationContent.put(ServiceConstant.EMAIL_AMT_PAID, reservation.getAmountDue());
	
			List<Map<String, Object>> reservations = new ArrayList<>();
	
			Email emailTemplate = emailCacheService.getCancellationEmailTemplate(reservation.getPropertyId());
			
			log.debug("Found cancellation email template from the cache for property id:{}", reservation.getPropertyId());
			emailTemplate.setTo(reservation.getProfile().getEmailAddress1());
	
			setRoomMarketingContent(reservationContent, response);
			reservations.add(reservationContent);
			actualContent.put(ServiceConstant.EMAIL_ROOM_RESERVATIONS, reservations);
			sendEmail(emailTemplate, actualContent);
			log.info("Email sent.");
			
		}catch(Exception ex) {
			// Unable to send email shouldn't be hard failure
			log.error("SendEmailFailure - Unable to send email for cancelled reservation:{}, itinerary:{} and exception:{}",
					reservation.getId(), reservation.getItineraryId(), ex);
		}

	}
}
