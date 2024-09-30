package org.hyperagents.yggdrasil.model.impl;


import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.hyperagents.yggdrasil.model.interfaces.Artifact;
import org.hyperagents.yggdrasil.model.interfaces.KnownArtifact;
import org.hyperagents.yggdrasil.model.interfaces.Workspace;
import org.hyperagents.yggdrasil.model.interfaces.YggdrasilAgent;
import org.hyperagents.yggdrasil.model.parser.EnvironmentParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class EnvironmentParserTest {

  private static final String METADATA_AGENT_BODY = "src/test/resources/a1_test_metadata.ttl";
  private static final String METADATA_WORKSPACE = "src/test/resources/w1_test_metadata.ttl";
  private static final String METADATA_ARTIFACT = "src/test/resources/c1_test_metadata.ttl";

  @Test
  void parse() throws IOException, URISyntaxException {
    // import test config file as jsonObject
    final var configString = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_conf.json").toURI()),
        StandardCharsets.UTF_8
    );
    final var environment = EnvironmentParser.parse(new JsonObject(configString));

    // expected outcomes
    final var expectedListOfAgents = new ArrayList<YggdrasilAgent>();
    expectedListOfAgents.add(
        new YggdrasilAgentImpl(
            "test_name",
            "http://localhost:8081",
            "http://localhost:8081/callback",
            List.of(
                new AgentBodyImpl(METADATA_AGENT_BODY, List.of("w1")),
                new AgentBodyImpl(METADATA_AGENT_BODY, List.of())
            )
        )
    );

    final var expectedListOfArtifacts = new ArrayList<Artifact>();
    expectedListOfArtifacts.add(
        new ArtifactImpl(
            "c1",
            "http://example.org/Counter",
            List.of(),
            null,
            METADATA_ARTIFACT,
            List.of("test_name")
        )
    );


    final var expectedListOfWorkspaces = new ArrayList<Workspace>();
    expectedListOfWorkspaces.add(
        new WorkspaceImpl(
            "w1",
            METADATA_WORKSPACE,
            null,
            new HashSet<>(expectedListOfAgents),
            new HashSet<>(expectedListOfArtifacts),
            null
        )
    );

    final var expectedListOfKnownArtifacts = new HashSet<KnownArtifact>();
    expectedListOfKnownArtifacts.add(
        new KnownArtifactImpl(
            "http://example.org/Counter",
            "org.hyperagents.yggdrasil.cartago.artifacts.CounterTD"
        )
    );

    final var expectedEnvironment = new EnvironmentImpl(expectedListOfAgents,
        expectedListOfWorkspaces, expectedListOfKnownArtifacts);

    Assertions.assertEquals(expectedListOfAgents, environment.getAgents(),
        "Agents are not equal");
    Assertions.assertEquals(expectedListOfWorkspaces, environment.getWorkspaces(),
        "Workspaces are not equal");
    Assertions.assertEquals(expectedListOfKnownArtifacts, environment.getKnownArtifacts(),
        "KnownArtifacts are not equal");
    Assertions.assertEquals(expectedEnvironment, environment,
        "Environments are not equal");
  }
}
