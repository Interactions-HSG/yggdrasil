package org.hyperagents.yggdrasil.messages.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hyperagents.yggdrasil.messages.*;

import java.util.Optional;

public class CartagoMessagebox implements Messagebox<CartagoMessage> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoMessagebox.class.getName());

  private final EventBus eventBus;

  public CartagoMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Future<Message<String>> sendMessage(final CartagoMessage message) {
    final var promise = Promise.<Message<String>>promise();
    switch (message) {
      case CartagoMessage.CreateWorkspace(String agentId, String envName, String workspaceName, String representation) ->
        this.eventBus.request(
          MessageAddresses.CARTAGO.getName(),
          representation,
          new DeliveryOptions()
            .addHeader(MessageHeaders.AGENT_ID.getName(), agentId)
            .addHeader(MessageHeaders.WORKSPACE_NAME.getName(), workspaceName)
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_WORKSPACE.getName())
            .addHeader(MessageHeaders.ENV_NAME.getName(), envName),
          promise
        );
      case CartagoMessage.CreateArtifact(String agentId, String workspaceName, String artifactName, String representation) -> {
        LOGGER.info("Collecting delivery options, workspace: " + workspaceName);
        LOGGER.info("Sending cartago message to create artifact...");
        this.eventBus.request(
          MessageAddresses.CARTAGO.getName(),
          representation,
          new DeliveryOptions()
            .addHeader(MessageHeaders.AGENT_ID.getName(), agentId)
            .addHeader(MessageHeaders.WORKSPACE_NAME.getName(), workspaceName)
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_ARTIFACT.getName())
            .addHeader(MessageHeaders.ARTIFACT_NAME.getName(), artifactName),
          promise
        );
      }
      case CartagoMessage.DoAction(
        String agentId,
        String workspaceName,
        String artifactName,
        String actionName,
        Optional<String> content
      ) ->
        this.eventBus.request(
          MessageAddresses.CARTAGO.getName(),
          content.orElse(null),
          new DeliveryOptions()
            .addHeader(MessageHeaders.AGENT_ID.getName(), agentId)
            .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.DO_ACTION.getName())
            .addHeader(MessageHeaders.WORKSPACE_NAME.getName(), workspaceName)
            .addHeader(MessageHeaders.ARTIFACT_NAME.getName(), artifactName)
            .addHeader(MessageHeaders.ACTION_NAME.getName(), actionName),
          promise
        );
    }
    return promise.future();
  }
}
