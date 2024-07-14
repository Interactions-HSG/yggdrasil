package org.hyperagents.yggdrasil.oauth;


import java.io.IOException;
import java.net.URI;

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

}
