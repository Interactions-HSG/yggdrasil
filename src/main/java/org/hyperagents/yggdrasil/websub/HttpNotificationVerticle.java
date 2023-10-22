package org.hyperagents.yggdrasil.websub;

import com.google.common.net.HttpHeaders;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.messages.MessageAddresses;
import org.hyperagents.yggdrasil.messages.MessageHeaders;
import org.hyperagents.yggdrasil.messages.MessageNotifications;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

import java.util.Optional;

public class HttpNotificationVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(HttpNotificationVerticle.class.getName());

  @Override
  public void start() {
    final var webSubHubUri = new HttpInterfaceConfigImpl(this.context.config()).getWebSubHubUri();

    this.vertx.eventBus().<String>consumer(MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(), message -> {
      if (this.isNotificationMessage(message) & webSubHubUri.isPresent()) {
        final var optEntityIRI = Optional.ofNullable(message.headers().get(MessageHeaders.REQUEST_URI.getName()));

        if (optEntityIRI.filter(s -> !s.isEmpty()).isPresent()) {
          final var entityIRI = optEntityIRI.get();
          final var optChanges = Optional.ofNullable(message.body());
          LOGGER.info("Dispatching notifications for: " + entityIRI + ", changes: " + optChanges.orElse(null));
          final var client = WebClient.create(this.vertx);

          NotificationSubscriberRegistry.getInstance().getCallbackIRIs(entityIRI).forEach(callbackIRI -> {
            final var request =
              client
                .postAbs(callbackIRI)
                .putHeader("Link", "<" + webSubHubUri.get() + ">; rel=\"hub\"")
                .putHeader("Link", "<" + entityIRI + ">; rel=\"self\"");

            if (
              message
                .headers()
                .get(MessageHeaders.REQUEST_METHOD.getName())
                .equals(MessageNotifications.ENTITY_DELETED.getName())
            ) {
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: entity deleted");
              request.send(reponseHandler(callbackIRI));
            } else if (optChanges.filter(s -> !s.isEmpty()).isPresent()) {
              final var changes = optChanges.get();
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: " + changes);

              request
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + changes.length())
                .sendBuffer(Buffer.buffer(changes), reponseHandler(callbackIRI));
            }
          });
        }
      }
    });
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> reponseHandler(final String callbackIRI) {
    return ar -> {
      final var response = ar.result();
      if (response == null) {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", operation failed: " + ar.cause().getMessage());
      } else if (response.statusCode() == HttpStatus.SC_OK) {
        LOGGER.info("Notification sent to: " + callbackIRI + ", status code: " + response.statusCode());
      } else {
        LOGGER.info("Failed to send notification to: " + callbackIRI + ", status code: " + response.statusCode());
      }
    };
  }

  private boolean isNotificationMessage(final Message<?> message) {
    final var requestMethod = message.headers().get(MessageHeaders.REQUEST_METHOD.getName());
    return requestMethod.equals(MessageNotifications.ENTITY_CREATED.getName())
      || requestMethod.equals(MessageNotifications.ENTITY_CHANGED.getName())
      || requestMethod.equals(MessageNotifications.ENTITY_DELETED.getName())
      || requestMethod.equals(MessageNotifications.ARTIFACT_OBS_PROP.getName());
  }
}
