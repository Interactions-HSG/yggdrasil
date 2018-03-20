package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import ro.andreiciortea.yggdrasil.core.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {
        vertx.deployVerticle(new HttpServerVerticle(),
                new DeploymentOptions().setConfig(config())
            );
  }

}
