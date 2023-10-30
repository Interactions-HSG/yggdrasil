package org.hyperagents.yggdrasil.cartago;

import cartago.ArtifactObsProperty;
import cartago.CartagoContext;
import cartago.util.agent.Percept;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hyperagents.yggdrasil.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.messages.Messagebox;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CartagoPerceptFetcher implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private final Map<String, CartagoContext> agentContexts;
  private final Messagebox<HttpNotificationDispatcherMessage> messagebox;

  CartagoPerceptFetcher(
    final Map<String, CartagoContext> agentContexts,
    final Messagebox<HttpNotificationDispatcherMessage> messagebox
  ) {
    this.agentContexts = agentContexts;
    this.messagebox = messagebox;
  }

  @Override
  public void run() {
    this.agentContexts.values().forEach(c -> this.fetchPercept(c).ifPresent(p -> {
      LOGGER.info(
        "Percept: "
        + p.getSignal()
        + String.format(
          ", [%s], [%s], [%s]",
          printProps(p.getAddedProperties()),
          printProps(p.getPropChanged()),
          printProps(p.getRemovedProperties())
        )
        + " for agent "
        + c.getName()
        + " from artifact "
        + p.getArtifactSource()
      );

      // Signals don't have an artifact source
      if (p.hasSignal() || p.getPropChanged() == null) {
        return;
      }

      final var source = p.getArtifactSource();
      final var artifactIri =
        HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(source.getWorkspaceId().getName()) + source.getName();
      LOGGER.info("artifactIri: " + artifactIri + ", percept: " + p.getPropChanged()[0].toString());

      this.messagebox.sendMessage(
        new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(artifactIri, p.getPropChanged()[0].toString())
      );
      LOGGER.info("message sent to notification verticle");
    }));
  }

  private Optional<Percept> fetchPercept(final CartagoContext context) {
    try {
      return Optional.ofNullable(context.fetchPercept());
    } catch (final InterruptedException e) {
      LOGGER.error("An error occurred while fetching a percept: " + e.getMessage());
    }
    return Optional.empty();
  }

  private String printProps(final ArtifactObsProperty[] props) {
    return Optional.ofNullable(props)
                   .stream()
                   .flatMap(Stream::of)
                   .map(p ->
                     String.format("%s, id: %d, fullId: %s, annot: %s", p, p.getId(), p.getFullId(), p.getAnnots())
                   )
                   .collect(Collectors.joining());
  }
}
