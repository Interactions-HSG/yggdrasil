package org.hyperagents.yggdrasil;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() {
    ConfigRetriever.create(this.vertx)
                   .getConfig()
                   .onSuccess(c -> {
                     this.vertx.deployVerticle(
                       new HttpServerVerticle(),
                       new DeploymentOptions().setConfig(c)
                     );
                     this.vertx.deployVerticle(
                       new RdfStoreVerticle(),
                       new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER).setConfig(c)
                     );
                     this.vertx.deployVerticle(
                       new HttpNotificationVerticle(),
                       new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER).setConfig(c)
                     );
                     this.vertx.deployVerticle(
                       new CartagoVerticle(),
                       new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER).setConfig(c)
                     );
                   });
  }
}
