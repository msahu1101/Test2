package com.mgm.services.booking.room.model.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class RoomSegmentResponse {

    private String segment;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date minTravelPeriodStart;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date maxTravelPeriodEnd;
    private List<Program> programs = new ArrayList<>();

    public RoomSegmentResponse(String segment, List<Program> programs) {
        this.segment = segment;
        this.programs = programs;

        if (!CollectionUtils.isEmpty(programs)) {
            final List<Date> startDates = programs.stream()
                    .filter(x-> x.getTravelPeriodStart() != null)
                    .map(Program::getTravelPeriodStart)
                    .collect(Collectors.toList());
            final List<Date> endDates = programs.stream()
                    .filter(x-> x.getTravelPeriodEnd() != null)
                    .map(Program::getTravelPeriodEnd)
                    .collect(Collectors.toList());

            this.minTravelPeriodStart = CollectionUtils.isNotEmpty(startDates) ? Collections.min(startDates) : null;
            this.maxTravelPeriodEnd = CollectionUtils.isNotEmpty(endDates) ? Collections.max(endDates) : null;
        }

    }

    @AllArgsConstructor
    public static @Data class Program {

        private String programId;
        private String propertyId;
        @JsonFormat(
                pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
        private Date travelPeriodStart;
        @JsonFormat(
                pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
        private Date travelPeriodEnd;
    }
}
