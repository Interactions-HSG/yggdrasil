package org.hyperagents.yggdrasil.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class OAuthHttpHandler {

    public static HttpClient client = HttpClient.newHttpClient();
    public static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static OpenIdClientMetadata baseClientMetadata = new OpenIdClientMetadata();


    public static String getIssuerFromWebID(String WebID) throws IOException {
        URL documentUrl = URI.create(WebID).toURL();
        InputStream inputStream = documentUrl.openStream();

        String baseURI = documentUrl.toString();
        RDFFormat format = RDFFormat.TURTLE;
        String result = null;
        try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
            while (res.hasNext()) {
                Statement st = res.next();
                if (st.getPredicate().toString().equals("http://www.w3.org/ns/solid/terms#oidcIssuer")) {
                    result = st.getObject().toString();
                    break;
                }
            }
        } catch (RDF4JException e) {
            result = "RDF parse Error";
        } finally {
            inputStream.close();
        }

        return result == null ? "Issuer not found" : result;

    }

    public static OpenIdProviderMetadata discoverIssuer(URI issuerWellKnownEndpoint) throws IOException,
            InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(issuerWellKnownEndpoint)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), OpenIdProviderMetadata.class);
    }

    public static CompletableFuture<OpenIdProviderMetadata> getOpenIdProviderMetadataAsync(URI issuerWellKnownEndpoint) {
        return discoverIssuerAsync(issuerWellKnownEndpoint)
                .handle(
                        (response, error) -> {
                            if (error != null) {
                                return null;
                            }
                            try {
                                return mapper.readValue(response.body(), OpenIdProviderMetadata.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

    }


    public static CompletableFuture<HttpResponse<String>> discoverIssuerAsync(URI issuerWellKnownEndpoint) {
        return client.sendAsync(
                HttpRequest.newBuilder()
                        .uri(issuerWellKnownEndpoint)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
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


    public void createDPoPHeader() {

    }


}
