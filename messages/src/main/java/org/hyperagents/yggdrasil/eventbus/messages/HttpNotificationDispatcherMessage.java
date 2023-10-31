package org.hyperagents.yggdrasil.eventbus.messages;

public sealed interface HttpNotificationDispatcherMessage {

  String requestIRI();

  String content();

  record EntityCreated(String requestIRI, String content) implements HttpNotificationDispatcherMessage {}

  record EntityChanged(String requestIRI, String content) implements HttpNotificationDispatcherMessage {}

  record EntityDeleted(String requestIRI, String content) implements HttpNotificationDispatcherMessage {}

  record ArtifactObsPropertyUpdated(String requestIRI, String content) implements HttpNotificationDispatcherMessage {}
}
