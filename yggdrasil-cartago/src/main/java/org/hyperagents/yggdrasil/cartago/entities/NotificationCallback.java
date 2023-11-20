package org.hyperagents.yggdrasil.cartago.entities;

import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.util.agent.Percept;
import java.util.Arrays;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

public class NotificationCallback implements ICartagoCallback {
  private static final Logger LOGGER = LogManager.getLogger(NotificationCallback.class);

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
                      this.httpConfig.getArtifactUri(
                        source.getWorkspaceId().getName(),
                        source.getName()
                      ),
                      p.toString()
                    )
                );
                LOGGER.info("message sent to notification verticle");
              });
    }
  }
}
