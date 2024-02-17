package org.hyperagents.yggdrasil.eventbus.messages;

/**
 * A sealed interface representing a message for the HTTP Notification Dispatcher.
 *
 * <p>This interface is used to define different types of messages
 * that can be sent to the HTTP Notification Dispatcher.
 * Each record implementing this interface represents a specific type of message.
 *
 * <p>The requestIri method is common to all messages and
 * returns the IRI of the request associated with the message.
 */
public sealed interface HttpNotificationDispatcherMessage {

  String requestIri();

  /**
   * A record representing a message that an entity has been created.
   *
   * <p>This record is used when an entity has been created
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the creation of the entity.
   * The content is a string representation of the created entity.
   *
   * @param requestIri The IRI of the request that resulted in the creation of the entity.
   * @param content The string representation of the created entity.
   */
  record EntityCreated(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that an entity has been changed.
   *
   * <p>This record is used when an entity has been changed
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the change of the entity.
   * The content is a string representation of the changed entity.
   *
   * @param requestIri The IRI of the request that resulted in the change of the entity.
   * @param content The string representation of the changed entity.
   */
  record EntityChanged(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that an entity has been deleted.
   *
   * <p>This record is used when an entity has been deleted
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the deletion of the entity.
   * The content is a string representation of the deleted entity.
   *
   * @param requestIri The IRI of the request that resulted in the deletion of the entity.
   * @param content The string representation of the deleted entity.
   */
  record EntityDeleted(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that an artifact's observable property has been updated.
   *
   * <p>This record is used when an observable property of an artifact has been updated
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the update
   * of the artifact's observable property.
   * The content is a string representation of the updated observable property.
   *
   * @param requestIri The request IRI that resulted in the update of the observable property.
   * @param content The string representation of the updated observable property.
   */
  record ArtifactObsPropertyUpdated(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that an action has been requested.
   *
   * <p>This record is used when an action has been requested
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the action request.
   * The content is a string representation of the requested action.
   *
   * @param requestIri The IRI of the request that resulted in the action request.
   * @param content The string representation of the requested action.
   */
  record ActionRequested(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that an action has succeeded.
   *
   * <p>This record is used when an action has succeeded
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the successful action.
   * The content is a string representation of the successful action.
   *
   * @param requestIri The IRI of the request that resulted in the successful action.
   * @param content The string representation of the successful action.
   */
  record ActionSucceeded(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that an action has failed.
   *
   * <p>This record is used when an action has failed
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the failed action.
   * The content is a string representation of the failed action.
   *
   * @param requestIri The IRI of the request that resulted in the failed action.
   * @param content The string representation of the failed action.
   */
  record ActionFailed(String requestIri, String content)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that a callback has been added.
   *
   * <p>This record is used when a callback has been added
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the addition of the callback.
   * The callbackIri is the IRI of the added callback.
   *
   * @param requestIri The IRI of the request that resulted in the addition of the callback.
   * @param callbackIri The IRI of the added callback.
   */
  record AddCallback(String requestIri, String callbackIri)
      implements HttpNotificationDispatcherMessage {}

  /**
   * A record representing a message that a callback has been removed.
   *
   * <p>This record is used when a callback has been removed
   * and a message needs to be sent to the HTTP Notification Dispatcher.
   * The requestIri is the IRI of the request that resulted in the removal of the callback.
   * The callbackIri is the IRI of the removed callback.
   *
   * @param requestIri The IRI of the request that resulted in the removal of the callback.
   * @param callbackIri The IRI of the removed callback.
   */
  record RemoveCallback(String requestIri, String callbackIri)
      implements HttpNotificationDispatcherMessage {}
}
