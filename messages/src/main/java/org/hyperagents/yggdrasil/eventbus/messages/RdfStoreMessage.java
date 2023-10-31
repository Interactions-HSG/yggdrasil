package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.Optional;

public sealed interface RdfStoreMessage {

  String requestUri();

  record GetEntity(String requestUri, Optional<String> contentType) implements RdfStoreMessage {}

  record UpdateEntity(String requestUri, String entityRepresentation) implements RdfStoreMessage {}

  record DeleteEntity(String requestUri) implements RdfStoreMessage {}

  record CreateEntity(String requestUri, String entityName, String entityRepresentation) implements RdfStoreMessage {}
}
