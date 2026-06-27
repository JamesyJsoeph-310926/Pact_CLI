package com.ust.sdet.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import static io.restassured.RestAssured.given;

public class WireMockServiceVirtualisationTest {
        @RegisterExtension
        static WireMockExtension wm = WireMockExtension.newInstance()
                        .options(wireMockConfig().dynamicPort()).build();

        private HttpClient client;

        @BeforeEach
        void pointConsumerAtWireMock() {
                io.restassured.RestAssured.baseURI = wm.baseUrl();
                client = HttpClient.newHttpClient();
        }

        @Test
        @DisplayName("M1/M2: GET /orders/{id} is virtualised and verified")
        void returnsConfirmedOrderOverRealHttp() {
                wm.stubFor(get(urlPathEqualTo("/orders/123")).willReturn(okJson("""
                                {"id":123,"status":"CONFIRMED","total":42.0}""")));
        }

        @Test
        @DisplayName("Stub Inventory - Success and Out Of Stock")
        void stubInventoryTwoOutcomes() {

                wm.stubFor(get(urlPathEqualTo("/inventory/SKU-9")).willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                                {"sku":"SKU-9","qty":5}
                                                """)));

                wm.stubFor(get(urlPathEqualTo("/inventory/SKU-0")).willReturn(aResponse()
                                .withStatus(409)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                                {"error":"OUT_OF_STOCK"}
                                                """)));

                given()
                                .when()
                                .get("/inventory/SKU-9")
                                .then()
                                .statusCode(200)
                                .body("qty", equalTo(5))
                                .body("sku", equalTo("SKU-9"));

                given()
                                .when()
                                .get("/inventory/SKU-0")
                                .then()
                                .statusCode(409)
                                .body("error", equalTo("OUT_OF_STOCK"));

                wm.verify(exactly(1), getRequestedFor(urlPathEqualTo("/inventory/SKU-9")));
        }

        @Test
        @DisplayName("Slow API - Force timeout then succeed")
        void slowApiTimeoutThenSuccess() throws Exception {
                wm.stubFor(get(urlPathEqualTo("/orders/slow")).willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withFixedDelay(3000)
                                .withBody("""
                                                {
                                                  "id":999,
                                                  "status":"CONFIRMED"
                                                }
                                                """)));

                String url = wm.baseUrl() + "/orders/slow";

                HttpRequest timeoutRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(1))
                                .GET()
                                .build();

                assertThrows(HttpTimeoutException.class, () -> client.send(timeoutRequest, ofString()));

                HttpRequest successRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build();

                var response = client.send(successRequest, ofString());

                assertEquals(200, response.statusCode());
                assertEquals("""
                                {
                                  "id":999,
                                  "status":"CONFIRMED"
                                }""".replaceAll("\\s+", ""),
                                response.body().replaceAll("\\s+", ""));
        }

        @Test
        @DisplayName("Stateful Order Flow - Pending then Confirmed")
        void statefulOrderFlow() {

                wm.stubFor(
                                get(urlPathEqualTo("/orders/42"))
                                                .inScenario("Order Fulfillment")
                                                .whenScenarioStateIs(STARTED)
                                                .willSetStateTo("CONFIRMED")
                                                .willReturn(
                                                                okJson("""
                                                                                {
                                                                                  "id":42,
                                                                                  "status":"PENDING"
                                                                                }
                                                                                """)));

                wm.stubFor(get(urlPathEqualTo("/orders/42"))
                                                .inScenario("Order Fulfillment")
                                                .whenScenarioStateIs("CONFIRMED")
                                                .willReturn(
                                                                okJson("""
                                                                                {
                                                                                  "id":42,
                                                                                  "status":"CONFIRMED"
                                                                                }
                                                                                """)));

                given()
                                .when()
                                .get("/orders/42")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("PENDING"));

                given()
                                .when()
                                .get("/orders/42")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("CONFIRMED"));

                wm.verify(exactly(2), getRequestedFor(urlPathEqualTo("/orders/42")));
        }
}
