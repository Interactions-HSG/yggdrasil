package org.hyperagents.yggdrasil.websub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class HttpNotificationVerticle extends AbstractVerticle {
  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.dispatcher";
  
  public static final String ENTITY_CREATED = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".entityCreated";
  public static final String ENTITY_CHANGED = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".entityChanged";
  public static final String ENTITY_DELETED = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".entityDeleted";
  public static final String ARTIFACT_OBS_PROP = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".artifactObsProp";
  
  private final static Logger LOGGER = LoggerFactory.getLogger(
      HttpNotificationVerticle.class.getName());
  
  private String webSubHubIRI = null;
  private String rdfSubHubIRI = null;
  
  @Override
  public void start() {
    webSubHubIRI = getSubHubIRI(config(), "websub-hub");
    rdfSubHubIRI = getSubHubIRI(config(), "rdfsub-hub");
    
    vertx.eventBus().consumer(BUS_ADDRESS, message -> {
      if (isNotificationMessage(message)) {
        String entityIRI = message.headers().get(HttpEntityHandler.REQUEST_URI);
        
        if (entityIRI != null && !entityIRI.isEmpty()) {
          String changes = (String) message.body();
          LOGGER.info("Dispatching notifications for: " + entityIRI + ", changes: " + changes);
          
          WebClient client = WebClient.create(vertx);
          
          List<String> linkHeaders = new ArrayList<String>();
          linkHeaders.add("<" + webSubHubIRI + ">; rel=\"hub\"");
          linkHeaders.add("<" + entityIRI + ">; rel=\"self\"");
          
          Set<String> callbacks = NotificationSubscriberRegistry.getInstance().getCallbackIRIs(entityIRI);
          
          for (String callbackIRI : callbacks) {
            HttpRequest<Buffer> request = client.postAbs(callbackIRI)
                .putHeader("Link", linkHeaders.get(0))
                .putHeader("Link", linkHeaders.get(1));
            
            if (message.headers().get(HttpEntityHandler.REQUEST_METHOD).equals(ENTITY_DELETED)) {
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: entity deleted");
              request.send(reponseHandler(callbackIRI));
            } else if (changes != null && !changes.isEmpty()) {
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: " + changes);
              request.putHeader(HttpHeaders.CONTENT_LENGTH, "" + changes.length())
                .sendBuffer(Buffer.buffer(changes), reponseHandler(callbackIRI));
            }
          }
          
          // TODO: refactor pushing data to the RDFSub hub
          String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
          if (requestMethod.equals(ENTITY_CREATED) || requestMethod.equals(ENTITY_CHANGED)) {
            String updateQuery = constructUpdateQuery(entityIRI, changes);
            LOGGER.info("SPARQL Update query: " + updateQuery);
            
            client.postAbs(rdfSubHubIRI + "publish")
              .putHeader("Content-Type", "application/sparql-update")
              .sendBuffer(Buffer.buffer(updateQuery), reponseHandler(rdfSubHubIRI + "publish"));
            
          }
        }
      }
    });
  }
  
  private Handler<AsyncResult<HttpResponse<Buffer>>> reponseHandler(String callbackIRI) {
    return ar -> {
      HttpResponse<Buffer> response = ar.result();
      
      if (response == null) {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", operation failed: " 
            + ar.cause().getMessage());
      } else if (response.statusCode() == HttpStatus.SC_OK 
          || response.statusCode() == HttpStatus.SC_ACCEPTED) {
        LOGGER.info("Notification sent to: " + callbackIRI + ", status code: " + response.statusCode());
      } else {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", status code: " 
            + response.statusCode());
      }
    };
  }
  
  private boolean isNotificationMessage(Message<Object> message) {
    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    
    if (requestMethod.equals(ENTITY_CREATED) || requestMethod.equals(ENTITY_CHANGED) 
        || requestMethod.equals(ENTITY_DELETED) || requestMethod.equals(ARTIFACT_OBS_PROP)) {
      return true;
    }
    return false;
  }
  
  private String getSubHubIRI(JsonObject config, String hubType) {
    JsonObject httpConfig = config.getJsonObject("http-config");
    
    if (httpConfig != null && httpConfig.getString(hubType) != null) {
      return httpConfig.getString(hubType);
    }
    return null;
  }
  
  // TODO: refactor, what follows is a quick-and-dirty implementation for the MASTech Dec 2020 demo
  private String constructUpdateQuery(String graphIRI, String graph) {
    String payload = "";
    
    try {
      Model model = readModelFromString(RDFFormat.TURTLE, graph, "http://example.org/");
      payload = writeToString(RDFFormat.TURTLE, model);
      
    } catch (RDFParseException | RDFHandlerException | IOException e) {
      LOGGER.error(e.getMessage());
    }
    
    String query = "INSERT DATA { GRAPH <http://hyperagents.org/> { " + payload + " } }";
    
    return query;
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
  
  private String writeToString(RDFFormat format, Model model) {
    OutputStream out = new ByteArrayOutputStream();
    
    List<String> namespaces = model.getNamespaces().stream().map(n -> n.getPrefix())
        .collect(Collectors.toList());
    
    for (String n : namespaces) {
      model.removeNamespace(n);
    }
    
    try {
      Rio.write(model, out, format, 
          new WriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, true)
            .set(BasicWriterSettings.PRETTY_PRINT, false));
    } finally {
      try {
        out.close();
      } catch (IOException e) {
        LOGGER.info(e.getMessage());
      }
    }
    
    return out.toString();
  }
}
