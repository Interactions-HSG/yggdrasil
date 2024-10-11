package org.hyperagents.yggdrasil.eventbus.messageboxes;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import java.util.function.Consumer;
import org.hyperagents.yggdrasil.eventbus.codecs.GenericMessageCodec;
import org.hyperagents.yggdrasil.eventbus.codecs.RdfStoreMessageMarshaller;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;

/**
 * This class represents a message box for RdfStoreMessage objects.
 * It implements the Messagebox interface and provides methods for initializing the message box,
 * sending messages, and receiving messages.
 */
public class RdfStoreMessagebox implements Messagebox<RdfStoreMessage> {
  private final EventBus eventBus;

  public RdfStoreMessagebox(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void init() {
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.GetEntity.class,
        new GenericMessageCodec<>(RdfStoreMessage.GetEntity.class, new RdfStoreMessageMarshaller())
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.GetEntityIri.class,
      new GenericMessageCodec<>(
        RdfStoreMessage.GetEntityIri.class,
          new RdfStoreMessageMarshaller()
      )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.CreateBody.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.CreateBody.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.CreateArtifact.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.CreateArtifact.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.CreateWorkspace.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.CreateWorkspace.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.DeleteEntity.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.DeleteEntity.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.ReplaceEntity.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.ReplaceEntity.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.UpdateEntity.class,
      new GenericMessageCodec<>(
        RdfStoreMessage.UpdateEntity.class,
          new RdfStoreMessageMarshaller()
      )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.GetWorkspaces.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.GetWorkspaces.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.GetArtifacts.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.GetArtifacts.class,
          new RdfStoreMessageMarshaller()
        )
    );
    this.eventBus.registerDefaultCodec(
        RdfStoreMessage.QueryKnowledgeGraph.class,
        new GenericMessageCodec<>(
          RdfStoreMessage.QueryKnowledgeGraph.class,
          new RdfStoreMessageMarshaller()
        )
    );
  }

  @Override
  public Future<Message<String>> sendMessage(final RdfStoreMessage message) {
    return this.eventBus.request(MessageAddresses.RDF_STORE.getName(), message);
  }

  @Override
  public void receiveMessages(final Consumer<Message<RdfStoreMessage>> messageHandler) {
    this.eventBus.consumer(MessageAddresses.RDF_STORE.getName(), messageHandler::accept);
  }
}
