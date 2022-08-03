package org.hyperagents.yggdrasil.moise;

import cartago.ArtifactId;
import cartago.Workspace;
import ora4mas.nopl.OrgBoard;
import org.hyperagents.yggdrasil.cartago.ActionDescription;
import org.hyperagents.yggdrasil.cartago.ArgumentConverter;
import org.hyperagents.yggdrasil.cartago.HypermediaInterface;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MoiseInterfaces {

  public HypermediaInterface getOrgBoardHypermediaInterface(Workspace workspace, ArtifactId artifactId){
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription createGroupDescription = new ActionDescription.Builder("createGroup", "http://example.org/createGroup", "/createGroup")
      .build();
    descriptions.add(createGroupDescription);
    ActionDescription destroyGroupDescription = new ActionDescription.Builder("destroyGroup", "http://example.org/destroyGroup", "/destroyGroup")
      .build();
    descriptions.add(destroyGroupDescription);
    ActionDescription createSchemeDescription = new ActionDescription.Builder("createScheme", "http://example.org/createScheme", "/createScheme")
      .build();
    descriptions.add(createSchemeDescription);
    ActionDescription destroySchemeDescription = new ActionDescription.Builder("destroyScheme", "http://example.org/destroyScheme", "/destroyScheme")
      .build();
    descriptions.add(destroySchemeDescription);
    ActionDescription createNormativeBoardDescription = new ActionDescription.Builder("createNormativeBoard", "http://example.org/createNormativeBoard", "/createNormativeBoard")
      .build();
    descriptions.add(createNormativeBoardDescription);
    Map<String, ArgumentConverter> converterMap = new Hashtable<>();

    HypermediaInterface hypermediaInterface = null;
    hypermediaInterface = new HypermediaInterface(OrgBoard.class, workspace, artifactId, descriptions, converterMap, Optional.empty(), Optional.empty());
    return hypermediaInterface;
  }
}
