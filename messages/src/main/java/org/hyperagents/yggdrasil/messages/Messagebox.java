package org.hyperagents.yggdrasil.messages;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;

public interface Messagebox<M> {

  Future<Message<String>> sendMessage(M message);
}
