package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.GUARD;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cartago.Tuple;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import java.util.List;

public class KnowledgeGraph extends Artifact {
  private final Vertx vertx = Vertx.vertx();
  private final RdfStoreMessagebox messagebox = new RdfStoreMessagebox(this.vertx.eventBus());

  @OPERATION
  public void select(final String query, final OpFeedbackParam<List<Tuple>> result) {
    final var future = this.messagebox
                           .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                             query,
                             List.of(),
                             List.of(),
                             "text/csv"
                           ));
    await("futureCompleted", future);
  }

  @OPERATION
  public void selectOne(final String query, final OpFeedbackParam<Tuple> result) {
    final var future = this.messagebox
                           .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                             query,
                             List.of(),
                             List.of(),
                             "text/csv"
                           ));
    await("futureCompleted", future);
  }

  @OPERATION
  public void ask(final String query, final OpFeedbackParam<Boolean> result) {
    final var future = this.messagebox
                           .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                             query,
                             List.of(),
                             List.of(),
                             "text/csv"
                           ));
    await("futureCompleted", future);
    result.set(Boolean.valueOf(future.result().body()));
  }

  @GUARD
  private boolean futureCompleted(final Future<?> future) {
    return future.isComplete();
  }
}
