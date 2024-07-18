package org.hyperagents.yggdrasil.oauth;

public class OpenIdClient {

    // REQUIRED. Unique Client Identifier. It MUST NOT be currently valid for any other registered Client.
    private final String client_id;

    // OPTIONAL. Client Secret. The same Client Secret value MUST NOT be assigned to multiple Clients. This value is
    // used by Confidential Clients to authenticate to the Token Endpoint, as described in Section 2.3.1 of OAuth 2
    // .0, and for the derivation of symmetric encryption key values, as described in Section 10.2 of OpenID Connect
    // Core 1.0 [OpenID.Core]. It is not needed for Clients selecting a token_endpoint_auth_method of private_key_jwt
    // unless symmetric encryption will be used.
    private final String client_secret;

    // OPTIONAL. Registration Access Token that can be used at the Client Configuration Endpoint to perform
    // subsequent operations upon the Client registration.
    private final String registration_access_token;

    // OPTIONAL. Location of the Client Configuration Endpoint where the Registration Access Token can be used to
    // perform subsequent operations upon the resulting Client registration. This URL MUST use the https scheme.
    // Implementations MUST either return both a Client Configuration Endpoint and a Registration Access Token or
    // neither of them.
    private final String registration_client_uri;

    // OPTIONAL. Time at which the Client Identifier was issued. Its value is a JSON number representing the number
    // of seconds from 1970-01-01T00:00:00Z as measured in UTC until the date/time.
    private final String client_id_issued_at;

    // REQUIRED if client_secret is issued. Time at which the client_secret will expire or 0 if it will not expire.
    // Its value is a JSON number representing the number of seconds from 1970-01-01T00:00:00Z as measured in UTC
    // until the date/time.
    private final String client_secret_expires_at;

    private final OpenIdClientMetadata metadata;

    public OpenIdClient(OpenIdClientMetadata metadata) {
        this.client_id = metadata.getClient_id();
        this.client_id_issued_at = metadata.getClient_id_issued_at();
        this.client_secret = metadata.getClient_secret();
        this.client_secret_expires_at = metadata.getClient_secret_expires_at();
        this.registration_access_token = metadata.getRegistration_access_token();
        this.registration_client_uri = metadata.getRegistration_client_uri();
        this.metadata = metadata;

    }



    public OpenIdClientMetadata getMetadata() {
        return metadata;
    }

    public String getClient_id() {
        return client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }


    public String getRegistration_access_token() {
        return registration_access_token;
    }


    public String getRegistration_client_uri() {
        return registration_client_uri;
    }


    public String getClient_id_issued_at() {
        return client_id_issued_at;
    }


    public String getClient_secret_expires_at() {
        return client_secret_expires_at;
    }

}
