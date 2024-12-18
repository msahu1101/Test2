package com.mgm.services.booking.room.logging;

import java.io.Serializable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import com.microsoft.applicationinsights.internal.common.LogTelemetryClientProxy;
import com.microsoft.applicationinsights.internal.common.TelemetryClientProxy;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

@Plugin(
        name = "AppInsightsMaskingAppender",
        category = "Core",
        elementType = "appender")
public class AppInsightsMaskingAppender extends AbstractAppender {

    private boolean isInitialized = false;
    private TelemetryClientProxy telemetryClientProxy;

    // Builder to create ApplicationInsights Appender Plugin, used by default by
    // log4j if present
    public static class MaskingAppenderBuilder implements org.apache.logging.log4j.core.util.Builder<AppInsightsMaskingAppender> {

        @PluginBuilderAttribute
        @Required(
                message = "No name provided for ApplicationInsightsAppender")
        private String name;

        // This is only needed when seperatly using Application Insights log4j2
        // appender
        // without application insights core module. otherwise AI-core module
        // will pick up Instrumentation-Key
        @PluginBuilderAttribute
        private String instrumentationKey;

        @PluginBuilderAttribute
        private boolean ignoreExceptions;

        @PluginElement("Layout")
        private Layout<? extends Serializable> layout;

        @PluginElement("Filter")
        private Filter filter;

        public MaskingAppenderBuilder setName(final String name) {
            this.name = name;
            return this;
        }

        public MaskingAppenderBuilder setInstrumentationKey(final String instrumentationKey) {
            this.instrumentationKey = instrumentationKey;
            return this;
        }

        public MaskingAppenderBuilder setIgnoreExceptions(boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public MaskingAppenderBuilder setLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return this;
        }

        public MaskingAppenderBuilder setFilter(Filter filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public AppInsightsMaskingAppender build() {
            return new AppInsightsMaskingAppender(name, instrumentationKey, filter, layout, ignoreExceptions);
        }
    }

    /**
     * Constructs new Application Insights appender.
     * 
     * @param name
     *            The appender name.
     * @param instrumentationKey
     *            The instrumentation key.
     */
    protected AppInsightsMaskingAppender(String name, String instrumentationKey) {
        super(name, null, null, false, null);

        try {
            telemetryClientProxy = new LogTelemetryClientProxy(instrumentationKey);
            this.isInitialized = true;
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            this.isInitialized = false;
            InternalLogger.INSTANCE.error(
                    "Failed to initialize appender with exception: %s. Generated Stack trace is %s.", e.toString(),
                    ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * @param name
     *            The Appender name
     * @param instrumentationKey
     *            The AI-resource iKey
     * @param filter
     *            log4j2 Filter object
     * @param layout
     *            Log4j2 Layout object
     * @param ignoreExceptions
     *            true/false to determine if exceptions should be ignored
     */
    protected AppInsightsMaskingAppender(String name, String instrumentationKey, Filter filter,
            Layout<? extends Serializable> layout, boolean ignoreExceptions) {

        super(name, filter, layout, ignoreExceptions, null);

        try {
            telemetryClientProxy = new LogTelemetryClientProxy(instrumentationKey);
            this.isInitialized = true;
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            this.isInitialized = false;
            InternalLogger.INSTANCE.error(
                    "Failed to initialize appender with exception: %s. Generated Stack trace is %s.", e.toString(),
                    ExceptionUtils.getStackTrace(e));
        }
    }

    public LogTelemetryClientProxy getTelemetryClientProxy() {
        return (LogTelemetryClientProxy) this.telemetryClientProxy;
    }

    /**
     * Logs a LogEvent using whatever logic this Appender wishes to use. It is
     * typically recommended to use a bridge pattern not only for the benefits
     * from decoupling an Appender from its implementation, but it is also handy
     * for sharing resources which may require some form of locking.
     *
     * @param event
     *            The LogEvent.
     */
    @Override
    public void append(LogEvent event) {
        if (!this.isStarted() || !this.isInitialized) {
            // trace not started or not initialized.
            return;
        }

        try {
            ApplicationInsightsMaskLogEvent aiEvent = new ApplicationInsightsMaskLogEvent(event);

            this.getTelemetryClientProxy().sendEvent(aiEvent);
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            InternalLogger.INSTANCE.error("Something unexpected happened while sending logs %s",
                    ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Returns a plugin Builder object which is used internally by Log4j2 to
     * create plugin
     * 
     * @return
     */
    @PluginBuilderFactory
    public static MaskingAppenderBuilder newBuilder() {
        return new MaskingAppenderBuilder();
    }

    /**
     * Creates new appender with the given name and instrumentation key. This
     * method is being called on the application startup upon Log4j system
     * initialization.
     * 
     * @param name
     *            The appender name.
     * @param instrumentationKey
     *            The instrumentation key.
     * @return New Application Insights appender.
     */
    @PluginFactory
    public static AppInsightsMaskingAppender createAppender(@PluginAttribute("name") String name,
            @PluginAttribute("instrumentationKey") String instrumentationKey) {

        return new AppInsightsMaskingAppender(name, instrumentationKey);
    }

}
