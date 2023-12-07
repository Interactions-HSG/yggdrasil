package org.hyperagents.yggdrasil;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;

public class DefaultMainVerticle extends AbstractVerticle {
  @Override
  public void start(final Promise<Void> startPromise) {
    ConfigRetriever.create(this.vertx)
                   .getConfig()
                   .compose(c ->
                     this.vertx.deployVerticle(
                       new HttpServerVerticle(),
                       new DeploymentOptions().setConfig(c)
                     )
                     .compose(i -> this.vertx.deployVerticle(
                       new RdfStoreVerticle(),
                       new DeploymentOptions().setConfig(c)
                     ))
                   )
                   .<Void>mapEmpty()
                   .onComplete(startPromise);
  }
}
