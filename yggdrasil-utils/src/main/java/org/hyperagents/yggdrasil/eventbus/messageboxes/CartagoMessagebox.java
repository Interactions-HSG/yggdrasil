package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;
import org.hyperagents.yggdrasil.eventbus.codecs.CartagoMessageMarshaller;
import org.hyperagents.yggdrasil.eventbus.codecs.GenericMessageCodec;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;

public class CartagoMessagebox implements Messagebox<CartagoMessage> {
  private final EventBus eventBus;

  public CartagoMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void init() {
    this.eventBus.registerDefaultCodec(
        CartagoMessage.CreateWorkspace.class,
        new GenericMessageCodec<>(
          CartagoMessage.CreateWorkspace.class,
          new CartagoMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        CartagoMessage.CreateSubWorkspace.class,
        new GenericMessageCodec<>(
          CartagoMessage.CreateSubWorkspace.class,
          new CartagoMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        CartagoMessage.JoinWorkspace.class,
        new GenericMessageCodec<>(
          CartagoMessage.JoinWorkspace.class,
          new CartagoMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        CartagoMessage.LeaveWorkspace.class,
        new GenericMessageCodec<>(
          CartagoMessage.LeaveWorkspace.class,
          new CartagoMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        CartagoMessage.Focus.class,
        new GenericMessageCodec<>(
          CartagoMessage.Focus.class,
          new CartagoMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        CartagoMessage.CreateArtifact.class,
        new GenericMessageCodec<>(
          CartagoMessage.CreateArtifact.class,
          new CartagoMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        CartagoMessage.DoAction.class,
        new GenericMessageCodec<>(
          CartagoMessage.DoAction.class,
          new CartagoMessageMarshaller()
        )
    );
  }

  @Override
  public Future<Message<String>> sendMessage(final CartagoMessage message) {
    final var promise = Promise.<Message<String>>promise();
    this.eventBus.request(MessageAddresses.CARTAGO.getName(), message, promise);
    return promise.future();
  }

  @Override
  public void receiveMessages(final Consumer<Message<CartagoMessage>> messageHandler) {
    this.eventBus.consumer(MessageAddresses.CARTAGO.getName(), messageHandler::accept);
  }
}
