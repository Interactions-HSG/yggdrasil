package org.hyperagents.yggdrasil.cartago;

import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.util.agent.Percept;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import java.util.Arrays;
import java.util.stream.Stream;

public class NotificationCallback implements ICartagoCallback {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationCallback.class.getName());

  private final HttpNotificationDispatcherMessagebox messagebox;

  public NotificationCallback(final HttpNotificationDispatcherMessagebox messagebox) {
    this.messagebox = messagebox;
  }

  @Override
  public void notifyCartagoEvent(final CartagoEvent event) {
    if (event instanceof ArtifactObsEvent e) {
      final var percept = new Percept(e);
      final var source = percept.getArtifactSource();
      Stream.concat(
        Arrays.stream(percept.getPropChanged()),
        Arrays.stream(percept.getAddedProperties())
      )
      .forEach(p -> {
        LOGGER.info("percept: " + p.toString());
        this.messagebox.sendMessage(
          new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
            HypermediaArtifactRegistry.getInstance()
                                      .getHttpArtifactsPrefix(source.getWorkspaceId().getName())
            + source.getName(),
            p.toString()
          )
        );
        LOGGER.info("message sent to notification verticle");
      });
    }
  }
}
