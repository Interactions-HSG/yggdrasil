package org.hyperagents.yggdrasil.jason;

import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxFactoryImpl;

public class VertxRegistry {

  private static VertxRegistry registry;

  private Vertx vertx;

  private VertxRegistry(){
    this.vertx = new VertxFactoryImpl().vertx();
  }

  public static VertxRegistry getInstance(){
    if (registry == null){
      registry = new VertxRegistry();
    }
    return registry;
  }

  public Vertx getVertx(){
    return vertx;
  }

  public void setVertx(Vertx v){
    this.vertx = v;
  }
}
