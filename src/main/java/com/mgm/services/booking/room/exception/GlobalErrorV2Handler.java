package com.mgm.services.booking.room.exception;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.google.common.base.Stopwatch;
import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.util.LiveSiteHelper;
import com.mgm.services.common.exception.AuthorizationException;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

/**
 * Global error handler for all RBS APIs.
 * 
 * @author swakulka
 *
 */
@Log4j2
@ControllerAdvice(annotations = V2Controller.class)
@Order(1)
public class GlobalErrorV2Handler extends ResponseEntityExceptionHandler {

    @Autowired
    ErrorResponseBuilder errResponseBuilder;
    
    @Autowired
    LiveSiteHelper liveSiteHelper;

    /**
     * Generic exception handler for all types of exceptions thrown from
     * applications.
     * 
     * @param ex      Exception thrown
     * @param request web request
     * @return Handles all exception gracefully and return am error message with
     *         appropriate status code
     */
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {

        
    	Stopwatch watch = Stopwatch.createStarted();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errResponse = null;

        final String exceptionType = ex.getClass().getName();
        
        log.warn("Request resulted in an error of type: {} ", exceptionType);

        if (exceptionType.equals(ValidationException.class.getName())) {
            ValidationException exception = (ValidationException) ex;
            String error = exception.getErrorCodes().get(0);
            ErrorCode errorCode = ErrorCode.getErrorCode(error);

            errResponse = errResponseBuilder.buildErrorResponse(
                    String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                            ErrorTypes.VALIDATION_ERROR.errorTypeCode(), errorCode.getNumericCode()),
                    errorCode.getDescription());
        } else if (exceptionType.equals(SystemException.class.getName())) {
            SystemException exception = (SystemException) ex;
            ErrorCode errorCode = exception.getErrorCode();

            errResponse = errResponseBuilder.buildErrorResponse(
                    String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                            ErrorTypes.SYSTEM_ERROR.errorTypeCode(), errorCode.getNumericCode()),
                    errorCode.getDescription());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (exceptionType.equals(BusinessException.class.getName())) {
            BusinessException exception = (BusinessException) ex;
            ErrorCode errorCode = exception.getErrorCode();
            errResponse = errResponseBuilder.buildErrorResponse(
                    String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                            ErrorTypes.FUNCTIONAL_ERROR.errorTypeCode(), errorCode.getNumericCode()),
                    (StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : errorCode.getDescription()));
        } else if(exceptionType.equals(AuthorizationException.class.getName())) {
        	AuthorizationException exception = (AuthorizationException) ex;
            ErrorCode errorCode = exception.getErrorCode();
            errResponse = errResponseBuilder.buildErrorResponse(
                    String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                            ErrorTypes.AUTHORIZATION_ERROR.errorTypeCode(), errorCode.getNumericCode()),
                    errorCode.getDescription());
            status = HttpStatus.UNAUTHORIZED;
        }
        
        else {
            errResponse = errResponseBuilder.buildErrorResponse(
                    String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                            ErrorTypes.SYSTEM_ERROR.errorTypeCode(), ErrorCode.SYSTEM_ERROR.getNumericCode()),
                    ex.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.error("Request resulted in an error: ", ex);
        log.info("[GlobalErrorV2Handler] Status: {}, Exception Type: {}, Trace: {}", status.value(), exceptionType, ExceptionUtils.getStackTrace(ex));
        
        ThreadContext.put("HTTP_ERROR_STATUS", String.valueOf(status.value()));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        HttpServletResponse httpResponse = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getResponse();
        watch.stop();
        
		log.info("Publishing error event to live site..");

        liveSiteHelper.publishErrorEventToLivesite(httpRequest, httpResponse, status.value(), errResponse.getError().getCode(), errResponse.getError().getMessage(), watch);
        return ResponseEntity.status(status).headers(responseHeaders).body(errResponse);
    }
}
