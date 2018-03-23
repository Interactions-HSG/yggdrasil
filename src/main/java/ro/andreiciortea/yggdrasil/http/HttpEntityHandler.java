package ro.andreiciortea.yggdrasil.http;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import com.google.gson.Gson;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.store.RdfStoreVerticle;

public class HttpEntityHandler {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());
  private Vertx vertx;
  
  public HttpEntityHandler() {
    vertx = Vertx.currentContext().owner();
  }
  
  // TODO: add payload validation
  
  public void handleGetEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri);
    
    vertx.eventBus().send(RdfStoreVerticle.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReply(routingContext));
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
    return reply -> {
      if (reply.succeeded()) {
        EventBusMessage response = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);
        
        if (response.succeded()) {
          LOGGER.info("Response succeeeded!");
          routingContext.response()
            .setStatusCode(succeededStatusCode)
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
            .end(response.getPayload());
        } else if (response.entityNotFound()) {
          routingContext.fail(HttpStatus.SC_NOT_FOUND);
        } else {
          LOGGER.info(response.getHeader(EventBusMessage.Headers.REPLY_STATUS));
          routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
      } else {
        LOGGER.info("Reply failed! " + reply.cause().getMessage());
        routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }
    };
  }
  
}
