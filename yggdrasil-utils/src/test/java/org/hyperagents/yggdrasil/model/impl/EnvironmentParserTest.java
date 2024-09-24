package org.hyperagents.yggdrasil.model.impl;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class EnvironmentParserTest {

  @Test
  void parse() throws IOException, URISyntaxException {
    // import test config file as jsonObject
    final var configString = Files.readString(
      Path.of(ClassLoader.getSystemResource("test_conf.json").toURI()),
      StandardCharsets.UTF_8
    );
    JsonObject config = new JsonObject(configString);

    final var environment = EnvironmentParser.parse(config);

    final var agents = environment.getAgents();
    final var workspaces = environment.getWorkspaces();
    final var knownArtifacts = environment.getKnownArtifacts();

    Assertions.assertEquals(1, agents.size());
    Assertions.assertEquals("agent_name", agents.getFirst().getName());
    Assertions.assertEquals("http://localhost:8081", agents.getFirst().getAgentUri());
    Assertions.assertEquals("http://localhost:8081/callback", agents.getFirst().getAgentCallbackUri());

  }
}
