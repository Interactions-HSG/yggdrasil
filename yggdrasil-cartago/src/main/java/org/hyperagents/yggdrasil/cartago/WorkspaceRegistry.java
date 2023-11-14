package org.hyperagents.yggdrasil.cartago;

import cartago.Workspace;
import cartago.WorkspaceDescriptor;
import cartago.WorkspaceId;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("PMD.ReplaceHashtableWithMap")
public final class WorkspaceRegistry {
  private static WorkspaceRegistry REGISTRY;

  private final Map<String, WorkspaceDescriptor> workspaceMap;
  private final BiMap<String, String> nameToUriMap;

  public static synchronized WorkspaceRegistry getInstance() {
    if (REGISTRY == null) {
      REGISTRY = new WorkspaceRegistry();
    }
    return REGISTRY;
  }

  private WorkspaceRegistry() {
    this.workspaceMap = new Hashtable<>();
    this.nameToUriMap = Maps.synchronizedBiMap(HashBiMap.create());
  }

  public void registerWorkspace(final WorkspaceDescriptor workspaceDescriptor) {
    this.workspaceMap.put(
        workspaceDescriptor.getWorkspace().getId().getName(),
        workspaceDescriptor
    );
  }

  public void registerWorkspace(final WorkspaceDescriptor descriptor, final String uri) {
    final var name = descriptor.getWorkspace().getId().getName();
    this.workspaceMap.put(name, descriptor);
    this.nameToUriMap.put(name, uri);
  }

  public Optional<Workspace> getWorkspace(final String name) {
    return Optional.ofNullable(this.workspaceMap.get(name)).map(WorkspaceDescriptor::getWorkspace);
  }

  public WorkspaceId getWorkspaceId(final Workspace workspace) {
    return this.workspaceMap
               .values()
               .stream()
               .filter(d -> d.getWorkspace().equals(workspace))
               .map(WorkspaceDescriptor::getId)
               .findFirst()
               .orElse(null);
  }

  public List<String> getAllWorkspaces() {
    return new ArrayList<>(this.workspaceMap.keySet());
  }

  public String getUri(final String name) {
    return this.nameToUriMap.get(name);
  }

  public String getName(final String uri) {
    return this.nameToUriMap.inverse().get(uri);
  }

  public Optional<String> getParentWorkspaceUriFromName(final String workspaceName) {
    return Optional.ofNullable(this.nameToUriMap.get(
      this.workspaceMap.get(workspaceName).getParentInfo().getId().getName()
    ));
  }

  public Optional<String> getParentWorkspaceUriFromUri(final String workspaceUri) {
    return this.getParentWorkspaceUriFromName(this.nameToUriMap.inverse().get(workspaceUri));
  }
}
