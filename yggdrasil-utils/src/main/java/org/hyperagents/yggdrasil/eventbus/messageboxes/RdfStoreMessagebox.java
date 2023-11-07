package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;
import org.hyperagents.yggdrasil.eventbus.codecs.GenericMessageCodec;
import org.hyperagents.yggdrasil.eventbus.codecs.RdfStoreMessageMarshaller;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;

public class RdfStoreMessagebox implements Messagebox<RdfStoreMessage> {
  private final EventBus eventBus;

  public RdfStoreMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void init() {
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.GetEntity.class,
        new GenericMessageCodec<>(RdfStoreMessage.GetEntity.class, new RdfStoreMessageMarshaller())
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.CreateEntity.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.CreateEntity.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.DeleteEntity.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.DeleteEntity.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.UpdateEntity.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.UpdateEntity.class,
          new RdfStoreMessageMarshaller()
        )
    );
  }

  @Override
  public Future<Message<String>> sendMessage(final RdfStoreMessage message) {
    final var promise = Promise.<Message<String>>promise();
    this.eventBus.request(MessageAddresses.RDF_STORE.getName(), message, promise);
    return promise.future();
  }

  @Override
  public void receiveMessages(final Consumer<Message<RdfStoreMessage>> messageHandler) {
    this.eventBus.consumer(MessageAddresses.RDF_STORE.getName(), messageHandler::accept);
  }
}
