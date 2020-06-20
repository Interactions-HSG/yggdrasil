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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class HttpServerVerticleTest {

  private Vertx vertx;

  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
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
    vertx.createHttpClient().getNow(8080, "localhost", "/", response -> {
        tc.assertEquals(response.statusCode(), 200);
        response.bodyHandler(body -> {
          tc.assertTrue(body.length() > 0);
          async.complete();
          });
        });
  }
  
  @Test
  public void testGetEntity(TestContext tc) {
    Async async = tc.async();
    
    createResourceAndThen(response -> {
        tc.assertEquals(response.statusCode(), 201);
        
        vertx.createHttpClient().get(8080, "localhost", "/environments/test_env")
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
            .handler(getResponse -> {
              tc.assertEquals(getResponse.statusCode(), 200);
              getResponse.bodyHandler(body -> {
                try {
                  assertIsomorphic(tc, RDFFormat.TURTLE,
                    "<http://localhost:8080/environments/test_env> a <http://w3id.org/eve#Environment>;\n"
                    + "<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/wksp1> .",
                    body.toString());
                } catch (RDFParseException | RDFHandlerException | IOException e) {
                  tc.fail(e);
                }
                
                async.complete();
              });
            }).end();
      });
  }
  
  @Test
  public void testGetEntityNotFound(TestContext tc) {
    Async async = tc.async();
    
    vertx.createHttpClient().get(8080, "localhost", "/environments/bla123")
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
        .handler(getResponse -> {
            tc.assertEquals(getResponse.statusCode(), 404);
            async.complete();
        }).end();
  }
  
  @Test
  public void testGetEntityWithoutContentType(TestContext tc) {
    // TODO: null content types crash
  }
  
  @Test
  public void testCreateEntity(TestContext tc) {
    Async async = tc.async();
    vertx.createHttpClient().post(8080, "localhost", "/environments/")
        .putHeader("Slug", "env1")
        .putHeader("Content-Type", "text/turtle")
        .handler(response -> {
          tc.assertEquals(response.statusCode(), 201);
          
          response.bodyHandler(body -> {
            try {
              assertIsomorphic(tc, RDFFormat.TURTLE,
                "<http://localhost:8080/environments/env1> a <http://w3id.org/eve#Environment> ;\n" + 
                "<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/wksp1> .", 
                body.toString());
            } catch (RDFParseException | RDFHandlerException | IOException e) {
              tc.fail(e);
            }
            
            async.complete();
          });
        }).end("<> a <http://w3id.org/eve#Environment> ;\n" + 
            "<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/wksp1> .");
  }
  
  @Test
  public void testUpdateEntity(TestContext tc) {
    Async async = tc.async();
    
    createResourceAndThen(response -> {
        tc.assertEquals(response.statusCode(), 201);
        
        vertx.createHttpClient().put(8080, "localhost", "/environments/test_env")
        .putHeader("Content-Type", "text/turtle")
        .handler(updateResponse -> {
          tc.assertEquals(updateResponse.statusCode(), 200);
          updateResponse.bodyHandler(body -> {
            try {
              assertIsomorphic(tc, RDFFormat.TURTLE,
                  "<http://localhost:8080/environments/test_env> a <http://w3id.org/eve#Environment>;\n" + 
                  "<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/wksp1>, "
                  + "<http://localhost:8080/workspaces/wksp2> .", 
                  body.toString());
            } catch (RDFParseException | RDFHandlerException | IOException e) {
              tc.fail(e);
            }
            
            async.complete();
          });
        })
        .end("<> a <http://w3id.org/eve#Environment> ;\n" + 
            "<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/wksp1>, "
            + "<http://localhost:8080/workspaces/wksp2> .");
      });
  }
  
  @Test
  public void testDeleteEntity(TestContext tc) {
    Async async = tc.async();
    
    createResourceAndThen(response -> {
        tc.assertEquals(response.statusCode(), 201);
        
        vertx.createHttpClient().delete(8080, "localhost", "/environments/test_env")
          .handler(updateResponse -> {
            tc.assertEquals(updateResponse.statusCode(), 200);
            async.complete();
          }).end();
        });
  }
  
  @Test
  public void testCartagoArtifact(TestContext tc) {
    vertx.deployVerticle(CartagoVerticle.class.getName(), tc.asyncAssertSuccess());
    
    Async async = tc.async();
    HttpClient client = vertx.createHttpClient();
    
    client.post(8080, "localhost", "/artifacts/")
        .putHeader("X-Artifact-Class", "org.hyperagents.yggdrasil.cartago.Counter")
        .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
        .putHeader("Slug", "c0")
        .putHeader("Content-Type", "text/turtle")
        .handler(response -> {
          tc.assertEquals(response.statusCode(), 201);
          
          client.post(8080, "localhost", "/artifacts/c0/increment")
          .putHeader("X-Agent-WebID", "http://andreiciortea.ro/#me")
          .putHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
          .handler(actionResponse -> {
            tc.assertEquals(actionResponse.statusCode(), 200);
            async.complete();
          })
          .end("[1]");
        })
        .end("<> a <http://w3id.org/eve#Artifact> .");
  }
  
  private void createResourceAndThen(Handler<HttpClientResponse> handler) {
    vertx.createHttpClient().post(8080, "localhost", "/environments/")
        .putHeader("Slug", "test_env")
        .putHeader("Content-Type", "text/turtle")
        .handler(handler)
        .end("<> a <http://w3id.org/eve#Environment> ;\n" + 
            "<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/wksp1> .");
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