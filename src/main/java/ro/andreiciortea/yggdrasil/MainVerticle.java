package ro.andreiciortea.yggdrasil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import ro.andreiciortea.yggdrasil.http.HttpNotificationDispatcherVerticle;
import ro.andreiciortea.yggdrasil.http.HttpServerVerticle;
import ro.andreiciortea.yggdrasil.store.RdfStoreVerticle;
import ro.andreiciortea.yggdrasil.store.td.TdStoreVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    vertx.deployVerticle(new HttpServerVerticle(),
        new DeploymentOptions().setConfig(config())
      );

    vertx.deployVerticle(new RdfStoreVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
      );

    vertx.deployVerticle(new HttpNotificationDispatcherVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
      );

    vertx.deployVerticle(new TdStoreVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
      );
  }
}
