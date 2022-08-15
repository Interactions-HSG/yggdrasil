package org.hyperagents.yggdrasil.moise;

import cartago.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import ora4mas.nopl.*;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.PubSubVerticle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.cartago.HypermediaInterface;
import org.hyperagents.yggdrasil.cartago.HypermediaInterfaceConstructor;
import org.hyperagents.yggdrasil.cartago.WorkspaceRegistry;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import java.util.List;


public class MoiseVerticle extends AbstractVerticle {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.moise";

  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";

  public static final String CREATE_GROUP = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createGroup";

  @Override
  public void start(){
    EventBus eventBus = vertx.eventBus();
    HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
    registry.addArtifactTemplate("http://example.org/OrgBoard", OrgBoard.class.getCanonicalName());
    registry.addArtifactTemplate("http://example.org/GroupBoard", GroupBoard.class.getCanonicalName());
    registry.addArtifactTemplate("http://example.org/NormativeBoard", NormativeBoard.class.getCanonicalName());
    registry.addArtifactTemplate("http://example.org/SchemeBoard", SchemeBoard.class.getCanonicalName());
    HypermediaArtifactRegistry artifactRegistry = HypermediaArtifactRegistry.getInstance();
    artifactRegistry.registerInterfaceConstructor(OrgBoard.class.getCanonicalName(), new HypermediaInterfaceConstructor() {
      @Override
      public HypermediaInterface createHypermediaInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId) {
        return MoiseInterfaces.getOrgBoardHypermediaInterface(workspace, artifactId);
      }
    });
    artifactRegistry.registerInterfaceConstructor(GroupBoard.class.getCanonicalName(), new HypermediaInterfaceConstructor() {
      @Override
      public HypermediaInterface createHypermediaInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId) {
        return MoiseInterfaces.getGroupBoardHypermediaInterface(workspace, artifactId);
      }
    });
    artifactRegistry.registerInterfaceConstructor(NormativeBoard.class.getCanonicalName(), new HypermediaInterfaceConstructor() {
      @Override
      public HypermediaInterface createHypermediaInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId) {
        return MoiseInterfaces.getNormativeBoardHypermediaInterface(workspace, artifactId);
      }
    });
    artifactRegistry.registerInterfaceConstructor(SchemeBoard.class.getCanonicalName(), new HypermediaInterfaceConstructor() {
      @Override
      public HypermediaInterface createHypermediaInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId) {
        return MoiseInterfaces.getSchemeBoardHypermediaInterface(workspace, artifactId);
      }
    });
    eventBus.send(PubSubVerticle.BUS_ADDRESS, "", new DeliveryOptions().addHeader(PubSubVerticle.REQUEST_METHOD, PubSubVerticle.SUBSCRIBE)
      .addHeader(PubSubVerticle.TOPIC_NAME, "cartago action"));
    eventBus.consumer(BUS_ADDRESS, this::handleMoiseRequest);
    /*while (true){
      createHypermediaInterfaces();
    }*/
  }

  private void handleMoiseRequest(Message<String> message) {
    if (message.replyAddress().equals(PubSubVerticle.BUS_ADDRESS) ){

      JsonObject jsonObject = (JsonObject) Json.decodeValue(message.body());
      String artifactType = jsonObject.getString("artifactType");
      String workspaceName = jsonObject.getString("workspace");
      if (artifactType.equals(OrgBoard.class.getCanonicalName()) && workspaceName != null) {
        createHypermediaInterfaces(workspaceName);
      }

    }

    String agentUri = message.headers().get(AGENT_ID);

    if (agentUri == null) {
      message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.");
      return;
    }

    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    try {
      switch (requestMethod){
        case CREATE_GROUP:

          System.out.println("group created");

      }

    } catch (Exception e){
      e.printStackTrace();
    }

  }

  public void createHypermediaInterfaces(){
    WorkspaceRegistry workspaceRegistry = WorkspaceRegistry.getInstance();
    HypermediaArtifactRegistry artifactRegistry = HypermediaArtifactRegistry.getInstance();
    List<String> workspaceList = workspaceRegistry.getAllWorkspaces();
    for (String name: workspaceList){
      Workspace w = workspaceRegistry.getWorkspace(name);
      String[] artifactNames = w.getArtifactList();
      for (int i= 0; i< artifactNames.length; i++){
        ArtifactId artifactId = w.getArtifact(artifactNames[i]);
        String artifactName = artifactId.getName();
        String artifactType = artifactId.getArtifactType();
        if (artifactType == OrgBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getOrgBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        } else if (artifactType == GroupBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getGroupBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        } else if (artifactType == NormativeBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getNormativeBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        } else if (artifactType == SchemeBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getSchemeBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        }
      }
    }
  }

  public void createHypermediaInterfaces(String workspaceName){
    WorkspaceRegistry workspaceRegistry = WorkspaceRegistry.getInstance();
    HypermediaArtifactRegistry artifactRegistry = HypermediaArtifactRegistry.getInstance();
    Workspace w = workspaceRegistry.getWorkspace(workspaceName);
    String[] artifactNames = w.getArtifactList();
      for (int i= 0; i< artifactNames.length; i++){
        ArtifactId artifactId = w.getArtifact(artifactNames[i]);
        String artifactName = artifactId.getName();
        String artifactType = artifactId.getArtifactType();
        if (artifactType == OrgBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getOrgBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        } else if (artifactType == GroupBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getGroupBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        } else if (artifactType == NormativeBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getNormativeBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        } else if (artifactType == SchemeBoard.class.getCanonicalName() && !artifactRegistry.hasHypermediaInterface(artifactName) ){
          HypermediaInterface hypermediaInterface = MoiseInterfaces.getSchemeBoardHypermediaInterface(w,artifactId);
          artifactRegistry.register(hypermediaInterface);
        }
      }
    }
}
