package com.mgm.services.booking.room.model.loyalty;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePlayerPromotionResponse {

    private String message;
    private Header header;

    @Data
    static class Header {
        String origin;
        String mgmCorerelationId;
        String apiVersion;
        String executionId;
        Status status;
    }

    @Data
    static class Status {
        String code;
        List<String> messages;
    }

}
