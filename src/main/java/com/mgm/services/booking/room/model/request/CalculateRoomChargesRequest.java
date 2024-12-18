package com.mgm.services.booking.room.model.request;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.model.HoldReservationBasicInfo;
import com.mgm.services.booking.room.model.reservation.PkgComponent;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * POJO to hold room reservation charges request
 * @author uttam
 *
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class CalculateRoomChargesRequest extends BaseRequest {

    @NotNull(message = "_invalid_property")
    private String propertyId;

    @NotNull(message = "_invalid_roomtype")
    private String roomTypeId;

    private String programId;
    private String guaranteeCode;
    private boolean isGroupCode;
    private boolean  perpetualPricing;
    private String promo;
    private boolean ignoreChannelMargins;
    private String customerDominantPlay;
    private int customerRank;    
    private boolean excludeNonOffer;
    private List<String> shoppedItineraryIds;

    private UserProfileRequest profile;

    @Valid
    private TripDetailsRequest tripDetails;
  
    private String confirmationNumber;
    private String redemptionCode;
    private String holdId;
    private List<String> specialRequests;
    private List<PkgComponent> pkgComponents;
    private List<HoldReservationBasicInfo> itemsInCart;

    @AssertTrue(
            message = "_invalid_property")
    public boolean isPropertyValid() {
        return ValidationUtil.isUuid(propertyId);
    }
    
    @AssertTrue(
            message = "_invalid_roomtype")
    public boolean isRoomValid() {
        return ValidationUtil.isUuid(roomTypeId) || ACRSConversionUtil.isAcrsRoomCodeGuid(roomTypeId);
    }
}
