package org.hyperagents.yggdrasil.messages.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.hyperagents.yggdrasil.messages.*;

import java.util.Optional;

public class RdfStoreMessagebox implements Messagebox<RdfStoreMessage> {
  private final EventBus eventBus;

  public RdfStoreMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Future<Message<String>> sendMessage(final RdfStoreMessage message) {
    final var promise = Promise.<Message<String>>promise();
    switch (message) {
      case RdfStoreMessage.GetEntity(String requestUri, Optional<String> contentType) -> {
        final var options =
          new DeliveryOptions()
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.GET_ENTITY.getName())
            .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri);
        contentType.ifPresent(c -> options.addHeader(MessageHeaders.CONTENT_TYPE.getName(), c));
        this.eventBus.request(
          MessageAddresses.RDF_STORE.getName(),
          null,
          options,
          promise
        );
      }
      case RdfStoreMessage.UpdateEntity(String requestUri, String entityRepresentation) ->
        this.eventBus.request(
          MessageAddresses.RDF_STORE.getName(),
          entityRepresentation,
          new DeliveryOptions()
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.UPDATE_ENTITY.getName())
            .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri),
          promise
        );
      case RdfStoreMessage.DeleteEntity(String requestUri) ->
        this.eventBus.request(
          MessageAddresses.RDF_STORE.getName(),
          null,
          new DeliveryOptions()
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.DELETE_ENTITY.getName())
            .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri),
          promise
        );
      case RdfStoreMessage.CreateEntity(String requestUri, String entityName, String entityRepresentation) ->
        this.eventBus.request(
          MessageAddresses.RDF_STORE.getName(),
          entityRepresentation,
          new DeliveryOptions()
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_ENTITY.getName())
            .addHeader(MessageHeaders.REQUEST_URI.getName(), requestUri)
            .addHeader(MessageHeaders.ENTITY_URI_HINT.getName(), entityName),
          promise
        );
    }
    return promise.future();
  }
}
