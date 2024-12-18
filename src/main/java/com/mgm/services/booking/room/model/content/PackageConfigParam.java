package com.mgm.services.booking.room.model.content;

public enum PackageConfigParam {
    segmentId("segmentId");

    private String value;

    PackageConfigParam(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
