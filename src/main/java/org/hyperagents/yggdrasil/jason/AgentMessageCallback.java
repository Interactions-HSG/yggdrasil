package org.hyperagents.yggdrasil.jason;

import java.util.*;

public class AgentMessageCallback {

  private final Queue<String> messages;

  private boolean newMessage;

  public AgentMessageCallback(){
    this.messages = new ArrayDeque<>();
    this.newMessage = false;
  }

  public void addMessage(String message){

    this.messages.add(message);
    this.newMessage = true;
  }

  public boolean hasNewMessage(){
    return newMessage;
  }

  public void noNewMessage(){
    newMessage = false;
  }

  public String retrieveMessage(){
    return this.messages.poll();
  }

  public boolean isEmpty(){
    return this.messages.isEmpty();
  }


}
