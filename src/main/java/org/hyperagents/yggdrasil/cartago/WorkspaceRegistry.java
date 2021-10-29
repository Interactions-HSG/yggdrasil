package org.hyperagents.yggdrasil.cartago;

import cartago.*;

import java.util.*;

public class WorkspaceRegistry {

  private static WorkspaceRegistry registry;

  private Map<String, WorkspaceDescriptor> workspaceMap;



  public static synchronized WorkspaceRegistry getInstance(){
    if (registry == null){
      registry = new WorkspaceRegistry();
    }
    return registry;

  }

  private WorkspaceRegistry(){
    workspaceMap = new Hashtable<>();

  }

  public void registerWorkspace(WorkspaceDescriptor workspaceDescriptor){
    workspaceMap.put(workspaceDescriptor.getWorkspace().getId().getName(), workspaceDescriptor);
  }

  public WorkspaceDescriptor getWorkspaceDescriptor(String name){
    return workspaceMap.get(name);
  }

  public Workspace getWorkspace(String name){return workspaceMap.get(name).getWorkspace();}

  public WorkspaceId getWorkspaceId(String name){return workspaceMap.get(name).getId();}

  public List<String> getAllWorkspaces(){
    Set<String> workspaces = workspaceMap.keySet();
    return new ArrayList<>(workspaces);
  }
}
