package ro.andreiciortea.yggdrasil.http;

import com.google.gson.Gson;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;


public class HttpTemplateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());
  private Vertx vertx;

  public HttpTemplateHandler() {
    vertx = Vertx.currentContext().owner();
    }

  // TODO: add payload validation

  public void handleGetTemplates(RoutingContext routingContext) {
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_TEMPLATES);

    vertx.eventBus().send(EventBusRegistry.TEMPLATE_HANDLER_BUS_ADDRESS,
      message.toJson(), handleReply(routingContext, HttpStatus.SC_OK));
  }

  private Handler<AsyncResult<Message<String>>> handleReply(RoutingContext routingContext, int succeededStatusCode) {

    return reply -> {
      if (reply.succeeded()) {
        EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);

        if (storeReply.succeded()) {
          HttpServerResponse httpResponse = routingContext.response();

          httpResponse
            .setStatusCode(succeededStatusCode)
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");

          if (storeReply.getPayload().isPresent() && storeReply.getPayload().get().length() > 0) {
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

  public void handleInstantiateTemp(RoutingContext routingContext) {
    // IRI of template repesentation to be created
    String entityIri = routingContext.request().absoluteURI();
    String body = routingContext.getBodyAsString();
    String slug = routingContext.request().getHeader("Slug");

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.INSTANTIATE_TEMPLATE)
      .setPayload(body)
      .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIri)
      .setHeader(EventBusMessage.Headers.ENTITY_IRI_HINT, slug);

    vertx.eventBus().send(EventBusRegistry.TEMPLATE_HANDLER_BUS_ADDRESS,
      message.toJson(), handleReply(routingContext, HttpStatus.SC_OK));

  }

  public void handleTemplateExtended(RoutingContext routingContext) {
    String body = routingContext.getBodyAsString();
    String artifactId = routingContext.request().getParam("artid");
    String[] uriSplitted = routingContext.request().absoluteURI().split(artifactId);
    String entityIri = uriSplitted[0] + artifactId;
    String activity = uriSplitted[1];

    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.TEMPLATE_ACTIVITY)
      .setPayload(body)
      .setHeader(EventBusMessage.Headers.ENTITY_IRI, entityIri)
      .setHeader(EventBusMessage.Headers.ENTITY_ACTIVITY, activity);

    vertx.eventBus().send(EventBusRegistry.TEMPLATE_HANDLER_BUS_ADDRESS,
      message.toJson(), handleReply(routingContext, HttpStatus.SC_OK));

  }
}
