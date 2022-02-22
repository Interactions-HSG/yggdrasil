package org.hyperagents.yggdrasil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.signifiers.SignifierVerticle;
import org.hyperagents.yggdrasil.signifiers.maze.ArticleMaze;
import org.hyperagents.yggdrasil.signifiers.maze.MazeCreator;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

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

    vertx.deployVerticle(new SignifierVerticle(),
      new DeploymentOptions().setWorker(true).setConfig(config())
    );

    JsonObject knownArtifacts = new JsonObject()
      .put("https://ci.mines-stetienne.fr/kg/ontology#PhantomX_3D",
        "org.hyperagents.yggdrasil.cartago.artifacts.PhantomX3D")
      .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.artifacts.Counter")
      .put("http://example.org/SpatialCalculator2D", "org.hyperagents.yggdrasil.cartago"
        + ".SpatialCalculator2D")
      .put("http://example.org/AgentProfileArtifact", "org.hyperagents.yggdrasil.signifiers.AgentProfileArtifact")
      .put("http://example.org/Adder", "org.hyperagents.yggdrasil.cartago.artifacts.Adder")
      .put("http://example.org/SignifierTest","org.hyperagents.yggdrasil.signifiers.SignifierTest")
      .put("http://example.org/SignifierArtifact", "org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact")
      .put("http://example.org/ArticleMaze", "org.hyperagents.yggdrasil.signifiers.maze.ArticleMaze");

    JsonObject cartagoConfig = config();
    cartagoConfig.put("known-artifacts", knownArtifacts);

    vertx.deployVerticle(new CartagoVerticle(),
      new DeploymentOptions().setWorker(true).setConfig(cartagoConfig)
    );

    MazeCreator.launchMaze();

  }

}
