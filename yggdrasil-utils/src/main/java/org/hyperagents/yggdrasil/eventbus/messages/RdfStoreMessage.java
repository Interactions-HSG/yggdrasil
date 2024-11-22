package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An interface representing a message for RDF store operations.
 *
 * <p>This interface is used to define different types of messages that can be sent to perform
 * operations on an RDF store. Each record implementing this interface represents a specific
 * operation.
 */
public sealed interface RdfStoreMessage {

  /**
   * A record representing a request to get an entity Iri from the RDF store.
   *
   * @param requestUri the Uri to be requested
   * @param slug       the wanted name
   */
  record GetEntityIri(String requestUri, String slug) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to get an entity from the RDF store.
   *
   * @param requestUri The URI of the request to get the entity.
   */
  record GetEntity(String requestUri) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to update an entity in the RDF store.
   *
   * @param requestUri           The URI of the request to update the entity.
   * @param entityRepresentation The string representation of the entity to be updated.
   */
  record ReplaceEntity(String requestUri, String entityRepresentation) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to update an entity in the RDF store.
   *
   * @param requestUri           The URI of the request to update the entity.
   * @param entityRepresentation The string representation of the entity to be updated.
   */
  record UpdateEntity(String requestUri, String entityRepresentation) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to delete an entity from the RDF store.
   *
   */
  record DeleteEntity(String workspaceName, String artifactName) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to create a body in the RDF store.
   *
   * @param workspaceName      The name of the workspace where the body will be created.
   * @param agentID            The ID of the agent for which the body will be created.
   * @param agentName          The name of the agent for which the body will be created.
   * @param bodyRepresentation The string representation of the body to be created.
   */
  record CreateBody(
      String workspaceName,
      String agentID,
      String agentName,
      String bodyRepresentation
  ) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to create an artifact in the RDF store.
   *
   * @param requestUri             The URI of the request to create the artifact.
   * @param artifactName           The name of the artifact to be created.
   * @param artifactRepresentation The string representation of the artifact to be created.
   */
  record CreateArtifact(
      String requestUri,
      String workspaceName,
      String artifactName,
      String artifactRepresentation
  ) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to create a workspace in the RDF store.
   *
   * @param requestUri              The URI of the request to create the workspace.
   * @param workspaceName           The name of the workspace to be created.
   * @param parentWorkspaceUri      The URI of the parent workspace, if any.
   * @param workspaceRepresentation The string representation of the workspace to be created.
   */
  record CreateWorkspace(
      String requestUri,
      String workspaceName,
      Optional<String> parentWorkspaceUri,
      String workspaceRepresentation
  ) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to get the workspaces from the RDF store.
   *
   * @param containerWorkspace The name of the container workspace.
   */
  record GetWorkspaces(String containerWorkspace) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to get the artifacts from the RDF store.
   *
   * @param workspaceName The name of the workspace.
   */
  record GetArtifacts(String workspaceName) implements RdfStoreMessage {
  }

  /**
   * A record representing a request to query the knowledge graph in the RDF store.
   *
   * @param query               The SPARQL query to be executed.
   * @param defaultGraphUris    The URIs of the default graphs to be used in the query.
   * @param namedGraphUris      The URIs of the named graphs to be used in the query.
   * @param responseContentType The content type of the response.
   */
  record QueryKnowledgeGraph(
      String query,
      List<String> defaultGraphUris,
      List<String> namedGraphUris,
      String responseContentType
  ) implements RdfStoreMessage {
    /**
     * Constructor for the QueryKnowledgeGraph record.
     *
     * @param query               The SPARQL query to be executed.
     * @param defaultGraphUris    The URIs of the default graphs to be used in the query.
     * @param namedGraphUris      The URIs of the named graphs to be used in the query.
     * @param responseContentType The content type of the response.
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

    /**
     * Getter for the defaultGraphUris field.
     *
     * @return A new list containing the URIs of the default graphs.
     */
    @Override
    public List<String> defaultGraphUris() {
      return new ArrayList<>(this.defaultGraphUris);
    }

    /**
     * Getter for the namedGraphUris field.
     *
     * @return A new list containing the URIs of the named graphs.
     */
    @Override
    public List<String> namedGraphUris() {
      return new ArrayList<>(this.namedGraphUris);
    }
  }
}
