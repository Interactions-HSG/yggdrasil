package org.hyperagents.yggdrasil.jason;

public class Message {

  private String content;

  private String sender;

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
