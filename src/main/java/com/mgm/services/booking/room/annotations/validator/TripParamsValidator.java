package com.mgm.services.booking.room.annotations.validator;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.annotations.ValidateTripParams;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.util.ValidationUtil;

import lombok.extern.log4j.Log4j2;
/**
 * Implementation for the @ValidateTripDate which validate the trip date.
 * 
 * @author jayveera
 *
 */
@Component
@Log4j2
public class TripParamsValidator implements ConstraintValidator<ValidateTripParams, Object> {

    private String startDate;
    private String endDate;
    private String numberOfAdults;
    
    @Override
    public void initialize(ValidateTripParams constraintAnnotation) {
        this.startDate = constraintAnnotation.startDate();
        this.endDate = constraintAnnotation.endDate();
        this.numberOfAdults = constraintAnnotation.numberOfAdults();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        String startDateVal = getFieldValue(value, startDate);
        String endDateVal = getFieldValue(value, endDate);
        String numberOfAdultsVal = getFieldValue(value, numberOfAdults);
                
        if(StringUtils.isEmpty(startDateVal) && StringUtils.isEmpty(endDateVal) && Integer.parseInt(numberOfAdultsVal) == 0) {
            return true;
        }
        boolean tripDatesValid = ValidationUtil.isTripDatesValid(toLocalDate(startDateVal), toLocalDate(endDateVal));
        boolean numberOfAdultsValid = Integer.parseInt(numberOfAdultsVal) > 0;
        if(!tripDatesValid) {
            setErrorMessage(context, "_invalid_dates");
        }
        if(!numberOfAdultsValid) {
            setErrorMessage(context, "_invalid_num_adults");
        }
        return tripDatesValid && numberOfAdultsValid;
    }
    
    private String getFieldValue(Object object, String fieldName) {
        try {
            Class<?> clazz = object.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (field.get(object) == null ? StringUtils.EMPTY : String.valueOf(field.get(object)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Exception occured while accessing field : " + fieldName, e);
            return StringUtils.EMPTY;
        }
    }
    
    private void setErrorMessage(ConstraintValidatorContext context, String errorMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }
    
    private LocalDate toLocalDate(String input) {
        return Optional.ofNullable(CommonUtil.getDate(input, ServiceConstant.ISO_8601_DATE_FORMAT))
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).orElse(null);
    }
}
