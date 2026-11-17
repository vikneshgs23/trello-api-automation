package com.trello.api.base;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/** Re-runs a failed test up to 2 more times. Only use it on stateless tests, such as the negative ones. */
public class RetryAnalyzer implements IRetryAnalyzer {
    private static final int MAX_RETRIES = 2;
    private int count = 0;

    @Override
    public boolean retry(ITestResult result) {
        return count++ < MAX_RETRIES;
    }
}