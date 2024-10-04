package org.hyperagents.yggdrasil;

import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.Assertions;

/**
 * Vocabulary class.
 */
public final class TConstants {


  public static final String TEST_HOST = "localhost";
  public static final int TEST_PORT = 8080;

  public static final String ONTOLOGY = "ontology";
  public static final String TD = "td";
  public static final String HMAS = "hmas";
  public static final String METADATA = "metadata";
  public static final String NAME = "name";

  public static final String ENABLED = "enabled";
  public static final String KNOWN_ARTIFACTS = "known-artifacts";
  public static final String WORKSPACES = "workspaces";

  public static final String CLASS = "class";
  public static final String COUNTER_ARTIFACT_CLASS = "http://example.org/Counter";
  public static final String TEMPLATE = "template";
  public static final String COUNTER_ARTIFACT_TEMPLATE_TD =
      "org.hyperagents.yggdrasil.cartago.artifacts.CounterTD";
  public static final String COUNTER_ARTIFACT_TEMPLATE_HMAS =
      "org.hyperagents.yggdrasil.cartago.artifacts.CounterHMAS";


  public static final String HTTP_CONFIG = "http-config";
  public static final String NOTIFICATION_CONFIG = "notification-config";
  public static final String ENVIRONMENT_CONFIG = "environment-config";

  public static final String TEST_AGENT_NAME = "test_agent";
  public static final String TEST_AGENT_BODY_NAME = "body_test_agent";
  public static final String TEST_AGENT_ID = "http://localhost:8080/agents/" + TEST_AGENT_NAME;
  public static final String AGENT_LOCALNAME_HEADER = "x-Agent-LocalName";
  public static final String AGENT_ID_HEADER = "X-Agent-WebID";
  public static final String MAIN_WORKSPACE_NAME = "test";
  public static final String ARTIFACT_NAME = "artifactName";
  public static final String ACTION_NAME = "actionName";
  public static final String ARTIFACT_CLASS = "artifactClass";
  public static final String COUNTER_ARTIFACT_NAME = "c0";
  public static final String COUNTER_ARTIFACT_ACTION_NAME = "inc";
  public static final String INIT_PARAMS = "initParams";
  public static final String EVENT_TYPE = "eventType";
  public static final String OK_STATUS_MESSAGE = "Status code should be OK";
  public static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";
  public static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";
  public static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  public static final String REPRESENTATIONS_EQUAL_MESSAGE = "The representations must be equal";
  public static final String ONTOLOGY_SPECIFIED_MESSAGE = "Ontology must be specified in test name";
  public static final String HUB_MODE_PARAM = "hub.mode";
  public static final String HUB_TOPIC_PARAM = "hub.topic";
  public static final String HUB_CALLBACK_PARAM = "hub.callback";
  public static final String HUB_MODE_SUBSCRIBE = "subscribe";
  public static final String HUB_PATH = "/hub/";
  public static final String WORKSPACES_PATH = "/workspaces/";
  public static final String ARTIFACTS_PATH = "/artifacts/";
  public static final String CALLBACK_URL = "http://" + TEST_HOST + ":" + 8081 + "/";


  public static final String SUB_WORKSPACE_NAME = "sub";
  public static final String COUNTER_ARTIFACT_URI =
      "http://localhost:8080/workspaces/sub/artifacts/c0";

  public static final String HINT_HEADER = "Slug";

  public static final JsonObject ENABLED_TRUE = JsonObject.of(
      ENABLED,
      true
  );

  public static final JsonObject httpConfig = JsonObject.of(
      "host",
      TEST_HOST,
      "port",
      TEST_PORT
  );
  public static final JsonObject notificationConfig = ENABLED_TRUE;

  public static final JsonObject cartagoEnv = JsonObject.of(
      KNOWN_ARTIFACTS,
      JsonArray.of(
          JsonObject.of(
              CLASS,
              COUNTER_ARTIFACT_CLASS,
              TEMPLATE,
              COUNTER_ARTIFACT_TEMPLATE_TD
          )
      ),
      WORKSPACES,
      JsonArray.of(
          JsonObject.of(
              NAME,
              "w1",
              METADATA,
              "src/main/resources/w1_test_metadata.ttl",
              "artifacts",
              JsonArray.of(
                  JsonObject.of(
                      NAME,
                      "c1",
                      CLASS,
                      COUNTER_ARTIFACT_CLASS,
                      METADATA,
                      "src/main/resources/c1_test_metadata.ttl"
                  )
              )
          )
      )
  ).mergeIn(ENABLED_TRUE);

  public static final JsonObject TDEnv = JsonObject.of(
      ENABLED,
      true,
      KNOWN_ARTIFACTS,
      JsonArray.of(
          JsonObject.of(
              CLASS,
              COUNTER_ARTIFACT_CLASS,
              TEMPLATE,
              COUNTER_ARTIFACT_TEMPLATE_TD
          )
      ),
      ONTOLOGY,
      TD
  );

  public static final JsonObject HMASEnv = JsonObject.of(
      ENABLED,
      true,
      KNOWN_ARTIFACTS,
      JsonArray.of(
          JsonObject.of(
              CLASS,
              COUNTER_ARTIFACT_CLASS,
              TEMPLATE,
              COUNTER_ARTIFACT_TEMPLATE_HMAS
          )
      ),
      ONTOLOGY,
      HMAS
  );

  private TConstants() {
  }

  /**
   * checks whether two thing descriptions are equal.
   *
   * @param expected td
   * @param actual td
   */
  public static void assertEqualsThingDescriptions(final String expected, final String actual) {
    final var areEqual = Models.isomorphic(
        TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, expected).getGraph()
            .orElseThrow(),
        TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, actual).getGraph()
            .orElseThrow()
    );
    if (!areEqual) {
      System.out.println(actual);
    }
    Assertions.assertTrue(
        areEqual,
        REPRESENTATIONS_EQUAL_MESSAGE
    );
  }

  /**
   * checks whether two HMAS representations are equal.
   *
   * @param expected hmas
   * @param actual hmas
   */
  public static void assertEqualsHMASDescriptions(final String expected, final String actual) {
    final var areEqual = Models.isomorphic(
        ResourceProfileGraphReader.getModelFromString(expected),
        ResourceProfileGraphReader.getModelFromString(actual)
    );
    if (!areEqual) {
      System.out.println(actual);
    }
    Assertions.assertTrue(
        areEqual,
        REPRESENTATIONS_EQUAL_MESSAGE
    );
  }
}
