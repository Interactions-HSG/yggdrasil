package org.hyperagents.yggdrasil;

import io.vertx.core.Launcher;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.cartago.artifacts.Adder;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.jason.JasonVerticle;
import org.hyperagents.yggdrasil.moise.MoiseVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

import java.io.FileInputStream;
import java.util.Properties;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    Properties p = new Properties();
    try {
      p.load(new FileInputStream("./log4j.properties"));
      org.apache.log4j.PropertyConfigurator.configure(p);
    } catch(Exception e){
      e.printStackTrace();
    }
    /*org.apache.log4j.Logger httpLogger = org.apache.log4j.Logger.getLogger("org.apache.hc.client5.http");
    httpLogger.setLevel(org.apache.log4j.Level.INFO);
    org.apache.log4j.Logger rioLogger = org.apache.log4j.Logger.getLogger("org.eclipse.rdf4j.rio");
    rioLogger.setLevel(org.apache.log4j.Level.INFO);*/
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
        .put("https://ci.mines-stetienne.fr/kg/ontology#PhantomX_3D",
            "org.hyperagents.yggdrasil.cartago.artifacts.PhantomX3D")
        .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.artifacts.Counter")
      .put("http://example.org/Adder", Adder.class.getCanonicalName())
        .put("http://example.org/SpatialCalculator2D", "org.hyperagents.yggdrasil.cartago"
            + ".SpatialCalculator2D");

    JsonObject cartagoConfig = config();
    cartagoConfig.put("known-artifacts", knownArtifacts);

    vertx.deployVerticle(new CartagoVerticle(),
        new DeploymentOptions().setWorker(true).setConfig(cartagoConfig)
      );

    vertx.deployVerticle(new MoiseVerticle(),
      new DeploymentOptions().setConfig(config())
    );

    vertx.deployVerticle(new JasonVerticle(),
      new DeploymentOptions().setConfig(config())
    );
  }

  /*public static void main(String[] args){
    Launcher.executeCommand("run", MainVerticle.class.getName());
  }*/
}
