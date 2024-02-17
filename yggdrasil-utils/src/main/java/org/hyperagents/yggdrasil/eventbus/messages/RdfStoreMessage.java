package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TODO: Javadoc.
 */
public sealed interface RdfStoreMessage {

  /**
   * TODO: Javadoc.
   */
  record GetEntity(String requestUri) implements RdfStoreMessage {}

  /**
   * TODO: Javadoc.
   */
  record UpdateEntity(String requestUri, String entityRepresentation) implements RdfStoreMessage {}

  /**
   * TODO: Javadoc.
   */
  record DeleteEntity(String requestUri) implements RdfStoreMessage {}

  /**
   * TODO: Javadoc.
   */
  record CreateBody(
      String workspaceName,
      String agentName,
      String bodyRepresentation
  ) implements RdfStoreMessage {}

  /**
   * TODO: Javadoc.
   */
  record CreateArtifact(
      String requestUri,
      String artifactName,
      String artifactRepresentation
  ) implements RdfStoreMessage {}

  /**
   * TODO: Javadoc.
   */
  record CreateWorkspace(
      String requestUri,
      String workspaceName,
      Optional<String> parentWorkspaceUri,
      String workspaceRepresentation
  ) implements RdfStoreMessage {}

  /**
   * TODO: Javadoc.
   */
  record QueryKnowledgeGraph(
      String query,
      List<String> defaultGraphUris,
      List<String> namedGraphUris,
      String responseContentType
  ) implements RdfStoreMessage {
    /**
     * TODO: Javadoc.
     */
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
