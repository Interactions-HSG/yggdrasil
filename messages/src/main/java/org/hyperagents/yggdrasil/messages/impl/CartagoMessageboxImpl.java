package org.hyperagents.yggdrasil.messages.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hyperagents.yggdrasil.messages.CartagoMessagebox;
import org.hyperagents.yggdrasil.messages.MessageAddresses;
import org.hyperagents.yggdrasil.messages.MessageHeaders;
import org.hyperagents.yggdrasil.messages.MessageRequestMethods;

import java.util.Optional;

public class CartagoMessageboxImpl implements CartagoMessagebox {
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoMessageboxImpl.class.getName());

  private final EventBus eventBus;

  public CartagoMessageboxImpl(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void createWorkspace(
    final String agentId,
    final String envName,
    final String workspaceName,
    final String representation,
    final Handler<AsyncResult<Message<String>>> handler
  ) {
    this.eventBus.request(
      MessageAddresses.CARTAGO.getName(),
      representation,
      new DeliveryOptions()
        .addHeader(MessageHeaders.AGENT_ID.getName(), agentId)
        .addHeader(MessageHeaders.WORKSPACE_NAME.getName(), workspaceName)
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.CREATE_WORKSPACE.getName())
        .addHeader(MessageHeaders.ENV_NAME.getName(), envName),
      handler
    );
  }

  @Override
  public void createArtifact(
    final String agentId,
    final String workspaceName,
    final String artifactName,
    final String representation,
    final Handler<AsyncResult<Message<String>>> handler) {
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
      handler
    );
  }

  @Override
  public void doAction(
    final String agentId,
    final String workspaceName,
    final String artifactName,
    final String actionName,
    final Optional<String> message,
    final Handler<AsyncResult<Message<Void>>> handler
    ) {
    this.eventBus.request(
      MessageAddresses.CARTAGO.getName(),
      message.orElse(null),
      new DeliveryOptions()
        .addHeader(MessageHeaders.AGENT_ID.getName(), agentId)
        .addHeader(MessageHeaders.REQUEST_METHOD.getName(), MessageRequestMethods.DO_ACTION.getName())
        .addHeader(MessageHeaders.WORKSPACE_NAME.getName(), workspaceName)
        .addHeader(MessageHeaders.ARTIFACT_NAME.getName(), artifactName)
        .addHeader(MessageHeaders.ACTION_NAME.getName(), actionName),
      handler
    );
  }
}
