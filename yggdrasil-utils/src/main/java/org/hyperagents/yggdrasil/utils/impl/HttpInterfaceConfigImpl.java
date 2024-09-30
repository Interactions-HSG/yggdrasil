package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
  private final String baseUriTrailingSlash;
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

    final String baseUri1;
    baseUri1 = httpConfig.flatMap(c -> JsonObjectUtils.getString(c, "base-uri", LOGGER::error))
        .orElseGet(()
            -> "http://" + (this.host.equals("0.0.0.0") ? "localhost" : this.host) + ":"
            + this.port + "/");
    this.baseUriTrailingSlash = baseUri1.endsWith("/") ? baseUri1 : baseUri1 + "/";
    this.baseUri = baseUri1.endsWith("/") ? baseUri1.substring(0, baseUri1.length() - 1) : baseUri1;
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
  public String getBaseUriTrailingSlash() {
    return this.baseUriTrailingSlash;
  }

  @Override
  public String getBaseUri() {
    return this.baseUri;
  }

  @Override
  public String getWorkspacesUri() {
    return this.baseUriTrailingSlash + "workspaces/";
  }

  @Override
  public String getWorkspaceUriTrailingSlash(final String workspaceName) {
    return this.getWorkspacesUri() + validateInput(workspaceName) + "/";
  }

  @Override
  public String getWorkspaceUri(final String workspaceName) {
    return this.getWorkspacesUri() + validateInput(workspaceName);
  }

  @Override
  public String getArtifactsUri(final String workspaceName) {
    return this.getWorkspaceUriTrailingSlash(workspaceName) + "artifacts/";
  }

  @Override
  public String getArtifactUriTrailingSlash(final String workspaceName, final String artifactName) {
    final var cleanArtifactName = validateInput(artifactName);
    return this.getArtifactsUri(workspaceName) + cleanArtifactName + "/";
  }

  @Override
  public String getArtifactUri(final String workspaceName, final String artifactName) {
    final var cleanArtifactName = validateInput(artifactName);
    return this.getArtifactsUri(workspaceName) + cleanArtifactName;
  }

  @Override
  public String getAgentBodiesUri(final String workspaceName) {
    return this.getWorkspaceUriTrailingSlash(workspaceName) + "artifacts/";
  }

  @Override
  public String getAgentBodyUriTrailingSlash(final String workspaceName, final String agentName) {
    final var cleanAgentName = validateInput(agentName);
    return this.getAgentBodiesUri(workspaceName) + "body_" + cleanAgentName + "/";
  }

  @Override
  public String getAgentBodyUri(final String workspaceName, final String agentName) {
    final var cleanAgentName = validateInput(agentName);
    return this.getAgentBodiesUri(workspaceName) + "body_" + cleanAgentName;
  }

  @Override
  public String getAgentUri(final String agentName) {
    final var cleanAgentName = validateInput(agentName);
    return this.baseUriTrailingSlash + "artifacts/" + cleanAgentName + "/";
  }


  // TODO: Add better validation

  /**
   * Validate the input string by removing any slashes.
   * The name cannot have any slashes since we use them as separators in the URI.
   *
   * @param stringInput The input string to validate.
   * @return The validated string.
   */
  private String validateInput(final String stringInput) {
    if (stringInput == null) {
      return "";
    }
    return stringInput.replaceAll("/", "");
  }
}

