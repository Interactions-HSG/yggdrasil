package org.hyperagents.yggdrasil.http;

import java.io.IOException;
import java.io.StringReader;

import org.apache.hc.core5.http.ContentType;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class HttpServerVerticleTest {
  private static final int TEST_PORT = 8080;
  private Vertx vertx;
  private WebClient client;
  
  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    
    vertx.deployVerticle(HttpServerVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(RdfStoreVerticle.class.getName(), tc.asyncAssertSuccess());
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
        tc.assertEquals(response.statusCode(), 200);
        tc.assertTrue(response.bodyAsString().length() > 0);
        async.complete();
      });
  }
  
  @Test
  public void testGetEntity(TestContext tc) {
    Async async = tc.async();
    
    createResourceAndThen(createAR -> {
      HttpResponse<Buffer> response = createAR.result();
      tc.assertEquals(response.statusCode(), 201);
      
      client.get(TEST_PORT, "localhost", "/environments/test_env")
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
        .send(ar -> {
          HttpResponse<Buffer> getResponse = ar.result();
          tc.assertEquals(getResponse.statusCode(), 200);
          
          try {
            assertIsomorphic(tc, RDFFormat.TURTLE,
              "<http://localhost:" + TEST_PORT + "/environments/test_env> "
                  + "a <http://w3id.org/eve#Environment>;\n"
              + "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1> .",
              getResponse.bodyAsString());
          } catch (RDFParseException | RDFHandlerException | IOException e) {
            tc.fail(e);
          }
            
          async.complete();
        });
      });
  }
  
  @Test
  public void testGetEntityNotFound(TestContext tc) {
    Async async = tc.async();
    
    client.get(TEST_PORT, "localhost", "/environments/bla123")
      .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
      .send(ar -> {
        HttpResponse<Buffer> getResponse = ar.result();
        tc.assertEquals(getResponse.statusCode(), 404);
        async.complete();
      });
  }
  
  @Test
  public void testGetEntityWithoutContentType(TestContext tc) {
    // TODO: null content types crash
  }
  
  @Test
  public void testCreateEntity(TestContext tc) {
    Async async = tc.async();
    
    client.post(TEST_PORT, "localhost", "/environments/")
      .putHeader("Slug", "env1")
      .putHeader("Content-Type", "text/turtle")
      .sendBuffer(Buffer.buffer("<> a <http://w3id.org/eve#Environment> ;\n" + 
        "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1> ."),
        ar -> {
          HttpResponse<Buffer> response = ar.result();
          tc.assertEquals(response.statusCode(), 201);
          
          try {
            assertIsomorphic(tc, RDFFormat.TURTLE,
              "<http://localhost:" + TEST_PORT + "/environments/env1> "
                  + "a <http://w3id.org/eve#Environment> ;\n" + 
              "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1> .", 
              response.bodyAsString());
          } catch (RDFParseException | RDFHandlerException | IOException e) {
            tc.fail(e);
          }
          
          async.complete();
        });
  }
  
  @Test
  public void testUpdateEntity(TestContext tc) {
    Async async = tc.async();
    
    createResourceAndThen(createAR -> {
        HttpResponse<Buffer> response = createAR.result();
        tc.assertEquals(response.statusCode(), 201);
        
        client.put(8080, "localhost", "/environments/test_env")
        .putHeader("Content-Type", "text/turtle")
        .sendBuffer(Buffer.buffer("<> a <http://w3id.org/eve#Environment> ;\n" + 
            "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1>, "
            + "<http://localhost:" + TEST_PORT + "/workspaces/wksp2> ."),
          ar -> {
            HttpResponse<Buffer> updateResponse = ar.result();
            tc.assertEquals(updateResponse.statusCode(), 200);
            try {
              assertIsomorphic(tc, RDFFormat.TURTLE,
                "<http://localhost:" + TEST_PORT + "/environments/test_env> "
                    + "a <http://w3id.org/eve#Environment>;\n" + 
                "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1>, "
                + "<http://localhost:" + TEST_PORT + "/workspaces/wksp2> .", 
                updateResponse.bodyAsString());
            } catch (RDFParseException | RDFHandlerException | IOException e) {
              tc.fail(e);
            }
            
            async.complete();
        });
      });
  }
  
  @Test
  public void testDeleteEntity(TestContext tc) {
    Async async = tc.async();
    
    createResourceAndThen(createAR -> {
        HttpResponse<Buffer> response = createAR.result();
        tc.assertEquals(response.statusCode(), 201);
        
        client.delete(8080, "localhost", "/environments/test_env")
          .send(ar -> {
            HttpResponse<Buffer> updateResponse = ar.result();
            tc.assertEquals(updateResponse.statusCode(), 200);
            async.complete();
          });
        });
  }
  
  @Test
  public void testCartagoVerticleNoArtifactTemplates(TestContext tc) {
    vertx.deployVerticle(CartagoVerticle.class.getName(), new DeploymentOptions().setWorker(true)
        .setConfig(null), tc.asyncAssertSuccess());
  }
  
  @Test
  public void testCartagoArtifact(TestContext tc) {
    // Register artifact template for this test
    JsonObject knownArtifacts = new JsonObject()
        .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.Counter");
    
    vertx.deployVerticle(CartagoVerticle.class.getName(), new DeploymentOptions().setWorker(true)
        .setConfig(new JsonObject().put("known-artifacts", knownArtifacts)), tc.asyncAssertSuccess());
    
    Async async = tc.async();
    
    client.post(8080, "localhost", "/workspaces/")
      .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
      .putHeader("Slug", "wksp1")
      .sendBuffer(Buffer.buffer(""), wkspAR -> {
        HttpResponse<Buffer> wkspResponse = wkspAR.result();
        tc.assertEquals(wkspResponse .statusCode(), 201);
        
        client.post(8080, "localhost", "/workspaces/wksp1/artifacts/")
          .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
          .putHeader("Slug", "c0")
          .putHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
          .sendBuffer(Buffer.buffer("{\"artifactClass\" : \"http://example.org/Counter\"}"), 
            ar -> {
              System.out.println("artifact created");
              HttpResponse<Buffer> response = ar.result();
              tc.assertEquals(response.statusCode(), 201);
              
              client.post(8080, "localhost", "/workspaces/wksp1/artifacts/c0/increment")
                .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
                .putHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .sendBuffer(Buffer.buffer("[1]"), actionAr -> {
                  System.out.println("operation executed");
                  HttpResponse<Buffer> actionResponse = actionAr.result();
                  tc.assertEquals(actionResponse.statusCode(), 200);
                  async.complete();
              });
            });
      });
  }
  
  private void createResourceAndThen(Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    client.post(8080, "localhost", "/environments/")
      .putHeader("Slug", "test_env")
      .putHeader("Content-Type", "text/turtle")
      .sendBuffer(Buffer.buffer("<> a <http://w3id.org/eve#Environment> ;\n" + 
        "<http://w3id.org/eve#contains> <http://localhost:" + TEST_PORT + "/workspaces/wksp1> ."), 
        handler);
  }
  
  private void assertIsomorphic(TestContext tc, RDFFormat format, String graph1, String graph2) 
    throws RDFParseException, RDFHandlerException, IOException {
  
    Model model1 = readModelFromString(format, graph1, "");
    Model model2 = readModelFromString(format, graph2, "");
    
    tc.assertTrue(Models.isomorphic(model1, model2));
  }
  
  private Model readModelFromString(RDFFormat format, String description, String baseURI) 
      throws RDFParseException, RDFHandlerException, IOException {
    StringReader stringReader = new StringReader(description);
    
    RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
    Model model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    
    rdfParser.parse(stringReader, baseURI);
    
    return model;
  }
}