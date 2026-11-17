package com.trello.api.base;

import com.trello.api.config.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

public abstract class BaseApiTest {

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
                .build();
        // Request logging is deliberately off, it would print your key and token.
    }
}
