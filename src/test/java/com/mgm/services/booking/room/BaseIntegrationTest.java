package com.mgm.services.booking.room;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgm.services.booking.room.constant.TestConstant;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BaseIntegrationTest {

    public static String baseUrl;
    public static String envPrefix;
    public static String deploymentEnv;
    protected static ObjectMapper mapper = new ObjectMapper();
    protected static final SimpleDateFormat v2Format = new SimpleDateFormat("yyyy-MM-dd");
    
    protected static <T> T getDefaultTestData(String fileName, Class<T> target) {
        String envFileName = getEnvSpecificFileName(fileName);
        return getObjectFromJSON(envFileName, target);
    }
    
    /**
     * This method returns the environment specific test data file. It tries to
     * load the file with the format <envPrefix>-<fileName> like
     * qa-test-data.json for qa environment If the file is not found, then it
     * loads the default file with the name <fileName> like test-data.json
     * 
     * @param fileName
     *            the <fileName>
     * @param fileFormat
     *            default is </><envPrefix><fileName>
     * @return environment specific file name
     */
    public static String getEnvSpecificFileName(String fileName) {
        String envFileName = String.format(TestConstant.TEST_DATA_FILE_FORMAT, "/", envPrefix,
                fileName.replaceFirst("/", ""));
        log.info(envFileName);
        File file = new File(BaseRoomBookingIntegrationTest.class.getResource(envFileName).getPath());
        if (!file.exists()) {
            envFileName = fileName;
        }
        log.info("Loading Data from File::{}", envFileName);
        return envFileName;
    }
    
    public static <T> T getObjectFromJSON(String filePath, Class<T> target) {

        File requestFile = new File(BaseRoomBookingIntegrationTest.class.getResource(filePath).getPath());
        T obj = convert(requestFile, target);

        return obj;

    }
    
    public static <T> T convert(File file, Class<T> target) {

        try {
            ArrayList<Module> modules = new ArrayList<>();
            modules.add(new JavaTimeModule());
            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build();
            return mapper.readValue(new FileInputStream(file), target);
        } catch (IOException e) {
            log.error("Exception trying to convert file to json: ", e);
        }
        return null;
    }

    public static <T> T convert(String json, Class<T> target) {

        try {
            ArrayList<Module> modules = new ArrayList<>();
            modules.add(new JavaTimeModule());
            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build();
            return mapper.readValue(json, target);
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

    public void addDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Exception trying to put the thread to sleep: ", e);
        }
    }
    
    protected static Date getFutureDateObj(int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, days);

        return c.getTime();
    }

    protected static String getFutureDate(int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, days);

        Date dateObj = c.getTime();
        return v2Format.format(dateObj);
    }

    protected static String getPastDate() {
        // Using -3 days from today as check-in date
        return getFutureDate(-3);
    }

    protected static String getCheckInDate() {
        // Using 6 days from today as check-in date
        return getFutureDate(5);
    }

    protected static String getCheckOutDate() {
        // Using 8 days from today check-out date
        return getFutureDate(8);
    }
    
    protected Date getDate(String dateStr) {
        try {
            return v2Format.parse(dateStr);
        } catch (ParseException e) {
            log.error("Error converting to date");
        }
        return null;
    }
    
    protected Date addDays(String dateStr, int days) {
        Date date = getDate(dateStr);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }
    
    protected String getDateAsString(Date date) {
        return v2Format.format(date);
    }
}
