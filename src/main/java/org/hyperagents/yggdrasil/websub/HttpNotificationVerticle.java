package org.hyperagents.yggdrasil.websub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class HttpNotificationVerticle extends AbstractVerticle {
  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.dispatcher";

  public static final String ENTITY_CREATED = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".entityCreated";
  public static final String ENTITY_CHANGED = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".entityChanged";
  public static final String ENTITY_DELETED = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".entityDeleted";
  public static final String ARTIFACT_OBS_PROP = "org.hyperagents.yggdrasil.eventbus.notifications"
      + ".artifactObsProp";

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

          WebClient client = WebClient.create(vertx);

          List<String> linkHeaders = new ArrayList<String>();
          linkHeaders.add("<" + webSubHubIRI + ">; rel=\"hub\"");
          linkHeaders.add("<" + entityIRI + ">; rel=\"self\"");

          Set<String> callbacks = NotificationSubscriberRegistry.getInstance().getCallbackIRIs(entityIRI);
          System.out.println("callbacks: "+ callbacks);
          for (String callbackIRI : callbacks) {
            HttpRequest<Buffer> request = client.postAbs(callbackIRI)
                .putHeader("Link", linkHeaders.get(0))
                .putHeader("Link", linkHeaders.get(1));

            if (message.headers().get(HttpEntityHandler.REQUEST_METHOD).equals(ENTITY_DELETED)) {
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: entity deleted");
              request.send(reponseHandler(callbackIRI));
            }
              else if (message.headers().get(HttpEntityHandler.REQUEST_METHOD).equals(ARTIFACT_OBS_PROP)){
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: " + changes);
              request.putHeader(HttpHeaders.CONTENT_LENGTH, "" + changes.length())
                .sendBuffer(Buffer.buffer(changes), reponseHandler(callbackIRI));
            } else if (changes != null && !changes.isEmpty()) {
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: " + changes);
              request.putHeader(HttpHeaders.CONTENT_LENGTH, "" + changes.length())
                .sendBuffer(Buffer.buffer(changes), reponseHandler(callbackIRI));
            }
          }
        }
      }
    });
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> reponseHandler(String callbackIRI) {
    return ar -> {
      HttpResponse<Buffer> response = ar.result();

      if (response == null) {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", operation failed: "
            + ar.cause().getMessage());
      } else if (response.statusCode() == HttpStatus.SC_OK) {
        LOGGER.info("Notification sent to: " + callbackIRI + ", status code: " + response.statusCode());
      } else {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", status code: "
            + response.statusCode());
      }
    };
  }

  private boolean isNotificationMessage(Message<Object> message) {
    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);

    if (requestMethod.equals(ENTITY_CREATED) || requestMethod.equals(ENTITY_CHANGED)
        || requestMethod.equals(ENTITY_DELETED) || requestMethod.equals(ARTIFACT_OBS_PROP)) {
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
