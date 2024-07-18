package org.hyperagents.yggdrasil.oauth;


import org.apache.commons.lang3.builder.Builder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

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


    private CompletableFuture<OpenIdProviderMetadata> setMetadataAsync() {
        return OAuthHttpHandler.getOpenIdProviderMetadataAsync(URI.create(issuer + WELL_KNOWN_ENPDOINT)).handle(
                (metadata, error) -> {
                    if (error != null) {
                        return null;
                    }
                    this.metadata = metadata;
                    return metadata;
                }
        );

    }

    public OpenIdProviderMetadata getMetadata() {
        return metadata;
    }

    public void register() throws IOException, InterruptedException, IllegalAccessException {
        this.client = OAuthHttpHandler.register(metadata);
    }

    public String getAuthUrl(String state, String challenge) {
        return new AuthUrlBuilder(metadata.getAuthorization_endpoint())
                .setClientId(client.getClient_id())
                .setRedirectUri(client.getMetadata().getRedirect_uris().getFirst())
                .setResponseType("code")
                .setScope("openid offline_access webid")
                .setState(state)
                .setCodeChallenge(challenge)
                .setCodeChallengeMethod("S256")
                .setPrompt("consent")
                .setResponseMode("query")
                .build()
                .toString();
    }

    public static class AuthUrl {
        private URI endpoint;
        private String client_id;
        private String redirect_uri;
        private String response_type;
        private String scope;
        private String state;
        private String code_challenge;
        private String code_challenge_method;
        private String prompt;
        private String response_mode;

        @Override
        public String toString() {
            return endpoint + "?"
                    + "client_id=" + client_id + "&"
                    + "redirect_uri=" + redirect_uri + "&"
                    + "response_type=" + response_type + "&"
                    + "scope=" + scope + "&"
                    + "state=" + state + "&"
                    + "code_challenge=" + code_challenge + "&"
                    + "code_challenge_method=" + code_challenge_method + "&"
                    + "prompt=" + prompt + "&"
                    + "response_mode=" + response_mode;
        }
    }

    public static class AuthUrlBuilder implements Builder<AuthUrl> {
        private final AuthUrl url;

        public AuthUrlBuilder(URI endpoint) {
            this.url = new AuthUrl();
            this.url.endpoint = endpoint;
        }

        public AuthUrlBuilder setClientId(String client_id) {
            this.url.client_id = URLEncoder.encode(client_id, StandardCharsets.UTF_8);
            return this; // Reference returned so calls can be chained
        }

        public AuthUrlBuilder setRedirectUri(String redirectUri) {
            this.url.redirect_uri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setResponseType(String responseType) {
            this.url.response_type = URLEncoder.encode(responseType, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setScope(String scope) {
            this.url.scope = URLEncoder.encode(scope, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setState(String state) {
            this.url.state = URLEncoder.encode(state, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setCodeChallenge(String codeChallenge) {
            this.url.code_challenge = URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setCodeChallengeMethod(String codeChallengeMethod) {
            this.url.code_challenge_method = URLEncoder.encode(codeChallengeMethod, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setPrompt(String prompt) {
            this.url.prompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrlBuilder setResponseMode(String responseMode) {
            this.url.response_mode = URLEncoder.encode(responseMode, StandardCharsets.UTF_8);
            return this; // Reference
        }

        public AuthUrl build() {
            return this.url;
        }
    }
}
