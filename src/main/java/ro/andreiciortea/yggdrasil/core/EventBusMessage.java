package ro.andreiciortea.yggdrasil.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;

public class EventBusMessage {

  public static enum MessageType {
    GET_ENTITY, CREATE_ENTITY, PATCH_ENTITY, UPDATE_ENTITY, DELETE_ENTITY, ACTIONS_ENTITY,
    CREATE_ARTIFACT_ENTITY,
    STORE_REPLY, STORE_REPLY_JSON,
    ENTITY_CREATED_NOTIFICATION, ENTITY_CHANGED_NOTIFICATION, ENTITY_DELETED_NOTIFICATION
  }

  public static enum Headers {
    REQUEST_IRI, ENTITY_IRI_HINT, REQUEST_CONTENT_TYPE,
    REPLY_STATUS
  }

  public static enum ReplyStatus {
    SUCCEEDED, ENTITY_NOT_FOUND, FAILED
  }

  private MessageType messageType;
  private Map<Headers,String> headers;
  private String payload;

  public EventBusMessage(MessageType messageType) {
    this.messageType = messageType;
    this.headers = new HashMap<Headers,String>();
  }

  public MessageType getMessageType() {
    return this.messageType;
  }

  public Optional<String> getHeader(Headers key) {
    return headers.get(key) == null ? Optional.empty() : Optional.of(headers.get(key));
  }

  public Map<Headers,String> getHeaders() {
    return this.headers;
  }

  public EventBusMessage setHeader(Headers key, String value) {
    this.headers.put(key, value);
    return this;
  }

  public Optional<String> getPayload() {
    if (payload == null) {
      return Optional.empty();
    }

    return Optional.of(payload);
  }

  public EventBusMessage setPayload(String payload) {
    this.payload = payload;
    return this;
  }

  public Optional<String> getReplyStatus() {
    return getHeader(Headers.REPLY_STATUS);
  }

  public boolean succeded() {
    if (!getHeader(Headers.REPLY_STATUS).isPresent()) {
      return false;
    }

    return getHeader(Headers.REPLY_STATUS).get().equalsIgnoreCase(ReplyStatus.SUCCEEDED.name());
  }

  public boolean entityNotFound() {
    if (!getHeader(Headers.REPLY_STATUS).isPresent()) {
      return false;
    }

    return getHeader(Headers.REPLY_STATUS).get().equalsIgnoreCase(ReplyStatus.ENTITY_NOT_FOUND.name());
  }

  public String toJson() {
    return new Gson().toJson(this, this.getClass());
  }
}
