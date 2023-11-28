package org.hyperagents.yggdrasil.websub;

import com.google.common.net.HttpHeaders;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

public class HttpNotificationVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(HttpNotificationVerticle.class);

  @Override
  public void start() {
    final var webSubHubUri = new HttpInterfaceConfigImpl(this.context.config()).getWebSubHubUri();

    final var ownMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      if (webSubHubUri.isPresent() && !message.body().requestIri().isEmpty()) {
        final var entityIri = message.body().requestIri();
        final var changes = message.body().content();
        final var client = WebClient.create(this.vertx);

        NotificationSubscriberRegistry.getInstance().getCallbackIris(entityIri).forEach(
            callbackIRI -> {
              final var request =
                  client
                    .postAbs(callbackIRI)
                    .putHeader("Link", "<" + webSubHubUri.get() + ">; rel=\"hub\"")
                    .putHeader("Link", "<" + entityIri + ">; rel=\"self\"");

              if (message.body() instanceof HttpNotificationDispatcherMessage.EntityDeleted) {
                request.send(this.reponseHandler(callbackIRI));
              } else if (
                  message.body()
                    instanceof HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated
              ) {
                request
                    .putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(changes.length()))
                    .sendBuffer(
                      Buffer.buffer(
                        Pattern.compile(
                          "https?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]"
                        )
                        .matcher(changes)
                        .replaceAll(r ->
                          changes.charAt(r.start() - 1) == '"' && changes.charAt(r.end()) == '"'
                          ? r.group()
                          : "\"" + r.group() + "\""
                        )
                      ),
                      this.reponseHandler(callbackIRI)
                    );
              } else if (!changes.isEmpty()) {
                request
                  .putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(changes.length()))
                  .sendBuffer(Buffer.buffer(changes), this.reponseHandler(callbackIRI));
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
