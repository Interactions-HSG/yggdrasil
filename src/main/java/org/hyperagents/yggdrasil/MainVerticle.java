package org.hyperagents.yggdrasil;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(final Promise<Void> startPromise) {
    ConfigRetriever.create(this.vertx)
                   .getConfig()
                   .compose(c ->
                     this.vertx.deployVerticle(
                       new HttpServerVerticle(),
                       new DeploymentOptions().setConfig(c)
                     )
                     .compose(v -> this.vertx.deployVerticle(
                       new RdfStoreVerticle(),
                       new DeploymentOptions().setConfig(c)
                     ))
                     .compose(v -> this.vertx.deployVerticle(
                       new HttpNotificationVerticle(),
                       new DeploymentOptions().setConfig(c)
                     ))
                     .compose(v -> this.vertx.deployVerticle(
                       new CartagoVerticle(),
                       new DeploymentOptions().setConfig(c)
                     ))
                   )
                   .<Void>mapEmpty()
                   .onComplete(startPromise);
  }
}
