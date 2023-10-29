package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringReader;

@RunWith(VertxUnitRunner.class)
public class HttpServerVerticleTest {
  private static final int TEST_PORT = 8080;

  private Vertx vertx;
  private WebClient client;

  @Before
  public void setUp(final TestContext tc) {
    this.vertx = Vertx.vertx();
    this.client = WebClient.create(this.vertx);

    this.vertx.deployVerticle(HttpServerVerticle.class.getName(), tc.asyncAssertSuccess());
    this.vertx.deployVerticle(RdfStoreVerticle.class.getName(), tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(final TestContext tc) {
    this.vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testThatTheServerIsStarted(final TestContext tc) {
    final var async = tc.async();
    this.client.get(TEST_PORT, "localhost", "/")
               .send(ar -> {
                 final var response = ar.result();
                 tc.assertEquals(HttpStatus.SC_OK, response.statusCode(), "Status code should be OK");
                 tc.assertTrue(!response.bodyAsString().isEmpty(), "Body length should be greater than zero");
                 async.complete();
               });
  }

  @Test
  @Ignore
  public void testGetEntity(final TestContext tc) {
    final var async = tc.async();

    this.createResourceAndThen(createAR -> {
      tc.assertEquals(HttpStatus.SC_CREATED, createAR.result().statusCode(),"Status code should be CREATED");

      this.client.get(TEST_PORT, "localhost", "/environments/test_env")
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
        .send(ar -> {
          final var getResponse = ar.result();
          tc.assertEquals(HttpStatus.SC_OK, getResponse.statusCode(), "Status code should be OK");

          try {
            // TODO: Check that the following makes sense (the response that comes back is certainly not isomorphic to the expected string)
            assertIsomorphic(
              tc,
              "<http://localhost:" + TEST_PORT + "/environments/test_env> "
                + "a <http://w3id.org/eve#Environment>;\n"
                + "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1> .",
              getResponse.bodyAsString()
            );
          } catch (final RDFParseException | RDFHandlerException | IOException e) {
            tc.fail(e);
          }

          async.complete();
        });
    });
  }

  @Test
  public void testGetEntityNotFound(final TestContext tc) {
    final var async = tc.async();

    this.client.get(TEST_PORT, "localhost", "/environments/bla123")
               .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
               .send(ar -> {
                 tc.assertEquals(HttpStatus.SC_NOT_FOUND, ar.result().statusCode(), "Status code should be NOT FOUND");
                 async.complete();
               });
  }

  @Test
  public void testEntityCORSHeaders(final TestContext tc) {
    final var async = tc.async();

    this.createResourceAndThen(createAR -> {
      tc.assertEquals(HttpStatus.SC_CREATED, createAR.result().statusCode(), "Status code should be CREATED");

      this.client.get(TEST_PORT, "localhost", "/environments/test_env")
                 .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
                 .send(ar -> {
                   final var getResponse = ar.result();

                   tc.assertEquals(HttpStatus.SC_OK, getResponse.statusCode(), "Response code should be OK");

                   tc.assertEquals(
                     "*",
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN),
                     "CORS origin should be open"
                   );
                   tc.assertEquals(
                     "true",
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS),
                     "CORS credentials should be allowed"
                   );

                   tc.assertTrue(
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains(HttpMethod.GET.name()),
                     "CORS should permit GET on entities");
                   tc.assertTrue(
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains(HttpMethod.POST.name()),
                     "CORS should permit POST on entities"
                   );
                   tc.assertTrue(
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains(HttpMethod.PUT.name()),
                     "CORS should permit PUT on entities"
                   );
                   tc.assertTrue(
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains(HttpMethod.DELETE.name()),
                     "CORS should permit DELETE on entities"
                   );
                   tc.assertTrue(getResponse.getHeader(
                     HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains(HttpMethod.HEAD.name()),
                     "CORS should permit HEAD on entities"
                   );
                   tc.assertTrue(
                     getResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains(HttpMethod.OPTIONS.name()),
                     "CORS should permit OPTIONS on entities"
                   );
                   async.complete();
                 });
    });
  }

  @Test
  @Ignore
  public void testGetEntityWithoutContentType(final TestContext tc) {
   final var async = tc.async();

    this.createResourceAndThen(createAR -> {
      tc.assertEquals(HttpStatus.SC_CREATED, createAR.result().statusCode(),"Status code should be CREATED");

      this.client.get(TEST_PORT, "localhost", "/environments/test_env")
                 .send(ar -> {
                   tc.assertEquals(HttpStatus.SC_NOT_FOUND, ar.result().statusCode(), "Status code should be NOT FOUND");
                   async.complete();
                 });
    });
  }

  @Test
  @Ignore
  public void testCreateEntity(final TestContext tc) {
    final var async = tc.async();

    this.client.post(TEST_PORT, "localhost", "/environments/")
               .putHeader("Slug", "env1")
               .putHeader("Content-Type", "text/turtle")
               .putHeader("X-Agent-WebID", "TestAgentID")
               .sendBuffer(
                 Buffer.buffer(
                   "<> a <http://w3id.org/eve#Environment> ;\n"
                   + "<http://w3id.org/eve#contains> <http://localhost:"
                   + TEST_PORT
                   + "/workspaces/wksp1> ."
                 ),
                 ar -> {
                   final var response = ar.result();
                   tc.assertEquals(HttpStatus.SC_CREATED, response.statusCode(), "Status code should be CREATED");

                   try {
                     // TODO: Check that the following makes sense (the response that comes back is certainly not isomorphic to the expected string)
                     assertIsomorphic(
                       tc,
                       "<http://localhost:"
                       + TEST_PORT
                       + "/environments/env1> "
                       + "a <http://w3id.org/eve#Environment> ;\n"
                       + "<http://w3id.org/eve#contains> <http://localhost:"
                       + TEST_PORT
                       + "/workspaces/wksp1> .",
                       response.bodyAsString()
                     );
                  } catch (final RDFParseException | RDFHandlerException | IOException e) {
                    tc.fail(e);
                  }

                  async.complete();
                });
  }

  @Test
  public void testCreateEntityUnauthorzedNoWebID(final TestContext tc) {
    final var async = tc.async();

    this.client.post(TEST_PORT, "localhost", "/environments/")
               .putHeader("Slug", "test_env")
               .putHeader("Content-Type", "text/turtle")
               .sendBuffer(
                 Buffer.buffer(
                   "<> a <http://w3id.org/eve#Environment> ;\n"
                   + "<http://w3id.org/eve#contains> <http://localhost:"
                   + TEST_PORT
                   + "/workspaces/wksp1> ."
                 ),
                 ar -> {
                   final var response = ar.result();
                   tc.assertEquals(
                     HttpStatus.SC_UNAUTHORIZED,
                     response.statusCode(),
                     "Status code should be UNAUTHORIZED"
                   );
                   async.complete();
                 }
               );
  }

  @Test
  public void testUpdateEntity(final TestContext tc) {
    final var async = tc.async();

    this.createResourceAndThen(createAR -> {
      tc.assertEquals(HttpStatus.SC_CREATED, createAR.result().statusCode(), "Status code should be CREATED");

      this.client.put(TEST_PORT, "localhost", "/environments/test_env")
                 .putHeader("Content-Type", "text/turtle")
                 .sendBuffer(
                   Buffer.buffer(
                     "<> a <http://w3id.org/eve#Environment> ;\n"
                     + "<http://w3id.org/eve#contains> <http://localhost:"
                     + TEST_PORT
                     + "/workspaces/wksp1>, "
                     + "<http://localhost:"
                     + TEST_PORT
                     + "/workspaces/wksp2> ."
                   ),
                   ar -> {
                     final var updateResponse = ar.result();
                     tc.assertEquals(HttpStatus.SC_OK, updateResponse.statusCode(), "Status code should be OK");

                     try {
                       assertIsomorphic(
                         tc,
                         "<http://localhost:"
                         + TEST_PORT
                         + "/environments/test_env> "
                         + "a <http://w3id.org/eve#Environment>;\n"
                         + "<http://w3id.org/eve#contains> <http://localhost:"
                         + TEST_PORT
                         + "/workspaces/wksp1>, "
                         + "<http://localhost:"
                         + TEST_PORT
                         + "/workspaces/wksp2> .",
                         updateResponse.bodyAsString());
                     } catch (final RDFParseException | RDFHandlerException | IOException e) {
                       tc.fail(e);
                     }

                     async.complete();
                   }
                 );
    });
  }

  @Test
  public void testDeleteEntity(final TestContext tc) {
    final var async = tc.async();

    this.createResourceAndThen(createAR -> {
      tc.assertEquals(HttpStatus.SC_CREATED, createAR.result().statusCode(), "Status code should be CREATED");

      this.client.delete(TEST_PORT, "localhost", "/environments/test_env")
                 .send(ar -> {
                   tc.assertEquals(HttpStatus.SC_OK, ar.result().statusCode(), "Status code should be OK");
                   async.complete();
                 });
    });
  }

  @Test
  public void testCartagoVerticleNoArtifactTemplates(final TestContext tc) {
    this.vertx.deployVerticle(
      CartagoVerticle.class.getName(),
      new DeploymentOptions().setWorker(true).setConfig(null),
      tc.asyncAssertSuccess()
    );

    // TODO: Why is this flagged as test?
  }

  @Test
  @Ignore
  public void testCartagoArtifact(final TestContext tc) {
    // Register artifact template for this test
    final var knownArtifacts =
      new JsonObject().put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.artifacts.Counter");

    this.vertx.deployVerticle(
      CartagoVerticle.class.getName(),
      new DeploymentOptions().setWorker(true).setConfig(new JsonObject().put("known-artifacts", knownArtifacts)),
      tc.asyncAssertSuccess()
    );

    final var async = tc.async();

    // TODO: This test seems wrong. Why would there be a localhost:8080/workspaces path?
    this.client
        .post(TEST_PORT, "localhost", "/workspaces/")
        .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
        .putHeader("Slug", "wksp1")
        .sendBuffer(Buffer.buffer(""), wkspAR -> {
          tc.assertEquals(HttpStatus.SC_CREATED, wkspAR.result().statusCode(), "Status code should be CREATED");

          this.client
              .post(TEST_PORT, "localhost", "/workspaces/wksp1/artifacts/")
              .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
              .putHeader("Slug", "c0")
              .putHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
              .sendBuffer(
                Buffer.buffer("{\"artifactClass\" : \"http://example.org/Counter\"}"),
                ar -> {
                  System.out.println("artifact created");
                  tc.assertEquals(HttpStatus.SC_CREATED, ar.result().statusCode(), "Status code should be CREATED");

                  this.client
                      .post(TEST_PORT, "localhost", "/workspaces/wksp1/artifacts/c0/increment")
                      .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
                      .putHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                      .sendBuffer(
                        Buffer.buffer("[1]"),
                        actionAr -> {
                          System.out.println("operation executed");
                          tc.assertEquals(HttpStatus.SC_OK, actionAr.result().statusCode(), "Status code should be OK");
                          async.complete();
                        }
                      );
                }
              );
        }
    );
  }

  private void createResourceAndThen(final Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    this.client.post(TEST_PORT, "localhost", "/environments/")
               .putHeader("Slug", "test_env")
               .putHeader("Content-Type", "text/turtle")
               .putHeader("X-Agent-WebID", "TestAgentID")
               .sendBuffer(
                 Buffer.buffer(
                   "<> a <http://w3id.org/eve#Environment> ;\n"
                    + "<http://w3id.org/eve#contains> <http://localhost:"
                    + TEST_PORT
                    + "/workspaces/wksp1> ."
                 ),
                 handler
               );
  }

  private void assertIsomorphic(final TestContext tc, final String graph1, final String graph2)
    throws RDFParseException, RDFHandlerException, IOException {
    tc.assertTrue(Models.isomorphic(
      this.readModelFromString(graph1, ""),
      this.readModelFromString(graph2, "")
    ));
  }

  private Model readModelFromString(final String description, final String baseURI)
    throws RDFParseException, RDFHandlerException, IOException {
    final var rdfParser = Rio.createParser(RDFFormat.TURTLE);
    final var model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    rdfParser.parse(new StringReader(description), baseURI);
    return model;
  }
}
