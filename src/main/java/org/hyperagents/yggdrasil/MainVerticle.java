package org.hyperagents.yggdrasil;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.model.Environment;
import org.hyperagents.yggdrasil.model.impl.EnvironmentParser;
import org.hyperagents.yggdrasil.store.RdfStoreVerticleHMAS;
import org.hyperagents.yggdrasil.store.RdfStoreVerticleTD;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;

import java.util.Objects;


/**
 * This is the MainVerticle of the Application.
 * <p>
 * It is the entry point of the Application
 * </p>
 */
public class MainVerticle extends AbstractVerticle {
  private static final String DEFAULT_CONF_VALUE = "default";

  @Override
  public void start(final Promise<Void> startPromise) {
    var config = ConfigRetriever.create(this.vertx).getConfig().result();

    // HttpConfig
    final var httpConfig = new HttpInterfaceConfigImpl(config);
    this.vertx.sharedData()
              .<String, HttpInterfaceConfig>getLocalMap("http-config")
              .put(DEFAULT_CONF_VALUE, httpConfig);

    // EnvironmentConfig
    final var environmentConfig = new EnvironmentConfigImpl(config);
    this.vertx.sharedData()
              .<String, EnvironmentConfig>getLocalMap("environment-config")
              .put(DEFAULT_CONF_VALUE, environmentConfig);

    // NotificationConfig
    final var notificationConfig = new WebSubConfigImpl(config, httpConfig);
    this.vertx.sharedData()
              .<String, WebSubConfig>getLocalMap("notification-config")
              .put(DEFAULT_CONF_VALUE, notificationConfig);

    // Environment
    this.vertx.sharedData()
              .<String, Environment>getLocalMap("environment")
              .put(DEFAULT_CONF_VALUE, EnvironmentParser.parse(config));

    // start the verticles
    this.vertx.deployVerticle(new HttpServerVerticle())
              .compose(v -> {
                if (Objects.equals(environmentConfig.getOntology(), "hmas")) {
                  return this.vertx.deployVerticle(new RdfStoreVerticleHMAS(), new DeploymentOptions().setConfig(config));
                }
                else return this.vertx.deployVerticle(new RdfStoreVerticleTD(), new DeploymentOptions().setConfig(config));
              })
              .compose(v -> notificationConfig.isEnabled() ? this.vertx.deployVerticle("org.hyperagents.yggdrasil.websub.HttpNotificationVerticle") : Future.succeededFuture())
              .compose(v -> {if (environmentConfig.isEnabled()) {
                  if (Objects.equals(environmentConfig.getOntology(), "hmas")) {
                    return this.vertx.deployVerticle("org.hyperagents.yggdrasil.cartago.CartagoVerticleHMAS");
                  } else {
                    return this.vertx.deployVerticle("org.hyperagents.yggdrasil.cartago.CartagoVerticleTD");
                }
              } else {
                return Future.succeededFuture();
              }}).<Void>mapEmpty()
              .onComplete(startPromise);
  }
}
