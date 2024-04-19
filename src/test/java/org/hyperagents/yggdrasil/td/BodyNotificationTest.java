package org.hyperagents.yggdrasil.td;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.rdf4j.model.util.Models;
import org.hyperagents.yggdrasil.MainVerticle;
import org.hyperagents.yggdrasil.hmas.CallbackServerVerticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class BodyNotificationTest {
  private static final String TEST_AGENT_NAME = "test_agent";
  private static final String TEST_AGENT_ID = "http://localhost:8080/agents/" + TEST_AGENT_NAME;
  private static final String AGENT_ID_HEADER = "X-Agent-WebID";
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final String COUNTER_ARTIFACT_CLASS = "http://example.org/Counter";
  private static final String BASE_ARTIFACT_CLASS = "http://example.org/Artifact";
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";
  private static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String REPRESENTATIONS_EQUAL_MESSAGE = "The representations must be equal";
  private static final String HUB_MODE_PARAM = "hub.mode";
  private static final String HUB_TOPIC_PARAM = "hub.topic";
  private static final String HUB_CALLBACK_PARAM = "hub.callback";
  private static final String HUB_MODE_SUBSCRIBE = "subscribe";
  private static final String HUB_PATH = "/hub/";
  private static final String WORKSPACES_PATH = "/workspaces/";
  private static final String ARTIFACTS_PATH = "/artifacts/";
  private static final String BODIES_PATH = "/agents/";
  private static final String CALLBACK_URL = "http://" + TEST_HOST + ":" + 8081 + "/";

  private List<Promise<Map.Entry<String, String>>> callbackMessages;
  private WebClient client;
  private int promiseIndex;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.client = WebClient.create(vertx);
    this.callbackMessages =
      Stream.generate(Promise::<Map.Entry<String, String>>promise)
            .limit(5)
            .collect(Collectors.toList());
    this.promiseIndex = 0;
    vertx
        .eventBus()
        .<String>consumer(
          "test",
          m -> {
            this.callbackMessages
                .get(this.promiseIndex)
                .complete(Map.entry(m.headers().get("entityIri"), m.body()));
            this.promiseIndex++;
          }
        );
    vertx.deployVerticle(new CallbackServerVerticle())
         .compose(r -> vertx.deployVerticle(
           new MainVerticle(),
           new DeploymentOptions().setConfig(JsonObject.of(
             "http-config",
             JsonObject.of(
               "host",
               TEST_HOST,
               "port",
               TEST_PORT
             ),
             "notification-config",
             JsonObject.of(
               "enabled",
               true
             ),
             "environment-config",
             JsonObject.of(
               "enabled",
               true,
               "ontology",
               "td",
               "known-artifacts",
               JsonArray.of(
                 JsonObject.of(
                   "class",
                   COUNTER_ARTIFACT_CLASS,
                   "template",
                   "org.hyperagents.yggdrasil.artifacts.CounterTD"
                 ),
                 JsonObject.of(
                   "class",
                   BASE_ARTIFACT_CLASS,
                   "template",
                   "org.hyperagents.yggdrasil.artifacts.BasicTDArtifact"
                 )
               )
             )
           ))
         ))
         .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close().onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testRun(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var workspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/output_test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/c0_counter_artifact_test_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var testAgentBodyRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/test_agent_body_test_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var workspaceWithArtifactAndBodyRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/test_workspace_c0_body_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.client
        .post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
        .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
        .putHeader("Slug", MAIN_WORKSPACE_NAME)
        .send()
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          this.assertEqualsThingDescriptions(
              workspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
                          .post(
                            TEST_PORT,
                            TEST_HOST,
                            WORKSPACES_PATH + MAIN_WORKSPACE_NAME + ARTIFACTS_PATH
                          )
                          .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
                          .sendJsonObject(JsonObject.of(
                            "artifactName",
                            COUNTER_ARTIFACT_NAME,
                            "artifactClass",
                            COUNTER_ARTIFACT_CLASS
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          this.assertEqualsThingDescriptions(
              artifactRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
                          .post(TEST_PORT, TEST_HOST, HUB_PATH)
                          .sendJsonObject(JsonObject.of(
                            HUB_MODE_PARAM,
                            HUB_MODE_SUBSCRIBE,
                            HUB_TOPIC_PARAM,
                            this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
                            HUB_CALLBACK_PARAM,
                            CALLBACK_URL
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r -> this.client
                          .post(TEST_PORT, TEST_HOST, HUB_PATH)
                          .sendJsonObject(JsonObject.of(
                            HUB_MODE_PARAM,
                            HUB_MODE_SUBSCRIBE,
                            HUB_TOPIC_PARAM,
                            this.getUrl(
                              WORKSPACES_PATH
                              + MAIN_WORKSPACE_NAME
                              + ARTIFACTS_PATH
                            ),
                            HUB_CALLBACK_PARAM,
                            CALLBACK_URL
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r -> this.client
                          .post(
                            TEST_PORT,
                            TEST_HOST,
                            WORKSPACES_PATH + MAIN_WORKSPACE_NAME + "/join"
                          )
                          .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
                          .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          this.assertEqualsThingDescriptions(
              testAgentBodyRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.getFirst().future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          this.assertEqualsThingDescriptions(
              workspaceWithArtifactAndBodyRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.callbackMessages.get(1).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME + ARTIFACTS_PATH),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          this.assertEqualsThingDescriptions(
              testAgentBodyRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.client
                          .post(TEST_PORT, TEST_HOST, HUB_PATH)
                          .sendJsonObject(JsonObject.of(
                            HUB_MODE_PARAM,
                            HUB_MODE_SUBSCRIBE,
                            HUB_TOPIC_PARAM,
                            this.getUrl(
                              WORKSPACES_PATH
                              + MAIN_WORKSPACE_NAME
                              + ARTIFACTS_PATH
                              + TEST_AGENT_NAME
                              + "/"
                            ),
                            HUB_CALLBACK_PARAM,
                            CALLBACK_URL
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r ->
          this.client
              .post(
                TEST_PORT,
                TEST_HOST,
                WORKSPACES_PATH
                  + MAIN_WORKSPACE_NAME
                  + ARTIFACTS_PATH
                  + COUNTER_ARTIFACT_NAME
                  + "/increment"
              )
              .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
              .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.bodyAsString(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r -> this.callbackMessages.get(2).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(
                WORKSPACES_PATH
                + MAIN_WORKSPACE_NAME
                + ARTIFACTS_PATH
                + TEST_AGENT_NAME
                + "/"
              ),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              JsonObject
                .of(
                  "artifactName",
                  COUNTER_ARTIFACT_NAME,
                  "actionName",
                  "inc",
                  "eventType",
                  "actionRequested"
                )
                .encode(),
              m.getValue(),
              REPRESENTATIONS_EQUAL_MESSAGE
          );
        })
        .compose(r -> this.callbackMessages.get(3).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(
                WORKSPACES_PATH
                + MAIN_WORKSPACE_NAME
                + ARTIFACTS_PATH
                + TEST_AGENT_NAME
                + "/"
              ),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              JsonObject
                .of(
                  "artifactName",
                  COUNTER_ARTIFACT_NAME,
                  "actionName",
                  "inc",
                  "eventType",
                  "actionSucceeded"
                )
                .encode(),
              m.getValue(),
              REPRESENTATIONS_EQUAL_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  private String getUrl(final String path) {
    return "http://" + TEST_HOST + ":" + TEST_PORT + path;
  }

  private void assertEqualsThingDescriptions(final String expected, final String actual) {
    Assertions.assertTrue(
        Models.isomorphic(
          TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE,expected).getGraph().get(),
          TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE,actual).getGraph().get()
        ),
        REPRESENTATIONS_EQUAL_MESSAGE
    );
  }
}
