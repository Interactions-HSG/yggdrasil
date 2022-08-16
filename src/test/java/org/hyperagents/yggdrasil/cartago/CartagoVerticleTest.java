package org.hyperagents.yggdrasil.cartago;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CartagoVerticleTest {

  private static final int TEST_PORT = 8080;
  private Vertx vertx;
  private WebClient client;

  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    JsonObject knownArtifacts = new JsonObject()
      .put("https://ci.mines-stetienne.fr/kg/ontology#PhantomX_3D",
        "org.hyperagents.yggdrasil.cartago.artifacts.PhantomX3D")
      .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.artifacts.Counter")
      .put("http://example.org/SpatialCalculator2D", "org.hyperagents.yggdrasil.cartago"
        + ".SpatialCalculator2D");

    JsonObject cartagoConfig = new JsonObject();
    cartagoConfig.put("known-artifacts", knownArtifacts);

    vertx.deployVerticle(HttpServerVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(RdfStoreVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(CartagoVerticle.class.getName(), new DeploymentOptions().setWorker(true).setConfig(cartagoConfig),  tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testCreateArtifactWithoutJoiningWorkspace(TestContext tc) {
    Async async = tc.async();
    JsonObject body = new JsonObject();
    body.put("artifactClass", "http://example.org/Counter");
    body.put("artifactName", "counter");
    client.post(TEST_PORT, "localhost", "/workspaces/").putHeader("X-Agent-WebID", "http://example.org/agent")
        .putHeader("Slug", "102").send(ar -> System.out.println("workspace 102 created"));
    System.out.println("workspace created");
    client.post(TEST_PORT, "localhost", "/workspaces/102/artifacts/").putHeader("X-Agent-WebID", "http://example.org/agent")
      .putHeader("Content-Type", "application/json")
      .sendJsonObject(body, ar -> {
        System.out.println("response received");
        HttpResponse<Buffer> response = ar.result();
        System.out.println("response: "+response);
        System.out.println("status code: "+response.statusCode());
        System.out.println("status message: "+response.statusMessage());
        System.out.println("body: "+response.bodyAsString());

        tc.assertEquals(HttpStatus.SC_FORBIDDEN, response.statusCode(), "Status code should be 403");
        async.complete();
      });
  }


  @Test
  public void testCreationArtifactWithoutWorkspace(TestContext tc) {
    Async async = tc.async();
    JsonObject body = new JsonObject();
    body.put("artifactClass", "http://example.org/Counter");
    body.put("artifactName", "counter");
    client.post(TEST_PORT, "localhost", "/workspaces/102/artifacts/").putHeader("X-Agent-WebID", "http://example.org/agent")
      .putHeader("Content-Type", "application/json")
      .sendJsonObject(body, ar -> {
        HttpResponse<Buffer> response = ar.result();
        System.out.println("response: "+response);
        System.out.println("status code: "+response.statusCode());
        System.out.println("status message: "+response.statusMessage());
        System.out.println("body: "+response.bodyAsString());

        tc.assertEquals(HttpStatus.SC_NOT_FOUND, response.statusCode(), "Status code should be 404");
        async.complete();
      });
  }
}
