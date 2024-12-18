package com.mgm.services.booking.room.constant;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CVSResponse;
import com.mgm.services.booking.room.util.ReservationUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ACRSConversionUtil {

    private static String V_DM = "-v-";
    private static String F_DM = "-d-";

    private static String RATE_PLAN_CODE_FORMAT =   "RPCD" + V_DM + "%s" + F_DM + "PROP" + V_DM + "%s";         //Example: id="RPCD-v-TPBAR-d-PROP-v-MV275"
    private static String GROUP_CODE_FORMAT =       "GRPCD" + V_DM + "%s" + F_DM + "PROP" + V_DM + "%s";        //Example: id="GRPCD-v-GRP1-d-PROP-v-MV021"
    private static String ROOM_CODE_FORMAT =        "ROOMCD" + V_DM + "%s" + F_DM + "PROP" + V_DM + "%s";       //Example: id="ROOMCD-v-DLUX-d-PROP-v-MV021"
    private static String COMPONENT_CODE_FORMAT =   "COMPONENTCD" + V_DM + "%s" + F_DM + "TYP" + V_DM + "%s"
            + F_DM + "PROP" + V_DM + "%s" + F_DM + "NRPCD" + V_DM + "%s";                                       //Example: id="COMPONENTCD-v-EARLYCHECKIN-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-RP2"

    private static Pattern RATE_PLAN_CODE_PATTERN = Pattern.compile(String.format(RATE_PLAN_CODE_FORMAT, "(.*)", "(.*)"));
    private static Pattern GROUP_CODE_PATTERN = Pattern.compile(String.format(GROUP_CODE_FORMAT, "(.*)", "(.*)"));
    private static Pattern ROOM_CODE_PATTERN = Pattern.compile(String.format(ROOM_CODE_FORMAT, "(.*)", "(.*)"));
    private static Pattern COMPONENT_CODE_PATTERN = Pattern.compile(String.format(COMPONENT_CODE_FORMAT, "(.*)", "(.*)", "(.*)", "(.*)"));

    final static Pattern PATRON_PROMO_PATTERN = Pattern.compile("(PTRN)([^0-9]*)([0-9]+)$");
    final static Pattern PO_OFFER_PATTERN = Pattern.compile("((CA)|(CO)|(CH))(.*)(S|T|P)([\\d]+)$");


    /*
     * Component Guid Generation/Extraction methods
     */
    public static String createComponentGuid(String componentCode, String nrpRatePlanCode, String componentType, String propertyCode) {
        if (StringUtils.isAnyEmpty(componentCode, nrpRatePlanCode, componentType, propertyCode)) {
            throw new IllegalArgumentException(String.format("Missing mandatory fields in creating Component guid, code=%s rateplan=%s type=%s property=%s",
                    componentCode, nrpRatePlanCode, componentType, propertyCode));
        }
        return String.format(COMPONENT_CODE_FORMAT, componentCode, componentType, propertyCode, nrpRatePlanCode);
    }

    public static boolean isAcrsComponentCodeGuid(String componentCodeGuid) {
        return null != getMatchingCode(componentCodeGuid, 1, COMPONENT_CODE_PATTERN);
    }

    public static String getComponentCode(String componentCodeGuid) {
        return getMatchingCode(componentCodeGuid, 1, COMPONENT_CODE_PATTERN);
    }

    public static String getComponentType(String componentCodeGuid) {
        return getMatchingCode(componentCodeGuid, 2, COMPONENT_CODE_PATTERN);
    }


    public static String getComponentNRPlanCode(String componentCodeGuid) {
        return getMatchingCode(componentCodeGuid, 4, COMPONENT_CODE_PATTERN);
    }

    /*
     * Group Code Guid Generation/Extraction methods
     */
    public static String createGroupCodeGuid(String groupCode, String propertyCode) {
        if (StringUtils.isAnyEmpty(groupCode, propertyCode)) {
            throw new IllegalArgumentException(String.format("Missing mandatory fields in creating Group Code guid, code=%s property=%s", groupCode, propertyCode));
        }
        return String.format(GROUP_CODE_FORMAT, groupCode, propertyCode);
    }

    public static boolean isAcrsGroupCodeGuid(String groupCodeGuid) {
        return null != getMatchingCode(groupCodeGuid, 1, GROUP_CODE_PATTERN);
    }

    public static String getGroupCode(String groupCodeGuid) {
        return getMatchingCode(groupCodeGuid, 1, GROUP_CODE_PATTERN);
    }

    /*
     * Room Code Guid Generation/Extraction methods
     */
    public static String createRoomCodeGuid(String roomCode, String propertyCode) {
        if (StringUtils.isAnyEmpty(roomCode, propertyCode)) {
            throw new IllegalArgumentException(String.format("Missing mandatory fields in creating Room Code guid, code=%s property=%s", roomCode, propertyCode));
        }
        return String.format(ROOM_CODE_FORMAT, roomCode, propertyCode);
    }

    public static boolean isAcrsRoomCodeGuid(String roomCodeGuid) {
        return null != getMatchingCode(roomCodeGuid, 1, ROOM_CODE_PATTERN);
    }

    public static String getRoomCode(String roomCodeGuid) {
        return getMatchingCode(roomCodeGuid, 1, ROOM_CODE_PATTERN);
    }

    /*
     * Rate plan Code Guid Generation/Extraction methods
     */
    public static String createRatePlanCodeGuid(String ratePlanCode, String propertyCode) {
        if (StringUtils.isAnyEmpty(ratePlanCode, propertyCode)) {
            throw new IllegalArgumentException(String.format("Missing mandatory fields in creating Rate Plan Code guid, code=%s property=%s", ratePlanCode, propertyCode));
        }
        return String.format(RATE_PLAN_CODE_FORMAT, ratePlanCode, propertyCode);
    }

    public static boolean isAcrsRatePlanGuid(String ratePlanCodeGuid) {
        return null != getMatchingCode(ratePlanCodeGuid, 1, RATE_PLAN_CODE_PATTERN);
    }

    public static String getRatePlanCode(String ratePlanCodeGuid) {
        return getMatchingCode(ratePlanCodeGuid, 1, RATE_PLAN_CODE_PATTERN);
    }

    /*
     * Property Code Extraction method
     */
    public static String getPropertyCode(String guid) {
        String propertyCode = getMatchingCode(guid, 2, GROUP_CODE_PATTERN);
        if (propertyCode == null) {
            propertyCode = getMatchingCode(guid, 2, ROOM_CODE_PATTERN);
        }
        if (propertyCode == null) {
            propertyCode = getMatchingCode(guid, 2, RATE_PLAN_CODE_PATTERN);
        }
        if (propertyCode == null) {
            propertyCode = getMatchingCode(guid, 3, COMPONENT_CODE_PATTERN);
        }
        return propertyCode;
    }

    private static String getMatchingCode(String guid, int groupPos, Pattern pattern) {
        if (null != guid) {
            if(guid.matches(pattern.pattern())) {  //Look for exact match
                final Matcher matcher = pattern.matcher(guid.trim());
                if(matcher.find()) return matcher.group(groupPos); // find() to check a subset match
            }
        }
        return null;
    }

    public static boolean isPORatePlan(String ratePlan) {
        if (StringUtils.isNotEmpty(ratePlan)) {
            final Matcher matcher = PO_OFFER_PATTERN.matcher(ratePlan);
            return matcher.find();
        }
        return false;
    }

    public static int getPOSegment(String ratePlan) {
        if (StringUtils.isNotEmpty(ratePlan)) {
            final Matcher matcher = PO_OFFER_PATTERN.matcher(ratePlan);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(matcher.groupCount()));
            }
        }
        return 0;
    }

    public static CVSResponse.DOMINANT_PLAY_TYPE getPODominantPlay(String ratePlan) {
        if (StringUtils.isNotEmpty(ratePlan)) {
            final Matcher matcher = PO_OFFER_PATTERN.matcher(ratePlan);
            if (matcher.find()) {
                return CVSResponse.DOMINANT_PLAY_TYPE.getDominantPlay(matcher.group(matcher.groupCount()-1));
            }
        }
        return null;
    }

    public static boolean isPatronPromo(String promoCode) {
        if (StringUtils.isNotEmpty(promoCode)) {
            final Matcher matcher = PATRON_PROMO_PATTERN.matcher(promoCode.trim());
            return matcher.find();
        }
        return false;
    }

    public static int getPatronPromoId(String code) {
        if (StringUtils.isNotEmpty(code)) {
            final Matcher matcher = PATRON_PROMO_PATTERN.matcher(code.trim());
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(matcher.groupCount()));
            }
        }
        return -1;
    }
    public static boolean isHDEPackage(RoomReservation reservation) {
        boolean hasHDEProgram = false;
        if(null != reservation) {
            String programId = reservation.getProgramId();
            if (StringUtils.isNotBlank(programId)) {
                hasHDEProgram = isHDEProgram(reservation.getProgramId());
            } else if(CollectionUtils.isNotEmpty(reservation.getBookings())){
                hasHDEProgram = reservation.getBookings().stream().map(RoomPrice::getProgramId).distinct()
                        .anyMatch(id -> isHDEProgram(id));
            }
        }
        return hasHDEProgram;

    }
    public static boolean isHDEProgram(String programId){
        boolean hdeProgram = false;
        if(StringUtils.isNotEmpty(programId)){
            String programCode = programId;
            if(isAcrsGroupCodeGuid(programId)){
                programCode = getGroupCode(programId);
            }
            hdeProgram = ReservationUtil.isBlockCodeHdePackage(programCode);
        }
        return hdeProgram;
    }

}
