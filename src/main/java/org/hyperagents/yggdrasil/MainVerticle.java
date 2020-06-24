package org.hyperagents.yggdrasil;

import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
  
  @Override
  public void start() {
    vertx.deployVerticle(new HttpServerVerticle(),
        new DeploymentOptions().setConfig(config())
      );

    vertx.deployVerticle(new RdfStoreVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
      );

    vertx.deployVerticle(new HttpNotificationVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(config())
      );

    JsonObject knownArtifacts = new JsonObject()
        .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.Counter");
    
    JsonObject cartagoConfig = config();
    cartagoConfig.put("known-artifacts", knownArtifacts);
    
    vertx.deployVerticle(new CartagoVerticle(), 
        new DeploymentOptions().setWorker(true).setConfig(cartagoConfig)
      );
  }
}
