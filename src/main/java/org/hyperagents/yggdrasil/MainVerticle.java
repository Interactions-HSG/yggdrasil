package org.hyperagents.yggdrasil;

import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpNotificationDispatcherVerticle;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

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

    vertx.deployVerticle(new CartagoVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
      );
  }
}
