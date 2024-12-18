package com.mgm.services.booking.room.model.response;

import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CVSResponse {

    private CVSCustomer customer;

    private Map<String, CVSCustomerGrade> customerGradeByPropertyMap = new HashMap<>();
    private Map<String, Integer> rankByPropertyIdMap = new HashMap<>();

    public static enum DOMINANT_PLAY_TYPE {
        TABLE, SLOTS, POKER;

        public static DOMINANT_PLAY_TYPE getDominantPlay(String data) {
            DOMINANT_PLAY_TYPE result = null;

            // Compare by exact match
            if (StringUtils.isNotEmpty(data)) {
                for(DOMINANT_PLAY_TYPE value: values()) {
                    if (data.equalsIgnoreCase(value.name())) {
                        result = value;
                    }
                }
            }

            // Compare by short form
            if ("S".equalsIgnoreCase(data)) {
                result = DOMINANT_PLAY_TYPE.SLOTS;
            } else if ("T".equalsIgnoreCase(data)) {
                result = DOMINANT_PLAY_TYPE.TABLE;
            } else if ("P".equalsIgnoreCase(data)) {
                result = DOMINANT_PLAY_TYPE.POKER;
            }

            return result;
        }
    }

    @Data
    public static class CVSCustomer {
        private CVSCustomerIdentity identity;
        private CVSCustomerValue [] customerValues;
    }

    @Data
    public static class CVSCustomerIdentity {
        private String corporateCustomerId;
        private String mlifeNo;
        private String mlifeTier;
    }

    @Data
    public static class CVSCustomerValue {
        private String property;
        private List<String> gsePropertyIds = new ArrayList<>();
        private CVSValue value;
    }

    @Data
    public static class CVSValue {
        private CVSCustomerGrade customerGrade;
    }

    @Data
    public static class CVSCustomerGrade {
        private Integer powerRank;
        private Integer segment;
        private String dominantPlay;
    }

    public int getSegmentOrRank(String propertyId) {
        initSegmentRankMap();
        final CVSCustomerGrade cvsCustomerGrade = customerGradeByPropertyMap.get(propertyId);
        if (cvsCustomerGrade != null) {
            final Integer segment = cvsCustomerGrade.getSegment();
            final Integer rank = cvsCustomerGrade.getPowerRank();
            if (null != segment && segment > 0) {
                return segment;
            }
            if (null != rank) {
                return rank;
            }
        }
        return 0;
    }

    public DOMINANT_PLAY_TYPE getDominantPlay(String propertyId) {
        initSegmentRankMap();
        final CVSCustomerGrade cvsCustomerGrade = customerGradeByPropertyMap.get(propertyId);
        if (cvsCustomerGrade != null) {
            final String dominantPlay = cvsCustomerGrade.getDominantPlay();
            if (StringUtils.isNotEmpty(dominantPlay)) {
                return DOMINANT_PLAY_TYPE.getDominantPlay(dominantPlay);
            }
        }
        return null;
    }
    
    public Map<String, Integer> getRanks() {
        initSegmentRankMap();
        return rankByPropertyIdMap;
    }

    private void initSegmentRankMap() {
        if (customerGradeByPropertyMap.size() == 0 && null != this.customer) {
            final CVSResponse.CVSCustomerValue[] customerValues = customer.getCustomerValues();
            if (null != customerValues) {
                for (CVSCustomerValue customerValue : customerValues) {
                    final List<String> propertyIds = customerValue.getGsePropertyIds();
                    final CVSValue value = customerValue.getValue();
                    if (null != value) {
                        final CVSCustomerGrade customerGrade = value.getCustomerGrade();
                        if (null != customerGrade) {
                            for (String propertyId : propertyIds) {
                                customerGradeByPropertyMap.put(propertyId, customerGrade);
                                rankByPropertyIdMap.put(propertyId, customerGrade.getPowerRank());
                            }
                        }
                    }
                }
            }
        }
    }

}
