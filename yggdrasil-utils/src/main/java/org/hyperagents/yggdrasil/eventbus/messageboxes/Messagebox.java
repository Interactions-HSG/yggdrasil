package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;

/**
 * TODO: Javadoc.
 */
public interface Messagebox<M> {

  /**
   * TODO: Javadoc.
   */
  void init();

  /**
   * TODO: Javadoc.
   */
  Future<Message<String>> sendMessage(M message);

  /**
   * TODO: Javadoc.
   */
  void receiveMessages(Consumer<Message<M>> messageHandler);
}
