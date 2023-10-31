package org.hyperagents.yggdrasil.websub;

import com.google.common.net.HttpHeaders;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

public class HttpNotificationVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(HttpNotificationVerticle.class.getName());

  @Override
  public void start() {
    final var webSubHubUri = new HttpInterfaceConfigImpl(this.context.config()).getWebSubHubUri();

    final var ownMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      if (webSubHubUri.isPresent()) {
        if (!message.body().requestIRI().isEmpty()) {
          final var entityIRI = message.body().requestIRI();
          LOGGER.info("Dispatching notifications for: " + entityIRI + ", changes: " + message.body().content());
          final var client = WebClient.create(this.vertx);

          NotificationSubscriberRegistry.getInstance().getCallbackIRIs(entityIRI).forEach(callbackIRI -> {
            final var request =
              client
                .postAbs(callbackIRI)
                .putHeader("Link", "<" + webSubHubUri.get() + ">; rel=\"hub\"")
                .putHeader("Link", "<" + entityIRI + ">; rel=\"self\"");

            if (message.body() instanceof HttpNotificationDispatcherMessage.EntityDeleted) {
              LOGGER.info("Sending notification to: " + callbackIRI + "; changes: entity deleted");
              request.send(reponseHandler(callbackIRI));
            } else if (!message.body().content().isEmpty()) {
              final var changes = message.body().content();
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
}
