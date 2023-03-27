package org.hyperagents.yggdrasil.coap;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.io.TDWriter;
import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.utils.CoapResource;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.yggdrasil.cartago.CartagoEntityHandler;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.http.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.store.RdfStore;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SubWorkspaceResource extends CoapResource {

  Vertx vertx;

  CartagoEntityHandler cartagoHandler;

  public SubWorkspaceResource(Vertx vertx){
    this.vertx = vertx;
    this.cartagoHandler = new CartagoEntityHandler(vertx);

  }
  @Override
  public void get(CoapExchange coapExchange) throws CoapCodeException {
    String uri = coapExchange.getRequestUri();
    if (isArtifact(uri)){
      handleGetEntity(coapExchange);

    }
  }

  @Override
  public void post(CoapExchange coapExchange) throws CoapCodeException {
    String uri = coapExchange.getRequestUri();
    if (isArtifact(uri)){ //TODO: change
      handleCreateArtifact(coapExchange);
    }
  }


  public boolean isArtifact(String uri){
    boolean b = false;
    if (uri.contains("artifacts")){
      b = true;
    }
    return b;
  }

  public void handleGetEntity(CoapExchange coapExchange) {
    String entityIRI = coapExchange.getRequestUri();

    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.GET_ENTITY)
      .addHeader(HttpEntityHandler.REQUEST_URI, entityIRI);

    Map<String, List<String>> headers = getHeaders(entityIRI);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options,
      handleStoreReply(coapExchange, HttpStatus.SC_OK, headers));
  }

  private Map<String, List<String>> getHeaders(String entityIRI) {

    Map<String,List<String>> headers = getWebSubHeaders(entityIRI);
    headers.putAll(getCORSHeaders());

    return headers;
  }

  private Map<String, List<String>> getWebSubHeaders(String entityIRI) {
    Map<String,List<String>> headers = new HashMap<>();

    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(Vertx.currentContext().config());
    Optional<String> webSubHubIRI = httpConfig.getWebSubHubIRI();

    webSubHubIRI.ifPresent(hubIRI -> headers.put("Link", Arrays.asList("<" + hubIRI + ">; rel=\"hub\"",
      "<" + entityIRI + ">; rel=\"self\"")));

    return headers;
  }

  private Map<String, ? extends List<String>> getCORSHeaders() {
    Map<String, List<String>> corsHeaders = new HashMap<>();

    corsHeaders.put(com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, List.of("*"));
    corsHeaders.put(com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, List.of("true"));
    corsHeaders.put(com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name()));

    return corsHeaders;
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(CoapExchange coapExchange,
                                                                 int succeededStatusCode) {
    return handleStoreReply(coapExchange, succeededStatusCode, new HashMap<>());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(CoapExchange coapExchange,
                                                                 int succeededStatusCode, Map<String,List<String>> headers) {

    return reply -> {
      if (reply.succeeded()) {

        //HttpServerResponse httpResponse = routingContext.response();
        //httpResponse.setStatusCode(succeededStatusCode);
        coapExchange.setResponseCode(Code.C203_VALID);
        //String mediatype = routingContext.request().getHeader("Accept");
        Integer mediatype = coapExchange.getRequestHeaders().getAccept();
        if (mediatype == null){
          mediatype = 0; //TODO: See what to put for text/turtle
        }
        if (mediatype.equals(50)){ //TODO: see what to put for application/ld+json
          //httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");
          coapExchange.getResponseHeaders().put(17, String.valueOf(50).getBytes(StandardCharsets.UTF_8));
        } else {

          //httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");
          coapExchange.getResponseHeaders().put(17, String.valueOf(0).getBytes(StandardCharsets.UTF_8));
        }

        // Note: forEach produces duplicate keys. If anyone wants to, please replace this with a Java 8 version ;-)
        for(String headerName : headers.keySet()) {
          //httpResponse.putHeader(headerName, String.join(",", headers.get(headerName))); //TODO: check
        }

        String storeReply = reply.result().body();
        if (mediatype.equals("application/ld+json")){
          ThingDescription td = TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, storeReply);
          storeReply = TDWriter.write(td, RDFFormat.JSONLD);
        }

        if (storeReply != null && !storeReply.isEmpty()) {
          //httpResponse.end(storeReply);
          coapExchange.setResponseBody(storeReply);
          coapExchange.sendResponse();
        } else {
          coapExchange.sendResponse();
        }
      } else {
        ReplyException exception = ((ReplyException) reply.cause());



        if (exception.failureCode() == HttpStatus.SC_NOT_FOUND) {
          //routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
          coapExchange.setResponseCode(Code.C404_NOT_FOUND);
          coapExchange.sendResponse();
        } else {
          //routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
          coapExchange.setResponseCode(Code.C500_INTERNAL_SERVER_ERROR);
          coapExchange.sendResponse();
        }
      }
    };
  }

  public void handleCreateArtifact(CoapExchange coapExchange) {
    String representation = coapExchange.getRequestBodyString();
    String workspaceName = getWorkspaceName(coapExchange.getRequestUri());
    String agentId = Arrays.toString(coapExchange.getRequestHeaders().getCustomOption(2048));

    if (agentId == null) {
      coapExchange.setResponseCode(Code.C401_UNAUTHORIZED);
      coapExchange.sendResponse();
    }

    JsonObject artifactInit = (JsonObject) Json.decodeValue(representation);
    String artifactName = artifactInit.getString("artifactName");

    Promise<String> cartagoPromise = Promise.promise();
    cartagoHandler.createArtifact(agentId, workspaceName, artifactName, representation,
      cartagoPromise);

    cartagoPromise.future().compose(result ->
      Future.future(promise -> storeEntity(coapExchange, artifactName, result, promise)));
  }

  public String getWorkspaceName(String uri){
    return uri; //TODO: change
  }

  private void storeEntity(CoapExchange coapExchange, String entityName, String representation,
                           Promise<Object> promise) {
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
      .addHeader(HttpEntityHandler.REQUEST_URI, coapExchange.getRequestUri())
      .addHeader(HttpEntityHandler.ENTITY_URI_HINT, entityName);
//        .addHeader(CONTENT_TYPE, context.request().getHeader("Content-Type"));

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, result -> {
      if (result.succeeded()) {
        coapExchange.setResponseCode(Code.C201_CREATED);
        coapExchange.setResponseBody(representation);
        coapExchange.sendResponse();
        promise.complete();
      } else {
        coapExchange.setResponseCode(Code.C500_INTERNAL_SERVER_ERROR);
        coapExchange.sendResponse();
        promise.fail("Could not store the entity representation.");
      }
    });
  }




}
