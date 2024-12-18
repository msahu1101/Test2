package com.mgm.services.booking.room.annotations.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.annotations.ValidateDateFormat;
/**
 * Implementation for the @ValidateDate which validate date format.
 * 
 * @author jayveera
 *
 */

@Component
public class DateFormatValidator implements ConstraintValidator<ValidateDateFormat, String> {
    private String pattern;
    private String errorMessage;
    
    @Override
    public void initialize(ValidateDateFormat constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
        this.errorMessage = constraintAnnotation.errorMessage();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(StringUtils.isEmpty(value)) {
            return true;
        }
        try {
            new SimpleDateFormat(pattern).parse(value);
            return true;
        } catch (ParseException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
            return false;
        }
    }
}
