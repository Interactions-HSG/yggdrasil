package org.hyperagents.yggdrasil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;

public class DefaultMainVerticle extends AbstractVerticle {
  @Override
  public void start() {
    this.vertx.deployVerticle(
        new HttpServerVerticle(),
        new DeploymentOptions().setConfig(this.config())
    );
    this.vertx.deployVerticle(
        new RdfStoreVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
    );
  }
}
