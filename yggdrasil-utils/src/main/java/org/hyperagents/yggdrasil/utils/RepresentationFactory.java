package org.hyperagents.yggdrasil.utils;

import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
import com.google.common.collect.ListMultimap;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;

public interface RepresentationFactory {

  String createPlatformRepresentation();

  String createWorkspaceRepresentation(String workspaceName, Set<String> artifactTemplates);

  String createArtifactRepresentation(
      String workspaceName,
      String artifactName,
      String semanticType,
      Model metadata,
      ListMultimap<String, Signifier> actionAffordances
  );

  String createBodyRepresentation(
      String workspaceName,
      String agentId,
      Model metadata
  );
}
