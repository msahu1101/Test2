/**
 * 
 */
package com.mgm.services.booking.room.model.request;

import java.io.Serializable;
import java.util.Map;

import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.model.RedemptionValidationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Price Request class
 * 
 * @author nitpande0
 *
 */
@EqualsAndHashCode(
        callSuper = true)
public @Data class BasePriceRequest extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 2651115289234401064L;

    private String source;
    private long customerId;
    private String mlifeNumber;
    private String propertyId;
    private String programId;
    private String promoCode;
    private String promo;
    private Map<String, RedemptionValidationResponse> myVegasRedemptionItems;
    private Customer customer;
    private boolean validMyVegasProgram;
    private String redemptionCode;
    private boolean isModifyFlow;

}
