package org.hyperagents.yggdrasil.messages.impl;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import org.apache.commons.rdf.api.IRI;
import org.hyperagents.yggdrasil.messages.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.messages.MessageAddresses;
import org.hyperagents.yggdrasil.messages.MessageHeaders;
import org.hyperagents.yggdrasil.messages.MessageNotifications;

public class HttpNotificationDispatcherMessageboxImpl implements HttpNotificationDispatcherMessagebox {
  private final EventBus eventBus;

  public HttpNotificationDispatcherMessageboxImpl(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void pushNotification(final MessageNotifications notificationType, final IRI requestIRI, final String entityGraph) {
    this.eventBus.send(
      MessageAddresses.HTTP_NOTIFICATION_DISPATCHER.getName(),
      entityGraph,
      new DeliveryOptions()
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), notificationType.getName())
        .addHeader(MessageHeaders.REQUEST_URI.getName(), requestIRI.getIRIString())
    );
  }
}
