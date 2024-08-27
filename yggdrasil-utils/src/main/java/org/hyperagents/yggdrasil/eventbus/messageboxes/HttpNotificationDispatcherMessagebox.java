package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.eventbus.codecs.GenericMessageCodec;
import org.hyperagents.yggdrasil.eventbus.codecs.HttpNotificationDispatcherMessageMarshaller;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * This class represents a messagebox for handling HTTP notification dispatcher messages.
 * It implements the Messagebox interface for HttpNotificationDispatcherMessage.
 */
public class HttpNotificationDispatcherMessagebox
    implements Messagebox<HttpNotificationDispatcherMessage> {
  private static final Logger LOGGER =
      LogManager.getLogger(HttpNotificationDispatcherMessagebox.class);

  private final EventBus eventBus;
  private final WebSubConfig config;

  public HttpNotificationDispatcherMessagebox(final EventBus eventBus, final WebSubConfig config) {
    this.eventBus = eventBus;
    this.config = config;
  }

  @Override
  public void init() {
    if (this.config.isEnabled()) {
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.EntityChanged.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.EntityChanged.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.EntityCreated.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.EntityCreated.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.EntityDeleted.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.EntityDeleted.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.ActionRequested.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.ActionRequested.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.ActionSucceeded.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.ActionSucceeded.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.ActionFailed.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.ActionFailed.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.AddCallback.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.AddCallback.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
      this.eventBus.registerDefaultCodec(
          HttpNotificationDispatcherMessage.RemoveCallback.class,
          new GenericMessageCodec<>(
            HttpNotificationDispatcherMessage.RemoveCallback.class,
            new HttpNotificationDispatcherMessageMarshaller()
          )
      );
    } else {
      LOGGER.warn("Notifications are not enabled, message exchange will not be initialized");
    }
  }

  @Override
  public Future<Message<String>> sendMessage(final HttpNotificationDispatcherMessage message) {
    if (this.config.isEnabled()) {
      this.eventBus.send(MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(), message);
    } else {
      LOGGER.warn("Notifications are not enabled, this message will be a dead letter");
    }
    final var promise = Promise.<Message<String>>promise();
    promise.complete();
    return promise.future();
  }

  @Override
  public void receiveMessages(
      final Consumer<Message<HttpNotificationDispatcherMessage>> messageHandler
  ) {
    if (this.config.isEnabled()) {
      this.eventBus.consumer(
          MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(),
          messageHandler::accept
      );
    } else {
      LOGGER.warn("Notifications are not enabled, no message will be received");
    }
  }
}
