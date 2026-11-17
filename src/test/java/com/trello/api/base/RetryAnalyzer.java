package com.trello.api.base;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a test only when it failed because Trello's edge servers answered 502, 503 or 504.
 * Real failures (wrong status code, wrong body, 4xx) are never retried.
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    private static final int MAX_RETRIES = 3;
    private static final long WAIT_MS = 2000;
    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        Throwable error = result.getThrowable();
        String message = error == null ? "" : String.valueOf(error.getMessage());
        boolean gatewayError = message.contains("but was <502>")
                || message.contains("but was <503>")
                || message.contains("but was <504>");

        if (gatewayError && attempts < MAX_RETRIES) {
            attempts++;
            try {
                Thread.sleep(WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }
        return false;
    }
}