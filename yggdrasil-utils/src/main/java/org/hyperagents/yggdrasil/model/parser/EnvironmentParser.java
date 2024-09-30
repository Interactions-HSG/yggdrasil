package org.hyperagents.yggdrasil.model.parser;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.model.impl.AgentBodyImpl;
import org.hyperagents.yggdrasil.model.impl.ArtifactImpl;
import org.hyperagents.yggdrasil.model.impl.EnvironmentImpl;
import org.hyperagents.yggdrasil.model.impl.KnownArtifactImpl;
import org.hyperagents.yggdrasil.model.impl.WorkspaceImpl;
import org.hyperagents.yggdrasil.model.impl.YggdrasilAgentImpl;
import org.hyperagents.yggdrasil.model.interfaces.AgentBody;
import org.hyperagents.yggdrasil.model.interfaces.Artifact;
import org.hyperagents.yggdrasil.model.interfaces.Environment;
import org.hyperagents.yggdrasil.model.interfaces.KnownArtifact;
import org.hyperagents.yggdrasil.model.interfaces.Workspace;
import org.hyperagents.yggdrasil.model.interfaces.YggdrasilAgent;
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
   * returns the List of agents specified in an Yggdrasil configuration file.
   *
   * @param config a valid Yggdrasil configuration
   * @return a List containing all agents
   */
  private static List<YggdrasilAgent> parseAgents(final JsonObject  config) {
    return JsonObjectUtils.getJsonArray(config, "agents", LOGGER::error)
        .stream().flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getValue)
          .flatMap(o -> (o instanceof JsonObject j ? Optional.of(j) : Optional.<JsonObject>empty())
            .stream()
          ))
        .<YggdrasilAgent>flatMap(o -> {
          final var name = JsonObjectUtils.getString(o, "name", LOGGER::error);
          final var agentUri = JsonObjectUtils.getString(o, "agent-uri", LOGGER::error);
          final var callbackUri = JsonObjectUtils.getString(o, "callback-uri", LOGGER::error);
          if (name.isEmpty() || agentUri.isEmpty()) {
            LOGGER.warn("Name or Uri of agent is empty, skipping");
            return Stream.empty();
          }

          final var bodies = JsonObjectUtils.getJsonArray(o, "body-config", LOGGER::error)
              .stream().flatMap(c -> IntStream.range(0, c.size()).mapToObj(c::getValue))
              .flatMap(q -> (q instanceof JsonObject s ? Optional.of(s) :
                Optional.<JsonObject>empty()).stream())
              .<AgentBody>flatMap(e -> {
                final var metadata = JsonObjectUtils.getString(e, "metadata", LOGGER::error);
                final var joined = JsonObjectUtils.getJsonArray(e, "join", LOGGER::error)
                    .stream().flatMap(q -> IntStream.range(0, q.size()).mapToObj(q::getValue))
                    .map(q -> (String) q).toList();

                if (metadata.isEmpty() && joined.isEmpty()) {
                  LOGGER.warn("Metadata and join is empty, skipping");
                  return Stream.empty();
                }

                return Stream.of(new AgentBodyImpl(metadata.orElse(null), joined));
              }).toList();

          return Stream.of(new YggdrasilAgentImpl(
              name.get(), agentUri.get(), callbackUri.orElse(null), bodies));
        }).collect(Collectors.toList());
  }

  private static Set<KnownArtifact> parseKnownArtifacts(final Optional<JsonObject> envConfig) {
    return envConfig
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
  }

  /**
   * parses the environment given a config.
   */
  public static Environment parse(final JsonObject config) {
    final var agents = parseAgents(config);

    final var envConfig =
        JsonObjectUtils.getJsonObject(config, "environment-config", LOGGER::error);
    final var knownArtifacts = parseKnownArtifacts(envConfig);

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
      agents,
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
                .stream().flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getValue))
                .map(a -> (String) a).toList();

            final Set<YggdrasilAgent> joiningAgents = new HashSet<>();
            agents.forEach(agent -> agent.getBodyConfig().forEach(body -> {
              if (body.getJoinedWorkspaces().contains(name.get())) {
                joiningAgents.add(agent);
              }
            })
            );

            if (!joiningAgents.stream().allMatch(a -> joinedBy.contains(a.getName()))
                || joinedBy.size() != joiningAgents.size()) {
              LOGGER.warn("Not all agents that have bodies specified as joining are listed in the"
                  + " joined-by of the workspace.");
            }

            // create a joined set so that it doesnt matter where it is specified that an agent
            // should join the workspace

            joinedBy.forEach(agentName -> joiningAgents.add(
                agents.stream().filter(agent -> agent.getName().equals(agentName))
                  .findFirst().orElseThrow()
            ));

            return Stream.of(new WorkspaceImpl(
              name.get(),
              JsonObjectUtils.getString(w, "metadata", LOGGER::error).orElse(null),
              JsonObjectUtils
                .getString(w, "parent-name", LOGGER::error)
                .filter(p -> {
                  if (!workspaceNames.contains(p)) {
                    LOGGER.warn(
                        "Workspace has an undefined parent without definition, ignoring parent name"
                    );
                  }
                  return workspaceNames.contains(p);
                }).orElse(null),
              joiningAgents,
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
                    artifactClass.orElse(null),
                    JsonObjectUtils
                      .getJsonArray(ar, "init-params", LOGGER::error)
                      .map(JsonArray::getList)
                      .orElse(Collections.emptyList()),
                    representation.orElse(null),
                    JsonObjectUtils.getString(ar, "metadata", LOGGER::error).orElse(null),
                    JsonObjectUtils.getJsonArray(ar, "focused-by", LOGGER::error)
                      .stream().flatMap(a -> IntStream.range(0, a.size()).mapToObj(a::getValue))
                      .map(a -> (String) a).toList()
                  ));
                })
                .collect(Collectors.toSet()),
              JsonObjectUtils.getString(w, "representation", LOGGER::error).orElse(null)
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
}
