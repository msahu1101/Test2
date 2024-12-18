package com.mgm.services.booking.room.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Health indicator specific to app use case to work with actuator health
 * endpoint
 *
 */
public class AppHealthIndicator implements HealthIndicator {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.boot.actuate.health.HealthIndicator#health()
     */
    @Override
    public Health health() {
        return new Health.Builder().up().build();
    }

}
