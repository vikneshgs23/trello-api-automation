package com.trello.api.tests;

import static io.restassured.RestAssured.given;

import com.trello.api.base.BaseApiTest;
import com.trello.api.base.RetryAnalyzer;
import com.trello.api.config.Config;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.Test;

/** Negative checks. If a status code differs on your first run, check Trello's response and adjust it. */
public class TrelloNegativeTest extends BaseApiTest {

    private RequestSpecification noAuth() {
        return given().baseUri(Config.baseUri()).basePath("/1");
    }

   @Test(retryAnalyzer = RetryAnalyzer.class)
    public void invalidToken_returns401() {
        noAuth().queryParam("key", Config.require("trello.key"))
                .queryParam("token", "invalid-token")
                .when().get("/members/me")
                .then().log().ifValidationFails()
                .statusCode(401);
    }

    // Trello answers 400 with the text "invalid token" when no key/token is sent at all.
   @Test(retryAnalyzer = RetryAnalyzer.class)
    public void missingCredentials_returns400() {
        noAuth()
                .when().get("/members/me")
                .then().log().ifValidationFails()
                .statusCode(400)
                .body(org.hamcrest.Matchers.containsString("invalid token"));
    }

   @Test(retryAnalyzer = RetryAnalyzer.class)
    public void createBoardWithoutName_returns400() {
        given().spec(spec)
                .when().post("/boards")
                .then().log().ifValidationFails()
                .statusCode(400);
    }

   @Test(retryAnalyzer = RetryAnalyzer.class)
    public void getUnknownBoard_returns404() {
        given().spec(spec)
                .when().get("/boards/{id}", "000000000000000000000000")
                .then().log().ifValidationFails()
                .statusCode(404);
    }
}
