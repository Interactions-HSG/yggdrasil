package org.hyperagents.yggdrasil.model.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.model.*;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

/**
 * This class is responsible for parsing the environment configuration
 * and creating an Environment object.
 * The parse() method takes a JsonObject as input and returns the parsed Environment.
 * The environment configuration should contain information about known artifacts,
 * workspaces, and their properties.
 * It uses JsonObjectUtils utility methods to extract the required information from the JsonObject.
 * The parsed Environment object contains a set of known artifacts
 * and a list of workspaces, each with its own properties.
 */
public final class EnvironmentParser {
  private static final Logger LOGGER = LogManager.getLogger(EnvironmentParser.class);

  private static final String NAME = "name";

  private EnvironmentParser() {
  }

  /**
   * parses the environment given a config.
   */
  public static Environment parse(final JsonObject config) {
    final var agents =
      JsonObjectUtils.getJsonArray(config, "agents", LOGGER::error)
        .stream().flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getValue)
      .flatMap(o -> (o instanceof JsonObject j ? Optional.of(j) : Optional.<JsonObject>empty())
        .stream()
      ))
      .<YggdrasilAgent>flatMap(o -> {
        final var name = JsonObjectUtils.getString(o,"name",LOGGER::error);
        final var agentUri = JsonObjectUtils.getString(o,"agent-uri",LOGGER::error);
        final var callbackUri = JsonObjectUtils.getString(o,"callback-uri",LOGGER::error);
        if (name.isEmpty() || agentUri.isEmpty() || callbackUri.isEmpty()) {
          LOGGER.warn("Name, Uri or CallbackUri of agent is empty, skipping");
          return Stream.empty();
        }

        final var bodies = JsonObjectUtils.getJsonArray(o,"body-config", LOGGER::error);
        final var t = bodies.stream().flatMap(c -> IntStream.range(0, c.size()).mapToObj(c::getValue))
          .flatMap(q -> (q instanceof JsonObject s ? Optional.of(s) : Optional.<JsonObject>empty()).stream())
          .<AgentBody>flatMap(e -> {
            final var metadata = JsonObjectUtils.getString(e,"metadata",LOGGER::error);
            final var joined = JsonObjectUtils.getJsonArray(e,"join", LOGGER::error)
              .stream().flatMap(q -> IntStream.range(0, q.size()).mapToObj(q::getValue)).map(q -> (String) q);
            return Stream.of(new AgentBodyImpl(metadata,joined.toList()));
          });
        return Stream.of(new AgentImpl(name.get(), agentUri.get(), callbackUri.get(), t.toList()));
      })
      .collect(Collectors.toSet());

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
        .toList();
    final var workspaceNames =
      jsonWorkspaces.stream()
        .flatMap(w -> JsonObjectUtils.getString(w, NAME, LOGGER::error).stream())
        .collect(Collectors.toSet());
    return new EnvironmentImpl(
      agents.stream().toList(),
      reorderWorkspaces(
        jsonWorkspaces
          .stream()
          .<Workspace>flatMap(w -> {
            final var name = JsonObjectUtils.getString(w, NAME, LOGGER::error);
            if (name.isEmpty()) {
              LOGGER.warn("Workspace missing name, skipping");
              return Stream.empty();
            }

            final var joinedBy = JsonObjectUtils.getJsonArray(w, "joined-by", LOGGER::error)
              .stream().flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getValue)).map(a -> (String) a).toList();


            return Stream.of(new WorkspaceImpl(
              name.get(),
              JsonObjectUtils.getString(w, "metadata", LOGGER::error).map(Path::of),
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
               agents.stream().filter(a -> joinedBy.contains(a.getName())).collect(Collectors.toSet())
              ,
              JsonObjectUtils
                .getJsonArray(w, "artifacts", LOGGER::error)
                .stream()
                .flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getJsonObject))
                .<Artifact>flatMap(ar -> {
                  final var artifactName = JsonObjectUtils.getString(ar, NAME, LOGGER::error);
                  final var artifactClass = JsonObjectUtils.getString(ar, "class", LOGGER::error);
                  if (artifactName.isEmpty()) {
                    LOGGER.warn("Artifact in workspace missing name, skipping");
                    return Stream.empty();
                  }
                  if (artifactClass.map(c -> !artifactClasses.contains(c)).orElse(false)) {
                    LOGGER.warn(
                      "Artifact in workspace not having a known artifact class, skipping"
                    );
                    return Stream.empty();
                  }
                  final var representation =
                    JsonObjectUtils.getString(ar, "representation", LOGGER::error);
                  if (artifactClass.isEmpty() && representation.isEmpty()) {
                    LOGGER.warn(
                      "Artifact in workspace not having a class for creating it"
                        + " or a static representation, skipping"
                    );
                    return Stream.empty();
                  }
                  return Stream.of(new ArtifactImpl(
                    artifactName.get(),
                    artifactClass,
                    JsonObjectUtils
                      .getJsonArray(ar, "init-params", LOGGER::error)
                      .map(JsonArray::getList)
                      .orElse(Collections.emptyList()),
                    representation.map(Path::of),
                    JsonObjectUtils.getString(ar, "metadata", LOGGER::error).map(Path::of),
                    JsonObjectUtils.getJsonArray(ar, "focused-by", LOGGER::error)
                      .stream().flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getValue)).map(a -> (String) a).toList()
                  ));
                })
                .collect(Collectors.toSet()),
              JsonObjectUtils.getString(w, "representation", LOGGER::error).map(Path::of)
            ));
          })
          .toList()
      ),
      knownArtifacts
    );
  }

  private static List<Workspace> reorderWorkspaces(final List<Workspace> unorderedWorkspaces) {
    final var workspacesToVisit =
      unorderedWorkspaces.stream()
        .filter(w -> w.getParentName().isEmpty())
        .collect(Collectors.toCollection(LinkedList::new));
    final var orderedWorkspaces = new ArrayList<>(workspacesToVisit);
    while (!workspacesToVisit.isEmpty()) {
      final var currentWorkspace = workspacesToVisit.remove();
      final var childrenWorkspaces =
        unorderedWorkspaces.stream()
          .filter(w ->
            w.getParentName().isPresent()
              && w.getParentName().get().equals(currentWorkspace.getName())
          )
          .toList();
      workspacesToVisit.addAll(childrenWorkspaces);
      orderedWorkspaces.addAll(childrenWorkspaces);
    }
    return orderedWorkspaces;
  }

  private record EnvironmentImpl(
    List<YggdrasilAgent> agents,
    List<Workspace> workspaces,
    Set<KnownArtifact> knownArtifacts
  ) implements Environment {

    @Override
    public List<YggdrasilAgent> getAgents() {return this.agents();}

    @Override
    public List<Workspace> getWorkspaces() {
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
    Optional<Path> metaData,
    Optional<String> parentName,
    Set<YggdrasilAgent> joinedAgents,
    Set<Artifact> artifacts,
    Optional<Path> representation
  ) implements Workspace {
    @Override
    public String getName() {
      return this.name();
    }

    @Override
    public Optional<Path> getMetaData() {
      return this.metaData();
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
    public Set<YggdrasilAgent> getAgents() {
      return this.joinedAgents();
    }

    @Override
    public Optional<Path> getRepresentation() {
      return this.representation();
    }
  }

  private record ArtifactImpl(
    String name,
    Optional<String> clazz,
    List<?> initializationParameters,
    Optional<Path> representation,
    Optional<Path> metaData,
    List<String> focusedBy
  ) implements Artifact {
    @Override
    public String getName() {
      return this.name();
    }

    @Override
    public Optional<String> getClazz() {
      return this.clazz();
    }

    @Override
    public List<?> getInitializationParameters() {
      return this.initializationParameters();
    }

    @Override
    public Optional<Path> getRepresentation() {
      return this.representation();
    }

    @Override
    public Optional<Path> getMetaData() {
      return this.metaData();
    }

    @Override
    public List<String> getFocusedBy() {return this.focusedBy();}
  }

  private record AgentImpl(
    String name,
    String agentUri,
    String agentCallbackUri,
    List<AgentBody> bodies
  ) implements YggdrasilAgent {

    @Override
    public String getName() {
      return this.name();
    }

    @Override
    public String getAgentUri() {
      return this.agentUri();
    }

    @Override
    public String getAgentCallbackUri() {
      return this.agentCallbackUri();
    }

    @Override
    public List<AgentBody> getBodyConfig() {
      return this.bodies();
    }

  }

  private record AgentBodyImpl(
    Optional<String> metadata,
    List<String> joined
  ) implements AgentBody {

    @Override
    public Optional<String> getMetadata() {
      return this.metadata();
    }

    @Override
    public List<String> getJoinedWorkspaces() {
      return this.joined();
    }
  }


}
