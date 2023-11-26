package org.hyperagents.yggdrasil.cartago;

import cartago.Workspace;
import cartago.WorkspaceDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WorkspaceRegistry {
  private final Map<String, WorkspaceDescriptor> workspaceDescriptors;
  private final Map<String, String> workspaceUris;

  public WorkspaceRegistry() {
    this.workspaceDescriptors = new HashMap<>();
    this.workspaceUris = new HashMap<>();
  }

  public void registerWorkspace(final WorkspaceDescriptor descriptor, final String uri) {
    final var name = descriptor.getWorkspace().getId().getName();
    this.workspaceDescriptors.put(name, descriptor);
    this.workspaceUris.put(name, uri);
  }

  public Optional<Workspace> getWorkspace(final String name) {
    return Optional.ofNullable(this.workspaceDescriptors.get(name))
                   .map(WorkspaceDescriptor::getWorkspace);
  }

  public String getUri(final String name) {
    return this.workspaceUris.get(name);
  }
}
