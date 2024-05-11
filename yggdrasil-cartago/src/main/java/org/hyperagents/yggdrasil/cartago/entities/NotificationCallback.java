package org.hyperagents.yggdrasil.cartago.entities;

import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.util.agent.Percept;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

public class NotificationCallback implements ICartagoCallback {
  private final HttpInterfaceConfig httpConfig;
  private final HttpNotificationDispatcherMessagebox messagebox;
  private final String workspaceName;
  private final String artifactName;

  public NotificationCallback(
      final HttpInterfaceConfig httpConfig,
      final HttpNotificationDispatcherMessagebox messagebox,
      final String workspaceName,
      final String artifactName
  ) {
    this.httpConfig = httpConfig;
    this.messagebox = messagebox;
    this.workspaceName = workspaceName;
    this.artifactName = artifactName;
  }

  @Override
  public void notifyCartagoEvent(final CartagoEvent event) {
    if (event instanceof ArtifactObsEvent e) {
      final var percept = new Percept(e);

      if (percept.hasSignal()) {
        Stream
          .of(
            Optional.ofNullable(percept.getSignal())
          )
          .flatMap(Optional::stream)
          .forEach(p -> this.messagebox.sendMessage(
            new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
              this.httpConfig.getArtifactUri(
                workspaceName,
                artifactName
              ),
              p.toString()
            )
          ));
        return;
      }

      assert percept.getArtifactSource().getName().equals(artifactName);
      assert percept.getArtifactSource().getWorkspaceId().getName().equals(workspaceName);

      Stream
          .of(
            Optional.ofNullable(percept.getPropChanged()),
            Optional.ofNullable(percept.getAddedProperties()),
            Optional.ofNullable(percept.getRemovedProperties())
          )
          .flatMap(Optional::stream)
          .flatMap(Arrays::stream)
          .forEach(p -> this.messagebox.sendMessage(
            new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
              this.httpConfig.getArtifactUri(
                workspaceName,
                artifactName
              ),
              p.toString()
            )
          ));
    }
  }
}
