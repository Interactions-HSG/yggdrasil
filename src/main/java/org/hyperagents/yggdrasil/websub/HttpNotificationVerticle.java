package org.hyperagents.yggdrasil.websub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class HttpNotificationVerticle extends AbstractVerticle {
  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.dispatcher";
  
  public static final String ENTITY_CREATED_NOTIFICATION = "org.hyperagents.yggdrasil.eventbus"
      + ".notifications.entityCreated";
  public static final String ENTITY_CHANGED_NOTIFICATION = "org.hyperagents.yggdrasil.eventbus"
      + ".notifications.entityChanged";
  public static final String ENTITY_DELETED_NOTIFICATION = "org.hyperagents.yggdrasil.eventbus"
      + ".notifications.entityDeleted";
  
  private final static Logger LOGGER = LoggerFactory.getLogger(
      HttpNotificationVerticle.class.getName());
  
  private String webSubHubIRI = null;
  
  @Override
  public void start() {
    webSubHubIRI = getWebSubHubIRI(config());
    
    vertx.eventBus().consumer(BUS_ADDRESS, message -> {
      if (isNotificationMessage(message)) {
        String entityIRI = message.headers().get(HttpEntityHandler.REQUEST_URI);
        
        if (entityIRI != null && !entityIRI.isEmpty()) {
          String changes = (String) message.body();
          LOGGER.info("Dispatching notifications for: " + entityIRI + ", changes: " + changes);
          
          HttpClient client = vertx.createHttpClient();
          
          List<String> linkHeaders = new ArrayList<String>();
          linkHeaders.add("<" + webSubHubIRI + ">; rel=\"hub\"");
          linkHeaders.add("<" + entityIRI + ">; rel=\"self\"");
          
          Set<String> callbacks = NotificationSubscriberRegistry.getInstance().getCallbackIRIs(entityIRI);
          
          for (String callbackIRI : callbacks) {
            HttpClientRequest httpRequest = client.postAbs(callbackIRI, reponseHandler(callbackIRI))
                                                    .putHeader("Link", linkHeaders);
            if (message.headers().get(HttpEntityHandler.REQUEST_METHOD)
                .compareTo(ENTITY_DELETED_NOTIFICATION) == 0) {
              httpRequest.end();
            } else if (changes != null && !changes.isEmpty()) {
              httpRequest
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + changes.length())
                .write(Buffer.factory.buffer(changes)).end();
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
  
  private boolean isNotificationMessage(Message<Object> message) {
    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    
    if (requestMethod.compareTo(ENTITY_CREATED_NOTIFICATION) == 0
        || requestMethod.compareTo(ENTITY_CHANGED_NOTIFICATION) == 0
        || requestMethod.compareTo(ENTITY_DELETED_NOTIFICATION) == 0) {
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
