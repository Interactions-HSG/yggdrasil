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
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

public class HttpNotificationVerticle extends AbstractVerticle {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(HttpNotificationVerticle.class.getName());

  @Override
  public void start() {
    final var webSubHubUri = new HttpInterfaceConfigImpl(this.context.config()).getWebSubHubUri();

    final var ownMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      if (webSubHubUri.isPresent() && !message.body().requestIri().isEmpty()) {
        final var entityIri = message.body().requestIri();
        LOGGER.info(
            "Dispatching notifications for: "
            + entityIri
            + ", changes: "
            + message.body().content()
        );
        final var client = WebClient.create(this.vertx);

        NotificationSubscriberRegistry.getInstance().getCallbackIris(entityIri).forEach(
            callbackIRI -> {
              final var request =
                  client
                    .postAbs(callbackIRI)
                    .putHeader("Link", "<" + webSubHubUri.get() + ">; rel=\"hub\"")
                    .putHeader("Link", "<" + entityIri + ">; rel=\"self\"");

              if (message.body() instanceof HttpNotificationDispatcherMessage.EntityDeleted) {
                LOGGER.info(
                    "Sending notification to: " + callbackIRI + "; changes: entity deleted"
                );
                request.send(reponseHandler(callbackIRI));
              } else if (!message.body().content().isEmpty()) {
                final var changes = message.body().content();
                LOGGER.info("Sending notification to: " + callbackIRI + "; changes: " + changes);

                request
                  .putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(changes.length()))
                  .sendBuffer(Buffer.buffer(changes), reponseHandler(callbackIRI));
              }
            }
        );
      }
    });
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> reponseHandler(final String callbackIri) {
    return ar -> {
      final var response = ar.result();
      if (response == null) {
        LOGGER.info(
            "Failed to send notification to: "
            + callbackIri
            + ", operation failed: "
            + ar.cause().getMessage()
        );
      } else if (response.statusCode() == HttpStatus.SC_OK) {
        LOGGER.info(
            "Notification sent to: "
            + callbackIri
            + ", status code: "
            + response.statusCode()
        );
      } else {
        LOGGER.info(
            "Failed to send notification to: "
            + callbackIri
            + ", status code: "
            + response.statusCode()
        );
      }
    };
  }
}
