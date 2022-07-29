package org.hyperagents.yggdrasil.moise;

import cartago.ArtifactId;
import cartago.Workspace;
import ora4mas.nopl.OrgBoard;
import org.hyperagents.yggdrasil.cartago.ActionDescription;
import org.hyperagents.yggdrasil.cartago.ArgumentConverter;
import org.hyperagents.yggdrasil.cartago.HypermediaInterface;

import java.util.*;

public class MoiseInterfaces {

  public HypermediaInterface getOrgBoardHypermediaInterface(Workspace workspace, ArtifactId artifactId){
    List<ActionDescription> descriptions = new ArrayList<>();
    Map<String, ArgumentConverter> converterMap = new Hashtable<>();
    HypermediaInterface hypermediaInterface = null;
    hypermediaInterface = new HypermediaInterface(OrgBoard.class, workspace, artifactId, descriptions, converterMap, Optional.empty(), Optional.empty());
    return hypermediaInterface;
  }
}
