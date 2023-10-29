package org.hyperagents.yggdrasil.messages;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.Optional;

public interface RdfStoreMessagebox {

  void sendGetEntityRequest(
    String requestUri,
    Optional<String> contentType,
    Handler<AsyncResult<Message<String>>> resultHandler
  );

  void sendUpdateEntityRequest(
    String requestUri,
    String entityRepresentation,
    Handler<AsyncResult<Message<String>>> resultHandler
  );

  void sendDeleteEntityRequest(
    String requestUri,
    Handler<AsyncResult<Message<String>>> resultHandler
  );

  void sendCreateEntityRequest(
    String requestUri,
    String entityName,
    String entityRepresentation,
    Handler<AsyncResult<Message<String>>> resultHandler
  );
}
