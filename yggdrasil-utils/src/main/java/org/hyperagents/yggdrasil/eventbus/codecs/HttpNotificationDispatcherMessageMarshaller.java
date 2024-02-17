package org.hyperagents.yggdrasil.eventbus.codecs;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;

/**
 * TODO: Javadoc.
 */
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
      case ACTION_REQUESTED -> new HttpNotificationDispatcherMessage.ActionRequested(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
      case ACTION_SUCCEEDED -> new HttpNotificationDispatcherMessage.ActionSucceeded(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
      case ACTION_FAILED -> new HttpNotificationDispatcherMessage.ActionFailed(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.NOTIFICATION_CONTENT.getName()).getAsString()
      );
      case ADD_CALLBACK -> new HttpNotificationDispatcherMessage.AddCallback(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.CALLBACK_IRI.getName()).getAsString()
      );
      case REMOVE_CALLBACK -> new HttpNotificationDispatcherMessage.RemoveCallback(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.CALLBACK_IRI.getName()).getAsString()
      );
    };
  }

  @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault", "PMD.SwitchDensity"})
  @Override
  public JsonElement serialize(
      final HttpNotificationDispatcherMessage message,
      final Type type,
      final JsonSerializationContext context
  ) {
    final var json = new JsonObject();
    json.addProperty(MessageFields.REQUEST_URI.getName(), message.requestIri());
    switch (message) {
      case HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ARTIFACT_OBS_PROP.getName()
        );
      }
      case HttpNotificationDispatcherMessage.EntityChanged m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ENTITY_CHANGED.getName()
        );
      }
      case HttpNotificationDispatcherMessage.EntityCreated m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ENTITY_CREATED.getName()
        );
      }
      case HttpNotificationDispatcherMessage.EntityDeleted m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ENTITY_DELETED.getName()
        );
      }
      case HttpNotificationDispatcherMessage.ActionFailed m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ACTION_FAILED.getName()
        );
      }
      case HttpNotificationDispatcherMessage.ActionRequested m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ACTION_REQUESTED.getName()
        );
      }
      case HttpNotificationDispatcherMessage.ActionSucceeded m -> {
        json.addProperty(MessageFields.NOTIFICATION_CONTENT.getName(), m.content());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ACTION_SUCCEEDED.getName()
        );
      }
      case HttpNotificationDispatcherMessage.AddCallback m -> {
        json.addProperty(MessageFields.CALLBACK_IRI.getName(), m.callbackIri());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.ADD_CALLBACK.getName()
        );
      }
      case HttpNotificationDispatcherMessage.RemoveCallback m -> {
        json.addProperty(MessageFields.CALLBACK_IRI.getName(), m.callbackIri());
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageNotifications.REMOVE_CALLBACK.getName()
        );
      }
    }
    return json;
  }
}
