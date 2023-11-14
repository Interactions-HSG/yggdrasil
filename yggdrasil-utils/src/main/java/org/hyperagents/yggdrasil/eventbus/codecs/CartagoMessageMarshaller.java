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
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;

public class CartagoMessageMarshaller
    implements JsonSerializer<CartagoMessage>, JsonDeserializer<CartagoMessage> {
  @Override
  public CartagoMessage deserialize(
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
      case CREATE_WORKSPACE -> new CartagoMessage.CreateWorkspace(
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString()
      );
      case CREATE_SUB_WORKSPACE -> new CartagoMessage.CreateSubWorkspace(
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.SUB_WORKSPACE_NAME.getName()).getAsString()
      );
      case JOIN_WORKSPACE -> new CartagoMessage.JoinWorkspace(
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString()
      );
      case LEAVE_WORKSPACE -> new CartagoMessage.LeaveWorkspace(
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString()
      );
      case FOCUS -> new CartagoMessage.Focus(
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ARTIFACT_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString()
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

  @SuppressWarnings({"PMD.SwitchDensity", "PMD.SwitchStmtsShouldHaveDefault"})
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public JsonElement serialize(
      final CartagoMessage message,
      final Type type,
      final JsonSerializationContext context
  ) {
    final var json = new JsonObject();
    json.addProperty(MessageFields.WORKSPACE_NAME.getName(), message.workspaceName());
    switch (message) {
      case CartagoMessage.CreateWorkspace ignored ->
        json.addProperty(
          MessageFields.REQUEST_METHOD.getName(),
          MessageRequestMethods.CREATE_WORKSPACE.getName()
        );
      case CartagoMessage.CreateSubWorkspace m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.CREATE_SUB_WORKSPACE.getName()
        );
        json.addProperty(MessageFields.SUB_WORKSPACE_NAME.getName(), m.subWorkspaceName());
      }
      case CartagoMessage.JoinWorkspace m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.JOIN_WORKSPACE.getName()
        );
        json.addProperty(MessageFields.AGENT_ID.getName(), m.agentId());
      }
      case CartagoMessage.LeaveWorkspace m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.LEAVE_WORKSPACE.getName()
        );
        json.addProperty(MessageFields.AGENT_ID.getName(), m.agentId());
      }
      case CartagoMessage.Focus m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.FOCUS.getName()
        );
        json.addProperty(MessageFields.AGENT_ID.getName(), m.agentId());
        json.addProperty(MessageFields.REQUEST_URI.getName(), m.callbackIri());
        json.addProperty(MessageFields.ARTIFACT_NAME.getName(), m.artifactName());
      }
      case CartagoMessage.CreateArtifact m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.CREATE_ARTIFACT.getName()
        );
        json.addProperty(MessageFields.AGENT_ID.getName(), m.agentId());
        json.addProperty(MessageFields.ARTIFACT_NAME.getName(), m.artifactName());
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), m.representation());
      }
      case CartagoMessage.DoAction m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.DO_ACTION.getName()
        );
        json.addProperty(MessageFields.AGENT_ID.getName(), m.agentId());
        json.addProperty(MessageFields.ARTIFACT_NAME.getName(), m.artifactName());
        json.addProperty(MessageFields.ACTION_NAME.getName(), m.actionName());
        json.addProperty(MessageFields.ACTION_CONTENT.getName(), m.content().orElse(null));
      }
    }
    return json;
  }
}
