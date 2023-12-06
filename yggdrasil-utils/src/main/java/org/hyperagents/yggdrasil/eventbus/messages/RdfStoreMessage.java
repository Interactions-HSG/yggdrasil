package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.ArrayList;
import java.util.List;
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

  record QueryKnowledgeGraph(
      String query,
      List<String> defaultGraphUris,
      List<String> namedGraphUris,
      String responseContentType
  ) implements RdfStoreMessage {
    public QueryKnowledgeGraph(
        final String query,
        final List<String> defaultGraphUris,
        final List<String> namedGraphUris,
        final String responseContentType
    ) {
      this.query = query;
      this.defaultGraphUris = new ArrayList<>(defaultGraphUris);
      this.namedGraphUris = new ArrayList<>(namedGraphUris);
      this.responseContentType = responseContentType;
    }

    @Override
    public List<String> defaultGraphUris() {
      return new ArrayList<>(this.defaultGraphUris);
    }

    @Override
    public List<String> namedGraphUris() {
      return new ArrayList<>(this.namedGraphUris);
    }
  }
}
