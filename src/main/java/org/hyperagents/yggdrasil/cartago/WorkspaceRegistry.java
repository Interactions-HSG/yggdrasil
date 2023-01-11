package org.hyperagents.yggdrasil.cartago;

import cartago.*;

import java.util.*;


public class WorkspaceRegistry {

  private static WorkspaceRegistry registry;

  private Map<String, WorkspaceDescriptor> workspaceMap;


  private Hashtable<String, String> nameUriMap;

  private Hashtable<String, String> uriNameMap;



  public static synchronized WorkspaceRegistry getInstance(){
    if (registry == null){
      registry = new WorkspaceRegistry();
    }
    return registry;

  }

  private WorkspaceRegistry(){
    workspaceMap = new Hashtable<>();
    nameUriMap = new Hashtable<>();
    uriNameMap = new Hashtable<>();

  }

  public void registerWorkspace(WorkspaceDescriptor workspaceDescriptor){
    workspaceMap.put(workspaceDescriptor.getWorkspace().getId().getName(), workspaceDescriptor);
  }

  public void registerWorkspace(WorkspaceDescriptor descriptor, String uri){
    String name = descriptor.getWorkspace().getId().getName();
    System.out.println("name: "+name);
    System.out.println("uri: "+uri);
    workspaceMap.put(name, descriptor);
    nameUriMap.put(name, uri);
    uriNameMap.put(uri, name);
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

  public String getUri(String name){
    return nameUriMap.get(name);
  }

  public String getName(String uri){
    return uriNameMap.get(uri);
  }

  public Optional<String> getParentWorkspaceUriFromName(String workspaceName){
    Optional<String> parentName = Optional.empty();
    String parentWorkspaceName = workspaceMap.get(workspaceName).getParentInfo().getId().getName();
    if (workspaceMap.containsKey(parentWorkspaceName)){
      parentName = Optional.of(nameUriMap.get(parentWorkspaceName));
    }
    return parentName;

  }

  public Optional<String> getParentWorkspaceUriFromUri(String workspaceUri){
    String workspaceName = uriNameMap.get(workspaceUri);
    return getParentWorkspaceUriFromName(workspaceName);

  }

  public WorkspaceId getWorkspaceId(Workspace workspace){
    WorkspaceId workspaceId = null;
    for (String key: workspaceMap.keySet()){
      WorkspaceDescriptor workspaceDescriptor = workspaceMap.get(key);
      Workspace w = workspaceDescriptor.getWorkspace();
      if (w.equals(workspace)){
        workspaceId = workspaceDescriptor.getId();
      }
    }
    return workspaceId;
  }
}
