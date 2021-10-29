package org.hyperagents.yggdrasil.cartago;

import cartago.*;


public class EventManagerCallback implements ICartagoCallback {

  EventManager manager;


  public EventManagerCallback(EventManager manager){
    this.manager = manager;
  }

  @Override
  public void notifyCartagoEvent(CartagoEvent ev) {
    manager.add(ev);

  }
}
