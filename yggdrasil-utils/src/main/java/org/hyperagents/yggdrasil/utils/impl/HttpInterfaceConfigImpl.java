package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import java.util.Optional;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class HttpInterfaceConfigImpl implements HttpInterfaceConfig {
  private final String host;
  private final String baseUri;
  private final int port;
  private final Optional<String> webSubHubUri;

  public HttpInterfaceConfigImpl(final JsonObject config) {
    final var httpConfig = Optional.ofNullable(config.getJsonObject("http-config"));
    this.host = httpConfig.map(c -> c.getString("host", "0.0.0.0")).orElse("0.0.0.0");
    this.port = httpConfig.map(c -> c.getInteger("port", 8080)).orElse(8080);
    this.webSubHubUri = httpConfig.map(c -> c.getString("websub-hub-uri"));
    this.baseUri =
      httpConfig
        .map(c -> c.getString("base-uri"))
        // Strip away the trailing slash (if any)
        .map(u -> u.endsWith("/") ? u.substring(0, u.length() - 1) : u)
        .orElseGet(() -> "http://"
                         + (this.host.equals("0.0.0.0") ? "localhost" : this.host)
                         + ":"
                         + this.port);
  }

  @Override
  public String getHost() {
    return this.host;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public String getBaseUri() {
    return this.baseUri;
  }

  @Override
  public Optional<String> getWebSubHubUri() {
    return this.webSubHubUri;
  }

  @Override
  public String getWorkspacesUri() {
    return this.baseUri + "/workspaces";
  }

  @Override
  public String getWorkspaceUri(final String workspaceName) {
    return this.getWorkspacesUri() + "/" + workspaceName;
  }

  @Override
  public String getArtifactsUri(final String workspaceName) {
    return this.getWorkspaceUri(workspaceName) + "/artifacts";
  }

  @Override
  public String getArtifactUri(final String workspaceName, final String artifactName) {
    return this.getArtifactsUri(workspaceName) + "/" + artifactName;
  }

  @Override
  public String getAgentBodiesUri(final String workspaceName) {
    return this.getWorkspaceUri(workspaceName) + "/agents";
  }

  @Override
  public String getAgentBodyUri(final String workspaceName, final String agentName) {
    return this.getAgentBodiesUri(workspaceName) + "/" + agentName;
  }
}
