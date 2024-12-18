package com.mgm.services.booking.room.model.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Response object for my vegas validation response mapping
 * 
 * @author vararora
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class RedemptionValidationResponseDTO implements Serializable{
    private static final long serialVersionUID = 4865903939190150223L;

    @JsonProperty(value="AccountingUnits")
    private Integer accountingUnits;
    
    @JsonProperty(value="AgentRedemptionSteps")
    private String agentRedemptionSteps;
    
    @JsonProperty(value="Cancelable")
    private String cancelable;
    
    @JsonProperty(value="ConfirmationCode")
    private String confirmationCode;
    
    @JsonProperty(value="CouponCode")
    private String couponCode;
    
    @JsonProperty(value="RedemptionCode")
    private String redemptionCode;
    
    @JsonProperty(value="RequiredConfirmationCode")
    private String requiredConfirmationCode;
    
    @JsonProperty(value="ReservationDate")
    private String reservationDate;
    
    @JsonProperty(value="RewardType")
    private String rewardType;
    
    @JsonProperty(value="Status")
    private String status;
    
    @JsonProperty(value="TermsAndConditions")
    private String termsAndConditions;
    
    @JsonProperty(value="Error")
    private Error error;
    
    @JsonProperty(value="MyVegasUser")
    private Customer customer = new Customer();
    
    @JsonProperty(value="DatesRedemptionIsUnavailable")
    private List<DatesRedemptionIsUnAvailable> datesRedemptionIsUnAvailable;

    /**
     * Error class
     * @author vararora
     *
     */
    public static @Data class Error implements Serializable{
        private static final long serialVersionUID = 4865983939190150567L;
        
        @JsonProperty(value="errorCode")
        private String code;
        
        @JsonProperty(value="message")
        private String message;
    }

    /**
     * Dates Redemption is unavailable
     * @author vararora
     *
     */
    public static @Data class DatesRedemptionIsUnAvailable implements Serializable {
        private static final long serialVersionUID = 4866203939190150444L;
        
        @JsonProperty(value="Begining")
        private String beginDate;
        
        @JsonProperty(value="End")
        private String endDate;

    }
    
    /**
     * Customer class
     * @author vararora
     *
     */
    public static @Data class Customer implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        @JsonProperty(value="FirstName")
        private String firstName;
        
        @JsonProperty(value="LastName")
        private String lastName;
        
        @JsonProperty(value="Email")
        private String emailID;
        
        @JsonProperty(value="MembershipID")
        private String membershipID;
        
        @JsonProperty(value="DateOfBirth")
        private String dateOfBirth;
        
        @JsonProperty(value="DateOfEnrollment")
        private String dateOfEnrollment;
        
        @JsonProperty(value="OutletSpendValue")
        private String outletSpendValue;
        
        @JsonProperty(value="Tier")
        private String tier;
        
        @JsonProperty(value="Addresses")
        private List<Address> address;
        
        @JsonProperty(value="Phones")
        private List<Phone> phones;
        
        @JsonProperty(value="Balances")
        private Balance balances;

    }
    
    /**
     * Address class
     * @author vararora
     *
     */
    public static @Data class Address implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        @JsonProperty(value="Type")
        private String type;
        
        @JsonProperty(value="Street1")
        private String street1;
        
        @JsonProperty(value="Street2")
        private String street2;
        
        @JsonProperty(value="City")
        private String city; 
        
        @JsonProperty(value="State")
        private String state;
        
        @JsonProperty(value="PostalCode")
        private String postalCode;
        
        @JsonProperty(value="Country")
        private String country;
        
    }
    
    /**
     * Phone class
     * @author vararora
     *
     */
    public static @Data class Phone implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        @JsonProperty(value="Type")
        private String type;
        
        @JsonProperty(value="Number")
        private String number;
        
    }
    
    /**
     * Balance class
     * @author vararora
     *
     */
    public static @Data class Balance implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        @JsonProperty(value="FreePlay")
        private String freePlay;
        
        @JsonProperty(value="Comp")
        private String comp;
        
        @JsonProperty(value="TierName")
        private String tierName;
        
        @JsonProperty(value="PointPlay")
        private String pointPlay; 
        
        @JsonProperty(value="ExpressComps")
        private String expressComps;
        
        @JsonProperty(value="HGSPoints")
        private String hgsPoints;
        
        @JsonProperty(value="TierCredits")
        private String tierCredits;
        
        @JsonProperty(value="Reward")
        private String reward;
        
    }

}
