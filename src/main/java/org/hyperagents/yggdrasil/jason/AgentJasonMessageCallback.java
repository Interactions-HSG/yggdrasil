package org.hyperagents.yggdrasil.jason;

import java.util.ArrayDeque;
import java.util.Queue;

public class AgentJasonMessageCallback {

  private final Queue<Message> messages;

  private boolean newMessage;

  public AgentJasonMessageCallback(){
    this.messages = new ArrayDeque<>();
    this.newMessage = false;
  }

  public void addMessage(String message, String agent){

    this.messages.add(new Message(message, agent));
    this.newMessage = true;
  }

  public boolean hasNewMessage(){
    return newMessage;
  }

  public void noNewMessage(){
    newMessage = false;
  }

  public Message retrieveMessage(){
    return this.messages.poll();
  }

  public boolean isEmpty(){
    return this.messages.isEmpty();
  }

}
