package org.hyperagents.yggdrasil.cartago.entities;

import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.util.agent.Percept;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

public class NotificationCallback implements ICartagoCallback {
  private final HttpInterfaceConfig httpConfig;
  private final HttpNotificationDispatcherMessagebox messagebox;

  public NotificationCallback(
      final HttpInterfaceConfig httpConfig,
      final HttpNotificationDispatcherMessagebox messagebox
  ) {
    this.httpConfig = httpConfig;
    this.messagebox = messagebox;
  }

  @Override
  public void notifyCartagoEvent(final CartagoEvent event) {
    if (event instanceof ArtifactObsEvent e) {
      final var percept = new Percept(e);

      // Signals don't have an artifact source
      // TODO
      if (percept.hasSignal()) {
        System.out.println(percept.getSignal().toString());
        return;
      }



      final var source = percept.getArtifactSource();
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
                source.getWorkspaceId().getName(),
                source.getName()
              ),
              p.toString()
            )
          ));
    }
  }
}
