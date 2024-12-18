package com.mgm.services.booking.room.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.mgm.services.booking.room.annotations.validator.DateFormatValidator;

/**
 * Validate the with below conditions. 
 * 1. Can be null. 
 * 2. Should be in the format as given. 
 * 
 * @author jayveera
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateFormatValidator.class)
public @interface ValidateDateFormat {
    /**
     * Returns the error code in case of invalid input.
     * 
     * @return error code
     */
    String message() default "";

    /**
     * Allows the specification of validation groups, to which this constraint
     * belongs.
     * 
     * @return validation group
     */
    Class<?>[] groups() default {};

    /**
     * Return payload that can be used by clients of the Bean Validation API to
     * assign custom payload objects to a constraint.
     * 
     * @return
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * return pattern
     * 
     * @return String - input date pattern.
     */
    String pattern();

    /**
     * Returns error message.
     * @return String - error message.
     */
    String errorMessage();
}
