package org.hyperagents.yggdrasil.cartago;

import cartago.ArtifactId;
import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.events.CartagoActionEvent;
import cartago.util.agent.Percept;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

public class NotificationCallback implements ICartagoCallback {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationCallback.class.getName());

  private Vertx vertx;

  public NotificationCallback(Vertx vertx){
    this.vertx = vertx;
  }

  @Override
  public void notifyCartagoEvent(CartagoEvent ev) {
    System.out.println("notify cartago event");
    System.out.println("event type: "+ ev.getClass().getCanonicalName());
    if (ev instanceof ArtifactObsEvent) {
      System.out.println("event is artifact obs event");
      ArtifactObsEvent obsEvent = (ArtifactObsEvent) ev;
      Percept percept = new Percept(obsEvent);
      System.out.println("percept: "+ percept);
      ArtifactId source = percept.getArtifactSource();
      System.out.println("artifact source: "+source);
      String artifactIri = HypermediaArtifactRegistry.getInstance()
        .getHttpArtifactsPrefix(source.getWorkspaceId().getName()) + source.getName();
      LOGGER.info("artifactIri: " + artifactIri + ", percept: " + percept.getPropChanged()[0].toString());
      DeliveryOptions options = new DeliveryOptions()
        .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle.ARTIFACT_OBS_PROP)
        .addHeader(HttpEntityHandler.REQUEST_URI, artifactIri);

      vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, percept.getPropChanged()[0].toString(),
        options);

      LOGGER.info("message sent to notification verticle");
    }

  }
}
