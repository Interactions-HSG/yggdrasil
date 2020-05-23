package org.hyperagents.yggdrasil.cartago;

import org.hyperagents.yggdrasil.http.HttpTemplateHandler;

import cartago.AgentIdCredential;
import cartago.CartagoException;
import cartago.CartagoService;
import cartago.ICartagoSession;
import cartago.Op;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CartagoVerticle extends AbstractVerticle {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTemplateHandler.class.getName());

  @Override
  public void start() {
    
    try {
      
      LOGGER.info("Starting CArtAgO node...");
      
      CartagoService.startNode();
      
      ICartagoSession session = CartagoService.startSession("main", new AgentIdCredential("agent-0"), null);
      session.doAction(new Op("println","Hello, world!"), null, -1);
    } catch (CartagoException e) {
      LOGGER.error(e.getMessage());
    }
  }
}
