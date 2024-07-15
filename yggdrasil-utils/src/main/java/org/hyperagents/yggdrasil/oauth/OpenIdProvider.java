package org.hyperagents.yggdrasil.oauth;


import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OpenIdProvider {

  private final static String WELL_KNOWN_ENPDOINT = "/.well-known/openid-configuration";
  private final String issuer;
  private OpenIdProviderMetadata metadata;
  private OpenIdClient client;


  public OpenIdProvider(String issuer) throws IOException, InterruptedException {
    this.issuer = issuer;
    setMetadata();
  }

  public String getIssuer() {
    return issuer;
  }

  private void setMetadata() throws IOException, InterruptedException {
    metadata = OAuthHttpHandler.discoverIssuer(URI.create(issuer + WELL_KNOWN_ENPDOINT));
  }

  public OpenIdProviderMetadata getMetadata() {
    return metadata;
  }

  public OpenIdClient register() throws IOException, InterruptedException, IllegalAccessException {
    this.client = OAuthHttpHandler.register(metadata);
    return this.client;
  }

  public String getAuthUrl(String state, String challenge) {
    return metadata.getAuthorization_endpoint() + "?"
      + "client_id=" + URLEncoder.encode(client.getClient_id(), StandardCharsets.UTF_8) + "&"
      + "redirect_uri=" + URLEncoder.encode(client.getMetadata().getRedirect_uris().getFirst(),
      StandardCharsets.UTF_8) + "&"
      + "response_type=" + URLEncoder.encode("code", StandardCharsets.UTF_8) + "&"
      + "scope=" + URLEncoder.encode("openid offline_access webid", StandardCharsets.UTF_8) + "&"
      + "state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) + "&"
      + "code_challenge=" + URLEncoder.encode(challenge, StandardCharsets.UTF_8) + "&"
      + "code_challenge_method=" + URLEncoder.encode("S256", StandardCharsets.UTF_8) + "&"
      + "prompt=" + URLEncoder.encode("consent", StandardCharsets.UTF_8) + "&"
      + "response_mode=" + URLEncoder.encode("query", StandardCharsets.UTF_8);
  }
}
