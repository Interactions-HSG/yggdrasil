package org.hyperagents.yggdrasil.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.CartagoDataBindingUtils;
import org.hyperagents.yggdrasil.core.EventBusMessage;
import org.hyperagents.yggdrasil.core.EventBusRegistry;
import org.hyperagents.yggdrasil.core.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.core.SubscriberRegistry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

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
    
    String agentUri = routingContext.request().getHeader("X-Agent-WebID");
    String artifactClass = routingContext.request().getHeader("X-Artifact-Class");
    
    if (artifactClass != null && !artifactClass.isEmpty()
          && agentUri != null && !agentUri.isEmpty()) {
      // The client wants to instantiate a known virtual artifact. 
      // Send request to CArtAgO verticle to instantiate the artifact.
      EventBusMessage cartagoRequest = new EventBusMessage(EventBusMessage.MessageType.INSTANTIATE_ARTIFACT)
          .setHeader(EventBusMessage.Headers.AGENT_WEBID, agentUri)
          .setHeader(EventBusMessage.Headers.ARTIFACT_CLASS, artifactClass)
          // TODO: the entity IRI should be decided before performing the CArtAgO operation
          .setHeader(EventBusMessage.Headers.ENTITY_IRI_HINT, slug)
          .setPayload(entityRepresentation);

      vertx.eventBus().send(EventBusRegistry.CARTAGO_BUS_ADDRESS, cartagoRequest.toJson(), response -> {
        if (response.succeeded()) {
          String artifactDescrption = (String) response.result().body();
          
          LOGGER.info("CArtAgO artifact created: " + artifactDescrption);
          
          // If the CArtAgO artifact was created successfully, generate the Thing Description and store it
          EventBusMessage storeRequest = new EventBusMessage(EventBusMessage.MessageType.CREATE_ENTITY)
              .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
              .setHeader(EventBusMessage.Headers.ENTITY_IRI_HINT, slug)
              .setHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE, contentType)
              .setPayload(artifactDescrption);

          vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, storeRequest.toJson(), handleStoreReply(routingContext, HttpStatus.SC_CREATED));
        }
      });
    }
  }
  
  public void handleNewAction(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    String entityRepresentation = routingContext.getBodyAsString();
    
    String agentUri = request.getHeader("X-Agent-WebID");
    String artifactName = request.params().get("artid");
    String artifactIri = "http://localhost:8080/artifacts/" + artifactName;
    String actionName = HypermediaArtifactRegistry.getInstance().getActionName(request.rawMethod(), 
        request.absoluteURI());

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_ENTITY)
        .setHeader(EventBusMessage.Headers.REQUEST_IRI, artifactIri)
        .setHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE, 
            request.getHeader(HttpHeaders.CONTENT_TYPE));
    
    vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS,
        message.toJson(), reply -> {
          if (reply.succeeded()) {
            EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), 
                EventBusMessage.class);
            
            if (storeReply.succeded()) {
              String artifactDescription = storeReply.getPayload().get();
              ThingDescription td = TDGraphReader.readFromString(TDFormat.RDF_TURTLE, 
                  artifactDescription);
              
              EventBusMessage cartagoMessage = new EventBusMessage(EventBusMessage.MessageType
                    .DO_ACTION)
                .setHeader(EventBusMessage.Headers.AGENT_WEBID, agentUri)
                .setHeader(EventBusMessage.Headers.ARTIFACT_NAME, artifactName)
                .setHeader(EventBusMessage.Headers.ACTION_NAME, actionName);
              
              Optional<ActionAffordance> affordance = td.getActions().stream().filter(action -> 
                  action.getTitle().get().compareTo(actionName) == 0).findFirst();
              
              if (affordance.isPresent()) {
                Optional<DataSchema> inputSchema = affordance.get().getInputSchema();
                
                if (inputSchema.isPresent() && inputSchema.get().getDatatype() == DataSchema.ARRAY) {
                  JsonElement payload = JsonParser.parseString(entityRepresentation);
                  List<Object> params = ((ArraySchema) inputSchema.get()).parseJson(payload);
                  
                  String serializedPayload = CartagoDataBindingUtils.toJson(params);
                  cartagoMessage.setPayload(serializedPayload);
                }
              }
              
              LOGGER.info("Sending message to CArtAgO verticle!");
              vertx.eventBus().send(EventBusRegistry.CARTAGO_BUS_ADDRESS, cartagoMessage.toJson());
              routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
            }
          }
        });
  }
  
  public void handleAction(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    String entityRepresentation = routingContext.getBodyAsString();
    
    String agentUri = request.getHeader("X-Agent-WebID");
    String artifactName = request.params().get("artid");
    String actionName = HypermediaArtifactRegistry.getInstance().getActionName(request.rawMethod(), 
        request.absoluteURI());
    
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.DO_ACTION)
        .setHeader(EventBusMessage.Headers.AGENT_WEBID, agentUri)
        .setHeader(EventBusMessage.Headers.ARTIFACT_NAME, artifactName)
        .setHeader(EventBusMessage.Headers.ACTION_NAME, actionName)
        .setPayload(entityRepresentation);
    
    vertx.eventBus().send(EventBusRegistry.CARTAGO_BUS_ADDRESS, message.toJson());
    
    routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
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
