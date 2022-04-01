package org.hyperagents.yggdrasil.jason;

import java.util.*;

public class AgentNotificationCallback {

  private String agentName;

  private Queue<String> notifications;

  public AgentNotificationCallback(String agentName){
    this.agentName = agentName;
    this.notifications = new ArrayDeque();
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
