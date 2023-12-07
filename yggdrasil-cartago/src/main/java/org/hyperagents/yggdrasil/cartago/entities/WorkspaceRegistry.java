package org.hyperagents.yggdrasil.cartago.entities;

import cartago.Workspace;
import cartago.WorkspaceDescriptor;
import java.util.Optional;

public interface WorkspaceRegistry {
  void registerWorkspace(final WorkspaceDescriptor descriptor, final String uri);

  Optional<Workspace> getWorkspace(final String name);
}
