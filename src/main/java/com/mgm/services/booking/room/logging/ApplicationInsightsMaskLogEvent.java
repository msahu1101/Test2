package com.mgm.services.booking.room.logging;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.spi.StandardLevel;

import com.mgm.services.booking.room.util.LogMask;
import com.microsoft.applicationinsights.internal.common.ApplicationInsightsEvent;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

public class ApplicationInsightsMaskLogEvent extends ApplicationInsightsEvent {

    private LogEvent logEvent;

    public ApplicationInsightsMaskLogEvent(LogEvent logEvent) {
        this.logEvent = logEvent;
    }

    @Override
    public String getMessage() {
        String redactedStr = LogMask.mask(this.logEvent.getMessage() != null ?
                this.logEvent.getMessage().getFormattedMessage() :
                "Log4j Trace");

        //Need to add all the files that make calls to ACRS
        if(this.logEvent.getLoggerName().toLowerCase().contains("acrs") || this.logEvent.getLoggerName().toLowerCase().contains("payment")) {
            return LogMask.maskACRSMessage(redactedStr);
        }
       return redactedStr;

    }

    @Override
    public boolean isException() {
        return this.logEvent.getThrown() != null;
    }

    @Override
    public Exception getException() {
        Exception exception = null;

        if (isException()) {
            Throwable throwable = this.logEvent.getThrown();
            exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
        }

        return exception;
    }

    @Override
    public Map<String, String> getCustomParameters() {

        Map<String, String> metaData = new HashMap<>();

        metaData.put("SourceType", "Log4j");

        addLogEventProperty("LoggerName", logEvent.getLoggerName(), metaData);
        addLogEventProperty("LoggingLevel", logEvent.getLevel() != null ? logEvent.getLevel().name() : null, metaData);
        addLogEventProperty("ThreadName", logEvent.getThreadName(), metaData);
        addLogEventProperty("TimeStamp", getFormattedDate(logEvent.getTimeMillis()), metaData);

        if (isException()) {
            addLogEventProperty("Logger Message", getMessage(), metaData);
        }

        if (logEvent.isIncludeLocation()) {
            StackTraceElement stackTraceElement = logEvent.getSource();

            addLogEventProperty("ClassName", stackTraceElement.getClassName(), metaData);
            addLogEventProperty("FileName", stackTraceElement.getFileName(), metaData);
            addLogEventProperty("MethodName", stackTraceElement.getMethodName(), metaData);
            addLogEventProperty("LineNumber", String.valueOf(stackTraceElement.getLineNumber()), metaData);
        }

        for (Map.Entry<String, String> entry : logEvent.getContextData().toMap().entrySet()) {
            addLogEventProperty(entry.getKey(), entry.getValue(), metaData);
        }

        return metaData;
    }

    @Override
    public SeverityLevel getNormalizedSeverityLevel() {
        int log4jLevelAsInt = logEvent.getLevel().intLevel();

        switch (StandardLevel.getStandardLevel(log4jLevelAsInt)) {
            case FATAL:
                return SeverityLevel.Critical;

            case ERROR:
                return SeverityLevel.Error;

            case WARN:
                return SeverityLevel.Warning;

            case INFO:
                return SeverityLevel.Information;

            case TRACE:
            case DEBUG:
            case ALL:
                return SeverityLevel.Verbose;

            default:
                InternalLogger.INSTANCE.error("Unknown Log4j v2 option, %d, using TRACE level as default", log4jLevelAsInt);
                return SeverityLevel.Verbose;
        }
    }
}

