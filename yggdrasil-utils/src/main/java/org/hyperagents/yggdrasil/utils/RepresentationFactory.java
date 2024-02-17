package org.hyperagents.yggdrasil.utils;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;

/**
 * This interface represents a factory for creating different representations in the Yggdrasil
 * system. It provides methods for creating platform, workspace, artifact, and body representations.
 */
public interface RepresentationFactory {

  /**
   * Creates a platform representation.
   *
   * @return the platform representation as a string
   */
  String createPlatformRepresentation();

  /**
   * Creates a workspace representation.
   *
   * @param workspaceName the name of the workspace
   * @param artifactTemplates the set of artifact templates
   * @return the workspace representation as a string
   */
  String createWorkspaceRepresentation(String workspaceName, Set<String> artifactTemplates);

  /**
   * Creates an artifact representation.
   *
   * @param workspaceName the name of the workspace
   * @param artifactName the name of the artifact
   * @param securityScheme the security scheme for the artifact
   * @param semanticType the semantic type of the artifact
   * @param metadata the metadata of the artifact
   * @param actionAffordances the action affordances of the artifact
   * @return the artifact representation as a string
   */
  String createArtifactRepresentation(
      String workspaceName,
      String artifactName,
      SecurityScheme securityScheme,
      String semanticType,
      Model metadata,
      ListMultimap<String, ActionAffordance> actionAffordances
  );

  /**
   * Creates a body representation.
   *
   * @param workspaceName the name of the workspace
   * @param agentId the ID of the agent
   * @param securityScheme the security scheme for the body
   * @param metadata the metadata of the body
   * @return the body representation as a string
   */
  String createBodyRepresentation(
      String workspaceName,
      String agentId,
      SecurityScheme securityScheme,
      Model metadata
  );
}
