package org.hyperagents.yggdrasil.utils;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;

public interface RepresentationFactory {

  String createPlatformRepresentation();

  String createWorkspaceRepresentation(String workspaceName, Set<String> artifactTemplates);

  String createArtifactRepresentation(
      final String workspaceName,
      final String artifactName,
      final SecurityScheme securityScheme,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, ActionAffordance> actionAffordances
  );
}
