package com.mgm.services.booking.room.validator.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.BillingAddressRequest;
import com.mgm.services.booking.room.model.request.CreditCardRequest;
import com.mgm.services.booking.room.model.request.RoomPaymentDetailsRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.constant.ServiceCommonConstant;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import com.mgm.services.common.util.ValidationUtil;

/**
 * Utility class providing common validation methods to be used across the
 * application/
 * 
 */
public final class ReservationValidatorUtil {

    private static final Pattern MASKED_NUMBER_PATTERN = Pattern.compile("^\\D+\\d{4}$");

    private static final int MIN_N0_OF_CHARS_CCTOKEN = 16;

    private static final int MAX_PHONE_NUMBER_LENGTH = 15;

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("[0-9()-.]+");

    /**
     * The booking type Enum to differentiate the validation checks
     *
     */
    public static enum BookingType {
        PREBOOK, CREATE, PARTY, MODIFY, SAVE, SHAREWITH
    }

    private ReservationValidatorUtil() {
        // Hiding implicit constructor
    }

    /**
     * This method shall validate all the mandatory fields in Room reservation
     * request
     * 
     * @param roomReservationRequest Room Reservation Request
     * @param errors                 validation errors
     * @param bookingType            the booking type
     */
    public static void validateMandatoryFields(RoomReservationRequest roomReservationRequest, Errors errors,
            BookingType bookingType) {
        if (StringUtils.isBlank(roomReservationRequest.getItineraryId())) {
            errors.rejectValue("roomReservation.itineraryId", ErrorCode.INVALID_ITINERARY_ID.getErrorCode());
        }
        if (!ValidationUtil.isUuid(roomReservationRequest.getPropertyId())) {
            errors.rejectValue("roomReservation.propertyId", ErrorCode.INVALID_PROPERTY_ID.getErrorCode());
        }
        final String roomTypeId = roomReservationRequest.getRoomTypeId();
        if (!ValidationUtil.isUuid(roomTypeId) && !ACRSConversionUtil.isAcrsRoomCodeGuid(roomTypeId)) {
            errors.rejectValue("roomReservation.roomTypeId", ErrorCode.INVALID_ROOMTYPE.getErrorCode());
        }

        // Profile object is mandatory for both create and modify reservation scenarios
        if (bookingType.equals(BookingType.CREATE) || bookingType.equals(BookingType.MODIFY)) {
            ValidationUtils.rejectIfEmpty(errors, "roomReservation.profile", ErrorCode.INVALID_PROFILE.getErrorCode());
        }
        ValidationUtils.rejectIfEmpty(errors, "roomReservation.bookings", ErrorCode.INVALID_BOOKINGS.getErrorCode());
        if (isBillingMandatory(bookingType)) {
            ValidationUtils.rejectIfEmpty(errors, "roomReservation.billing", ErrorCode.INVALID_BILLING.getErrorCode());
        }
        ValidationUtils.rejectIfEmpty(errors, "roomReservation.tripDetails",
                ErrorCode.INVALID_TRIP_DETAILS.getErrorCode());
        if (bookingType.equals(BookingType.MODIFY)) {
            if (StringUtils.isBlank(roomReservationRequest.getConfirmationNumber())) {
                errors.rejectValue("roomReservation.confirmationNumber",
                        ErrorCode.NO_CONFIRMATION_NUMBER.getErrorCode());
            }

            if (roomReservationRequest.getState() == null
                    || !roomReservationRequest.getState().equals(ReservationState.Booked)) {
                errors.rejectValue("roomReservation.confirmationNumber",
                        ErrorCode.INVALID_BOOKING_STATE.getErrorCode());
            }
        }

        if (null != roomReservationRequest.getRoutingInstructions()
                && !roomReservationRequest.getRoutingInstructions().isEmpty() && roomReservationRequest
                        .getRoutingInstructions().stream().filter(rI -> rI.getAuthorizerId() == null).count() > 0) {
            errors.rejectValue("roomReservation.routingInstructions",
                    ErrorCode.INVALID_ROUTINGINSTRUCTIONS.getErrorCode());
        }
    }

    private static boolean isBillingMandatory(BookingType bookingType) {
        // for channel ice -- in case of create and modify reservation services
        // -- billing is not mandatory
        return !((bookingType.equals(BookingType.CREATE) || bookingType.equals(BookingType.MODIFY))
                && "ice".equalsIgnoreCase(getChannelHeader()));
    }

    private static String getChannelHeader() {
       return CommonUtil.getChannelHeaderWithFallback(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest());
    }

    /**
     * This method shall check whether the customer Id is a valid value or not
     * 
     * @param profile    User Profile Request
     * @param customerId customer Id
     * @param errors     validation errors
     */
    public static void validateCustomerId(UserProfileRequest profile, long customerId, Errors errors) {
        if (profile != null && customerId <= 0) {
            errors.rejectValue("roomReservation.customerId", ErrorCode.INVALID_CUSTOMER_ID.getErrorCode());
        }
    }

    /**
     * This method shall validate the User Profile Request object
     * 
     * @param fieldNamePrefix - prefix for field name
     * @param profile         User Profile Request
     * @param errors          validation errors
     */
    public static void validateProfile(String fieldNamePrefix, UserProfileRequest profile, Errors errors) {

        if (StringUtils.isNotEmpty(profile.getEmailAddress1())
                && !ValidationUtil.isValidEmailFormat(profile.getEmailAddress1())) {
            errors.rejectValue(fieldNamePrefix + ".emailAddress1", ErrorCode.INVALID_EMAIL.getErrorCode());
        }
        if (StringUtils.isNotEmpty(profile.getEmailAddress2())
                && !ValidationUtil.isValidEmailFormat(profile.getEmailAddress2())) {
            errors.rejectValue(fieldNamePrefix + ".emailAddress2", ErrorCode.INVALID_EMAIL.getErrorCode());
        }
        // firstName and lastName are mandatory inside the profile object
        if (StringUtils.isEmpty(profile.getFirstName()) || !ValidationUtil.isValidName(profile.getFirstName())) {
            errors.rejectValue(fieldNamePrefix + ".firstName", ErrorCode.INVALID_NAME.getErrorCode());
        }
        if (StringUtils.isEmpty(profile.getLastName()) || !ValidationUtil.isValidName(profile.getLastName())) {
            errors.rejectValue(fieldNamePrefix + ".lastName", ErrorCode.INVALID_NAME.getErrorCode());
        }

        validateAdditionalProfileFields(fieldNamePrefix, profile, errors);
    }

    private static void validateAdditionalProfileFields(String fieldNamePrefix, UserProfileRequest profile,
            Errors errors) {

        if (profile.getDateOfBirth() != null) {
            ZoneId defaultZoneId = ZoneId.of(ServiceCommonConstant.DEFAULT_TIME_ZONE);
            LocalDate dob = profile.getDateOfBirth().toInstant().atZone(defaultZoneId).toLocalDate();
            if (!ValidationUtil.isValidDOB(dob)) {
                errors.rejectValue(fieldNamePrefix + ".dateOfBirth", ErrorCode.INVALID_AGE.getErrorCode());
            }
        }

        if (CollectionUtils.isNotEmpty(profile.getPhoneNumbers())) {
            validateProfilePhone(fieldNamePrefix, profile.getPhoneNumbers(), errors);
        }
        if (CollectionUtils.isNotEmpty(profile.getAddresses())) {
            validateProfileAddresses(fieldNamePrefix, profile.getAddresses(), errors);
        }
    }

    private static void validateProfilePhone(String fieldNamePrefix, List<ProfilePhone> phones, Errors errors) {
        if (CollectionUtils.isNotEmpty(phones)) {
            phones.stream().forEach(phone -> {
                if (StringUtils.isEmpty(phone.getNumber()) || !isValidPhone(phone.getNumber())) {
                    errors.rejectValue(fieldNamePrefix + ".phoneNumbers", ErrorCode.INVALID_PHONE.getErrorCode());
                }
                if (StringUtils.isEmpty(phone.getType())
                        || !listContains(ServiceConstant.APPLICABLE_PHONE_TYPE.split(","), phone.getType())) {
                    errors.rejectValue(fieldNamePrefix + ".phoneNumbers", ErrorCode.INVALID_PHONE.getErrorCode());
                }
            });
        } else {
            errors.rejectValue(fieldNamePrefix + ".phoneNumbers", ErrorCode.INVALID_PHONE.getErrorCode());
        }
    }

    /**
     * this method checks the validity of the phone number provided
     * 
     * @param phoneNumber the phone number
     * @return isValid
     */
    public static boolean isValidPhone(String phoneNumber) {
        boolean isValidPhone = false;
        if (StringUtils.isNotBlank(phoneNumber)) {
            final String phone = phoneNumber.replaceAll("[^0-9]", "");
            phoneNumber = phoneNumber.replaceAll("\\s", StringUtils.EMPTY);
            final boolean isValidMatch = PHONE_NUMBER_PATTERN.matcher(StringUtils.trimToEmpty(phoneNumber)).matches();
            if (isValidMatch && phone.length() <= MAX_PHONE_NUMBER_LENGTH) {
                isValidPhone = true;
            }
        }
        return isValidPhone;
    }

    private static boolean listContains(String[] applicableValues, String input) {
        return Arrays.stream(applicableValues).anyMatch(input::equals);
    }

    private static void validateProfileAddresses(String fieldNamePrefix, List<ProfileAddress> addresses,
            Errors errors) {
        addresses.stream().forEach(address -> {
            validateStreet1(address.getStreet1(), fieldNamePrefix + ".addresses[0].street1", errors, ErrorCode.INVALID_STREET);
            validateStreet2(address.getStreet2(), fieldNamePrefix + ".addresses[0].street1", errors, ErrorCode.INVALID_STREET);
            validateCity(address.getCity(), fieldNamePrefix + ".addresses[0].city", errors, ErrorCode.INVALID_CITY);
            validateState(address.getState(), address.getCountry(), fieldNamePrefix + ".addresses[0].state", errors, ErrorCode.INVALID_STATE);
            validatePostalCode(address.getPostalCode(), address.getCountry(),
                    fieldNamePrefix + ".addresses[0].postalCode", errors, ErrorCode.INVALID_POSTALCODE);
            if (StringUtils.isNotEmpty(address.getType())
                    && !listContains(ServiceConstant.APPLICABLE_ADDRESS_TYPE.split(","), address.getType())) {
                errors.rejectValue(fieldNamePrefix + ".addresses[0].type",
                        ErrorCode.INVALID_ADDRESS_TYPE.getErrorCode());
            }
        });
    }

    /**
     * This method shall validate the Room Payment Details Request
     * 
     * @param billing     List of Room Payment Details Request
     * @param errors      validation errors
     * @param bookingType the booking type
     */
    public static void validateBilling(List<RoomPaymentDetailsRequest> billing, Errors errors,
            BookingType bookingType) {
        if (CollectionUtils.isNotEmpty(billing)) {

            if (bookingType.equals(BookingType.PARTY) && billing.size() > 1) {
                errors.rejectValue("roomReservation.billing", ErrorCode.INVALID_BILLING_MULTI_CARDS.getErrorCode());
                return;
            }

            billing.stream().forEach(billingDetails -> {
                BillingAddressRequest address = billingDetails.getAddress();
                if (null != address) {
                    validateBillingAddress(address, errors);
                } else {
                    errors.rejectValue("roomReservation.billing[0].address", ErrorCode.INVALID_ADDRESS.getErrorCode());
                }

                CreditCardRequest payment = billingDetails.getPayment();
                if (null != payment) {
                    validateBillingPayment(payment, errors);
                }
            });
        }
    }

    private static void validateBillingAddress(BillingAddressRequest billingAddress, Errors errors) {
        validateStreet1(billingAddress.getStreet1(), "roomReservation.billing[0].address.street1", errors, ErrorCode.INVALID_BILLING_STREET);
        validateStreet2(billingAddress.getStreet2(), "roomReservation.billing[0].address.street1", errors, ErrorCode.INVALID_BILLING_STREET);
        validateCity(billingAddress.getCity(), "roomReservation.billing[0].address.city", errors, ErrorCode.INVALID_BILLING_CITY);
        if (StringUtils.isNotEmpty(billingAddress.getState())) {
            validateState(billingAddress.getState(), billingAddress.getCountry(),
                    "roomReservation.billing[0].address.state", errors, ErrorCode.INVALID_BILLING_STATE);
        }
        if (StringUtils.isNotEmpty(billingAddress.getPostalCode())) {
            validatePostalCode(billingAddress.getPostalCode(), billingAddress.getCountry(),
                    "roomReservation.billing[0].address.postalCode", errors, ErrorCode.INVALID_BILLING_POSTALCODE);
        }
    }

    private static void validateBillingPayment(CreditCardRequest payment, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "roomReservation.billing[0].payment.ccToken",
                ErrorCode.INVALID_CARD.getErrorCode());

        if (StringUtils.isNotEmpty(payment.getCcToken()) && payment.getCcToken().length() < MIN_N0_OF_CHARS_CCTOKEN) {
            errors.rejectValue("roomReservation.billing[0].payment.ccToken", ErrorCode.INVALID_CARD.getErrorCode());
        }
        if (StringUtils.isNotEmpty(payment.getCardNumber())
                && !ValidationUtil.isValidCardNumber(payment.getCardNumber())) {
            errors.rejectValue("roomReservation.billing[0].payment.cardNumber", ErrorCode.INVALID_CARD.getErrorCode());
        }

        if (StringUtils.isNotEmpty(payment.getCardHolder())
                && !ValidationUtil.isValidCardHolder(payment.getCardHolder())) {
            errors.rejectValue("roomReservation.billing[0].payment.cardHolder",
                    ErrorCode.INVALID_CARDHOLDER.getErrorCode());
        }
        if (StringUtils.isNotEmpty(payment.getCvv()) && !StringUtils.isNumeric(payment.getCvv())) {
            errors.rejectValue("roomReservation.billing[0].payment.cvv", ErrorCode.INVALID_CVV.getErrorCode());
        }
    }

    /**
     * This method shall validate the Trip Details Request
     * 
     * @param fieldNamePrefix field name prefix
     * @param tripDetails     Trip Details Request
     * @param errors          validation errors
     * @param bookingType     the booking type
     */
    public static void validateTripDetails(String fieldNamePrefix, TripDetailsRequest tripDetails, Errors errors,
            BookingType bookingType) {
        ValidationUtils.rejectIfEmpty(errors, fieldNamePrefix + ".checkInDate", ErrorCode.INVALID_DATES.getErrorCode());
        ValidationUtils.rejectIfEmpty(errors, fieldNamePrefix + ".checkOutDate",
                ErrorCode.INVALID_DATES.getErrorCode());
        if (tripDetails.getCheckInDate() != null && tripDetails.getCheckOutDate() != null
                && tripDetails.getCheckInDate().after(tripDetails.getCheckOutDate())) {
            errors.rejectValue(fieldNamePrefix + ".checkOutDate", ErrorCode.INVALID_DATES.getErrorCode());
        }
        if (tripDetails.getNumAdults() <= 0) {
            errors.rejectValue(fieldNamePrefix + ".numAdults", ErrorCode.INVALID_NUM_ADULTS.getErrorCode());
        }
        if (bookingType.equals(BookingType.PARTY) && tripDetails.getNumRooms() <= 1) {
            errors.rejectValue(fieldNamePrefix + ".numRooms", ErrorCode.INVALID_NUM_ROOMS.getErrorCode());
        }
        if ((bookingType.equals(BookingType.CREATE) || bookingType.equals(BookingType.MODIFY)
                || bookingType.equals(BookingType.SAVE)) && tripDetails.getNumRooms() != 1) {
            errors.rejectValue(fieldNamePrefix + ".numRooms", ErrorCode.INVALID_NUM_ROOMS.getErrorCode());
        }
    }

    private static boolean isValidMaskedNumber(String maskedNumber) {
        return StringUtils.isEmpty(maskedNumber)
                || MASKED_NUMBER_PATTERN.matcher(StringUtils.trimToEmpty(maskedNumber)).matches();
    }

    private static void validateStreet1(String street1, String fieldPath, Errors errors, ErrorCode errorCode) {
        if (StringUtils.isNotEmpty(street1) && !ValidationUtil.isValidStreet1(street1)) {
            errors.rejectValue(fieldPath, errorCode.getErrorCode());
        }
    }

    private static void validateStreet2(String street2, String fieldPath, Errors errors, ErrorCode errorCode) {
        if (StringUtils.isNotEmpty(street2) && !ValidationUtil.isValidStreet2(street2)) {
            errors.rejectValue(fieldPath, errorCode.getErrorCode());
        }
    }

    private static void validateCity(String city, String fieldPath, Errors errors, ErrorCode errorCode) {
        if (StringUtils.isNotEmpty(city) && !ValidationUtil.isValidCity(city)) {
            errors.rejectValue(fieldPath, errorCode.getErrorCode());
        }
    }

    private static void validateState(String state, String country, String fieldPath, Errors errors, ErrorCode errorCode) {
        if (!ValidationUtil.isValidState(state, country)) {
            errors.rejectValue(fieldPath, errorCode.getErrorCode());
        }
    }

    private static void validatePostalCode(String postalCode, String country, String fieldPath, Errors errors, ErrorCode errorCode) {
        if (!ValidationUtil.isValidPostalCode(postalCode, country)) {
            errors.rejectValue(fieldPath, errorCode.getErrorCode());
        }
    }

}
