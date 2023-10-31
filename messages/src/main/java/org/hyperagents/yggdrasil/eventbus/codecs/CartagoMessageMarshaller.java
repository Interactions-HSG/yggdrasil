package org.hyperagents.yggdrasil.eventbus.codecs;

import com.google.gson.*;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;

import java.lang.reflect.Type;
import java.util.Optional;

public class CartagoMessageMarshaller implements JsonSerializer<CartagoMessage>, JsonDeserializer<CartagoMessage> {
  @Override
  public CartagoMessage deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context)
    throws JsonParseException {
    final var jsonObject = json.getAsJsonObject();
    return switch (
      MessageRequestMethods.getFromName(jsonObject.get(MessageFields.REQUEST_METHOD.getName()).getAsString())
                           .orElseThrow(() -> new JsonParseException("The request method is not valid"))
    ) {
      case CREATE_WORKSPACE -> new CartagoMessage.CreateWorkspace(
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.ENV_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case CREATE_ARTIFACT -> new CartagoMessage.CreateArtifact(
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ARTIFACT_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case DO_ACTION -> new CartagoMessage.DoAction(
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ARTIFACT_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ACTION_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ACTION_CONTENT.getName()).isJsonNull()
        ? Optional.empty()
        : Optional.of(jsonObject.get(MessageFields.ACTION_CONTENT.getName()).getAsString())
      );
      default -> throw new JsonParseException("The request method is not valid");
    };
  }

  @Override
  public JsonElement serialize(final CartagoMessage message, final Type type, final JsonSerializationContext context) {
    final var json = new JsonObject();
    json.addProperty(MessageFields.AGENT_ID.getName(), message.agentId());
    json.addProperty(MessageFields.WORKSPACE_NAME.getName(), message.workspaceName());
    switch (message) {
      case CartagoMessage.CreateWorkspace m -> {
        json.addProperty(MessageFields.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_WORKSPACE.getName());
        json.addProperty(MessageFields.ENV_NAME.getName(), m.envName());
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), m.representation());
      }
      case CartagoMessage.CreateArtifact m -> {
        json.addProperty(MessageFields.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_ARTIFACT.getName());
        json.addProperty(MessageFields.ARTIFACT_NAME.getName(), m.artifactName());
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), m.representation());
      }
      case CartagoMessage.DoAction m -> {
        json.addProperty(MessageFields.REQUEST_METHOD.getName(), MessageRequestMethods.DO_ACTION.getName());
        json.addProperty(MessageFields.ARTIFACT_NAME.getName(), m.artifactName());
        json.addProperty(MessageFields.ACTION_NAME.getName(), m.actionName());
        json.addProperty(MessageFields.ACTION_CONTENT.getName(), m.content().orElse(null));
      }
    }
    return json;
  }
}
