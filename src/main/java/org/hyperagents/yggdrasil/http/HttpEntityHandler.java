package org.hyperagents.yggdrasil.http;

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
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.core.EventBusMessage;
import org.hyperagents.yggdrasil.core.EventBusRegistry;
import org.hyperagents.yggdrasil.core.SubscriberRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpEntityHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());
  private Vertx vertx;

  private String webSubHubIRI = null;

  public HttpEntityHandler() {
    vertx = Vertx.currentContext().owner();

    JsonObject httpConfig = Vertx.currentContext().config().getJsonObject("http-config");

    if (httpConfig != null && httpConfig.getString("websub-hub") != null) {
      webSubHubIRI = httpConfig.getString("websub-hub");
    }
  }


  public void handleGetEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String contentType = routingContext.request().getHeader("Content-Type");

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
        .setHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE, contentType);

    Map<String,List<String>> headers = new HashMap<String,List<String>>();

    if (webSubHubIRI != null) {
      headers.put("Link", Arrays.asList("<" + webSubHubIRI + ">; rel=\"hub\"",
                                          "<" + entityIri + ">; rel=\"self\"")
          );
    }

    vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS,
        message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK, headers));
  }

  // TODO: add payload validation
  public void handleCreateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();
    String contentType = routingContext.request().getHeader("Content-Type");

    String slug = routingContext.request().getHeader("Slug");

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.CREATE_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
        .setHeader(EventBusMessage.Headers.ENTITY_IRI_HINT, slug)
        .setHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE, contentType)
        .setPayload(entityRepresentation);

    vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_CREATED));
  }

  public void handlePatchEntity(RoutingContext routingContext) {
    // TODO
  }

  // TODO: add payload validation
  public void handleUpdateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.UPDATE_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
        .setPayload(entityRepresentation);

    vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReplyNext(routingContext));
  }

  public void handleDeleteEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.DELETE_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri);

    vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(), handleStoreReplyNext(routingContext));
  }

  public void handleEntitySubscription(RoutingContext routingContext) {
    JsonObject subscribeRequest = routingContext.getBodyAsJson();

    String mode = subscribeRequest.getString("hub.mode");
    String entityIRI = subscribeRequest.getString("hub.topic");
    String callbackIRI = subscribeRequest.getString("hub.callback");

    if (mode.equalsIgnoreCase("subscribe")) {
      EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_ENTITY)
          .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIRI);

      vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, message.toJson(),
          reply -> {
            if (reply.succeeded()) {
              EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);

              if (storeReply.succeded()) {
                SubscriberRegistry.getInstance().addCallbackIRI(entityIRI, callbackIRI);
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
      SubscriberRegistry.getInstance().removeCallbackIRI(entityIRI, callbackIRI);
      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    }
    else {
      routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
    }
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext (RoutingContext routingContext) {
    return handleStoreReplyNext(routingContext, HttpStatus.SC_OK);
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext(RoutingContext routingContext, int succeededStatusCode) {
    return handleStoreReplyNext(routingContext, succeededStatusCode, new HashMap<String,List<String>>());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext(RoutingContext routingContext,
                                                                 int succeededStatusCode, Map<String,List<String>> headers) {
    if (succeededStatusCode == HttpStatus.SC_OK) {
      return handleStoreReply(routingContext, succeededStatusCode, headers);
    } else {
      routingContext.next();
      return null;
    }
  }

//  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext) {
//    return handleStoreReply(routingContext, HttpStatus.SC_OK);
//  }

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

          if (storeReply.getPayload().isPresent()) {
            httpResponse.end(storeReply.getPayload().get());
          } else {
            httpResponse.end();
          }
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
