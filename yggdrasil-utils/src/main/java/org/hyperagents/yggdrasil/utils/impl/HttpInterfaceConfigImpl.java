package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

/**
 * Implementation of the HttpInterfaceConfig interface
 * that provides configuration for an HTTP interface.
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class HttpInterfaceConfigImpl implements HttpInterfaceConfig {
  private static final Logger LOGGER = LogManager.getLogger(HttpInterfaceConfigImpl.class);

  private final String host;
  private final String baseUri;
  private final int port;

  /**
   * Constructs a new HttpInterfaceConfigImpl object with the specified configuration.
   *
   * @param config The JSON configuration object.
   */
  public HttpInterfaceConfigImpl(final JsonObject config) {
    final var httpConfig = JsonObjectUtils.getJsonObject(config, "http-config", LOGGER::error);
    this.host = httpConfig.flatMap(c -> JsonObjectUtils.getString(c, "host", LOGGER::error))
                          .orElse("0.0.0.0");
    this.port = httpConfig.flatMap(c -> JsonObjectUtils.getInteger(c, "port", LOGGER::error))
                          .orElse(8080);
    this.baseUri =
            httpConfig
                    .flatMap(c -> JsonObjectUtils.getString(c, "base-uri", LOGGER::error)).orElseGet(()
                -> "http://" + (this.host.equals("0.0.0.0") ? "localhost" : this.host) + ":" + this.port + "/");
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
  public String getWorkspacesUri() {
    return this.baseUri + "workspaces/";
  }

  @Override
  public String getWorkspaceUri(final String workspaceName) {
    return this.getWorkspacesUri() + workspaceName + "/";
  }

  @Override
  public String getArtifactsUri(final String workspaceName) {
    return this.getWorkspaceUri(workspaceName) + "artifacts/";
  }

  @Override
  public String getArtifactUri(final String workspaceName, final String artifactName) {
    return this.getArtifactsUri(workspaceName) + artifactName + "/";
  }

  @Override
  public String getAgentBodiesUri(final String workspaceName) {
    return this.getWorkspaceUri(workspaceName) + "agents/";
  }

  @Override
  public String getAgentBodyUri(final String workspaceName, final String agentName) {
    return this.getAgentBodiesUri(workspaceName) + agentName + "/";
  }

  @Override
  public String getAgentUri(final String agentName) {
    return this.baseUri + "/agents/" + agentName + "/";
  }
}
