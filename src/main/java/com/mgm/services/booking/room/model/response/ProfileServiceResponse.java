package com.mgm.services.booking.room.model.response;

import lombok.Data;

public @Data
class ProfileServiceResponse {
    private ProfileService customer;

    @Data
    public static class ProfileService {
        private String id;

    }

}
