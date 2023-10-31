package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;

import java.util.function.Consumer;

public interface Messagebox<M> {

  void init();

  Future<Message<String>> sendMessage(M message);

  void receiveMessages(Consumer<Message<M>> messageHandler);
}
