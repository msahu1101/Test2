package com.mgm.services.booking.room.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Log4j2

public class JSonMapper extends ObjectMapper {

    public static class DoubleSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            final BigDecimal decimal = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
            generator.writeNumber(decimal.toPlainString());
        }
    }

    public static final class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

        @Override
        public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }

    }

    public static final class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> implements ContextualDeserializer {
        private final String format;
        private final TimeZone timeZone;

        public ZonedDateTimeDeserializer(String format, TimeZone timeZone) {
            this.format = format;
            this.timeZone = timeZone;
        }

        public ZonedDateTimeDeserializer() {
            this.format = null;
            this.timeZone = null;
        }

        @Override
        public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            if (p.getCurrentTokenId() == JsonTokenId.ID_STRING) {
                String str = p.getText().trim();
                DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
                if (!StringUtils.isEmpty(str)) {
                    if (!StringUtils.isEmpty(format)) {
                        formatter = DateTimeFormatter.ofPattern(format);
                        ZoneId of = ZoneOffset.UTC;
                        if (timeZone != null) {
                            of = timeZone.toZoneId();
                        }
                        final ZoneOffset zoneOffSet = of.getRules().getOffset(ZonedDateTime.now().toInstant());
                        str = str + zoneOffSet.getId();
                    }
                    return ZonedDateTime.parse(str, formatter);
                } else {
                    return null;
                }

            }
            return (ZonedDateTime) ctxt.handleUnexpectedToken(handledType(), p.getCurrentToken(), p,
                    "expected any date string in ISO-8601 date-time format or predefined custom format");

        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            final JsonFormat.Value value = findFormatOverrides(ctxt, property, handledType());
            return new ZonedDateTimeDeserializer(value.getPattern(), value.getTimeZone());
        }

        protected JsonFormat.Value findFormatOverrides(DeserializationContext ctxt, BeanProperty prop, Class<?> typeForDefaults) {
            if (prop != null) {
                return prop.findPropertyFormat(ctxt.getConfig(), typeForDefaults);
            }
            return ctxt.getDefaultPropertyFormat(typeForDefaults);
        }

    }

    public static final class LocalDateSerializer extends JsonSerializer<LocalDate> {

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

    }

    public static final class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

            if (p.getCurrentTokenId() == JsonTokenId.ID_STRING) {
                final String str = p.getText().trim();
                return (str.length() == 0) ? null : LocalDate.parse(str);
            }
            return (LocalDate) ctxt.handleUnexpectedToken(handledType(), p.getCurrentToken(), p, "expected String (yyyy-MM-dd)");

        }

    }

    public JSonMapper() {
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //
        this.setSerializationInclusion(Include.NON_NULL);
        this.setSerializationInclusion(Include.NON_EMPTY);
        //
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addSerializer(Double.class, new DoubleSerializer());

        this.registerModule(module);
    }

    public <T> T readValue(String content, Class<T> valueType) {
        try {
            return super.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            log.error("Unable to read redis json value {}-{}", e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    public String writeValueAsString(Object value) {
        try {
            return super.writeValueAsString(value);
        }catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }


    public byte[] writeValueAsBytes(Object value) {
        try {
            return super.writeValueAsBytes(value);
        }catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }
}
