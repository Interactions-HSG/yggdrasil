package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;
import org.hyperagents.yggdrasil.eventbus.codecs.GenericMessageCodec;
import org.hyperagents.yggdrasil.eventbus.codecs.HttpNotificationDispatcherMessageMarshaller;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;

public class HttpNotificationDispatcherMessagebox
    implements Messagebox<HttpNotificationDispatcherMessage> {
  private final EventBus eventBus;

  public HttpNotificationDispatcherMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void init() {
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
  }

  @Override
  public Future<Message<String>> sendMessage(final HttpNotificationDispatcherMessage message) {
    final var promise = Promise.<Message<String>>promise();
    this.eventBus.send(MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(), message);
    promise.complete();
    return promise.future();
  }

  @Override
  public void receiveMessages(
      final Consumer<Message<HttpNotificationDispatcherMessage>> messageHandler
  ) {
    this.eventBus.consumer(
        MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(),
        messageHandler::accept
    );
  }
}
