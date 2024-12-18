package com.mgm.services.booking.room.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.Message;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.exception.ValidationException;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Global error handlers for all exceptions thrown by controllers or rolled up
 * all the way to controllers from service or dao implementations.
 *
 */
@Log4j2
@ControllerAdvice
public class GlobalErrorHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_STR = "Unexpected backend exception of type {}";

    /**
     * Generic exception handler for all types of exceptions thrown from
     * applications.
     * 
     * @param ex
     *            Exception thrown
     * @param request
     *            web request
     * @return Handles all exception gracefully and return am error message with
     *         appropriate status code
     */
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Message> handleAllExceptions(Exception ex, WebRequest request) {

        Message errorMessage = new Message(ServiceConstant.MESSAGE_TYPE_ERROR, ErrorCode.RUNTIME_ERROR.getErrorCode(),
                (String) ex.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;

        final String exceptionType = ex.getClass().getName();
        log.warn(ERROR_STR, exceptionType);

        // If exception is from aurora, use 500 status code and use 400 for
        // all other failures
        if (exceptionType.equals(EAuroraException.class.getName())) {
        	
            errorMessage = new Message(ServiceConstant.MESSAGE_TYPE_ERROR, ErrorCode.SYSTEM_ERROR.getErrorCode(),
                    "Exception from booking engine");
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } else if (exceptionType.equals(ValidationException.class.getName())) {

            ValidationException exception = (ValidationException) ex;
            String error = exception.getErrorCodes().get(0);
            ErrorCode errorCode = ErrorCode.getErrorCode(error);
            errorMessage = new Message(ServiceConstant.MESSAGE_TYPE_ERROR, errorCode.getErrorCode(),
                    errorCode.getDescription());

        } else if (exceptionType.equals(BusinessException.class.getName())) {

            BusinessException exception = (BusinessException) ex;
            ErrorCode errorCode = exception.getErrorCode();
            errorMessage = new Message(ServiceConstant.MESSAGE_TYPE_ERROR, errorCode.getErrorCode(),
                    errorCode.getDescription());

        } else if (exceptionType.equals(SystemException.class.getName())) {

            SystemException exception = (SystemException) ex;
            ErrorCode errorCode = exception.getErrorCode();
            errorMessage = new Message(ServiceConstant.MESSAGE_TYPE_ERROR, errorCode.getErrorCode(),
                    errorCode.getDescription());
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } else if (exceptionType.equals(WebExchangeBindException.class.getName())) {

            WebExchangeBindException exception = (WebExchangeBindException) ex;
            String error = exception.getAllErrors().get(0).getDefaultMessage();
            ErrorCode errorCode = ErrorCode.getErrorCode(error);
            errorMessage = new Message(ServiceConstant.MESSAGE_TYPE_ERROR, errorCode.getErrorCode(),
                    errorCode.getDescription());
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        log.error("Request resulted in an error: ", ex);
        log.error("[GlobalErrorHandler] Status: {}, Exception Type: {}, Trace: {}", status.value(), exceptionType, ExceptionUtils.getStackTrace(ex));

        return ResponseEntity.status(status).headers(responseHeaders).body(errorMessage);

    }
}
