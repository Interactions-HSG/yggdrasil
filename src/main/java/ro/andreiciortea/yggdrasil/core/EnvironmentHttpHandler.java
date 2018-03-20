package ro.andreiciortea.yggdrasil.core;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

public class EnvironmentHttpHandler {
  
  public void handleGetEnvironment(String environmentIRI, Handler<AsyncResult<Message<String>>> handler) {
    // TODO
  }
  
  public void handleCreateEnvironment(String environmentIRI, Handler<AsyncResult<Message<String>>> handler) {
    // TODO
  }
  
  public void handleDeleteEnvironment(String environmentIRI, Handler<AsyncResult<Message<String>>> handler) {
    // TODO
  }
}
