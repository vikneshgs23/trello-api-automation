# Trello API Automation (REST Assured + TestNG)

API test suite for the [Trello REST API](https://developer.atlassian.com/cloud/trello/rest/api-group-actions/), written in Java with REST Assured and TestNG. It covers the main board workflow end to end (board, lists, cards, labels, due dates, comments, archive, delete) plus negative tests for authentication and validation.

[![API tests](https://github.com/vikneshgs23/trello-api-automation/actions/workflows/api-tests.yml/badge.svg)](https://github.com/vikneshgs23/trello-api-automation/actions/workflows/api-tests.yml)

**Live test report:** https://vikneshgs23.github.io/trello-api-automation/emailable-report.html
(updated by every run; runs automatically every day)

## Why API tests

This workflow started as a Selenium UI automation project during my QSpiders training. Trello now asks for an email OTP when it sees a fresh browser session, which blocks automated UI login. Testing the same workflow through the REST API is stable, needs no manual login, and runs for anyone with an API key and token.

## Tech stack

- Java 21+ (developed on JDK 25)
- Maven
- REST Assured 6.2.0
- TestNG 7.12.0

## Project structure

```
src/test/java/com/trello/api/
├── config/Config.java              # reads key/token from env vars or config.properties
├── base/BaseApiTest.java           # shared authenticated request specification
└── tests/
    ├── TrelloBoardFlowTest.java    # end-to-end flow, tests 00 to 10
    └── TrelloNegativeTest.java     # auth and validation negative tests
src/test/resources/
└── config.properties.example       # template for your own credentials
testng.xml                          # suite definition
```

## Setup

1. **Create a Trello API key and token**
   - Log in to Trello and open the Power-Up Admin Portal: `https://trello.com/power-ups/admin`
   - Click **New**, fill in the form (choose "My app doesn't use Power-up capabilities"), and create it.
   - Open the app's **API key** section, generate a key, then use the **Token** link next to it and click **Allow** to get a token.
2. **Add your credentials**
   - Copy `src/test/resources/config.properties.example` to `src/test/resources/config.properties` and fill in your values:
     ```
     trello.key=YOUR_TRELLO_API_KEY
     trello.token=YOUR_TRELLO_API_TOKEN
     trello.member.email=
     ```
   - Or set the environment variables `TRELLO_KEY`, `TRELLO_TOKEN` and optionally `TRELLO_MEMBER_EMAIL`.
   - `config.properties` is git-ignored. Never commit your key or token. Treat the token like a password.
3. **Run the tests**
   ```
   mvn test
   ```
   Reports are written to `target/surefire-reports`.

## Test coverage

| Test | What it checks |
|---|---|
| Trello_00 | Credentials are valid (`GET /members/me`) |
| Trello_01 | Create board |
| Trello_02 | Add lists to the board |
| Trello_03 | Add a card to a list |
| Trello_04 | Move the card to another list |
| Trello_05 | Set a due date on the card |
| Trello_06 | Create a label and attach it to the card |
| Trello_07 | Invite a member by email (skipped if `trello.member.email` is empty) |
| Trello_08 | Archive the card |
| Trello_09 | Add a comment to the card |
| Trello_10 | Delete the board, then confirm `GET` returns 404 |
| Negative | Invalid token, missing credentials, board without a name, unknown board id |

## Design notes

- **One board per run.** `Trello_01` creates a board and the later tests reuse it. `Trello_10` deletes it.
- **Automatic cleanup.** An `@AfterClass` hook deletes the board even if a test fails midway, so no test data is left in the Trello account.
- **Test order.** `priority` sets the run order and `dependsOnMethods` skips tests whose data was never created (for example, no card tests if the card was never created).
- **No secrets in logs.** Request logging is off, and only the response is logged when a validation fails, so the key and token are never printed.
- **Invite test is repeatable.** Trello returns `403 Member already invited` when the address already has a pending invitation, so the test accepts either a 200 or that response.

## Findings from testing the real API

Behaviour I verified by running the tests against Trello, which was different from what I first assumed:

- A request with a wrong token returns **401**.
- A request with **no** key or token returns **400** with the text `invalid token`, not 401.
- Inviting an address that already has a pending invitation returns **403** with `Member already invited`.
- Creating a board without a name returns **400**, and reading a deleted or unknown board returns **404**.

## Notes

- The invite test sends a real invitation email to the address in `trello.member.email`. Leave it empty to skip that test.
- Trello rate limits API calls, so avoid running the suite in a tight loop.