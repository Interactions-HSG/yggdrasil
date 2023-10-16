package org.hyperagents.yggdrasil.jason;

public class Message {

  private final String content;

  private final String sender;

  public Message(String content, String sender){
    this.content = content;
    this.sender = sender;
  }

  public String getContent() {
    return content;
  }

  public String getSender(){
    return sender;
  }

}
