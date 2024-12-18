package com.mgm.services.booking.room.service.helper;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.ocrs.*;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Helper class for FindReservation Service.
 * 
 * @author laknaray
 */
@Component
@Log4j2
public class FindReservationServiceHelper {
    @Value("${idms.token.validation.enabled}")
    private boolean validateTokenEnabled;

    public boolean validateTokenOrServiceBasedRole(String tokenScope){
        if(validateTokenEnabled){
            return tokenContainsScope(tokenScope);
        }else{
           return tokenContainsServiceRole();
        }
    }
    
    /**
     * Gets the HttpServletRequest object and check for the existence of the
     * <code>scope</code> in JWT token.
     * 
     * @param scope
     *            scope
     * @return true, only if the JWT Token in the request has <code>scope</code>
     */
    public boolean tokenContainsScope(String scope) {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        return TokenValidationUtil.tokenContainsScope(TokenValidationUtil.extractJwtToken(httpRequest), scope);
    }


    /**
     * Gets the HttpServletRequest object and check for the existence of the
     * <code>mgm_service</code> in JWT token.
     *
     * @return true, only if the JWT Token in the request has <code>scope</code>
     */
    public boolean tokenContainsServiceRole() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        return TokenValidationUtil.tokenContainsServiceRole(TokenValidationUtil.extractJwtToken(httpRequest));
    }
    /**
     * Compare firstName and lastName of the find reservation request with the
     * reservation object.
     * 
     * @param firstName
     *            first name string
     * @param lastName
     *            last name string
     * @param roomReservation
     *            reservation object
     * @return true, if firstName and lastName matches with the reservation
     *         object, else false.
     */
    public boolean isFirstNameLastNameMatching(String firstName, String lastName, RoomReservation roomReservation) {
        return null != roomReservation.getProfile()
                && firstName.equalsIgnoreCase(roomReservation.getProfile().getFirstName())
                && lastName.equalsIgnoreCase(roomReservation.getProfile().getLastName());
    }

    /**
     * Gets the HttpServletRequest object and determines the JWT token presnt is
     * a guest token or not..
     * 
     * @return true, only if the JWT Token is a guest token.
     */
    public boolean isTokenAGuestToken() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        return TokenValidationUtil.isTokenAGuestToken(TokenValidationUtil.extractJwtToken(httpRequest));
    }

    /**
     * Gets the HttpServletRequest object and compare the mlife number present
     * in JWT token with <code>reservationMlifeNum</code>.
     * 
     * @param reservationMlifeNum
     *            mlife number
     * @return true, only if the mlife number present in JWT Token and
     *         <code>reservationMlifeNum</code> are same.
     */
    public boolean isMlifeNumMatching(int reservationMlifeNum) {
        boolean isMatching = false;
        if (reservationMlifeNum > 0) {
            HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes()).getRequest();

            Map<String, String> mlifeNumClaim = TokenValidationUtil.getClaimsFromToken(
                    TokenValidationUtil.extractJwtToken(httpRequest),
                    Collections.singletonList(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
            if (!mlifeNumClaim.isEmpty()
                    && StringUtils.isNotEmpty(mlifeNumClaim.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM))) {
                int jwtMlifeNum = Integer.parseInt(mlifeNumClaim.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
                isMatching = reservationMlifeNum == jwtMlifeNum;
            }
        }
        return isMatching;
    }

    /**
     * Gets the HttpServletRequest object and check for the existence of
     * <code>com.mgm.id</code> claim in the JWT token.
     * 
     * @return true, only if <code>com.mgm.id</code> claim is present in the JWT
     *         token and it's not empty.
     */
    public boolean isTokenHasMgmId() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        Map<String, String> mgmIdClaim = TokenValidationUtil.getClaimsFromToken(
                TokenValidationUtil.extractJwtToken(httpRequest),
                Collections.singletonList(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
        return !mgmIdClaim.isEmpty() && StringUtils.isNotEmpty(mgmIdClaim.get(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
    }

    /**
     * Gets the HttpServletRequest object and compare the mgmId present in JWT
     * token with <code>mgmId</code>.
     * 
     * @param mgmId
     *            mgmId as string
     * @return true, only if the mgmId present in JWT Token and
     *         <code>mgmId</code> are same.
     */
    public boolean isMgmIdMatching(String mgmId) {
        boolean isMatching = false;
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        Map<String, String> mgmIdClaim = TokenValidationUtil.getClaimsFromToken(
                TokenValidationUtil.extractJwtToken(httpRequest),
                Collections.singletonList(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
        if (!mgmIdClaim.isEmpty() && StringUtils.isNotEmpty(mgmIdClaim.get(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM))) {
            String jwtMgmId = mgmIdClaim.get(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM);
            isMatching = null != mgmId && null != jwtMgmId && mgmId.equals(jwtMgmId);
        }
        return isMatching;
    }

    /**
     * Reservation is considered as loyalty reservation, if reservation has
     * mlife number.
     * 
     * @return true only if the reservation has positive mlife number otherwise
     *         false
     */
    public boolean isLoyaltyReservation(RoomReservation reservation) {
        return null != reservation && null != reservation.getProfile() && reservation.getProfile().getMlifeNo() > 0;
    }

    public void updateRoomReservation(OcrsReservation ocrsReservation, RoomReservation roomReservation, String reqConfNumber) {

        RoomStay roomStay = ocrsReservation.getRoomStays().getRoomStay().get(0);
        String operaState = roomStay.getReservationStatusType();
        roomReservation.setOperaState(operaState);
        ReservationState state = ReservationState.Booked;
        if (StringUtils.equals("CANCELED", operaState)) {
            state = ReservationState.Cancelled;
        }
        roomReservation.setState(state);

        for (GuestCountItem count : roomStay.getGuestCounts().getGuestCount()) {
            if (count.getAgeQualifyingCode().equals("ADULT")) {
                roomReservation.setNumAdults(count.getMfCount());
            } else if (count.getAgeQualifyingCode().equals("CHILD")) {
                roomReservation.setNumChildren(count.getMfCount());
            }
        }
        
        roomReservation.setThirdParty(true);
        roomReservation.setOperaConfirmationNumber(ocrsReservation.getReservationID());
        roomReservation.setBookDate(ocrsReservation.getOriginalBookingDate());
        
        // find and set confirmation number. Logic specially for share-withs
        for (ResGuest rg : ocrsReservation.getResGuests()
                .getResGuest()) {
            if (null != rg.getReservationReferences()) {
                for (ReservationReference rr : rg.getReservationReferences()
                        .getReservationReference()) {
                    if (rr.getReferenceNumber()
                            .equals(reqConfNumber)) {
                        // if reservation is found in ocrs, set opera
                        // confirmation number from respective guest obj
                        roomReservation.setOperaConfirmationNumber(rg.getReservationID());
                        break;
                    }
                }
            }
        }
        
        // If requested reservation is secondary, set primary confirmation number
        if (!roomReservation.getOperaConfirmationNumber()
                .equals(ocrsReservation.getReservationID())) {
            roomReservation.setPrimarySharerConfirmationNumber(ocrsReservation.getReservationID());
        }

        ocrsReservation.getResGuests().getResGuest().stream()
                .filter(g -> g.getReservationID().equals(ocrsReservation.getReservationID())).findFirst()
                .ifPresent(guest -> {
                    roomReservation.setCheckInDate(convertOcrsMillisToDateWithTimezone(guest.getArrivalTime()));
                    roomReservation.setCheckOutDate(convertOcrsMillisToDateWithTimezone(guest.getDepartureTime()));
                });
        if(ocrsReservation!=null && ocrsReservation.getMfImage()!=null) {
        	roomReservation.setNumRooms(ocrsReservation.getMfImage().getNumRooms());
        }

        Profile ocrsProfile = getPrimaryProfile(ocrsReservation);
        ReservationProfile gseProfile = new ReservationProfile();
        if (null != ocrsProfile) {
            gseProfile.setOperaId(ocrsProfile.getMfResortProfileID());

            if (null != ocrsProfile.getIndividualName()) {
                gseProfile.setTitle(ocrsProfile.getIndividualName().getNamePrefix());
                gseProfile.setFirstName(ocrsProfile.getIndividualName().getNameFirst());
                gseProfile.setLastName(ocrsProfile.getIndividualName().getNameSur());
            }
            if(null!=ocrsProfile && null!=gseProfile) {
            	setPhoneNumbers(ocrsProfile, gseProfile);
            	setEmailAddress(ocrsProfile, gseProfile);
            	setMlifeNo(ocrsReservation, gseProfile);
            	setAddresses(ocrsProfile, gseProfile);
            }
        }

        roomReservation.setProfile(gseProfile);
    }

    public Profile getPrimaryProfile(OcrsReservation ocrsReservation) {
        List<String> rphs = new ArrayList<>();
        ocrsReservation.getResGuests().getResGuest().forEach(guest -> {
            String profileRphs = StringUtils.EMPTY;
            if (guest.getReservationID().equalsIgnoreCase(ocrsReservation.getReservationID())) {
                profileRphs = guest.getProfileRPHs();
            }
            if (StringUtils.isNotEmpty(profileRphs)) {
                rphs.addAll(Arrays.asList(profileRphs.replaceAll("\\s", "").split(",")));
            }
        });

        for (ResProfile resProfile : ocrsReservation.getResProfiles().getResProfile()) {
            if (resProfile.getProfile().getProfileType().equalsIgnoreCase("guest")
                    && rphs.contains(String.valueOf(resProfile.getResProfileRPH()))) {
                return resProfile.getProfile();

            }
        }

        return null;
    }

    private void setPhoneNumbers(Profile ocrsProfile, ReservationProfile gseProfile) {

        ocrsProfile.getPhoneNumbers().getPhoneNumber().stream().filter(p -> "Y".equals(p.getMfPrimaryYN())).findFirst()
                .ifPresent(number -> {
                    ProfilePhone profilePhone = new ProfilePhone();
                    profilePhone.setType(number.getPhoneNumberType());
                    profilePhone.setNumber(number.getPhoneNumber());
                    gseProfile.setPhoneNumbers(Collections.singletonList(profilePhone));
                });
    }

    private void setEmailAddress(Profile ocrsProfile, ReservationProfile gseProfile) {

        ocrsProfile.getElectronicAddresses().getElectronicAddress().stream().filter(e -> "Y".equals(e.getMfPrimaryYN()))
                .findFirst().ifPresent(email -> gseProfile.setEmailAddress1(email.getEaddress()));

    }

    private void setMlifeNo(OcrsReservation ocrsReservation, ReservationProfile gseProfile) {

        ocrsReservation.getSelectedMemberships().getSelectedMembership().forEach(membership -> {
            if (membership.getProgramCode().equals("PC")) {
                gseProfile.setMlifeNo(Integer.parseInt(membership.getAccountID()));
            }
        });
    }

    private void setAddresses(Profile ocrsProfile, ReservationProfile gseProfile) {

        ocrsProfile.getPostalAddresses().getPostalAddress().stream().filter(a -> "Y".equals(a.getMfPrimaryYN()))
                .findFirst().ifPresent(ocrsAddress -> {
                    ProfileAddress gseAddress = new ProfileAddress();
                    gseAddress.setType(ocrsAddress.getAddressType());
                    gseAddress.setPreferred(true);
                    gseAddress.setStreet1(ocrsAddress.getAddress1());
                    gseAddress.setStreet2(ocrsAddress.getAddress2());
                    gseAddress.setCity(ocrsAddress.getCity());
                    gseAddress.setState(ocrsAddress.getStateCode());
                    gseAddress.setCountry(ocrsAddress.getCountryCode());
                    gseAddress.setPostalCode(ocrsAddress.getPostalCode());
                    gseProfile.setAddresses(Collections.singletonList(gseAddress));
                });

    }

    private Date convertOcrsMillisToDateWithTimezone(long millis) {
        // Read date in OCRS Timezone
        SimpleDateFormat sdf = new SimpleDateFormat(ServiceConstant.ISO_8601_DATE_FORMAT, Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone(ServiceConstant.OCRS_TIMEZONE));
        String date = sdf.format(new Date(millis));

        // parse it back to default timezone
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        sdf1.setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
        Date checkInDate = null;

        try {
            checkInDate = sdf1.parse(date);
        } catch (ParseException e) {
            log.error("Failed to parse date {} due to {}", millis, e.getMessage());
        }

        return checkInDate;
    }
}
