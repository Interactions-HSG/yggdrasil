package org.hyperagents.yggdrasil.cartago;

import cartago.*;
import io.vertx.core.Vertx;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

public class HypermediaAgentBodyArtifact extends AgentBodyArtifact {

  private String callbackIri = "http://example.org";

  private Vertx vertx = Vertx.vertx();

  @OPERATION
  public void setCallbackUri(String callbackUri){
    this.callbackIri = callbackUri;
  }

  @OPERATION
  public void setVertx(Vertx vertx){
    this.vertx = vertx;
  }

  @OPERATION
  public void focus(String artifactName){
    String workspaceName = this.getId().getWorkspaceId().getName();
    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    AgentId agentId = this.getCurrentOpAgentId();
    IEventFilter filter = p -> true;
    ICartagoCallback callback = new NotificationCallback(this.vertx, this.callbackIri );
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    String artifactIri = HypermediaArtifactRegistry.getInstance()
      .getHttpArtifactsPrefix(workspaceName) + this.getId().getName();
    try {
      workspace.focus(agentId, filter, callback, artifactId);
      workspace.getArtifactDescriptor(artifactName).addObserver(agentId, filter, callback);
      System.out.println("artifact IRI: "+ artifactIri);
      System.out.println("callback IRI: "+ callbackIri);
      NotificationSubscriberRegistry.getInstance().addCallbackIRI(artifactIri, callbackIri);
    } catch (Exception e){
      e.printStackTrace();
    }

  }
}
