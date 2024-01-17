package org.hyperagents.yggdrasil.eventbus.messages;

public sealed interface HttpNotificationDispatcherMessage {

  String requestIri();

  String content();

  record EntityCreated(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  record EntityChanged(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  record EntityDeleted(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  record ArtifactObsPropertyUpdated(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  record ActionRequested(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  record ActionSucceeded(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  record ActionFailed(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}
}
