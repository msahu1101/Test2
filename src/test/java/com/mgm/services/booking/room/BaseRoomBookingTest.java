package com.mgm.services.booking.room;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.mgm.services.booking.room.config.CustomDataDeserializer;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.crs.reservation.CustomData;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

@Log4j2
public class BaseRoomBookingTest {

    protected static ObjectMapper mapper = null;
    protected static ObjectMapper crsMapper = null;

    @BeforeClass
    public static void runOnceBeforeClass() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // CRSMapper
        crsMapper = new ObjectMapper();
        final SimpleModule customDataModule = new SimpleModule();
        customDataModule.addDeserializer(CustomData.class, new CustomDataDeserializer());
        crsMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(customDataModule)
                .registerModule(ReservationUtil.getJavaTimeModuleISO());
    }

    public BaseRoomBookingTest() {
        super();
    }

    protected static void staticInit() {
        // Static initializer called from child classes
        // Used for initializing static dependencies.
        if (null == mapper || null == crsMapper) {
            runOnceBeforeClass();
        }
    }
    
    public <T> T convert(File file, Class<T> target) {

        try {

            return mapper.readValue(file, target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public <T> T convert(String fileName, Class<T> target) {

        File file = new File(getClass().getResource(fileName).getPath());
        return convert(file, target);
    }
    
    public <T> T convertCrs(String fileName, Class<T> target) {
	File file = new File(getClass().getResource(fileName).getPath());
	return convertCrs(file, target);

    }
    public <T> T convertCrs(File file, Class<T> target) {
        try {
            return crsMapper.readValue(file, target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
            fail();
        }
        return null;
    }

    public <T> T convertCrs(File file, CollectionType type) {
        try {
            return crsMapper.readValue(file, type);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public <T> List<T> convertCrsList(String fileName, Class<T> target) {
        File file = new File(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        return convertCrsList(file, target);
    }

    public <T> List<T> convertCrsList(File file, Class<T> target) {
        try {
            return crsMapper.readValue(file, crsMapper.getTypeFactory().constructCollectionType(List.class, target));
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public <T> T convert(File file, CollectionType type) {

        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    protected Date getFutureDate(int days) {
        // Adds number of days from current date
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, days);

        return cal.getTime();
    }

    protected LocalDate getFutureLocalDate(int days) {
        return getFutureDate(days).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    protected String getErrorMessage(ErrorCode errorCode) {
    	return "<" + errorCode.getErrorCode() + ">[ " + errorCode.getDescription() + " ]";
    }
    public <T> T convertRBSReq(String fileName, Class<T> target) {
        File file = new File(getClass().getResource(fileName).getPath());
        return convertRBSReq(file, target);

    }
    public <T> T convertRBSReq(File file, Class<T> target) {
        try {
            ObjectMapper reqMapper = new ObjectMapper();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            reqMapper.setDateFormat(df);
            reqMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            reqMapper.setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
            return reqMapper.readValue(file, target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }
}