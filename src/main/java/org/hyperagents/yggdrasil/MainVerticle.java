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
      .put("http://example.org/Adder", "org.hyperagents.yggdrasil.cartago.artifacts.Adder")
      .put("http://example.org/SignifierArtifact", "org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact")
      .put("http://example.org/Maze", "org.hyperagents.yggdrasil.signifiers.maze.GeneralMaze")
      .put("http://example.org/Maze1", "org.hyperagents.yggdrasil.signifiers.maze.Maze1")
      .put("http://example.org/Maze2", "org.hyperagents.yggdrasil.signifiers.maze.Maze2")
      .put("http://example.org/Maze3", "org.hyperagents.yggdrasil.signifiers.maze.Maze3")
      .put("http://example.org/Maze4", "org.hyperagents.yggdrasil.signifiers.maze.Maze4");

    JsonObject cartagoConfig = config();
    cartagoConfig.put("known-artifacts", knownArtifacts);

    vertx.deployVerticle(new CartagoVerticle(),
      new DeploymentOptions().setWorker(true).setConfig(cartagoConfig)
    );

    /*ProcessBuilder processBuilder = new ProcessBuilder();
    System.out.println("process builder created");
    processBuilder.command("./signifiers/maze/scripts/init.sh");
    System.out.println("script launched");*/
  }

  public void addArtifact(JsonObject object, Class classe){
    String name = classe.getName();
    String key = "http://example.org/"+name;
    String packageName = classe.getPackage().getName();
    String value = packageName+"."+name;
    object.put(key, value);

  }
}
