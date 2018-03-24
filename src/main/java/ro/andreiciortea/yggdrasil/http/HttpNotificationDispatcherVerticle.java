package ro.andreiciortea.yggdrasil.http;

import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.core.SubscriberRegistry;

public class HttpNotificationDispatcherVerticle extends AbstractVerticle {
  
  private final static Logger LOGGER = LoggerFactory.getLogger(HttpNotificationDispatcherVerticle.class.getName());
  
  @Override
  public void start() {
    
    vertx.eventBus().consumer(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, message -> {
      EventBusMessage notification = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);
      
      if (isNotificationMessage(notification)) {
        Optional<String> entityIRI = notification.getHeader(EventBusMessage.Headers.REQUEST_IRI);
        
        if (entityIRI.isPresent()) {
          LOGGER.info("Dispatching notifications for: " + entityIRI.get() + ", changes: " + notification.getPayload());
          
          Optional<String> changes = notification.getPayload();
          
          WebClientOptions options = new WebClientOptions().setKeepAlive(false);
          WebClient client = WebClient.create(vertx, options);
          
          Set<String> callbacks = SubscriberRegistry.getInstance().getCallbackIRIs(entityIRI.get());
          
          for (String callbackIRI : callbacks) {
            if (changes.isPresent()) {
              client.post(callbackIRI)
                .sendBuffer(Buffer.factory.buffer(changes.get()), reponseHandler(callbackIRI));
            }
            else if (notification.getMessageType() == EventBusMessage.MessageType.ENTITY_DELETED_NOTIFICATION) {
              client.post(callbackIRI)
                .send(reponseHandler(callbackIRI));
            }
          }
        }
      }
    });
  }
  
  private Handler<AsyncResult<HttpResponse<Buffer>>> reponseHandler(String callbackIRI) {
    return response -> {
      if (response.succeeded()) {
        LOGGER.info("Notification sent to: " + callbackIRI + 
            ", status code: " + response.result().statusCode());
      } else {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", cause: " + response.cause().getMessage());
      }
    };
  }
  
  private boolean isNotificationMessage(EventBusMessage message) {
    if (message.getMessageType() == EventBusMessage.MessageType.ENTITY_CREATED_NOTIFICATION
        || message.getMessageType() == EventBusMessage.MessageType.ENTITY_CHANGED_NOTIFICATION
        || message.getMessageType() == EventBusMessage.MessageType.ENTITY_DELETED_NOTIFICATION) {
      return true;
    }
    
    return false;
  }
}
