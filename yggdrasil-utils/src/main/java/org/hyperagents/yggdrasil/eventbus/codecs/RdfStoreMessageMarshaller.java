package org.hyperagents.yggdrasil.eventbus.codecs;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;

/**
 * This class is responsible for serializing and deserializing
 * RdfStoreMessage objects to and from JSON.
 */
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
      case RdfStoreMessage.GetEntityIri(String requestUri, String slug) -> {
        json.addProperty(MessageFields.REQUEST_URI.getName(), requestUri);
        json.addProperty(MessageFields.ENTITY_URI_HINT.getName(), slug);
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.GET_ENTITY_IRI.getName()
        );
      }

      case RdfStoreMessage.ReplaceEntity(String requestUri, String entityRepresentation) -> {
        json.addProperty(MessageFields.REQUEST_URI.getName(), requestUri);
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.UPDATE_ENTITY.getName()
        );
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), entityRepresentation);
      }

      case RdfStoreMessage.UpdateEntity(String requestUri, String entityRepresentation) -> {
        json.addProperty(MessageFields.REQUEST_URI.getName(), requestUri);
        json.addProperty(
            MessageFields.REQUEST_URI.getName(),
            MessageRequestMethods.PATCH_ENTITY.getName()
        );
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), entityRepresentation);
      }

      case RdfStoreMessage.GetWorkspaces(String containerWorkspace) -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.GET_WORKSPACES.getName()
        );
        json.addProperty(MessageFields.REQUEST_URI.getName(), containerWorkspace);
      }

      case RdfStoreMessage.GetArtifacts(String workspaceName) -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.GET_ARTIFACTS.getName()
        );
        json.addProperty(MessageFields.WORKSPACE_NAME.getName(), workspaceName);
      }

      case RdfStoreMessage.QueryKnowledgeGraph(
          String query,
          List<String> defaultGraphUris,
          List<String> namedGraphUris,
          String responseContentType
        ) -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.QUERY.getName()
        );
        json.addProperty(MessageFields.QUERY.getName(), query);
        final var encodedDefaultGraphUris = new JsonArray();
        defaultGraphUris.forEach(encodedDefaultGraphUris::add);
        json.add(MessageFields.DEFAULT_GRAPH_URIS.getName(), encodedDefaultGraphUris);
        final var encodedNamedGraphUris = new JsonArray();
        namedGraphUris.forEach(encodedNamedGraphUris::add);
        json.add(MessageFields.NAMED_GRAPH_URIS.getName(), encodedNamedGraphUris);
        json.addProperty(MessageFields.CONTENT_TYPE.getName(), responseContentType);
      }
      case RdfStoreMessage.CreateBody m -> {
        json.addProperty(
            MessageFields.REQUEST_METHOD.getName(),
            MessageRequestMethods.CREATE_BODY.getName()
        );
        json.addProperty(MessageFields.WORKSPACE_NAME.getName(), m.workspaceName());
        json.addProperty(MessageFields.AGENT_ID.getName(), m.agentID());
        json.addProperty(MessageFields.AGENT_NAME.getName(), m.agentName());
        json.addProperty(MessageFields.ENTITY_REPRESENTATION.getName(), m.bodyRepresentation());
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
      case GET_ENTITY_IRI -> new RdfStoreMessage.GetEntityIri(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_URI_HINT.getName()).getAsString()
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
      case CREATE_BODY -> new RdfStoreMessage.CreateBody(
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.AGENT_ID.getName()).getAsString(),
        jsonObject.get(MessageFields.AGENT_NAME.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case UPDATE_ENTITY -> new RdfStoreMessage.ReplaceEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case PATCH_ENTITY -> new RdfStoreMessage.UpdateEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString(),
        jsonObject.get(MessageFields.ENTITY_REPRESENTATION.getName()).getAsString()
      );
      case DELETE_ENTITY -> new RdfStoreMessage.DeleteEntity(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString()
      );
      case GET_WORKSPACES -> new RdfStoreMessage.GetWorkspaces(
        jsonObject.get(MessageFields.REQUEST_URI.getName()).getAsString()
      );
      case GET_ARTIFACTS -> new RdfStoreMessage.GetArtifacts(
        jsonObject.get(MessageFields.WORKSPACE_NAME.getName()).getAsString()
      );
      case QUERY -> new RdfStoreMessage.QueryKnowledgeGraph(
        jsonObject.get(MessageFields.QUERY.getName()).getAsString(),
        jsonObject.get(MessageFields.DEFAULT_GRAPH_URIS.getName())
                  .getAsJsonArray()
                  .asList()
                  .stream()
                  .map(JsonElement::getAsString)
                  .collect(Collectors.toList()),
        jsonObject.get(MessageFields.NAMED_GRAPH_URIS.getName())
                  .getAsJsonArray()
                  .asList()
                  .stream()
                  .map(JsonElement::getAsString)
                  .collect(Collectors.toList()),
        jsonObject.get(MessageFields.CONTENT_TYPE.getName()).getAsString()
      );
      default -> throw new JsonParseException("The request method is not valid");
    };
  }
}
