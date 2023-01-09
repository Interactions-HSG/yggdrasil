package org.hyperagents.yggdrasil.sem;

import ch.unisg.ics.interactions.hmas.core.hostables.ResourceProfile;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.AgentProfile;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ArtifactProfile;

public interface SignifierExposureMechanism {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.sem";

  public static final String ADJUST_ENTITY = "org.hyperagents.yggdrasil.eventbus.methods.getAdjustedEntity";

  ArtifactProfile getComplementaryProfile(ArtifactProfile artifactProfile, AgentProfile agentProfile);
}
