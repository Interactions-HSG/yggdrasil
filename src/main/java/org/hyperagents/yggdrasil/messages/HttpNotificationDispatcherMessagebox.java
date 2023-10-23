package org.hyperagents.yggdrasil.messages;

import org.apache.commons.rdf.api.IRI;

public interface HttpNotificationDispatcherMessagebox {

  void pushNotification(MessageNotifications notificationType, IRI requestIRI, String entityGraph);

  void pushNotification(MessageNotifications notificationType, String requestIRI, String message);
}
