package ro.andreiciortea.yggdrasil.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.http.HttpStatus;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.core.SubscriberRegistry;

public class HttpNotificationDispatcherVerticle extends AbstractVerticle {
  
  private final static Logger LOGGER = LoggerFactory.getLogger(HttpNotificationDispatcherVerticle.class.getName());
  private String webSubHubIRI = null;
  
  @Override
  public void start() {
    webSubHubIRI = getWebSubHubIRI(config());
    
    vertx.eventBus().consumer(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, message -> {
      EventBusMessage notification = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);
      
      if (isNotificationMessage(notification)) {
        Optional<String> entityIRI = notification.getHeader(EventBusMessage.Headers.REQUEST_IRI);
        
        if (entityIRI.isPresent()) {
          LOGGER.info("Dispatching notifications for: " + entityIRI.get() + ", changes: " + notification.getPayload());
          
          HttpClient client = vertx.createHttpClient();
          
          List<String> linkHeaders = new ArrayList<String>();
          linkHeaders.add("<" + webSubHubIRI + ">; rel=\"hub\"");
          linkHeaders.add("<" + entityIRI.get() + ">; rel=\"self\"");
          
          Optional<String> changes = notification.getPayload();
          Set<String> callbacks = SubscriberRegistry.getInstance().getCallbackIRIs(entityIRI.get());
          
          for (String callbackIRI : callbacks) {
            HttpClientRequest httpRequest = client.postAbs(callbackIRI, reponseHandler(callbackIRI))
                                                    .putHeader("Link", linkHeaders);
            
            if (notification.getMessageType() == EventBusMessage.MessageType.ENTITY_DELETED_NOTIFICATION) {
              httpRequest.end();
            } else if (changes.isPresent()) {
              httpRequest
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + changes.get().length())
                .write(Buffer.factory.buffer(changes.get())).end();
            }
          }
        }
      }
    });
  }
  
  private Handler<HttpClientResponse> reponseHandler(String callbackIRI) {
    return response -> {
      if (response.statusCode() == HttpStatus.SC_OK) {
        LOGGER.info("Notification sent to: " + callbackIRI + 
            ", status code: " + response.statusCode());
      } else {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", status code: " + response.statusCode());
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
  
  private String getWebSubHubIRI(JsonObject config) {
    JsonObject httpConfig = config.getJsonObject("http-config");
    
    if (httpConfig != null && httpConfig.getString("websub-hub") != null) {
      return httpConfig.getString("websub-hub");
    }
    
    return null;
  }
}
