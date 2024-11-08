package org.hyperagents.yggdrasil;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.model.interfaces.Environment;
import org.hyperagents.yggdrasil.model.parser.EnvironmentParser;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;

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
    ConfigRetriever.create(this.vertx).getConfig().compose(c -> {

      // HttpConfig
      final var httpConfig = new HttpInterfaceConfigImpl(c);
      this.vertx.sharedData().<String, HttpInterfaceConfig>getLocalMap("http-config")
        .put(DEFAULT_CONF_VALUE, httpConfig);

      // EnvironmentConfig
      final var environmentConfig = new EnvironmentConfigImpl(c);
      this.vertx.sharedData().<String, EnvironmentConfig>getLocalMap("environment-config")
        .put(DEFAULT_CONF_VALUE, environmentConfig);

      // NotificationConfig
      final var notificationConfig = new WebSubConfigImpl(c, httpConfig);
      this.vertx.sharedData().<String, WebSubConfig>getLocalMap("notification-config")
        .put(DEFAULT_CONF_VALUE, notificationConfig);

      // Environment
      this.vertx.sharedData().<String, Environment>getLocalMap("environment")
        .put(DEFAULT_CONF_VALUE, EnvironmentParser.parse(c));

      // start the verticles
      return this.vertx.deployVerticle(new HttpServerVerticle()).compose(
        v -> this.vertx.deployVerticle(new RdfStoreVerticle(),
          new DeploymentOptions().setConfig(c))).compose(v -> notificationConfig.isEnabled()
        ?
        this.vertx.deployVerticle("org.hyperagents.yggdrasil.websub.HttpNotificationVerticle") :
        Future.succeededFuture()).compose(v -> new EnvironmentConfigImpl(c).isEnabled()
        ?
        this.vertx.deployVerticle("org.hyperagents.yggdrasil.cartago.CartagoVerticle") :
        Future.succeededFuture());
    }).<Void>mapEmpty().onComplete(startPromise);
  }
}
