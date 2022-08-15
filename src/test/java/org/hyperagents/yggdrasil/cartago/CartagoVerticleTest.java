package org.hyperagents.yggdrasil.cartago;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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

    vertx.deployVerticle(HttpServerVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(RdfStoreVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(CartagoVerticle.class.getName(), tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testThatTheServerIsStarted(TestContext tc) {
    Async async = tc.async();
    client.get(TEST_PORT, "localhost", "/")
      .send(ar -> {
        HttpResponse<Buffer> response = ar.result();
        tc.assertEquals(HttpStatus.SC_OK, response.statusCode(), "Status code should be OK");
        tc.assertTrue(response.bodyAsString().length() > 0, "Body length should be greater than zero");
        async.complete();
      });
  }

  @Test
  public void testCreationArtifactWithoutWorkspace(TestContext tc) {
    Async async = tc.async();
    client.get(TEST_PORT, "localhost", "/workspaces/102/artifacts/")
      .send(ar -> {
        HttpResponse<Buffer> response = ar.result();
        tc.assertEquals(HttpStatus.SC_NOT_FOUND, response.statusCode(), "Status code should be 404");
        async.complete();
      });
  }
}
