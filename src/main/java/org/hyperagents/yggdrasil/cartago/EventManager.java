package org.hyperagents.yggdrasil.cartago;

import cartago.CartagoEvent;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public class EventManager {

  private Queue<CartagoEventWrapper> events;

  public EventManager(){
    events = new LinkedTransferQueue<>();
  }


  public boolean add(CartagoEvent event){
    boolean b = this.events.add(new CartagoEventWrapper(event));
    return b;
  }

  public int size(){
    return events.size();
  }


  public CartagoEventWrapper remove(){
    return this.events.remove();
  }

}
