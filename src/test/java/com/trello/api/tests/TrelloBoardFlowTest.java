package com.trello.api.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.trello.api.base.BaseApiTest;
import com.trello.api.config.Config;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * API version of the 10 Trello test cases. One board is created in Trello_01, reused by the
 * later tests and deleted in Trello_10. The @AfterClass hook deletes it too if a test fails midway,
 * so no boards are left behind. 'priority' sets the run order, 'dependsOnMethods' skips a test
 * when the data it needs was never created.
 */
public class TrelloBoardFlowTest extends BaseApiTest {

    private String boardName;
    private String boardId;
    private String listOneId;
    private String listTwoId;
    private String cardId;
    private String labelId;

    private static final String LIST_ONE = "ToDo-Api";
    private static final String LIST_TWO = "Doing-Api";
    private static final String CARD_NAME = "ApiCardOne";
    private static final String COMMENT = "Comment added by API test";

    @Test(priority = 0)
    public void Trello_00_credentialsAreValid() {
        given().spec(spec)
                .when().get("/members/me")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("username", notNullValue());
    }

    @Test(priority = 1, dependsOnMethods = "Trello_00_credentialsAreValid")
    public void Trello_01_createBoard() {
        boardName = "ApiBoard-" + System.currentTimeMillis();
        boardId = given().spec(spec)
                .queryParam("name", boardName)
                .queryParam("defaultLists", false)
                .when().post("/boards")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("name", equalTo(boardName))
                .body("closed", equalTo(false))
                .extract().path("id");
        Assert.assertNotNull(boardId, "Board id missing in response");
    }

    @Test(priority = 2, dependsOnMethods = "Trello_01_createBoard")
    public void Trello_02_addLists() {
        listOneId = createList(LIST_ONE);
        listTwoId = createList(LIST_TWO);

        given().spec(spec)
                .when().get("/boards/{id}/lists", boardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("id", hasItems(listOneId, listTwoId))
                .body("name", hasItems(LIST_ONE, LIST_TWO));
    }

    @Test(priority = 3, dependsOnMethods = "Trello_02_addLists")
    public void Trello_03_addCard() {
        cardId = given().spec(spec)
                .queryParam("idList", listOneId)
                .queryParam("name", CARD_NAME)
                .when().post("/cards")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("name", equalTo(CARD_NAME))
                .body("idList", equalTo(listOneId))
                .extract().path("id");
    }

    @Test(priority = 4, dependsOnMethods = "Trello_03_addCard")
    public void Trello_04_moveCardBetweenLists() {
        given().spec(spec)
                .queryParam("idList", listTwoId)
                .when().put("/cards/{id}", cardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("idList", equalTo(listTwoId));

        given().spec(spec)
                .when().get("/lists/{id}/cards", listTwoId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("id", hasItem(cardId));

        given().spec(spec)
                .when().get("/lists/{id}/cards", listOneId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("id", not(hasItem(cardId)));
    }

    @Test(priority = 5, dependsOnMethods = "Trello_03_addCard")
    public void Trello_05_addDueDate() {
        String due = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS).toString();
        String returned = given().spec(spec)
                .queryParam("due", due)
                .when().put("/cards/{id}", cardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("due", notNullValue())
                .extract().path("due");
        Assert.assertEquals(Instant.parse(returned), Instant.parse(due), "Due date saved incorrectly");
    }

    @Test(priority = 6, dependsOnMethods = "Trello_03_addCard")
    public void Trello_06_addLabel() {
        labelId = given().spec(spec)
                .queryParam("name", "ApiLabel")
                .queryParam("color", "green")
                .queryParam("idBoard", boardId)
                .when().post("/labels")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("name", equalTo("ApiLabel"))
                .extract().path("id");

        given().spec(spec)
                .queryParam("value", labelId)
                .when().post("/cards/{id}/idLabels", cardId)
                .then().log().ifValidationFails()
                .statusCode(200);

        given().spec(spec)
                .when().get("/cards/{id}", cardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("idLabels", hasItem(labelId));
    }

    @Test(priority = 7, dependsOnMethods = "Trello_01_createBoard")
    public void Trello_07_inviteMemberToBoard() {
        String email = Config.get("trello.member.email");
        if (email == null) {
            throw new SkipException("trello.member.email is not configured, skipping the invite test");
        }
        // Sends a real invitation to that address.
        Response response = given().spec(spec)
                .queryParam("email", email)
                .queryParam("type", "normal")
                .body("{\"fullName\":\"API Test Member\"}")
                .when().put("/boards/{id}/members", boardId)
                .then().extract().response();

        // 403 "Member already invited" means a pending invitation already exists from an earlier run, so that is accepted too.
        boolean invited = response.statusCode() == 200;
        boolean alreadyInvited = response.statusCode() == 403 && response.asString().contains("already invited");
        Assert.assertTrue(invited || alreadyInvited,
                "Unexpected invite response: " + response.statusCode() + " " + response.asString());
    }

    // Runs before the archive test (priority 9) because it comments on the card while the card is still open.
    @Test(priority = 8, dependsOnMethods = "Trello_03_addCard")
    public void Trello_09_addCommentToCard() {
        given().spec(spec)
                .queryParam("text", COMMENT)
                .when().post("/cards/{id}/actions/comments", cardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("data.text", equalTo(COMMENT));

        given().spec(spec)
                .queryParam("filter", "commentCard")
                .when().get("/cards/{id}/actions", cardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("data.text", hasItem(COMMENT));
    }

    @Test(priority = 9, dependsOnMethods = "Trello_03_addCard")
    public void Trello_08_archiveCard() {
        given().spec(spec)
                .queryParam("closed", true)
                .when().put("/cards/{id}", cardId)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("closed", equalTo(true));

        given().spec(spec)
                .when().get("/boards/{id}/cards", boardId)       // open cards only
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("id", not(hasItem(cardId)));
    }

    @Test(priority = 10, dependsOnMethods = "Trello_01_createBoard")
    public void Trello_10_deleteBoard() {
        given().spec(spec)
                .when().delete("/boards/{id}", boardId)
                .then().log().ifValidationFails()
                .statusCode(200);

        given().spec(spec)
                .when().get("/boards/{id}", boardId)
                .then().log().ifValidationFails()
                .statusCode(404);

        boardId = null;   // already deleted, so the cleanup hook has nothing to do
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        if (boardId != null) {
            given().spec(spec).when().delete("/boards/{id}", boardId);
        }
    }

    private String createList(String name) {
        return given().spec(spec)
                .queryParam("name", name)
                .queryParam("idBoard", boardId)
                .when().post("/lists")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("name", equalTo(name))
                .extract().path("id");
    }
}
