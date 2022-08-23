package org.hyperagents.yggdrasil.cartago;

import cartago.*;

import java.util.*;


public class WorkspaceRegistry {

  private static WorkspaceRegistry registry;

  private Map<String, WorkspaceDescriptor> workspaceMap;


  private Hashtable<String, String> nameUriMap;

  private Hashtable<String, String> uriNameMap;

  private Hashtable<String, Set<String>> workspaceAgents;

  private Hashtable<String, Set<String>> workspaceArtifacts;



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
    workspaceAgents = new Hashtable<>();
    workspaceArtifacts = new Hashtable<>();

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

  public boolean hasWorkspace(String workspaceName){
    return this.workspaceMap.containsKey(workspaceName);
  }

  public boolean containsArtifact(String workspaceName, String artifactName){
    boolean b = false;
    if (workspaceArtifacts.containsKey(workspaceName)){
      Set<String> artifacts = workspaceArtifacts.get(workspaceName);
      if (artifacts.contains(artifactName)){
        b = true;
      }
    }
    return b;
  }

  public void addArtifact(String workspaceName, String artifactName){
    if (workspaceArtifacts.containsKey(workspaceName)){
      Set<String> artifacts = workspaceArtifacts.get(workspaceName);
      artifacts.add(artifactName);
      workspaceArtifacts.put(workspaceName, artifacts);
    } else {
      Set<String> l = new HashSet<>();
      l.add(artifactName);
      workspaceArtifacts.put(workspaceName, l);
    }

  }


}
