package org.hyperagents.yggdrasil.cartago;

import cartago.AgentBodyArtifact;
import cartago.CartagoException;
import cartago.OPERATION;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Vertx;
import java.util.NoSuchElementException;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

public class HypermediaAgentBodyArtifact extends AgentBodyArtifact {
  private String callbackIri = "http://example.org";
  private Vertx vertx = Vertx.vertx();

  @OPERATION
  public void setCallbackUri(final String callbackUri) {
    this.callbackIri = callbackUri;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @OPERATION
  public void setVertx(final Vertx vertx) {
    this.vertx = vertx;
  }

  @OPERATION
  public void focus(final String artifactName) {
    final var workspaceName = this.getId().getWorkspaceId().getName();
    final var workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName).orElseThrow();
    final var agentId = this.getCurrentOpAgentId();
    try {
      workspace.focus(
          agentId,
          p -> true,
          new NotificationCallback(new HttpNotificationDispatcherMessagebox(this.vertx.eventBus())),
          workspace.getArtifact(artifactName)
      );
      NotificationSubscriberRegistry
          .getInstance()
          .addCallbackIri(
            HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceName)
            + this.getId().getName(),
            this.callbackIri
          );
    } catch (final NoSuchElementException | CartagoException e) {
      this.failed(e.getMessage());
    }
  }
}
