package org.hyperagents.yggdrasil.sem;

import ch.unisg.ics.interactions.hmas.core.hostables.ResourceProfile;

public interface SignifierExposureMechanism {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.sem";

  public static final String ADJUST_ENTITY = "org.hyperagents.yggdrasil.eventbus.methods.getAdjustedEntity";

  ResourceProfile getComplementaryProfile(ResourceProfile artifactProfile, ResourceProfile agentProfile);
}
