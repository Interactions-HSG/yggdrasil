package org.hyperagents.yggdrasil.cartago.entities;

import cartago.Workspace;
import cartago.WorkspaceDescriptor;
import java.util.Optional;

/**
 * Interface that defines the API of the WorkspaceRegistry.
 */
public interface WorkspaceRegistry {
  void registerWorkspace(final WorkspaceDescriptor descriptor, final String uri);

  Optional<WorkspaceDescriptor> getWorkspaceDescriptor(final String name);

  Optional<Workspace> getWorkspace(final String name);

  void deleteWorkspace(final String name);
}
