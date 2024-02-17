package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;

/**
 * Represents a message box that can send and receive messages.
 *
 * @param <M> the type of messages that can be sent and received
 */
public interface Messagebox<M> {

  /**
   * Initializes the message box.
   */
  void init();

  /**
   * Sends a message to the message box and returns a future 
   * representing the result of the send operation.
   *
   * @param message the message to send
   * @return a future representing the result of the send operation
   */
  Future<Message<String>> sendMessage(M message);

  /**
   * Registers a message handler to receive messages from the message box.
   *
   * @param messageHandler the message handler to register
   */
  void receiveMessages(Consumer<Message<M>> messageHandler);
}
