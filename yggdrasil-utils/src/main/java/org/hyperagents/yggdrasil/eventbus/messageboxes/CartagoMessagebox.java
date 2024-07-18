package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.eventbus.codecs.CartagoMessageMarshaller;
import org.hyperagents.yggdrasil.eventbus.codecs.GenericMessageCodec;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;

/**
 * Represents a message box for Cartago messages.
 */
public class CartagoMessagebox implements Messagebox<CartagoMessage> {
  private static final Logger LOGGER = LogManager.getLogger(CartagoMessagebox.class);

  private final EventBus eventBus;
  private final EnvironmentConfig config;

  public CartagoMessagebox(final EventBus eventBus, final EnvironmentConfig config) {
    this.eventBus = eventBus;
    this.config = config;
  }

  @Override
  public void init() {
    if (this.config.isEnabled()) {
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
      this.eventBus.registerDefaultCodec(
        CartagoMessage.DeleteEntity.class,
        new GenericMessageCodec<>(
          CartagoMessage.DeleteEntity.class,
          new CartagoMessageMarshaller()
        )
      );
    } else {
      LOGGER.warn("Dynamic environments are not enabled, message exchange will not be initialized");
    }
  }

  @Override
  public Future<Message<String>> sendMessage(final CartagoMessage message) {
    if (this.config.isEnabled()) {
      return this.eventBus.request(MessageAddresses.CARTAGO.getName(), message);
    } else {
      LOGGER.warn("Dynamic environments are not enabled, this message will be a dead letter");
      final var promise = Promise.<Message<String>>promise();
      promise.complete();
      return promise.future();
    }
  }

  @Override
  public void receiveMessages(final Consumer<Message<CartagoMessage>> messageHandler) {
    if (this.config.isEnabled()) {
      this.eventBus.consumer(MessageAddresses.CARTAGO.getName(), messageHandler::accept);
    } else {
      LOGGER.warn("Dynamic environments are not enabled, no message will be received");
    }
  }
}
