package com.mgm.services.booking.room.exception;

public class TestExecutionException extends RuntimeException {

    /** Default Serial version UID */
    private static final long serialVersionUID = 1L;

    public TestExecutionException(String message) {
        super(message);
    }

    public TestExecutionException(Throwable cause) {
        super(cause);
    }

    public TestExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
