package org.hyperagents.yggdrasil.model.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.hyperagents.yggdrasil.model.Artifact;
import org.hyperagents.yggdrasil.model.Environment;
import org.hyperagents.yggdrasil.model.FocusingAgent;
import org.hyperagents.yggdrasil.model.JoinedAgent;
import org.hyperagents.yggdrasil.model.KnownArtifact;
import org.hyperagents.yggdrasil.model.Workspace;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

public final class EnvironmentParser {
  private static final Logger LOGGER = Logger.getLogger(EnvironmentParser.class);

  private EnvironmentParser() {}

  public static Environment parse(final JsonObject config) {
    final var envConfig =
        JsonObjectUtils.getJsonObject(config, "environment-config", LOGGER::error);
    final var knownArtifacts =
        envConfig
          .flatMap(c -> JsonObjectUtils.getJsonArray(c, "known-artifacts", LOGGER::error))
          .stream()
          .flatMap(a -> IntStream.range(0, a.size())
                                 .mapToObj(a::getValue)
                                 .flatMap(o ->
                                   (
                                     o instanceof JsonObject j
                                     ? Optional.of(j)
                                     : Optional.<JsonObject>empty()
                                   )
                                   .stream()
                                 ))
          .<KnownArtifact>flatMap(o -> {
            final var clazz = JsonObjectUtils.getString(o, "class", LOGGER::error);
            final var template = JsonObjectUtils.getString(o, "template", LOGGER::error);
            if (clazz.isEmpty() || template.isEmpty()) {
              LOGGER.warn("Known artifact missing class or template, skipping");
              return Stream.empty();
            }
            return Stream.of(new KnownArtifactImpl(clazz.get(), template.get()));
          })
          .collect(Collectors.toSet());
    final var artifactClasses = knownArtifacts.stream()
                                              .map(KnownArtifact::getClazz)
                                              .collect(Collectors.toSet());
    final var jsonWorkspaces =
        envConfig.flatMap(c -> JsonObjectUtils.getJsonArray(c, "workspaces", LOGGER::error))
                 .stream()
                 .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getJsonObject))
                 .toList();
    final var workspaceNames =
        jsonWorkspaces.stream()
                      .flatMap(w -> JsonObjectUtils.getString(w, "name", LOGGER::error).stream())
                      .collect(Collectors.toSet());
    return new EnvironmentImpl(
      jsonWorkspaces
        .stream()
        .<Workspace>flatMap(w -> {
          final var name = JsonObjectUtils.getString(w, "name", LOGGER::error);
          if (name.isEmpty()) {
            LOGGER.warn("Workspace missing name, skipping");
            return Stream.empty();
          }
          final var agentNames =
              JsonObjectUtils.getJsonArray(w, "agents", LOGGER::error)
                             .stream()
                             .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getString))
                             .collect(Collectors.toCollection(HashSet::new));
          return Stream.of(new WorkspaceImpl(
            name.get(),
            JsonObjectUtils
              .getString(w, "parent-name", LOGGER::error)
              .filter(p -> {
                if (!workspaceNames.contains(p)) {
                  LOGGER.warn(
                      "Workspace has an undefined parent without definition, ignoring parent name"
                  );
                }
                return workspaceNames.contains(p);
              }),
            agentNames.stream().map(JoinedAgentImpl::new).collect(Collectors.toSet()),
            JsonObjectUtils
                .getJsonArray(w, "artifacts", LOGGER::error)
                .stream()
                .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getJsonObject))
                .<Artifact>flatMap(ar -> {
                  final var artifactName = JsonObjectUtils.getString(ar, "name", LOGGER::error);
                  final var artifactClass = JsonObjectUtils.getString(ar, "clazz", LOGGER::error);
                  if (artifactName.isEmpty() || artifactClass.isEmpty()) {
                    LOGGER.warn("Workspace artifact missing name or class, skipping");
                    return Stream.empty();
                  }
                  if (!artifactClasses.contains(artifactClass.get())) {
                    LOGGER.warn("Workspace artifact not having a known artifact class, skipping");
                    return Stream.empty();
                  }
                  return Stream.of(new ArtifactImpl(
                    artifactName.get(),
                    artifactClass.get(),
                    JsonObjectUtils
                      .getJsonArray(ar, "init-params", LOGGER::error)
                      .map(JsonArray::getList)
                      .orElse(Collections.emptyList()),
                    JsonObjectUtils
                      .getJsonArray(ar, "focused-by", LOGGER::error)
                      .stream()
                      .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getJsonObject))
                      .<FocusingAgent>flatMap(ag -> {
                        final var agentUri =
                            JsonObjectUtils.getString(ag, "agent-uri", LOGGER::error);
                        final var callbackUri =
                            JsonObjectUtils.getString(ag, "callback-uri", LOGGER::error);
                        if (agentUri.isEmpty() || callbackUri.isEmpty()) {
                          LOGGER.warn("Focusing agent missing uri or callback, skipping");
                          return Stream.empty();
                        }
                        if (!agentNames.contains(agentUri.get())) {
                          LOGGER.warn(
                              "Focusing agent not defined to join workspace, adding it"
                          );
                          agentNames.add(agentUri.get());
                        }
                        return Stream.of(new FocusingAgentImpl(
                          agentUri.get(),
                          callbackUri.get()
                        ));
                      })
                      .collect(Collectors.toSet())
                  ));
                })
                .collect(Collectors.toSet())
          ));
        })
        .collect(Collectors.toSet()),
      knownArtifacts
    );
  }

  private record EnvironmentImpl(
      Set<Workspace> workspaces,
      Set<KnownArtifact> knownArtifacts
  ) implements Environment {

    @Override
    public Set<Workspace> getWorkspaces() {
      return this.workspaces();
    }

    @Override
    public Set<KnownArtifact> getKnownArtifacts() {
      return this.knownArtifacts();
    }
  }

  private record KnownArtifactImpl(String clazz, String template)
      implements KnownArtifact {
    @Override
    public String getClazz() {
      return this.clazz();
    }

    @Override
    public String getTemplate() {
      return this.template();
    }
  }

  private record WorkspaceImpl(
      String name,
      Optional<String> parentName,
      Set<JoinedAgent> joinedAgents,
      Set<Artifact> artifacts
  ) implements Workspace {
    @Override
    public String getName() {
      return this.name();
    }

    @Override
    public Optional<String> getParentName() {
      return this.parentName();
    }

    @Override
    public Set<Artifact> getArtifacts() {
      return this.artifacts();
    }

    @Override
    public Set<JoinedAgent> getAgents() {
      return this.joinedAgents();
    }
  }

  private record JoinedAgentImpl(String name) implements JoinedAgent {
    @Override
    public String getName() {
      return this.name();
    }
  }

  private record ArtifactImpl(
      String name,
      String clazz,
      List<?> initializationParameters,
      Set<FocusingAgent> focusingAgents
  ) implements Artifact {
    @Override
    public String getName() {
      return this.name();
    }

    @Override
    public String getClazz() {
      return this.clazz();
    }

    @Override
    public List<?> getInitializationParameters() {
      return this.initializationParameters();
    }

    @Override
    public Set<FocusingAgent> getFocusingAgents() {
      return this.focusingAgents();
    }
  }

  private record FocusingAgentImpl(String name, String callback) implements FocusingAgent {
    @Override
    public String getName() {
      return this.name();
    }

    @Override
    public String getCallback() {
      return this.callback();
    }
  }
}
