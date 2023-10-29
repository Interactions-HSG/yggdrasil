package org.hyperagents.yggdrasil.messages.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.hyperagents.yggdrasil.messages.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.messages.MessageAddresses;
import org.hyperagents.yggdrasil.messages.MessageHeaders;
import org.hyperagents.yggdrasil.messages.MessageRequestMethods;

import java.util.Optional;

public class RdfStoreMessageboxImpl implements RdfStoreMessagebox {
  private final EventBus eventBus;

  public RdfStoreMessageboxImpl(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void sendGetEntityRequest(
    final String requestUri,
    final Optional<String> contentType,
    final Handler<AsyncResult<Message<String>>> resultHandler
  ) {
    final var options =
      new DeliveryOptions()
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.GET_ENTITY.getName())
        .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri);
    contentType.ifPresent(c -> options.addHeader(MessageHeaders.CONTENT_TYPE.getName(), c));
    this.eventBus.request(
      MessageAddresses.RDF_STORE.getName(),
      null,
      options,
      resultHandler
    );
  }

  @Override
  public void sendUpdateEntityRequest(
    final String requestUri,
    final String entityRepresentation,
    final Handler<AsyncResult<Message<String>>> resultHandler
  ) {
    this.eventBus.request(
      MessageAddresses.RDF_STORE.getName(),
      entityRepresentation,
      new DeliveryOptions()
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.UPDATE_ENTITY.getName())
        .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri),
      resultHandler);
  }

  @Override
  public void sendDeleteEntityRequest(
    final String requestUri,
    final Handler<AsyncResult<Message<String>>> resultHandler
  ) {
    this.eventBus.request(
      MessageAddresses.RDF_STORE.getName(),
      null,
      new DeliveryOptions()
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.DELETE_ENTITY.getName())
        .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri),
      resultHandler
    );
  }

  @Override
  public void sendCreateEntityRequest(
    final String requestUri,
    final String entityName,
    final String entityRepresentation,
    final Handler<AsyncResult<Message<String>>> resultHandler
  ) {
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_ENTITY.getName())
      .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri)
      .addHeader(MessageHeaders.ENTITY_URI_HINT.getName(), entityName);
    this.eventBus.request(
      MessageAddresses.RDF_STORE.getName(),
      entityRepresentation,
      options,
      resultHandler
    );
  }
}
