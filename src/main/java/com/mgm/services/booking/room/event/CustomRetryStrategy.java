/**
 * 
 */
package com.mgm.services.booking.room.event;

import com.microsoft.rest.retry.RetryStrategy;

import okhttp3.Response;

/**
 * Customized RetryStrategy with retryCount and retryInterval.
 * 
 * @author laknaray
 *
 */
public class CustomRetryStrategy extends RetryStrategy {

    private int retryCount;
    private long retryInterval;
    
    protected CustomRetryStrategy(int retryCount, long retryInterval) {
        super("Custom Retry Strategy", true);
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
    }

    @Override
    public boolean shouldRetry(int retryCount, Response response) {
        if (retryCount > this.retryCount || response.isSuccessful()) {
            return false;
        }
        try {
            Thread.sleep(retryInterval);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

}
