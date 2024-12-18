package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Aurora Price Request class
 * @author nitpande0
 *
 */
public @SuperBuilder(toBuilder = true) @Getter @NoArgsConstructor class AuroraPriceRequest {

    private String source;
    private long customerId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numGuests;
    private String propertyId;
    private List<String> propertyIds;
    private String programId;
    private List<String> roomTypeIds;
    private List<String> programIds;
    private boolean enableMrd;
    private List<String> auroraItineraryIds;
    private boolean programRate;
    private int numChildren;
    private boolean ignoreChannelMargins;
    private int numRooms;
    private String operaConfirmationNumber;
    private String confirmationNumber;
    private String customerDominantPlay;
    private int customerRank;
    private boolean isGroupCode;
    private boolean isPerpetualOffer;
    private String mlifeNumber;
    private boolean includeSoldOutRooms;
    private boolean includeDefaultRatePlans;
    private String promoCode;
    private String promo;
    private boolean needLowestPrice;
    private String groupCnfNumber;
}

