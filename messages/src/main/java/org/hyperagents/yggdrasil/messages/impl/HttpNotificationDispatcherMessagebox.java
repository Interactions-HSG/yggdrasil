package org.hyperagents.yggdrasil.messages.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.apache.commons.rdf.api.IRI;
import org.hyperagents.yggdrasil.messages.*;

public class HttpNotificationDispatcherMessagebox implements Messagebox<HttpNotificationDispatcherMessage> {
  private final EventBus eventBus;

  public HttpNotificationDispatcherMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Future<Message<String>> sendMessage(final HttpNotificationDispatcherMessage message) {
    final var promise = Promise.<Message<String>>promise();
    switch (message) {
      case HttpNotificationDispatcherMessage.EntityCreated(IRI requestIri, String entityGraph) ->
        this.pushNotification(MessageNotifications.ENTITY_CREATED, requestIri.getIRIString(), entityGraph);
      case HttpNotificationDispatcherMessage.EntityChanged(IRI requestIri, String entityGraph) ->
        this.pushNotification(MessageNotifications.ENTITY_CHANGED, requestIri.getIRIString(), entityGraph);
      case HttpNotificationDispatcherMessage.EntityDeleted(IRI requestIri, String entityGraph) ->
        this.pushNotification(MessageNotifications.ENTITY_DELETED, requestIri.getIRIString(), entityGraph);
      case HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(String requestIri, String entityGraph) ->
        this.pushNotification(MessageNotifications.ARTIFACT_OBS_PROP, requestIri, entityGraph);
    }
    promise.complete();
    return promise.future();
  }

  private void pushNotification(final MessageNotifications notificationType, final String requestIRI, final String message) {
    this.eventBus.send(
      MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(),
      message,
      new DeliveryOptions()
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), notificationType.getName())
        .addHeader(MessageHeaders.REQUEST_URI.getName(), requestIRI)
    );
  }
}
