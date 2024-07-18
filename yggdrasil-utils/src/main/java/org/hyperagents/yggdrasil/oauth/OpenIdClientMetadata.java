package org.hyperagents.yggdrasil.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.util.List;

public class OpenIdClientMetadata {
    // REQUIRED. Unique Client Identifier. It MUST NOT be currently valid for any other registered Client.
    private String client_id;

    // OPTIONAL. Client Secret. The same Client Secret value MUST NOT be assigned to multiple Clients. This value is
    // used by Confidential Clients to authenticate to the Token Endpoint, as described in Section 2.3.1 of OAuth 2
    // .0, and for the derivation of symmetric encryption key values, as described in Section 10.2 of OpenID Connect
    // Core 1.0 [OpenID.Core]. It is not needed for Clients selecting a token_endpoint_auth_method of private_key_jwt
    // unless symmetric encryption will be used.
    private String client_secret;

    // OPTIONAL. Registration Access Token that can be used at the Client Configuration Endpoint to perform
    // subsequent operations upon the Client registration.
    private String registration_access_token;

    // OPTIONAL. Location of the Client Configuration Endpoint where the Registration Access Token can be used to
    // perform subsequent operations upon the resulting Client registration. This URL MUST use the https scheme.
    // Implementations MUST either return both a Client Configuration Endpoint and a Registration Access Token or
    // neither of them.
    private String registration_client_uri;

    // OPTIONAL. Time at which the Client Identifier was issued. Its value is a JSON number representing the number
    // of seconds from 1970-01-01T00:00:00Z as measured in UTC until the date/time.
    private String client_id_issued_at;

    // REQUIRED if client_secret is issued. Time at which the client_secret will expire or 0 if it will not expire.
    // Its value is a JSON number representing the number of seconds from 1970-01-01T00:00:00Z as measured in UTC
    // until the date/time.
    private String client_secret_expires_at;
    public  final List<String> redirect_uris = List.of("http://localhost:8080/callback");
    // REQUIRED. Array of Redirection URI values used by the Client. One of these registered Redirection URI values
    // MUST exactly match the redirect_uri parameter value used in each Authorization Request, with the matching
    // performed as described in Section 6.2.1 of [RFC3986] (Simple String Comparison).
    public  final List<String> response_types = List.of("code");
    // OPTIONAL. JSON [RFC8259] array containing a list of the OAuth 2.0 response_type values that the Client is
    // declaring that it will restrict itself to using. If omitted, the default is that the Client will use only the
    // code Response Type
    public  List<String> grant_types = List.of("authorization_code","refresh_token");
    // OPTIONAL. JSON array containing a list of the OAuth 2.0 Grant Types that the Client is declaring that it will
    // restrict itself to using. The Grant Type values used by OpenID Connect are:
    // authorization_code: The Authorization Code Grant Type described in OAuth 2.0 Section 4.1.
    // implicit: The Implicit Grant Type described in OAuth 2.0 Section 4.2.
    // refresh_token: The Refresh Token Grant Type described in OAuth 2.0 Section 6.
    // The following table lists the correspondence between response_type values that the Client will use and
    // grant_type values that MUST be included in the registered grant_types list:
    // code: authorization_code
    // id_token: implicit
    // id_token token: implicit
    // code id_token: authorization_code, implicit
    // code token: authorization_code, implicit
    // code id_token token: authorization_code, implicit
    // If omitted, the default is that the Client will use only the authorization_code Grant Type.
    public  String application_type = "web";
    // OPTIONAL. Kind of the application. The default, if omitted, is web. The defined values are native or web. Web
    // Clients using the OAuth Implicit Grant Type MUST only register URLs using the https scheme as redirect_uris;
    // they MUST NOT use localhost as the hostname. Native Clients MUST only register redirect_uris using custom URI
    // schemes or loopback URLs using the http scheme; loopback URLs use localhost or the IP loopback literals 127.0
    // .0.1 or [::1] as the hostname. Authorization Servers MAY place additional constraints on Native Clients.
    // Authorization Servers MAY reject Redirection URI values using the http scheme, other than the loopback case
    // for Native Clients. The Authorization Server MUST verify that all the registered redirect_uris conform to
    // these constraints. This prevents sharing a Client ID across different types of Clients.
    public  List<String> contacts;
    // OPTIONAL. Array of e-mail addresses of people responsible for this Client. This might be used by some providers
    // to enable a Web user interface to modify the Client information.
    public  String client_name = "Yggdrasil";
    // OPTIONAL. Name of the Client to be presented to the End-User. If desired,
    // representation of this Claim in different languages and scripts is represented as described in Section 2.1.
    public  String logo_uri;
    // OPTIONAL. URL that references a logo for the Client application. If present,
    // the server SHOULD display this image to the End-User during approval. The value of this field MUST point to a
    // valid image file. If desired, representation of this Claim in different languages and scripts is represented
    // as described in Section 2.1.
    public  String client_uri;
    // OPTIONAL. URL of the home page of the Client. The value of this field MUST point to a valid Web page. If present
    // the server SHOULD display this URL to the End-User in a followable fashion. If desired,
    // representation of this Claim in different languages and scripts is represented as described in Section 2.1.

    public  String policy_uri;
    // OPTIONAL. URL that the Relying Party Client provides to the End-User to read about how the profile data will be
    // used. The value of this field MUST point to a valid web page. The OpenID Provider SHOULD display this URL to
    // the End-User if it is given. If desired, representation of this Claim in different languages and scripts is
    // represented as described in Section 2.1.
    public  String tos_uri;
    // OPTIONAL. URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
    // service. The value of this field MUST point to a valid web page. The OpenID Provider SHOULD display this URL
    // to the End-User if it is given. If desired, representation of this Claim in different languages and scripts is
    // represented as described in Section 2.1.
    public  String jwks_uri;
    // OPTIONAL. URL for the Client's JWK Set [JWK] document, which MUST use the https scheme. If the Client signs
    // requests to the Server, it contains the signing key(s) the Server uses to validate signatures from the Client.
    // The JWK Set MAY also contain the Client's encryption keys(s), which are used by the Server to encrypt
    // responses to the Client. When both signing and encryption keys are made available, a use (public key use)
    // parameter value is REQUIRED for all keys in the referenced JWK Set to indicate each key's intended usage.
    // Although some algorithms allow the same key to be used for both signatures and encryption, doing so is NOT
    // RECOMMENDED, as it is less secure. The JWK x5c parameter MAY be used to provide X.509 representations of keys
    // provided. When used, the bare key values MUST still be present and MUST match those in the certificate. The
    // JWK Set MUST NOT contain private or symmetric key values.
    public  String jwks;
    // OPTIONAL. Client's JWK Set [JWK] document, passed by value. The semantics of the jwks parameter are the same as
    // the jwks_uri parameter, other than that the JWK Set is passed by value, rather than by reference. This
    // parameter is intended only to be used by Clients that, for some reason, are unable to use the jwks_uri
    // parameter, for instance, by native applications that might not have a location to host the contents of the JWK
    // Set. If a Client can use jwks_uri, it MUST NOT use jwks. One significant downside of jwks is that it does not
    // enable key rotation (which jwks_uri does, as described in Section 10 of OpenID Connect Core 1.0 [OpenID.Core])
    // . The jwks_uri and jwks parameters MUST NOT be used together. The JWK Set MUST NOT contain private or
    // symmetric key values.
    public  String sector_identifier_uri;
    // OPTIONAL. URL using the https scheme to be used in calculating Pseudonymous Identifiers by the OP. The URL
    // references a file with a single JSON array of redirect_uri values. Please see Section 5. Providers that use
    // pairwise sub (subject) values SHOULD utilize the sector_identifier_uri value provided in the Subject
    // Identifier calculation for pairwise identifiers.
    public  String subject_type;
    // OPTIONAL. subject_type requested for responses to this Client. The subject_types_supported discovery parameter
    // contains a list of the supported subject_type values for the OP. Valid types include pairwise and public.

    public  String id_token_signed_response_alg;
    // OPTIONAL. JWS alg algorithm [JWA] REQUIRED for signing the ID Token issued to this Client. The value none MUST
    // NOT be used as the ID Token alg value unless the Client uses only Response Types that return no ID Token from
    // the Authorization Endpoint (such as when only using the Authorization Code Flow). The default, if omitted, is
    // RS256. The public key for validating the signature is provided by retrieving the JWK Set referenced by the
    // jwks_uri element from OpenID Connect Discovery 1.0 [OpenID.Discovery].

    public  String id_token_encrypted_response_alg;
    // OPTIONAL. JWE alg algorithm [JWA] REQUIRED for encrypting the ID Token issued to this Client. If this is
    // requested, the response will be signed then encrypted, with the result being a Nested JWT, as defined in [JWT]
    // . The default, if omitted, is that no encryption is performed.

    public  String id_token_encrypted_response_enc;
    // OPTIONAL. JWE enc algorithm [JWA] REQUIRED for encrypting the ID Token issued to this Client. If
    // id_token_encrypted_response_alg is specified, the default id_token_encrypted_response_enc value is
    // A128CBC-HS256. When id_token_encrypted_response_enc is included, id_token_encrypted_response_alg MUST also be
    // provided.

    public  String userinfo_signed_response_alg;
    // OPTIONAL. JWS alg algorithm [JWA] REQUIRED for signing UserInfo Responses. If this is specified,
    // the response will be JWT [JWT] serialized, and signed using JWS. The default, if omitted, is for the UserInfo
    // Response to return the Claims as a UTF-8 [RFC3629] encoded JSON object using the application/json content-type.

    public  String userinfo_encrypted_response_alg;
    // OPTIONAL. JWE [JWE] alg algorithm [JWA] REQUIRED for encrypting UserInfo Responses. If both signing and
    // encryption are requested, the response will be signed then encrypted, with the result being a Nested JWT, as
    // defined in [JWT]. The default, if omitted, is that no encryption is performed.

    public  String userinfo_encrypted_response_enc;
    // OPTIONAL. JWE enc algorithm [JWA] REQUIRED for encrypting UserInfo Responses. If
    // userinfo_encrypted_response_alg is specified, the default userinfo_encrypted_response_enc value is
    // A128CBC-HS256. When userinfo_encrypted_response_enc is included, userinfo_encrypted_response_alg MUST also be
    // provided.

    public  String request_object_signing_alg;
    // OPTIONAL. JWS [JWS] alg algorithm [JWA] that MUST be used for signing Request Objects sent to the OP. All
    // Request Objects from this Client MUST be rejected, if not signed with this algorithm. Request Objects are
    // described in Section 6.1 of OpenID Connect Core 1.0 [OpenID.Core]. This algorithm MUST be used both when the
    // Request Object is passed by value (using the request parameter) and when it is passed by reference (using the
    // request_uri parameter). Servers SHOULD support RS256. The value none MAY be used. The default, if omitted, is
    // that any algorithm supported by the OP and the RP MAY be used.

    public  String request_object_encryption_alg;
    // OPTIONAL. JWE [JWE] alg algorithm [JWA] the RP is declaring that it may use for encrypting Request Objects sent
    // to the OP. This parameter SHOULD be included when symmetric encryption will be used, since this signals to the
    // OP that a client_secret value needs to be returned from which the symmetric key will be derived, that might
    // not otherwise be returned. The RP MAY still use other supported encryption algorithms or send unencrypted
    // Request Objects, even when this parameter is present. If both signing and encryption are requested, the
    // Request Object will be signed then encrypted, with the result being a Nested JWT, as defined in [JWT]. The
    // default, if omitted, is that the RP is not declaring whether it might encrypt any Request Objects.

    public  String request_object_encryption_enc;
    // OPTIONAL. JWE enc algorithm [JWA] the RP is declaring that it may use for encrypting Request Objects sent to
    // the OP. If request_object_encryption_alg is specified, the default request_object_encryption_enc value is
    // A128CBC-HS256. When request_object_encryption_enc is included, request_object_encryption_alg MUST also be
    // provided.

    public  String token_endpoint_auth_method;
    // OPTIONAL. Requested Client Authentication method for the Token Endpoint. The options are client_secret_post,
    //client_secret_basic, client_secret_jwt, private_key_jwt, and none,
    // as described in Section 9 of OpenID Connect Core 1.0 [OpenID.Core]. Other authentication methods MAY be defined
    // by extensions. If omitted, the default is client_secret_basic -- the HTTP Basic Authentication Scheme
    // specified in Section 2.3.1 of OAuth 2.0 [RFC6749].

    public  String token_endpoint_auth_signing_alg;
    // OPTIONAL. JWS [JWS] alg algorithm [JWA] that MUST be used for signing the JWT [JWT] used to authenticate the
    // Client at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods. All Token
    // Requests using these authentication methods from this Client MUST be rejected, if the JWT is not signed with
    // this algorithm. Servers SHOULD support RS256. The value none MUST NOT be used. The default, if omitted, is
    // that any algorithm supported by the OP and the RP MAY be used.

    public  List<String> default_max_age;
    // OPTIONAL. Default Maximum Authentication Age. Specifies that the End-User MUST be actively authenticated if the
    // End-User was authenticated longer ago than the specified number of seconds. The max_age request parameter
    // overrides this default value. If omitted, no default Maximum Authentication Age is specified.

    public  List<String> require_auth_time;
    // public. Boolean value specifying whether the auth_time Claim in the ID Token is REQUIRED. It is REQUIRED when
    // the value is true. (If this is false, the auth_time Claim can still be dynamically requested as an individual
    // Claim for the ID Token using the claims request parameter described in Section 5.5.1 of OpenID Connect Core 1
    // .0 [OpenID.Core].) If omitted, the default value is false.

    public  List<String> default_acr_values;
    // OPTIONAL. Default requested Authentication Context Class Reference values. Array of strings that specifies the
    // default acr values that the OP is being requested to use for processing requests from this Client, with the
    // values appearing in order of preference. The Authentication Context Class satisfied by the authentication
    // performed is returned as the acr Claim Value in the issued ID Token. The acr Claim is requested as a Voluntary
    // Claim by this parameter. The acr_values_supported discovery element contains a list of the supported acr
    // values supported by the OP. Values specified in the acr_values request parameter or an individual acr Claim
    // request override these default values.

    public  String initiate_login_uri;
    // OPTIONAL. URI using the https scheme that a third party can use to initiate a login by the RP,
    // as specified in Section 4 of OpenID Connect Core 1.0 [OpenID.Core]. The URI MUST accept requests via both GET
    // and POST. The Client MUST understand the login_hint and iss parameters and SHOULD support the target_link_uri
    // parameter.

    public  List<String> request_uris;
    // OPTIONAL. Array of request_uri values that are pre-registered by the RP for use at the OP. These URLs MUST use
    // the https scheme unless the target Request Object is signed in a way that is verifiable by the OP. Servers MAY
    // cache the contents of the files referenced by these URIs and not retrieve them at the time they are used in a
    // request. OPs can require that request_uri values used be pre-registered with the
    // require_request_uri_registration discovery parameter.
    // If the contents of the request file could ever change,
    // these URI values SHOULD include the base64url-encoded SHA-256 hash value of the file contents referenced by the
    // URI as the value of the URI fragment. If the fragment value used for a URI changes, that signals the server
    // that its cached value for that URI with the old fragment value is no longer valid.

    public  String getInstantiatedFieldsAsJson() throws IllegalAccessException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonObject = objectMapper.createObjectNode();
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(this);
            if (value != null) {
                jsonObject.putPOJO(field.getName(), value);
            }
        }

        return jsonObject.toString();

    }

    public List<String> getRedirect_uris() {
        return redirect_uris;
    }

    public List<String> getResponse_types() {
        return response_types;
    }

    public List<String> getGrant_types() {
        return grant_types;
    }

    public void setGrant_types(List<String> grant_types) {
        this.grant_types = grant_types;
    }

    public String getApplication_type() {
        return application_type;
    }

    public void setApplication_type(String application_type) {
        this.application_type = application_type;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getLogo_uri() {
        return logo_uri;
    }

    public void setLogo_uri(String logo_uri) {
        this.logo_uri = logo_uri;
    }

    public String getClient_uri() {
        return client_uri;
    }

    public void setClient_uri(String client_uri) {
        this.client_uri = client_uri;
    }

    public String getPolicy_uri() {
        return policy_uri;
    }

    public void setPolicy_uri(String policy_uri) {
        this.policy_uri = policy_uri;
    }

    public String getTos_uri() {
        return tos_uri;
    }

    public void setTos_uri(String tos_uri) {
        this.tos_uri = tos_uri;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getSector_identifier_uri() {
        return sector_identifier_uri;
    }

    public void setSector_identifier_uri(String sector_identifier_uri) {
        this.sector_identifier_uri = sector_identifier_uri;
    }

    public String getSubject_type() {
        return subject_type;
    }

    public void setSubject_type(String subject_type) {
        this.subject_type = subject_type;
    }

    public String getId_token_signed_response_alg() {
        return id_token_signed_response_alg;
    }

    public void setId_token_signed_response_alg(String id_token_signed_response_alg) {
        this.id_token_signed_response_alg = id_token_signed_response_alg;
    }

    public String getId_token_encrypted_response_alg() {
        return id_token_encrypted_response_alg;
    }

    public void setId_token_encrypted_response_alg(String id_token_encrypted_response_alg) {
        this.id_token_encrypted_response_alg = id_token_encrypted_response_alg;
    }

    public String getId_token_encrypted_response_enc() {
        return id_token_encrypted_response_enc;
    }

    public void setId_token_encrypted_response_enc(String id_token_encrypted_response_enc) {
        this.id_token_encrypted_response_enc = id_token_encrypted_response_enc;
    }

    public String getUserinfo_signed_response_alg() {
        return userinfo_signed_response_alg;
    }

    public void setUserinfo_signed_response_alg(String userinfo_signed_response_alg) {
        this.userinfo_signed_response_alg = userinfo_signed_response_alg;
    }

    public String getUserinfo_encrypted_response_alg() {
        return userinfo_encrypted_response_alg;
    }

    public void setUserinfo_encrypted_response_alg(String userinfo_encrypted_response_alg) {
        this.userinfo_encrypted_response_alg = userinfo_encrypted_response_alg;
    }

    public String getUserinfo_encrypted_response_enc() {
        return userinfo_encrypted_response_enc;
    }

    public void setUserinfo_encrypted_response_enc(String userinfo_encrypted_response_enc) {
        this.userinfo_encrypted_response_enc = userinfo_encrypted_response_enc;
    }

    public String getRequest_object_signing_alg() {
        return request_object_signing_alg;
    }

    public void setRequest_object_signing_alg(String request_object_signing_alg) {
        this.request_object_signing_alg = request_object_signing_alg;
    }

    public String getRequest_object_encryption_alg() {
        return request_object_encryption_alg;
    }

    public void setRequest_object_encryption_alg(String request_object_encryption_alg) {
        this.request_object_encryption_alg = request_object_encryption_alg;
    }

    public String getRequest_object_encryption_enc() {
        return request_object_encryption_enc;
    }

    public void setRequest_object_encryption_enc(String request_object_encryption_enc) {
        this.request_object_encryption_enc = request_object_encryption_enc;
    }

    public String getToken_endpoint_auth_method() {
        return token_endpoint_auth_method;
    }

    public void setToken_endpoint_auth_method(String token_endpoint_auth_method) {
        this.token_endpoint_auth_method = token_endpoint_auth_method;
    }

    public String getToken_endpoint_auth_signing_alg() {
        return token_endpoint_auth_signing_alg;
    }

    public void setToken_endpoint_auth_signing_alg(String token_endpoint_auth_signing_alg) {
        this.token_endpoint_auth_signing_alg = token_endpoint_auth_signing_alg;
    }

    public List<String> getDefault_max_age() {
        return default_max_age;
    }

    public void setDefault_max_age(List<String> default_max_age) {
        this.default_max_age = default_max_age;
    }

    public List<String> getRequire_auth_time() {
        return require_auth_time;
    }

    public void setRequire_auth_time(List<String> require_auth_time) {
        this.require_auth_time = require_auth_time;
    }

    public List<String> getDefault_acr_values() {
        return default_acr_values;
    }

    public void setDefault_acr_values(List<String> default_acr_values) {
        this.default_acr_values = default_acr_values;
    }

    public String getInitiate_login_uri() {
        return initiate_login_uri;
    }

    public void setInitiate_login_uri(String initiate_login_uri) {
        this.initiate_login_uri = initiate_login_uri;
    }

    public List<String> getRequest_uris() {
        return request_uris;
    }

    public void setRequest_uris(List<String> request_uris) {
        this.request_uris = request_uris;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    public String getRegistration_access_token() {
        return registration_access_token;
    }

    public void setRegistration_access_token(String registration_access_token) {
        this.registration_access_token = registration_access_token;
    }

    public String getRegistration_client_uri() {
        return registration_client_uri;
    }

    public void setRegistration_client_uri(String registration_client_uri) {
        this.registration_client_uri = registration_client_uri;
    }

    public String getClient_id_issued_at() {
        return client_id_issued_at;
    }

    public void setClient_id_issued_at(String client_id_issued_at) {
        this.client_id_issued_at = client_id_issued_at;
    }

    public String getClient_secret_expires_at() {
        return client_secret_expires_at;
    }

    public void setClient_secret_expires_at(String client_secret_expires_at) {
        this.client_secret_expires_at = client_secret_expires_at;
    }
}
