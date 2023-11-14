package org.hyperagents.yggdrasil.cartago.entities;

import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.util.agent.Percept;
import java.util.Arrays;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;

public class NotificationCallback implements ICartagoCallback {
  private static final Logger LOGGER = LogManager.getLogger(NotificationCallback.class);

  private final HttpNotificationDispatcherMessagebox messagebox;

  public NotificationCallback(final HttpNotificationDispatcherMessagebox messagebox) {
    this.messagebox = messagebox;
  }

  @Override
  public void notifyCartagoEvent(final CartagoEvent event) {
    if (event instanceof ArtifactObsEvent e) {
      final var percept = new Percept(e);

      // Signals don't have an artifact source
      if (percept.hasSignal()) {
        return;
      }

      final var source = percept.getArtifactSource();
      Optional.ofNullable(percept.getPropChanged())
              .stream()
              .flatMap(Arrays::stream)
              .forEach(p -> {
                LOGGER.info("percept: " + p.toString());
                this.messagebox.sendMessage(
                    new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
                      HypermediaArtifactRegistry.getInstance()
                                                          .getHttpArtifactsPrefix(
                                                            source.getWorkspaceId().getName()
                                                          )
                      + source.getName(),
                      p.toString()
                    )
                );
                LOGGER.info("message sent to notification verticle");
              });
    }
  }
}
