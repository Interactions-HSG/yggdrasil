package org.hyperagents.yggdrasil.oauth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OAuthHttpHandler {

    public static HttpClient client = HttpClient.newHttpClient();
    public static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static OpenIdClientMetadata baseClientMetadata = new OpenIdClientMetadata();

    public static OpenIdProviderMetadata discoverIssuer(URI issuerWellKnownEndpoint) throws IOException,
            InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(issuerWellKnownEndpoint)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), OpenIdProviderMetadata.class);
    }

    public static OpenIdClient register(OpenIdProviderMetadata metadata) throws IllegalAccessException, IOException,
            InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(metadata.getRegistration_endpoint())
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                baseClientMetadata.getInstantiatedFieldsAsJson()
                        )
                ).build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new OpenIdClient(mapper.readValue(response.body(), OpenIdClientMetadata.class));


    }


}
