package com.mgm.services.booking.room.config;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.mgm.services.booking.room.model.crs.reservation.CustomData;
import com.mgm.services.booking.room.model.request.ReservationCustomData;

public class CustomDataDeserializer extends JsonDeserializer<CustomData>{
    @Override
    public CustomData deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        final ObjectCodec oc = p.getCodec();
        final ReservationCustomData readValue = oc.readValue(p, ReservationCustomData.class);
        return readValue;
    }

}
