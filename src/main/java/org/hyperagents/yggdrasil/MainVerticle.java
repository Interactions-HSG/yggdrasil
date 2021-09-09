package org.hyperagents.yggdrasil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.signifiers.SignifierVerticle;
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
      .put("http://example.org/Maze", "org.hyperagents.yggdrasil.signifiers.maze.GeneralMaze")
      .put("http://example.org/Maze1", "org.hyperagents.yggdrasil.signifiers.maze.Maze1")
      .put("http://example.org/Maze11", "org.hyperagents.yggdrasil.signifiers.maze.Maze11")
      .put("http://example.org/Maze2", "org.hyperagents.yggdrasil.signifiers.maze.Maze2")
      .put("http://example.org/Maze3", "org.hyperagents.yggdrasil.signifiers.maze.Maze3")
      .put("http://example.org/Maze4", "org.hyperagents.yggdrasil.signifiers.maze.Maze4")
      .put("http://example.org/Maze5", "org.hyperagents.yggdrasil.signifiers.maze.Maze5")
      .put("http://example.org/Maze6", "org.hyperagents.yggdrasil.signifiers.maze.Maze6")
      .put("http://example.org/ConveyingWorkshop", "org.hyperagents.yggdrasil.signifiers.maze.ConveyingWorkshop");

    JsonObject cartagoConfig = config();
    cartagoConfig.put("known-artifacts", knownArtifacts);

    vertx.deployVerticle(new CartagoVerticle(),
      new DeploymentOptions().setWorker(true).setConfig(cartagoConfig)
    );

  }

}
