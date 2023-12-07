package org.hyperagents.yggdrasil.eventbus.codecs;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;

public class HttpNotificationDispatcherMessageMarshaller
    implements JsonSerializer<HttpNotificationDispatcherMessage>,
               JsonDeserializer<HttpNotificationDispatcherMessage> {
  @Override
  public HttpNotificationDispatcherMessage deserialize(
      final JsonElement json,
      final Type type,
      final JsonDeserializationContext context
  ) throws JsonParseException {
    final var jsonObject = json.getAsJsonObject();
    return switch (
      MessageNotifications.getFromName(
                           jsonObject.get(MessageFields.REQUEST_METHOD.getName()).getAsString()
                          )
                          .orElseThrow(
                            () -> new JsonParseException("The request method is not valid")
                          )
    ) {
      case ENTITY_CREATED -> new HttpNotificationDispatcherMessage.EntityCreated(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
      case ENTITY_CHANGED -> new HttpNotificationDispatcherMessage.EntityChanged(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
      case ENTITY_DELETED -> new HttpNotificationDispatcherMessage.EntityDeleted(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
      case ARTIFACT_OBS_PROP -> new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
    };
  }

  @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public JsonElement serialize(
      final HttpNotificationDispatcherMessage message,
      final Type type,
      final JsonSerializationContext context
  ) {
    final var json = new JsonObject();
    json.addProperty(MessageFields.REQUEST_URI.getName(), message.requestIri());
    json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), message.content());
    switch (message) {
      case HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated ignored ->
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ARTIFACT_OBS_PROP.getName()
        );
      case HttpNotificationDispatcherMessage.EntityChanged ignored ->
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ENTITY_CHANGED.getName()
        );
      case HttpNotificationDispatcherMessage.EntityCreated ignored ->
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ENTITY_CREATED.getName()
        );
      case HttpNotificationDispatcherMessage.EntityDeleted ignored ->
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ENTITY_DELETED.getName()
        );
    }
    return json;
  }
}
