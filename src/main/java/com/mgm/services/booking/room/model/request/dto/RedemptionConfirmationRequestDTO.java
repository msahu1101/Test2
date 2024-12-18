package com.mgm.services.booking.room.model.request.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * Response object for my vegas validation response mapping
 * 
 * @author vararora
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class RedemptionConfirmationRequestDTO implements Serializable{
    private static final long serialVersionUID = 4865903939190150223L;

    // Setting default value till there is a clarification
    private Integer accountingUnits = 0;
    private String agentRedemptionSteps;
    private boolean cancelable;
    private String confirmationNumber;
    private String couponCode;
    private String redemptionCode;
    private String requiredConfirmationCode;
    private String reservationDate;
    private String rewardType;
    private String status;
    private String termsAndConditions;
    private Customer customer = new Customer();
    private List<DatesRedemptionIsUnAvailable> datesRedemptionIsUnAvailable;

    /**
     * Dates Redemption is unavailable
     * @author vararora
     *
     */
    public static @Data class DatesRedemptionIsUnAvailable implements Serializable {
        private static final long serialVersionUID = 4866203939190150444L;
        private String beginDate;
        private String endDate;

    }
    
    /**
     * Customer class
     * @author vararora
     *
     */
    public static @Data class Customer implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        private String firstName;
        private String lastName;
        private String emailID;
        private String membershipID; 
        private String dateOfBirth;
        private String dateOfEnrollment;
        private String outletSpendValue;
        private String tier;
        private List<Address> address;
        private List<Phone> phones;
        private Balance balances;

    }
    
    /**
     * Address class
     * @author vararora
     *
     */
    public static @Data class Address implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        private String type;
        private String street1;
        private String street2;
        private String city; 
        private String state;
        private String postalCode;
        private String country;
        
    }
    
    /**
     * Phone class
     * @author vararora
     *
     */
    public static @Data class Phone implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        private String type;
        private String number;
        
    }
    
    /**
     * Balance class
     * @author vararora
     *
     */
    public static @Data class Balance implements Serializable {
        private static final long serialVersionUID = 4866201459190150985L;
        private String freePlay;
        private String comp;
        private String tierName;
        private String pointPlay; 
        private String expressComps;
        private String hgsPoints;
        private String tierCredits;
        private String reward;
        
    }

}
