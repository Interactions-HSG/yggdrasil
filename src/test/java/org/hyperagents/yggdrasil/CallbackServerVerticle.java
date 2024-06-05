package org.hyperagents.yggdrasil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.Optional;
import org.apache.hc.core5.http.HttpStatus;

public class CallbackServerVerticle extends AbstractVerticle {
  private HttpServer server;

  @Override
  public void start(final Promise<Void> startPromise) {
    final var router = Router.router(this.vertx);
    router.route().handler(BodyHandler.create());
    router.post("/")
          .handler(routingContext -> {
            this.vertx
                .eventBus()
                .send(
                  "test",
                  Optional.ofNullable(routingContext.body().asString()).orElse(""),
                  new DeliveryOptions()
                    .addHeader(
                      "entityIri",
                      routingContext.request()
                                    .headers()
                                    .getAll("Link")
                                    .stream()
                                    .filter(s -> s.matches("<.*?>; rel=\"self\""))
                                    .map(s -> s.substring(s.indexOf('<') + 1, s.indexOf('>')))
                                    .findFirst()
                                    .orElse("")
                    )
                );
            routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
          });
    this.server = this.vertx.createHttpServer();
    this.server.requestHandler(router)
               .listen(8081, "localhost")
               .<Void>mapEmpty()
               .onComplete(startPromise);
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    this.server.close(stopPromise);
  }
}
