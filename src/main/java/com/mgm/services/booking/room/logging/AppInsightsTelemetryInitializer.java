package com.mgm.services.booking.room.logging;

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/*
 * Custom TelemetryInitializer that overrides the default SDK
 * behavior of treating response codes >= 400 as failed requests
 *
 */
public class AppInsightsTelemetryInitializer implements TelemetryInitializer {

    @Override
    public void initialize(Telemetry item) {

        RequestTelemetry telemetry = (RequestTelemetry) item;

        if (null == telemetry) {
            return;
        }

        int status = Integer.parseInt(telemetry.getResponseCode());
        if (status >= 400 && status < 500) {

            // If we set the Success property, the SDK won't change it:
            telemetry.setSuccess(true);

            // Allow to filter these requests in the portal:
            telemetry.getProperties().put("Overridden400s", "true");
        }

    }

}
