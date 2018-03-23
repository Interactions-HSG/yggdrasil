package ro.andreiciortea.yggdrasil.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import com.google.gson.Gson;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.store.RdfStoreVerticle;

public class HttpEntityHandler {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());
  private Vertx vertx;
  
  private String webSubHubIRI = null;
  
  public HttpEntityHandler() {
    vertx = Vertx.currentContext().owner();
    
    JsonObject httpConfig = Vertx.currentContext().config().getJsonObject("http-config");
    
    if (httpConfig != null) {
      webSubHubIRI = httpConfig.getString("websub-hub");
    }
  }
  
  // TODO: add payload validation
  
  public void handleGetEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri);

    Map<String,List<String>> headers = new HashMap<String,List<String>>();
    
    if (webSubHubIRI != null) {
      headers.put("Link", Arrays.asList("<" + webSubHubIRI + ">; rel=\"hub\"", 
                                          "<" + entityIri + ">; rel=\"self\"")
          );
    }
    
    vertx.eventBus().send(RdfStoreVerticle.RDF_STORE_ENTITY_BUS_ADDRESS, 
        message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK, headers));
  }
  
  public void handleCreateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();
    
    String slug = routingContext.request().getHeader("Slug");
    
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.CREATE_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
        .setHeader(EventBusMessage.Headers.ENTITY_IRI_HINT, slug)
        .setPayload(entityRepresentation);
    
    vertx.eventBus().send(RdfStoreVerticle.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_CREATED));
  }
  
  public void handlePatchEntity(RoutingContext routingContext) {
    // TODO
  }
  
  public void handleUpdateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();
    
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.UPDATE_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
        .setPayload(entityRepresentation);
    
    vertx.eventBus().send(RdfStoreVerticle.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReply(routingContext));
  }
  
  public void handleDeleteEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.DELETE_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri);
    
    vertx.eventBus().send(RdfStoreVerticle.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReply(routingContext));
  }
  
  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext) {
    return handleStoreReply(routingContext, HttpStatus.SC_OK);
  }
  
  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext, int succeededStatusCode) {
    return handleStoreReply(routingContext, succeededStatusCode, new HashMap<String,List<String>>());
  }
  
  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext, 
      int succeededStatusCode, Map<String,List<String>> headers) {
    
    return reply -> {
      if (reply.succeeded()) {
        EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);
        
        if (storeReply.succeded()) {
          HttpServerResponse httpResponse = routingContext.response();
          
          httpResponse
            .setStatusCode(succeededStatusCode)
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");
          
          headers.forEach((k, v) -> {
            httpResponse.putHeader(k, v);
          });
          
          httpResponse.end(storeReply.getPayload());
        } else if (storeReply.entityNotFound()) {
          routingContext.fail(HttpStatus.SC_NOT_FOUND);
        } else {
          LOGGER.error(storeReply.getHeader(EventBusMessage.Headers.REPLY_STATUS));
          routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
      } else {
        LOGGER.error("Reply failed! " + reply.cause().getMessage());
        routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }
    };
  }
  
}
