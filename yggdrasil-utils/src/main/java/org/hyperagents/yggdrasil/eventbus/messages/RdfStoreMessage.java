package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.Optional;

public sealed interface RdfStoreMessage {

  record GetEntity(String requestUri) implements RdfStoreMessage {}

  record UpdateEntity(String requestUri, String entityRepresentation) implements RdfStoreMessage {}

  record DeleteEntity(String requestUri) implements RdfStoreMessage {}

  record CreateArtifact(
      String requestUri,
      String artifactName,
      String artifactRepresentation
  ) implements RdfStoreMessage {}

  record CreateWorkspace(
      String requestUri,
      String workspaceName,
      Optional<String> parentWorkspaceUri,
      String workspaceRepresentation
  ) implements RdfStoreMessage {}

  record Query(String query) implements RdfStoreMessage {}
}
