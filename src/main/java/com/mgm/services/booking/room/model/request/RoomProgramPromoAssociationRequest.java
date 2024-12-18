package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomProgramPromoAssociationRequest extends BaseRequest {

    private String propertyId;
    private String promo;
    private List<String> programIds;

}
