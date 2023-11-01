package org.hyperagents.yggdrasil.eventbus.codecs;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;

public class RdfStoreMessageMarshaller
    implements JsonSerializer<RdfStoreMessage>, JsonDeserializer<RdfStoreMessage> {

  @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault", "PMD.SwitchDensity"})
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public JsonElement serialize(
      final RdfStoreMessage message,
      final Type type,
      final JsonSerializationContext context
  ) {
    final var json = new JsonObject();
    json.addProperty(MessageFields.REQUEST_URI.getName(), message.requestUri());
    switch (message) {
      case RdfStoreMessage.CreateEntity m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.CREATE_ENTITY.getName()
        );
        json.addProperty(MessageFields.ENTITY_URI_HINT.getName(), m.entityName());
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), m.entityRepresentation());
      }
      case RdfStoreMessage.DeleteEntity ignored ->
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.DELETE_ENTITY.getName()
        );
      case RdfStoreMessage.GetEntity m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.GET_ENTITY.getName()
        );
        json.addProperty(MessageFields.CONTENT_TYPE.getName(), m.contentType().orElse(null));
      }
      case RdfStoreMessage.UpdateEntity(String ignored, String entityRepresentation) -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.UPDATE_ENTITY.getName()
        );
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), entityRepresentation);
      }
    }
    return json;
  }

  @Override
  public RdfStoreMessage deserialize(
      final JsonElement json,
      final Type type,
      final JsonDeserializationContext context
  ) throws JsonParseException {
    final var jsonObject = json.getAsJsonObject();
    return switch (
      MessageRequestMethods.getFromName(
                             jsonObject.get(MessageFields.REQUEST_METHOD.getName()).getAsString()
                           )
                           .orElseThrow(
                             () -> new JsonParseException("The request method is not valid")
                           )
    ) {
      case GET_ENTITY -> new RdfStoreMessage.GetEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.CONTENT_TYPE.getName()).isJsonNull()
        ? Optional.empty()
        : Optional.of(jsonObject.get(MessageFields.CONTENT_TYPE.getName()).getAsString())
      );
      case CREATE_ENTITY -> new RdfStoreMessage.CreateEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_URI_HINT.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case UPDATE_ENTITY -> new RdfStoreMessage.UpdateEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case DELETE_ENTITY -> new RdfStoreMessage.DeleteEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString()
      );
      default -> throw new JsonParseException("The request method is not valid");
    };
  }
}
