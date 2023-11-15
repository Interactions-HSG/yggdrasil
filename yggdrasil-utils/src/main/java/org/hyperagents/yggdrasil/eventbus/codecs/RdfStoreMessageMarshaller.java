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
import java.util.Optional;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;

public class RdfStoreMessageMarshaller
    implements JsonSerializer<RdfStoreMessage>, JsonDeserializer<RdfStoreMessage> {

  @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault", "PMD.SwitchDensity"})
  @Override
  public JsonElement serialize(
      final RdfStoreMessage message,
      final Type type,
      final JsonSerializationContext context
  ) {
    final var json = new JsonObject();
    switch (message) {
      case RdfStoreMessage.CreateArtifact m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.CREATE_ARTIFACT.getName()
        );
        json.addProperty(MessageFields.REQUEST_URI.getName(), m.requestUri());
        json.addProperty(MessageFields.ENTITY_URI_HINT.getName(), m.artifactName());
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), m.artifactRepresentation());
      }
      case RdfStoreMessage.CreateWorkspace m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.CREATE_WORKSPACE.getName()
        );
        json.addProperty(MessageFields.REQUEST_URI.getName(), m.requestUri());
        json.addProperty(MessageFields.ENTITY_URI_HINT.getName(), m.workspaceName());
        json.addProperty(
            MessageFields.PARENT_WORKSPACE_URI.getName(),
            m.parentWorkspaceUri().orElse(null)
        );
        json.addProperty(
            MessageFields.ENTITY_REPRESENTATION.getName(),
            m.workspaceRepresentation()
        );
      }
      case RdfStoreMessage.DeleteEntity(String requestUri) -> {
        json.addProperty(MessageFields.REQUEST_URI.getName(), requestUri);
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.DELETE_ENTITY.getName()
        );
      }
      case RdfStoreMessage.GetEntity(String requestUri) -> {
        json.addProperty(MessageFields.REQUEST_URI.getName(), requestUri);
        json.addProperty(
          MessageFields.REQUEST_METHOD.getName(),
          MessageRequestMethods.GET_ENTITY.getName()
        );
      }
      case RdfStoreMessage.UpdateEntity(String requestUri, String entityRepresentation) -> {
        json.addProperty(MessageFields.REQUEST_URI.getName(), requestUri);
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.UPDATE_ENTITY.getName()
        );
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), entityRepresentation);
      }
      case RdfStoreMessage.Query(String query) -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.QUERY.getName()
        );
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), query);
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
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString()
      );
      case CREATE_ARTIFACT -> new RdfStoreMessage.CreateArtifact(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_URI_HINT.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case CREATE_WORKSPACE -> new RdfStoreMessage.CreateWorkspace(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_URI_HINT.getName()).getAsString(),
        jsonObject.get(MessageFields.PARENT_WORKSPACE_URI.getName()).isJsonNull()
        ? Optional.empty()
        : Optional.of(jsonObject.get(MessageFields.PARENT_WORKSPACE_URI.getName()).getAsString()),
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
