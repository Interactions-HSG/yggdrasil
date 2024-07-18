package org.hyperagents.yggdrasil.oauth;


import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenIdProviders {

    private final Map<URI, OpenIdProvider> providerMap;

    public OpenIdProviders() {
        this.providerMap = new ConcurrentHashMap<>();
    }

    public synchronized OpenIdProvider useProvider(URI issuer) throws IOException, InterruptedException {
        if (providerMap.containsKey(issuer)) {
            return providerMap.get(issuer);
        }
        OpenIdProvider newProvider = new OpenIdProvider(issuer.toString());
        providerMap.put(issuer, newProvider);
        return newProvider;
    }

    public Map<URI, OpenIdProvider> getProviderMap() {
        return providerMap;
    }
}
