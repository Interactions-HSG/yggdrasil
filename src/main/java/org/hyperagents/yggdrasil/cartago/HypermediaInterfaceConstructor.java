package org.hyperagents.yggdrasil.cartago;

import cartago.ArtifactDescriptor;
import cartago.ArtifactId;
import cartago.Workspace;

public interface HypermediaInterfaceConstructor {

  HypermediaInterface createHypermediaInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId);
}
