package org.hyperagents.yggdrasil.messages;

import org.apache.commons.rdf.api.IRI;

public sealed interface HttpNotificationDispatcherMessage {

  record EntityCreated(IRI requestIRI, String entityGraph) implements HttpNotificationDispatcherMessage {}

  record EntityChanged(IRI requestIRI, String entityGraph) implements HttpNotificationDispatcherMessage {}

  record EntityDeleted(IRI requestIRI, String entityGraph) implements HttpNotificationDispatcherMessage {}

  record ArtifactObsPropertyUpdated(String requestIri, String entityGraph) implements HttpNotificationDispatcherMessage {}
}
