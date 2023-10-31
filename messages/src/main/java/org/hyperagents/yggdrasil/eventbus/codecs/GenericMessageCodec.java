package org.hyperagents.yggdrasil.eventbus.codecs;

import com.google.gson.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class GenericMessageCodec<T> implements MessageCodec<T, T> {
  private static final int INT_LENGHT = 4;

  private final Gson gson;
  private final Class<T> messageType;

  public <M extends JsonSerializer<? super T> & JsonDeserializer<? super T>> GenericMessageCodec(
    final Class<T> messageType,
    final M messageMarshaller
  ) {
    this.messageType = messageType;
    this.gson = new GsonBuilder().registerTypeHierarchyAdapter(messageType, messageMarshaller)
                                 .serializeNulls()
                                 .create();
  }

  @Override
  public void encodeToWire(final Buffer buffer, final T message) {
    final var serializedMessage = this.gson.toJson(message);
    buffer.appendInt(serializedMessage.length());
    buffer.appendString(serializedMessage);
  }

  @Override
  public T decodeFromWire(final int pos, final Buffer buffer) {
    return this.gson.fromJson(buffer.getString(pos + INT_LENGHT, buffer.getInt(pos)), this.messageType);
  }

  @Override
  public T transform(final T message) {
    return message;
  }

  @Override
  public String name() {
    return this.messageType.getName() + "Codec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
