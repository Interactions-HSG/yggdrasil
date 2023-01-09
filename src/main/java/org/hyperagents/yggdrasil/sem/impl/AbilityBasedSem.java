package org.hyperagents.yggdrasil.sem.impl;

import ch.unisg.ics.interactions.hmas.core.hostables.Artifact;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import org.eclipse.rdf4j.model.IRI;
import org.hyperagents.yggdrasil.sem.SignifierExposureMechanism;

import java.util.*;

public class AbilityBasedSem implements SignifierExposureMechanism {


  @Override
  public ArtifactProfile getComplementaryProfile(ArtifactProfile artifactProfile, AgentProfile agentProfile) {

    Artifact artifact = artifactProfile.getArtifact();
    ArtifactProfile.Builder adjustedProfileBuilder = new ArtifactProfile.Builder(artifact);

    Optional<IRI> artifactProfileIRI = artifactProfile.getIRI();
    if (artifactProfileIRI.isPresent()) {
      adjustedProfileBuilder.setIRI(artifactProfileIRI.get());
    }

    SituatedAgent agent = agentProfile.getAgent();
    Set<Ability> abilities = agent.getAbilities();

    Set<Signifier> signifiers = artifactProfile.getExposedSignifiers();

    boolean relevant;
    for (Signifier signifier : signifiers) {
      Set<Ability> recommendedAbilities = signifier.getRecommendedAbilities();

      relevant = true;
      for (Ability recAbility : recommendedAbilities) {
        if(!abilities.stream()
          .anyMatch(a -> a.getSemanticTypes()
            .containsAll(recAbility.getSemanticTypes()))) {
          relevant = false;
          break;
        }
      }
      if (relevant) {
        adjustedProfileBuilder.exposeSignifier(signifier);
      }
    }

    return adjustedProfileBuilder.build();
  }
}
