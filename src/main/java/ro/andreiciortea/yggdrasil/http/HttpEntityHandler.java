package ro.andreiciortea.yggdrasil.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private Map<String,Set<String>> subscriptions;
  
  public HttpEntityHandler() {
    vertx = Vertx.currentContext().owner();
    
    JsonObject httpConfig = Vertx.currentContext().config().getJsonObject("http-config");
    
    if (httpConfig != null && httpConfig.getString("websub-hub") != null) {
      webSubHubIRI = httpConfig.getString("websub-hub");
      subscriptions = new HashMap<String,Set<String>>();
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
  
  public void handleEntitySubscription(RoutingContext routingContext) {
    JsonObject subscribeRequest = routingContext.getBodyAsJson();
    
    String mode = subscribeRequest.getString("hub.mode");
    String entityIRI = subscribeRequest.getString("hub.topic");
    String callbackIRI = subscribeRequest.getString("hub.callback");
    
    Set<String> callbacks = subscriptions.get(entityIRI);
    
    if (callbacks == null) {
      callbacks = new HashSet<String>();
      subscriptions.put(entityIRI, callbacks);
    }
    
    if (mode.equalsIgnoreCase("subscribe")) {
      EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_ENTITY)
          .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIRI);
      
      vertx.eventBus().send(RdfStoreVerticle.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(),
          reply -> {
            if (reply.succeeded()) {
              EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);
              
              if (storeReply.succeded()) {
                Set<String> callbackIRIs = subscriptions.get(entityIRI);
                callbackIRIs.add(callbackIRI);
                subscriptions.put(entityIRI, callbackIRIs);
                
                routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
              }
              else if (storeReply.entityNotFound()) {
                routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
              }
              else {
                routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
              }
            }
            else {
              routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
            }
          });
    }
    else if (mode.equalsIgnoreCase("unsubscribe")) {
      callbacks.remove(callbackIRI);
      subscriptions.put(entityIRI, callbacks);
      
      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    }
    else {
      routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
    }
    
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
        }
        else if (storeReply.entityNotFound()) {
          routingContext.fail(HttpStatus.SC_NOT_FOUND);
        }
        else {
          LOGGER.error(storeReply.getHeader(EventBusMessage.Headers.REPLY_STATUS));
          routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
      }
      else {
        LOGGER.error("Reply failed! " + reply.cause().getMessage());
        routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }
    };
  }
}
