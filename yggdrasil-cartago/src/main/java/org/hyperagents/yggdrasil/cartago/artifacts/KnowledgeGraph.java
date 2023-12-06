package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.GUARD;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

public class KnowledgeGraph extends Artifact {
  private final Vertx vertx = Vertx.currentContext().owner();
  private final RdfStoreMessagebox messagebox = new RdfStoreMessagebox(this.vertx.eventBus());
  private final Map<UUID, Boolean> completions = new HashMap<>();

  @OPERATION
  public void select(final String query, final OpFeedbackParam<String[][][]> result) {
    try {
      final var response = (JsonObject) Json.decodeValue(this.executeQuery(query));
      final var bindings = this.getBindingsNames(response);
      result.set(
        JsonObjectUtils.getJsonObject(response, "results", this::failed)
                       .flatMap(rs -> JsonObjectUtils.getJsonArray(
                         rs,
                         "bindings",
                         this::failed
                       ))
                       .stream()
                       .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getJsonObject))
                       .map(r ->
                         bindings.stream()
                                 .flatMap(n ->
                                   JsonObjectUtils.getJsonObject(r, n, this::failed)
                                                  .flatMap(b ->
                                                      JsonObjectUtils
                                                        .getString(b, "value", this::failed)
                                                  )
                                                  .map(v -> new String[] {n, v})
                                                  .stream())
                                 .toArray(String[][]::new)
                       )
                       .toArray(String[][][]::new)
      );
    } catch (final Exception e) {
      this.failed(e.getMessage());
    }
  }

  @OPERATION
  public void selectOne(final String query, final OpFeedbackParam<String[][]> result) {
    try {
      final var response = (JsonObject) Json.decodeValue(this.executeQuery(query));
      final var bindings = this.getBindingsNames(response);
      JsonObjectUtils.getJsonObject(response, "results", this::failed)
                     .flatMap(rs -> JsonObjectUtils.getJsonArray(
                       rs,
                       "bindings",
                       this::failed
                     ))
                     .filter(a -> !a.isEmpty())
                     .map(a -> a.getJsonObject(0))
                     .map(r ->
                       bindings.stream()
                               .flatMap(n ->
                                 JsonObjectUtils.getJsonObject(r, n, this::failed)
                                                .flatMap(b ->
                                                  JsonObjectUtils
                                                    .getString(b, "value", this::failed)
                                                )
                                                .map(v -> new String[] {n, v})
                                                .stream()
                               )
                               .toArray(String[][]::new)
                     )
                     .ifPresent(result::set);
    } catch (final Exception e) {
      this.failed(e.getMessage());
    }
  }

  @OPERATION
  public void ask(final String query, final OpFeedbackParam<Boolean> result) {
    result.set(((JsonObject) Json.decodeValue(this.executeQuery(query))).getBoolean("boolean"));
  }

  private List<String> getBindingsNames(final JsonObject response) {
    return JsonObjectUtils.getJsonObject(response, "head", this::failed)
                          .flatMap(h -> JsonObjectUtils.getJsonArray(
                            h,
                            "vars",
                            this::failed
                          ))
                          .stream()
                          .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getString))
                          .toList();
  }

  private String executeQuery(final String query) {
    final var operationId = UUID.randomUUID();
    final var future = this.messagebox
                           .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                             query,
                             List.of(),
                             List.of(),
                             "application/sparql-results+json"
                           ))
                           .onComplete(r -> this.execInternalOp("setComplete", operationId));
    this.completions.put(operationId, false);
    await("futureCompleted", operationId);
    this.completions.remove(operationId);
    return future.result().body();
  }

  @INTERNAL_OPERATION
  private void setComplete(final UUID operationId) {
    this.completions.put(operationId, true);
  }

  @GUARD
  private boolean futureCompleted(final UUID operationId) {
    return this.completions.get(operationId);
  }
}
