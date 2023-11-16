package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.io.StringReader;
import org.apache.hc.core5.http.ContentType;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Disabled
@ExtendWith(VertxExtension.class)
public class HttpServerVerticleTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String TEST_ROOT_WORKSPACE_PATH = "/workspaces/main";
  private static final String TEST_WORKSPACE_PATH = "/workspaces/wksp1";
  private static final String SLUG_HEADER = "Slug";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String AGENT_WEBID = "X-Agent-WebID";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";

  private Vertx vertx;
  private WebClient client;

  private static String createMainWorkspaceGraph(
      final boolean hasBlankNode,
      final boolean hasTwoWorkspaces
  ) {
    return (hasBlankNode
            ? "<>"
            : String.format("<http://%s:%d%s>", TEST_HOST, TEST_PORT, TEST_ROOT_WORKSPACE_PATH))
           + " a <https://purl.org/hmas/core/Workspace>;\n"
           + "<https://purl.org/hmas/core/contains> "
           + String.format("<http://%s:%d%s>", TEST_HOST, TEST_PORT, TEST_WORKSPACE_PATH)
           + (hasTwoWorkspaces
              ? String.format(", <http://%s:%d/workspaces/wksp2> .", TEST_HOST, TEST_PORT)
              : " .");
  }

  @BeforeEach
  public void setUp(final VertxTestContext tc) {
    this.vertx = Vertx.vertx();
    this.client = WebClient.create(this.vertx);

    this.vertx.deployVerticle(HttpServerVerticle.class.getName(), tc.succeedingThenComplete());
    this.vertx.deployVerticle(RdfStoreVerticle.class.getName(), tc.succeedingThenComplete());
    this.vertx.deployVerticle(
        HttpNotificationVerticle.class.getName(),
        tc.succeedingThenComplete()
    );
  }

  @AfterEach
  public void tearDown(final VertxTestContext tc) {
    this.vertx.close(tc.succeedingThenComplete());
  }

  @Test
  public void testThatTheServerIsStarted(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();
    this.client.get(TEST_PORT, TEST_HOST, "/")
               .send(ar -> {
                 final var response = ar.result();
                 Assertions.assertEquals(
                     HttpStatus.SC_OK,
                     response.statusCode(),
                     OK_STATUS_MESSAGE
                 );
                 Assertions.assertFalse(
                     response.bodyAsString().isEmpty(),
                     "Body length should be greater than zero"
                 );
                 checkpoint.flag();
               });
  }

  @Test
  @Disabled
  public void testGetEntity(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.createResourceAndThen(createAR -> {
      Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          createAR.result().statusCode(),
          CREATED_STATUS_MESSAGE
      );

      this.client
          .get(TEST_PORT, TEST_HOST, TEST_ROOT_WORKSPACE_PATH)
          .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
          .send(ar -> {
            final var getResponse = ar.result();
            Assertions.assertEquals(HttpStatus.SC_OK, getResponse.statusCode(), OK_STATUS_MESSAGE);

            try {
              // TODO: Check that the following makes sense
              // (the response that comes back is certainly not isomorphic to the expected string)
              assertIsomorphic(
                  createMainWorkspaceGraph(false, false),
                  getResponse.bodyAsString()
              );
            } catch (final RDFParseException | RDFHandlerException | IOException e) {
              tc.failNow(e);
            }

            checkpoint.flag();
          });
    });
  }

  @Test
  public void testGetEntityNotFound(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.client.get(TEST_PORT, TEST_HOST, "/environments/bla123")
               .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
               .send(ar -> {
                 Assertions.assertEquals(
                     HttpStatus.SC_NOT_FOUND,
                     ar.result().statusCode(),
                     "Status code should be NOT FOUND"
                 );
                 checkpoint.flag();
               });
  }

  @Test
  public void testEntityCorsHeaders(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.createResourceAndThen(createAR -> {
      Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          createAR.result().statusCode(),
          CREATED_STATUS_MESSAGE
      );

      this.client.get(TEST_PORT, TEST_HOST, TEST_ROOT_WORKSPACE_PATH)
                 .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                 .send(ar -> {
                   final var getResponse = ar.result();

                   Assertions.assertEquals(
                       HttpStatus.SC_OK,
                       getResponse.statusCode(),
                       OK_STATUS_MESSAGE
                   );

                   Assertions.assertEquals(
                       "*",
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN),
                       "CORS origin should be open"
                   );
                   Assertions.assertEquals(
                       "true",
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS),
                       "CORS credentials should be allowed"
                   );

                   Assertions.assertTrue(
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                                  .contains(HttpMethod.GET.name()),
                       "CORS should permit GET on entities"
                   );
                   Assertions.assertTrue(
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                                  .contains(HttpMethod.POST.name()),
                       "CORS should permit POST on entities"
                   );
                   Assertions.assertTrue(
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                                  .contains(HttpMethod.PUT.name()),
                       "CORS should permit PUT on entities"
                   );
                   Assertions.assertTrue(
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                                  .contains(HttpMethod.DELETE.name()),
                       "CORS should permit DELETE on entities"
                   );
                   Assertions.assertTrue(getResponse.getHeader(
                       HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                                  .contains(HttpMethod.HEAD.name()),
                       "CORS should permit HEAD on entities"
                   );
                   Assertions.assertTrue(
                       getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                                  .contains(HttpMethod.OPTIONS.name()),
                       "CORS should permit OPTIONS on entities"
                   );
                   checkpoint.flag();
                 });
    });
  }

  @Test
  @Disabled
  public void testGetEntityWithoutContentType(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.createResourceAndThen(createAR -> {
      Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          createAR.result().statusCode(),
          CREATED_STATUS_MESSAGE
      );

      this.client.get(TEST_PORT, TEST_HOST, TEST_ROOT_WORKSPACE_PATH)
                 .send(ar -> {
                   Assertions.assertEquals(
                       HttpStatus.SC_NOT_FOUND,
                       ar.result().statusCode(),
                       "Status code should be NOT FOUND"
                   );
                   checkpoint.flag();
                 });
    });
  }

  @Test
  @Disabled
  public void testCreateEntity(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.client.post(TEST_PORT, TEST_HOST, "/environments/")
               .putHeader(SLUG_HEADER, "env1")
               .putHeader(CONTENT_TYPE_HEADER, TURTLE_CONTENT_TYPE)
               .putHeader(AGENT_WEBID, "TestAgentID")
               .sendBuffer(
                 Buffer.buffer(createMainWorkspaceGraph(true, false)),
                 ar -> {
                   final var response = ar.result();
                   Assertions.assertEquals(
                       HttpStatus.SC_CREATED,
                       response.statusCode(),
                       CREATED_STATUS_MESSAGE
                   );

                   try {
                     // TODO: Check that the following makes sense
                     // (the response that comes back is certainly not isomorphic to the expected
                     //  string)
                     assertIsomorphic(
                         createMainWorkspaceGraph(false, false),
                         response.bodyAsString()
                     );
                   } catch (final RDFParseException | RDFHandlerException | IOException e) {
                     tc.failNow(e);
                   }

                   checkpoint.flag();
                 });
  }

  @Test
  public void testCreateEntityUnauthorzedNoWebId(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.client.post(TEST_PORT, TEST_HOST, "/environments/")
               .putHeader(SLUG_HEADER, "test_env")
               .putHeader(CONTENT_TYPE_HEADER, TURTLE_CONTENT_TYPE)
               .sendBuffer(
                 Buffer.buffer(createMainWorkspaceGraph(true, false)),
                 ar -> {
                   final var response = ar.result();
                   Assertions.assertEquals(
                       HttpStatus.SC_UNAUTHORIZED,
                       response.statusCode(),
                       "Status code should be UNAUTHORIZED"
                   );
                   checkpoint.flag();
                 });
  }

  @Test
  public void testUpdateEntity(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.createResourceAndThen(createAR -> {
      Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          createAR.result().statusCode(),
          CREATED_STATUS_MESSAGE
      );

      this.client.put(TEST_PORT, TEST_HOST, TEST_ROOT_WORKSPACE_PATH)
                 .putHeader(CONTENT_TYPE_HEADER, TURTLE_CONTENT_TYPE)
                 .sendBuffer(
                   Buffer.buffer(createMainWorkspaceGraph(true, true)),
                   ar -> {
                     final var updateResponse = ar.result();
                     Assertions.assertEquals(
                         HttpStatus.SC_OK,
                         updateResponse.statusCode(),
                         OK_STATUS_MESSAGE
                     );

                     try {
                       assertIsomorphic(
                           createMainWorkspaceGraph(false, true),
                           updateResponse.bodyAsString()
                       );
                     } catch (final RDFParseException | RDFHandlerException | IOException e) {
                       tc.failNow(e);
                     }

                     checkpoint.flag();
                   });
    });
  }

  @Test
  public void testDeleteEntity(final VertxTestContext tc) {
    final var checkpoint = tc.checkpoint();

    this.createResourceAndThen(createAR -> {
      Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          createAR.result().statusCode(),
          CREATED_STATUS_MESSAGE
      );

      this.client.delete(TEST_PORT, TEST_HOST, TEST_ROOT_WORKSPACE_PATH)
                 .send(ar -> {
                   Assertions.assertEquals(
                       HttpStatus.SC_OK,
                       ar.result().statusCode(),
                       OK_STATUS_MESSAGE
                   );
                   checkpoint.flag();
                 });
    });
  }

  @Test
  @Disabled
  public void testCartagoArtifact(final VertxTestContext tc) {
    // Register artifact template for this test
    final var knownArtifacts =
        new JsonObject()
          .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.artifacts.Counter");

    this.vertx.deployVerticle(
        CartagoVerticle.class.getName(),
        new DeploymentOptions().setWorker(true)
                               .setConfig(new JsonObject().put("known-artifacts", knownArtifacts)),
        tc.succeedingThenComplete()
    );

    final var checkpoint = tc.checkpoint();

    // TODO: This test seems wrong. Why would there be a localhost:8080/workspaces path?
    this.client
        .post(TEST_PORT, TEST_HOST, "/workspaces/")
        .putHeader(AGENT_WEBID, "http://andreiciortea.ro/#me")
        .putHeader(SLUG_HEADER, "wksp1")
        .sendBuffer(Buffer.buffer(""), wkspAR -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              wkspAR.result().statusCode(),
              CREATED_STATUS_MESSAGE
          );

          this.client
              .post(TEST_PORT, TEST_HOST, TEST_WORKSPACE_PATH + "/artifacts/")
              .putHeader(AGENT_WEBID, "http://andreiciortea.ro/#me")
              .putHeader(SLUG_HEADER, "c0")
              .putHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
              .sendBuffer(
                Buffer.buffer("{\"artifactClass\" : \"http://example.org/Counter\"}"),
                ar -> {
                  System.out.println("artifact created");
                  Assertions.assertEquals(
                      HttpStatus.SC_CREATED,
                      ar.result().statusCode(),
                      CREATED_STATUS_MESSAGE
                  );

                  this.client
                      .post(
                          TEST_PORT,
                          TEST_HOST,
                          TEST_WORKSPACE_PATH + "/artifacts/c0/increment"
                      )
                      .putHeader(AGENT_WEBID, "http://andreiciortea.ro/#me")
                      .putHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                      .sendBuffer(
                        Buffer.buffer("[1]"),
                        actionAr -> {
                          System.out.println("operation executed");
                          Assertions.assertEquals(
                              HttpStatus.SC_OK,
                              actionAr.result().statusCode(),
                              OK_STATUS_MESSAGE
                          );
                          checkpoint.flag();
                        }
                      );
                }
              );
        });
  }

  private void createResourceAndThen(final Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    this.client.post(TEST_PORT, TEST_HOST, "/workspaces/")
               .putHeader(SLUG_HEADER, "test_workspace")
               .putHeader(CONTENT_TYPE_HEADER, TURTLE_CONTENT_TYPE)
               .putHeader(AGENT_WEBID, "TestAgentID")
               .sendBuffer(Buffer.buffer(createMainWorkspaceGraph(true, false)), handler);
  }

  private void assertIsomorphic(final String graph1, final String graph2)
      throws RDFParseException, RDFHandlerException, IOException {
    Assertions.assertTrue(
        Models.isomorphic(
          this.readModelFromString(graph1, ""),
          this.readModelFromString(graph2, "")
        ),
        "The models should be equal"
    );
  }

  private Model readModelFromString(final String description, final String baseUri)
      throws RDFParseException, RDFHandlerException, IOException {
    final var rdfParser = Rio.createParser(RDFFormat.TURTLE);
    final var model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    rdfParser.parse(new StringReader(description), baseUri);
    return model;
  }
}
