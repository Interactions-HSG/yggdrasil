package ro.andreiciortea.yggdrasil.http;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;


public class HttpTemplateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());
  private Vertx vertx;

  private String webSubHubIRI = null;

  public HttpTemplateHandler() {
    vertx = Vertx.currentContext().owner();
    }

  // TODO: add payload validation

  public void handleGetTemplates(RoutingContext routingContext) {
    EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_TEMPLATES);

    vertx.eventBus().send(EventBusRegistry.TEMPLATE_HANDLER_BUS_ADDRESS,
      message.toJson(), handleReply(routingContext, HttpStatus.SC_OK));
  }

  private Handler<AsyncResult<Message<String>>> handleReply(RoutingContext routingContext,
                                                            int succeededStatusCode) {

    return reply -> {
      // TODO: implement
      System.out.println("Handle templates reply");
    };
  }
}
