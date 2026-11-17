package com.trello.api.base;

import com.trello.api.config.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.testng.annotations.BeforeClass;

public abstract class BaseApiTest {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_WAIT_MS = 2000;

    /** Authenticated request: base URL, /1 path, key + token and JSON content type. */
    protected RequestSpecification spec;

    @BeforeClass(alwaysRun = true)
    public void buildSpec() {
        spec = new RequestSpecBuilder()
                .setBaseUri(Config.baseUri())
                .setBasePath("/1")
                .addQueryParam("key", Config.require("trello.key"))
                .addQueryParam("token", Config.require("trello.token"))
                .setContentType(ContentType.JSON)
                .setConfig(retryOnGatewayErrors())
                .build();
        // Request logging is deliberately off, it would print your key and token.
    }

    /**
     * Trello's edge servers occasionally answer 502/503/504 for a request that is fine.
     * Retry those up to 3 times, 2 seconds apart, so one blip doesn't fail the whole run.
     * Real failures (400, 401, 403, 404 and so on) are never retried.
     */
    private static RestAssuredConfig retryOnGatewayErrors() {
        ServiceUnavailableRetryStrategy strategy = new ServiceUnavailableRetryStrategy() {
            @Override
            public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                int status = response.getStatusLine().getStatusCode();
                return executionCount <= MAX_RETRIES && (status == 502 || status == 503 || status == 504);
            }

            @Override
            public long getRetryInterval() {
                return RETRY_WAIT_MS;
            }
        };
        return RestAssuredConfig.config().httpClient(
                HttpClientConfig.httpClientConfig().httpClientFactory(
                        () -> HttpClientBuilder.create().setServiceUnavailableRetryStrategy(strategy).build()));
    }
}