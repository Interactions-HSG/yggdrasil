package org.hyperagents.yggdrasil.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

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
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class HttpEntityHandler {
  public static final String REQUEST_METHOD = "org.hyperagents.yggdrasil.eventbus.headers.requestMethod";
  public static final String REQUEST_URI = "org.hyperagents.yggdrasil.eventbus.headers.requestUri";
  public static final String ENTITY_URI_HINT = "org.hyperagents.yggdrasil.eventbus.headers.slug";
  public static final String CONTENT_TYPE = "org.hyperagents.yggdrasil.eventbus.headers.contentType";
  
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
    
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
        .addHeader(REQUEST_URI, entityIri)
        .addHeader(CONTENT_TYPE, contentType);
    
    Map<String,List<String>> headers = new HashMap<String,List<String>>();

    if (webSubHubIRI != null) {
      headers.put("Link", Arrays.asList("<" + webSubHubIRI + ">; rel=\"hub\"",
                                          "<" + entityIri + ">; rel=\"self\"")
          );
    }
    
    vertx.eventBus().send(RdfStore.BUS_ADDRESS, null, options, handleStoreReply(routingContext, 
        HttpStatus.SC_OK, headers));
  }
  
  // TODO: add payload validation
  public void handleCreateEntity(RoutingContext routingContext) {
    String entityRepresentation = routingContext.getBodyAsString();
    String slug = routingContext.request().getHeader("Slug");
    String agentUri = routingContext.request().getHeader("X-Agent-WebID");
    
    Optional<String> artifactClass = Optional.empty();
    HypermediaArtifactRegistry artifactRegistry = HypermediaArtifactRegistry.getInstance();
    Set<IRI> types;
    
    try {
      types = new RdfPayload(RDFFormat.TURTLE, entityRepresentation, "").getSemanticTypes();
    } catch (RDFParseException | RDFHandlerException | IOException e) {
      e.printStackTrace();
      routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
      return;
    }
    
    for (IRI type : types) {
      artifactClass = artifactRegistry.getArtifactTemplate(type.stringValue());
      if (artifactClass.isPresent()) {
        break;
      }
    }
    
    if (artifactClass.isPresent() && agentUri != null && !agentUri.isEmpty()) {
      // The client wants to instantiate a known virtual artifact. 
      // Send request to CArtAgO verticle to instantiate the artifact.
      DeliveryOptions options = new DeliveryOptions()
          .addHeader(CartagoVerticle.AGENT_ID, agentUri)
          .addHeader(REQUEST_METHOD, CartagoVerticle.INSTANTIATE_ARTIFACT)
          .addHeader(CartagoVerticle.ARTIFACT_CLASS, artifactClass.get())
          .addHeader(ENTITY_URI_HINT, slug);
      
      vertx.eventBus().send(CartagoVerticle.BUS_ADDRESS, entityRepresentation, options,
          response -> {
              if (response.succeeded()) {
                String artifactDescription = (String) response.result().body();
                
                LOGGER.info("CArtAgO artifact created: " + artifactDescription);
                
                // If the CArtAgO artifact was created successfully, generate the Thing Description 
                // and store it
                createEntity(routingContext, artifactDescription);
              }
            });
    } else {
      createEntity(routingContext, entityRepresentation);
    }
  }
  
  public void handleAction(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    String entityRepresentation = routingContext.getBodyAsString();
    
    String agentUri = request.getHeader("X-Agent-WebID");
    String artifactName = request.params().get("artid");
    String artifactIri = "http://localhost:8080/artifacts/" + artifactName;
    String actionName = HypermediaArtifactRegistry.getInstance().getActionName(request.rawMethod(), 
        request.absoluteURI());

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
        .addHeader(REQUEST_URI, artifactIri)
        .addHeader(CONTENT_TYPE, request.getHeader(HttpHeaders.CONTENT_TYPE));
    
    vertx.eventBus().send(RdfStore.BUS_ADDRESS, null, options, reply -> {
          if (reply.succeeded()) {
              String artifactDescription = (String) reply.result().body();
              ThingDescription td = TDGraphReader.readFromString(TDFormat.RDF_TURTLE, 
                  artifactDescription);
              
              DeliveryOptions cartagoOptions = new DeliveryOptions()
                  .addHeader(CartagoVerticle.AGENT_ID, agentUri)
                  .addHeader(REQUEST_METHOD, CartagoVerticle.PERFORM_ACTION)
                  .addHeader(CartagoVerticle.ARTIFACT_NAME, artifactName)
                  .addHeader(CartagoVerticle.ACTION_NAME, actionName);
              
              Optional<ActionAffordance> affordance = td.getActions().stream().filter(action -> 
                  action.getTitle().get().compareTo(actionName) == 0).findFirst();
              
              String serializedPayload = null;
              
              if (affordance.isPresent()) {
                Optional<DataSchema> inputSchema = affordance.get().getInputSchema();
                
                if (inputSchema.isPresent() && inputSchema.get().getDatatype() == DataSchema.ARRAY) {
                  JsonElement payload = JsonParser.parseString(entityRepresentation);
                  List<Object> params = ((ArraySchema) inputSchema.get()).parseJson(payload);
                  
                  serializedPayload = CartagoDataBundle.toJson(params);
                }
              }
              
              vertx.eventBus().send(CartagoVerticle.BUS_ADDRESS, serializedPayload, cartagoOptions);
              routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
            }
        });
  }
  
  public void handlePatchEntity(RoutingContext routingContext) {
    // TODO
  }

  // TODO: add payload validation
  public void handleUpdateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.UPDATE_ENTITY)
        .addHeader(REQUEST_URI, entityIri);
    
    vertx.eventBus().send(RdfStore.BUS_ADDRESS, entityRepresentation, options, 
        handleStoreReplyNext(routingContext));
  }

  public void handleDeleteEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.DELETE_ENTITY)
        .addHeader(REQUEST_URI, entityIri);
    
    vertx.eventBus().send(RdfStore.BUS_ADDRESS, null, options, handleStoreReplyNext(routingContext));
  }

  public void handleEntitySubscription(RoutingContext routingContext) {
    JsonObject subscribeRequest = routingContext.getBodyAsJson();

    String mode = subscribeRequest.getString("hub.mode");
    String entityIri = subscribeRequest.getString("hub.topic");
    String callbackIri = subscribeRequest.getString("hub.callback");

    if (mode.equalsIgnoreCase("subscribe")) {
      DeliveryOptions options = new DeliveryOptions()
          .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
          .addHeader(REQUEST_URI, entityIri);
      
      vertx.eventBus().send(RdfStore.BUS_ADDRESS, null, options, reply -> {
            if (reply.succeeded()) {
              NotificationSubscriberRegistry.getInstance().addCallbackIRI(entityIri, callbackIri);
              routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
            } else {
              ReplyException exception = ((ReplyException) reply.cause());
              
              if (exception.failureCode() == HttpStatus.SC_NOT_FOUND) {
                routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
              } else {
                routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
              }
            }
          });
    }
    else if (mode.equalsIgnoreCase("unsubscribe")) {
      NotificationSubscriberRegistry.getInstance().removeCallbackIRI(entityIri, callbackIri);
      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    }
    else {
      routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
    }
  }
  
  private void createEntity(RoutingContext context, String representation) {
    String entityIri = context.request().absoluteURI();
    String slug = context.request().getHeader("Slug");
    String contentType = context.request().getHeader("Content-Type");
    
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.CREATE_ENTITY)
        .addHeader(REQUEST_URI, entityIri)
        .addHeader(ENTITY_URI_HINT, slug)
        .addHeader(CONTENT_TYPE, contentType);
    
    vertx.eventBus().send(RdfStore.BUS_ADDRESS, representation, options, handleStoreReply(context, 
        HttpStatus.SC_CREATED));
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext (RoutingContext routingContext) {
    return handleStoreReplyNext(routingContext, HttpStatus.SC_OK);
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext(RoutingContext routingContext, 
      int succeededStatusCode) {
    return handleStoreReplyNext(routingContext, succeededStatusCode, 
        new HashMap<String,List<String>>());
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

  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext, 
      int succeededStatusCode) {
    return handleStoreReply(routingContext, succeededStatusCode, new HashMap<String,List<String>>());
  }
  
  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext,
      int succeededStatusCode, Map<String,List<String>> headers) {

    return reply -> {
      if (reply.succeeded()) {
        HttpServerResponse httpResponse = routingContext.response();
        
        httpResponse
          .setStatusCode(succeededStatusCode)
          .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");

        headers.forEach((k, v) -> {
          httpResponse.putHeader(k, v);
        });
        
        String storeReply = reply.result().body();
        if (storeReply != null && !storeReply.isEmpty()) {
          httpResponse.end(storeReply);
        } else {
          httpResponse.end();
        }
      } else {
        ReplyException exception = ((ReplyException) reply.cause());
        
        if (exception.failureCode() == HttpStatus.SC_NOT_FOUND) {
          routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
        } else {
          routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
        }
      }
    };
  }
}
