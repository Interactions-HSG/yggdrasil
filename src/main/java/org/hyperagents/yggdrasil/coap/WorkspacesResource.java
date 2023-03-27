package org.hyperagents.yggdrasil.coap;

import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.utils.CoapResource;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.CartagoEntityHandler;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.RdfStore;

import java.util.Arrays;

public class WorkspacesResource extends CoapResource {

  Vertx vertx;

  CartagoEntityHandler cartagoHandler;

  public WorkspacesResource(Vertx vertx) {
    this.vertx = vertx;
    this.cartagoHandler = new CartagoEntityHandler(vertx);
  }
  @Override
  public void get(CoapExchange coapExchange) throws CoapCodeException {
    coapExchange.setResponseCode(Code.C405_METHOD_NOT_ALLOWED);
    coapExchange.sendResponse();
  }

  @Override
  public void post(CoapExchange coapExchange) throws CoapCodeException {
    handleCreateWorkspace(coapExchange);
  }

  public void handleCreateWorkspace(CoapExchange coapExchange) {
    System.out.println("handle create workspace");
    String representation = coapExchange.getRequestBodyString();
    String workspaceName = Arrays.toString(coapExchange.getRequestHeaders().getCustomOption(2049));
    String agentId = Arrays.toString(coapExchange.getRequestHeaders().getCustomOption(2048));

    if (agentId == null) {
      coapExchange.setResponseCode(Code.C401_UNAUTHORIZED);
      coapExchange.sendResponse();
    }

    Promise<String> cartagoPromise = Promise.promise();
    cartagoHandler.createWorkspace(agentId, workspaceName, representation, cartagoPromise);

    cartagoPromise.future().compose(result -> Future.future(promise -> storeEntity(coapExchange, workspaceName, result, promise)));
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
