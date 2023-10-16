package org.hyperagents.yggdrasil.jason;

import java.util.*;

public class AgentNotificationCallback {

  private final Queue<String> notifications;

  public AgentNotificationCallback(){
    this.notifications = new ArrayDeque<>();
  }

  public void addNotification(String notification){
    this.notifications.add(notification);
  }

  public String retrieveNotification(){
    return this.notifications.poll();
  }

  public boolean isEmpty(){
    return this.notifications.isEmpty();
  }


}
