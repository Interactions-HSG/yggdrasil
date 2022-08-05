package org.hyperagents.yggdrasil.moise;

import cartago.ArtifactId;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ora4mas.nopl.OrgBoard;
import org.hyperagents.yggdrasil.cartago.ActionDescription;
import org.hyperagents.yggdrasil.cartago.ArgumentConverter;
import org.hyperagents.yggdrasil.cartago.HypermediaInterface;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MoiseInterfaces {

  public static HypermediaInterface getOrgBoardHypermediaInterface(Workspace workspace, ArtifactId artifactId){
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription createGroupDescription = new ActionDescription.Builder("createGroup", "http://example.org/createGroup", "/createGroup")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(createGroupDescription);
    ActionDescription destroyGroupDescription = new ActionDescription.Builder("destroyGroup", "http://example.org/destroyGroup", "/destroyGroup")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(destroyGroupDescription);
    ActionDescription createSchemeDescription = new ActionDescription.Builder("createScheme", "http://example.org/createScheme", "/createScheme")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(createSchemeDescription);
    ActionDescription destroySchemeDescription = new ActionDescription.Builder("destroyScheme", "http://example.org/destroyScheme", "/destroyScheme")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
      .build()).build();
    descriptions.add(destroySchemeDescription);
    ActionDescription createNormativeBoardDescription = new ActionDescription.Builder("createNormativeBoard", "http://example.org/createNormativeBoard", "/createNormativeBoard")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
      .build()).build();
    descriptions.add(createNormativeBoardDescription);
    Map<String, ArgumentConverter> converterMap = new Hashtable<>();

    HypermediaInterface hypermediaInterface = null;
    hypermediaInterface = new HypermediaInterface(OrgBoard.class, workspace, artifactId, descriptions, converterMap, Optional.empty(), Optional.empty());
    return hypermediaInterface;
  }

  public static HypermediaInterface getGroupBoardHypermediaInterface(Workspace workspace, ArtifactId artifactId){
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription debugDescription = new ActionDescription.Builder("debug", "http://example.org/debug", "/debug")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(debugDescription);
    ActionDescription destroyDescription = new ActionDescription.Builder("destroy", "http://example.org/destroy", "/destroy")
      .build();
    descriptions.add(destroyDescription);
    ActionDescription setParentGroupDescription = new ActionDescription.Builder("setParentGroup", "http://example.org/setParentGroup", "/setParentGroup")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(setParentGroupDescription);
    ActionDescription adoptRoleDescription = new ActionDescription.Builder("adoptRole", "http://example.org/adoptRole", "/adoptRole")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(adoptRoleDescription);
    ActionDescription leaveRoleDescription = new ActionDescription.Builder("leaveRole", "http://example.org/leaveRole", "/leaveRole")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(leaveRoleDescription);
    ActionDescription addSchemeDescription = new ActionDescription.Builder("addScheme", "http://example.org/addScheme", "/addScheme")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(addSchemeDescription);
    ActionDescription addSchemeWhenFormationOkDescription = new ActionDescription.Builder("addSchemeWhenFormationOk", "http://example.org/addSchemeWhenFormationOk", "/addSchemeWhenFormationOk")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(addSchemeWhenFormationOkDescription);
    ActionDescription removeSchemeDescription = new ActionDescription.Builder("removeScheme", "http://example.org/removeScheme", "/removeScheme")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(removeSchemeDescription);
    ActionDescription admCommandDescription = new ActionDescription.Builder("admCommand", "http://example.org/admCommand", "/admCommand")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(admCommandDescription);
    ActionDescription addRoleDescription = new ActionDescription.Builder("addRole", "http://example.org/addRole", "/addRole")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(addRoleDescription);
    ActionDescription setCardinalityDescription = new ActionDescription.Builder("setCardinality", "http://example.org/setCardinality", "/setCardinality")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
          .addItem(new IntegerSchema.Builder().build())
            .addItem(new IntegerSchema.Builder().build())
        .build())
      .build();
    descriptions.add(setCardinalityDescription);
    Map<String, ArgumentConverter> converterMap = new Hashtable<>();

    HypermediaInterface hypermediaInterface = null;
    hypermediaInterface = new HypermediaInterface(OrgBoard.class, workspace, artifactId, descriptions, converterMap, Optional.empty(), Optional.empty());
    return hypermediaInterface;
  }

  public static HypermediaInterface getSchemeBoardHypermediaInterface(Workspace workspace, ArtifactId artifactId){
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription debugDescription = new ActionDescription.Builder("debug", "http://example.org/debug", "/debug")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(debugDescription);
    ActionDescription destroyDescription = new ActionDescription.Builder("destroy", "http://example.org/destroy", "/destroy")
      .build();
    descriptions.add(destroyDescription);
    ActionDescription commitMissionDescription = new ActionDescription.Builder("commitMission", "http://example.org/commitMission", "/commitMission")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(commitMissionDescription);
    ActionDescription leaveMissionDescription = new ActionDescription.Builder("leaveMission", "http://example.org/leaveMission", "/leaveMission")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(leaveMissionDescription);
    ActionDescription goalAchievedDescription = new ActionDescription.Builder("goalAchieved", "http://example.org/goalAchieved", "/goalAchieved")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(goalAchievedDescription);
    ActionDescription setArgumentValueDescription = new ActionDescription.Builder("setArgumentValue", "http://example.org/setArgumentValue", "/setArgumentValue")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build()) //To change, input is object
        .build())
      .build();
    descriptions.add(setArgumentValueDescription);
    ActionDescription resetGoalDescription = new ActionDescription.Builder("resetGoal", "http://example.org/resetGoal", "/resetGoal")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(resetGoalDescription);
    ActionDescription getStateDescription = new ActionDescription.Builder("getState", "http://example.org/getState", "/getState")
      .build();
    descriptions.add(getStateDescription);
    ActionDescription mergeStateDescription = new ActionDescription.Builder("mergeState", "http://example.org/mergeState", "/mergeState")
      .setInputSchema(new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build()) //To change, input is object
        .build())
      .build();
    descriptions.add(mergeStateDescription);
    ActionDescription admCommandDescription = new ActionDescription.Builder("admCommand", "http://example.org/admCommand", "/admCommand")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(admCommandDescription);
    ActionDescription addMissionDescription = new ActionDescription.Builder("addMission", "http://example.org/addMission", "/addMission")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(addMissionDescription);
    ActionDescription setCardinalityDescription = new ActionDescription.Builder("setCardinality", "http://example.org/setCardinality", "/setCardinality")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
        .addItem(new IntegerSchema.Builder().build())
        .addItem(new IntegerSchema.Builder().build())
        .build())
      .build();
    descriptions.add(setCardinalityDescription);
    Map<String, ArgumentConverter> converterMap = new Hashtable<>();

    HypermediaInterface hypermediaInterface = null;
    hypermediaInterface = new HypermediaInterface(OrgBoard.class, workspace, artifactId, descriptions, converterMap, Optional.empty(), Optional.empty());
    return hypermediaInterface;
  }



  public static HypermediaInterface getNormativeBoardHypermediaInterface(Workspace workspace, ArtifactId artifactId){
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription loadDescription = new ActionDescription.Builder("load", "http://example.org/load", "/load")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(loadDescription);
    ActionDescription debugDescription = new ActionDescription.Builder("debug", "http://example.org/debug", "/debug")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(debugDescription);
    ActionDescription addFactDescription = new ActionDescription.Builder("addFact", "http://example.org/addFact", "/addFact")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(addFactDescription);
    ActionDescription removeFactDescription = new ActionDescription.Builder("removeFact", "http://example.org/removeFact", "/removeFact")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(removeFactDescription);
    ActionDescription doSubscribeDFPDescription = new ActionDescription.Builder("doSubscribeDFP", "http://example.org/doSubscribeDFP", "/doSubscribeDFP")
      .setInputSchema(new ArraySchema.Builder().addItem(new StringSchema.Builder().build()).build())
      .build();
    descriptions.add(doSubscribeDFPDescription);
    ActionDescription destroyDescription = new ActionDescription.Builder("destroy", "http://example.org/destroy", "/destroy")
      .build();
    descriptions.add(destroyDescription);
    Map<String, ArgumentConverter> converterMap = new Hashtable<>();

    HypermediaInterface hypermediaInterface = null;
    hypermediaInterface = new HypermediaInterface(OrgBoard.class, workspace, artifactId, descriptions, converterMap, Optional.empty(), Optional.empty());
    return hypermediaInterface;
  }
}
