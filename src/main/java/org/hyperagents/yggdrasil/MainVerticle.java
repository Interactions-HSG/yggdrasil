package org.hyperagents.yggdrasil;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;

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
                     .compose(v ->
                       new WebSubConfigImpl(c, new HttpInterfaceConfigImpl(c)).isEnabled()
                       ? this.vertx.deployVerticle(
                           "org.hyperagents.yggdrasil.websub.HttpNotificationVerticle",
                           new DeploymentOptions().setConfig(c)
                         )
                       : Future.succeededFuture()
                     )
                     .compose(v ->
                       new EnvironmentConfigImpl(c).isEnabled()
                       ? this.vertx.deployVerticle(
                           "org.hyperagents.yggdrasil.cartago.CartagoVerticle",
                           new DeploymentOptions().setConfig(c)
                         )
                       : Future.succeededFuture()
                       )
                   )
                   .<Void>mapEmpty()
                   .onComplete(startPromise);
  }
}
