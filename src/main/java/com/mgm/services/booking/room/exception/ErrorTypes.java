package com.mgm.services.booking.room.exception;

/**
 * Enum to maintain the different error types defined for 
 * RBS service
 * @author swakulka
 *
 */
public enum ErrorTypes {

    VALIDATION_ERROR(1), FUNCTIONAL_ERROR(2), SYSTEM_ERROR (3),
    AUTHORIZATION_ERROR(7);

    private int errorTypeCode;

    ErrorTypes(int errorTypeCode) {
        this.errorTypeCode = errorTypeCode;
    }

    /**
     * return error type code.
     * 
     * @return errorTypeCode type code for error.
     */
    public int errorTypeCode() {
        return this.errorTypeCode;
    }
}
