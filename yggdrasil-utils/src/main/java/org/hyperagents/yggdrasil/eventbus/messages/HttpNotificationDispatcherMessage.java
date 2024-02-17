package org.hyperagents.yggdrasil.eventbus.messages;

/**
 * TODO: Javadoc.
 */
public sealed interface HttpNotificationDispatcherMessage {

  String requestIri();

  /**
   * TODO: Javadoc.
   */
  record EntityCreated(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record EntityChanged(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record EntityDeleted(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record ArtifactObsPropertyUpdated(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record ActionRequested(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record ActionSucceeded(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record ActionFailed(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record AddCallback(String requestIri, String callbackIri)
      implements HttpNotificationDispatcherMessage {}

  /**
   * TODO: Javadoc.
   */
  record RemoveCallback(String requestIri, String callbackIri)
      implements HttpNotificationDispatcherMessage {}
}
