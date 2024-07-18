package org.hyperagents.yggdrasil.oauth;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class OpenIdProviderMetadata {
    // REQUIRED. URL using the https scheme with no query or fragment components that the OP
    // asserts as its Issuer Identifier. If Issuer discovery is supported (see Section 2), this
    // value MUST be identical to the issuer value returned by WebFinger. This also MUST be
    // identical to the iss Claim value in ID Tokens issued from this Issuer.
    @JsonProperty(required = true)
    private URI issuer;

    // REQUIRED. URL of the OP's OAuth 2.0 Authorization Endpoint [OpenID.Core]. This URL MUST
    // use the https scheme and MAY contain port, path, and query parameter components.
    @JsonProperty(required = true)
    private URI authorization_endpoint;

    // URL of the OP's OAuth 2.0 Token Endpoint [OpenID.Core]. This is REQUIRED unless only the
    // Implicit Flow is used. This URL MUST use the https scheme and MAY contain port, path, and
    // query parameter components.
    private URI token_endpoint;

    // RECOMMENDED. URL of the OP's UserInfo Endpoint [OpenID.Core]. This URL MUST use the https
    // scheme and MAY contain port, path, and query parameter components.
    private URI userinfo_endpoint;

    // REQUIRED. URL of the OP's JWK Set [JWK] document, which MUST use the https scheme. This
    // contains the signing key(s) the RP uses to validate signatures from the OP. The JWK Set
    // MAY also contain the Server's encryption key(s), which are used by RPs to encrypt requests
    // to the Server. When both signing and encryption keys are made available, a use (public key
    // use) parameter value is REQUIRED for all keys in the referenced JWK Set to indicate each
    // key's intended usage. Although some algorithms allow the same key to be used for both
    // signatures and encryption, doing so is NOT RECOMMENDED, as it is less secure. The JWK x5c
    // parameter MAY be used to provide X.509 representations of keys provided. When used, the
    // bare key values MUST still be present and MUST match those in the certificate. The JWK Set
    // MUST NOT contain private or symmetric key values.
    @JsonProperty(required = true)
    private URI jwks_uri;

    // RECOMMENDED. URL of the OP's Dynamic Client Registration Endpoint [OpenID.Registration],
    // which MUST use the https scheme.
    private URI registration_endpoint;

    // RECOMMENDED. JSON array containing a list of the OAuth 2.0 [RFC6749] scope values that
    // this server supports. The server MUST support the openid scope value. Servers MAY choose
    // not to advertise some supported scope values even when this parameter is used, although
    // those defined in [OpenID.Core] SHOULD be listed, if supported.
    private List<String> scopes_supported;

    // REQUIRED. JSON array containing a list of the OAuth 2.0 response_type values that this OP
    // supports. Dynamic OpenID Providers MUST support the code, id_token, and the id_token token
    // Response Type values.
    @JsonProperty(required = true)
    private  List<String> response_types_supported;

    // OPTIONAL. JSON array containing a list of the OAuth 2.0 response_mode values that this OP
    // supports, as specified in OAuth 2.0 Multiple Response Type Encoding Practices [OAuth
    // .Responses]. If omitted, the default for Dynamic OpenID Providers is ["query", "fragment"].
    private List<String> response_modes_supported;

    // OPTIONAL. JSON array containing a list of the OAuth 2.0 Grant Type values that this OP
    // supports. Dynamic OpenID Providers MUST support the authorization_code and implicit Grant
    // Type values and MAY support other Grant Types. If omitted, the default value is
    // ["authorization_code", "implicit"].
    private  List<String> grant_types_supported;

    //OPTIONAL. JSON array containing a list of the Authentication Context Class References that
    // this OP supports.
    private  List<String> acr_values_supported;

    //REQUIRED. JSON array containing a list of the Subject Identifier types that this OP
    // supports. Valid types include pairwise and public.
    @JsonProperty(required = true)
    private  List<String> subject_types_supported;

    //REQUIRED. JSON array containing a list of the JWS signing algorithms (alg values) supported
    // by the OP for the ID Token to encode the Claims in a JWT [JWT]. The algorithm RS256 MUST
    // be included. The value none MAY be supported but MUST NOT be used unless the Response Type
    // used returns no ID Token from the Authorization Endpoint (such as when using the
    // Authorization Code Flow).
    @JsonProperty(required = true)
    private  List<String> id_token_signing_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values)
    // supported by the OP for the ID Token to encode the Claims in a JWT [JWT].
    private  List<String> id_token_encryption_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values)
    // supported by the OP for the ID Token to encode the Claims in a JWT [JWT].
    private  List<String> id_token_encryption_enc_values_supported;

    //OPTIONAL. JSON array containing a list of the JWS [JWS] signing algorithms (alg values)
    // [JWA] supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT]. The value
    // none MAY be included.
    private  List<String> userinfo_signing_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the JWE [JWE] encryption algorithms (alg values)
    // [JWA] supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
    private  List<String> userinfo_encryption_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) [JWA]
    // supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
    private  List<String> userinfo_encryption_enc_values_supported;

    //OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported
    // by the OP for Request Objects, which are described in Section 6.1 of OpenID Connect Core 1
    // .0 [OpenID.Core]. These algorithms are used both when the Request Object is passed by
    // value (using the request parameter) and when it is passed by reference (using the
    // request_uri parameter). Servers SHOULD support none and RS256.
    private  List<String> request_object_signing_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values)
    // supported by the OP for Request Objects. These algorithms are used both when the Request
    // Object is passed by value and when it is passed by reference.
    private  List<String> request_object_encryption_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values)
    // supported by the OP for Request Objects. These algorithms are used both when the Request
    // Object is passed by value and when it is passed by reference.
    private  List<String> request_object_encryption_enc_values_supported;

    //OPTIONAL. JSON array containing a list of Client Authentication methods supported by this
    // Token Endpoint. The options are client_secret_post, client_secret_basic,
    // client_secret_jwt, and private_key_jwt, as described in Section 9 of OpenID Connect Core 1
    // .0 [OpenID.Core]. Other authentication methods MAY be defined by extensions. If omitted,
    // the default is client_secret_basic -- the HTTP Basic Authentication Scheme specified in
    // Section 2.3.1 of OAuth 2.0 [RFC6749].
    private  List<String> token_endpoint_auth_methods_supported;

    //OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported
    // by the Token Endpoint for the signature on the JWT [JWT] used to authenticate the Client
    // at the Token Endpoint for the private_key_jwt and client_secret_jwt authentication methods
    // . Servers SHOULD support RS256. The value none MUST NOT be used.
    private  List<String> token_endpoint_auth_signing_alg_values_supported;

    //OPTIONAL. JSON array containing a list of the display parameter values that the OpenID
    // Provider supports. These values are described in Section 3.1.2.1 of OpenID Connect Core 1
    // .0 [OpenID.Core].
    private  List<String> display_values_supported;

    //OPTIONAL. JSON array containing a list of the Claim Types that the OpenID Provider supports
    // . These Claim Types are described in Section 5.6 of OpenID Connect Core 1.0 [OpenID.Core].
    // Values defined by this specification are normal, aggregated, and distributed. If omitted,
    // the implementation supports only normal Claims.
    private  List<String> claim_types_supported;

    //RECOMMENDED. JSON array containing a list of the Claim Names of the Claims that the OpenID
    // Provider MAY be able to supply values for. Note that for privacy or other reasons, this
    // might not be an exhaustive list.
    private  List<String> claims_supported;

    //OPTIONAL. URL of a page containing human-readable information that developers might want or
    // need to know when using the OpenID Provider. In particular, if the OpenID Provider does
    // not support Dynamic Client Registration, then information on how to register Clients needs
    // to be provided in this documentation.
    private  String service_documentation;

    //OPTIONAL. Languages and scripts supported for values in Claims being returned, represented
    // as a JSON array of BCP47 [RFC5646] language tag values. Not all languages and scripts are
    // necessarily supported for all Claim values.
    private  List<String> claims_locales_supported;

    //OPTIONAL. Languages and scripts supported for the user interface, represented as a JSON
    // array of BCP47 [RFC5646] language tag values.
    private  List<String> ui_locales_supported;

    //OPTIONAL. Boolean value specifying whether the OP supports use of the claims parameter,
    // with true indicating support. If omitted, the default value is false.
    private  String claims_parameter_supported;

    //OPTIONAL. Boolean value specifying whether the OP supports use of the request parameter,
    // with true indicating support. If omitted, the default value is false.
    private  String request_parameter_supported;

    //OPTIONAL. Boolean value specifying whether the OP supports use of the request_uri
    // parameter, with true indicating support. If omitted, the default value is true.
    private  String request_uri_parameter_supported;

    //OPTIONAL. Boolean value specifying whether the OP requires any request_uri values used to
    // be pre-registered using the request_uris registration parameter. Pre-registration is
    // REQUIRED when the value is true. If omitted, the default value is false.
    private  String require_request_uri_registration;

    //OPTIONAL. URL that the OpenID Provider provides to the person registering the Client to
    // read about the OP's requirements on how the Relying Party can use the data provided by the
    // OP. The registration process SHOULD display this URL to the person registering the Client
    // if it is given.
    private  String op_policy_uri;

    //OPTIONAL. URL that the OpenID Provider provides to the person registering the Client to
    // read about the OpenID Provider's terms of service. The registration process SHOULD display
    // this URL to the person registering the Client if it is given.
    private  String op_tos_uri;

    public URI getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        if(!Objects.equals(URI.create(issuer).getScheme(), "https")) {
            throw new RuntimeException("Must be using https scheme");
        }
        this.issuer = URI.create(issuer);
    }

    public URI getAuthorization_endpoint() {
        return authorization_endpoint;
    }

    public void setAuthorization_endpoint(String authorization_endpoint) {
        if(!Objects.equals(URI.create(authorization_endpoint).getScheme(), "https")) {
            throw new RuntimeException("Must be using https scheme");
        }
        this.authorization_endpoint = URI.create(authorization_endpoint);
    }

    public URI getToken_endpoint() {
        return token_endpoint;
    }

    public void setToken_endpoint(String token_endpoint) {
        if(!Objects.equals(URI.create(token_endpoint).getScheme(), "https")) {
            throw new RuntimeException("Must be using https scheme");
        }
        this.token_endpoint = URI.create(token_endpoint);
    }

    public URI getUserinfo_endpoint() {
        return userinfo_endpoint;
    }

    public void setUserinfo_endpoint(String userinfo_endpoint) {
        if(!Objects.equals(URI.create(userinfo_endpoint).getScheme(), "https")) {
            throw new RuntimeException("Must be using https scheme");
        }
        this.userinfo_endpoint = URI.create(userinfo_endpoint);
    }

    public URI getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        if(!Objects.equals(URI.create(jwks_uri).getScheme(), "https")) {
            throw new RuntimeException("Must be using https scheme");
        }
        this.jwks_uri = URI.create(jwks_uri);
    }

    public URI getRegistration_endpoint() {
        return registration_endpoint;
    }

    public void setRegistration_endpoint(String registration_endpoint) {
        if(!Objects.equals(URI.create(registration_endpoint).getScheme(), "https")) {
            throw new RuntimeException("Must be using https scheme");
        }
        this.registration_endpoint = URI.create(registration_endpoint);
    }

    public List<String> getScopes_supported() {
        return scopes_supported;
    }

    public void setScopes_supported(List<String> scopes_supported) {
        this.scopes_supported = scopes_supported;
    }

    public List<String> getResponse_types_supported() {
        return response_types_supported;
    }

    public void setResponse_types_supported(List<String> response_types_supported) {
        this.response_types_supported = response_types_supported;
    }

    public List<String> getResponse_modes_supported() {
        return response_modes_supported;
    }

    public void setResponse_modes_supported(List<String> response_modes_supported) {
        this.response_modes_supported = response_modes_supported;
    }

    public List<String> getGrant_types_supported() {
        return grant_types_supported;
    }

    public void setGrant_types_supported(List<String> grant_types_supported) {
        this.grant_types_supported = grant_types_supported;
    }

    public List<String> getAcr_values_supported() {
        return acr_values_supported;
    }

    public void setAcr_values_supported(List<String> acr_values_supported) {
        this.acr_values_supported = acr_values_supported;
    }

    public List<String> getSubject_types_supported() {
        return subject_types_supported;
    }

    public void setSubject_types_supported(List<String> subject_types_supported) {
        this.subject_types_supported = subject_types_supported;
    }

    public List<String> getId_token_signing_alg_values_supported() {
        return id_token_signing_alg_values_supported;
    }

    public void setId_token_signing_alg_values_supported(List<String> id_token_signing_alg_values_supported) {
        this.id_token_signing_alg_values_supported = id_token_signing_alg_values_supported;
    }

    public List<String> getId_token_encryption_alg_values_supported() {
        return id_token_encryption_alg_values_supported;
    }

    public void setId_token_encryption_alg_values_supported(List<String> id_token_encryption_alg_values_supported) {
        this.id_token_encryption_alg_values_supported = id_token_encryption_alg_values_supported;
    }

    public List<String> getId_token_encryption_enc_values_supported() {
        return id_token_encryption_enc_values_supported;
    }

    public void setId_token_encryption_enc_values_supported(List<String> id_token_encryption_enc_values_supported) {
        this.id_token_encryption_enc_values_supported = id_token_encryption_enc_values_supported;
    }

    public List<String> getUserinfo_signing_alg_values_supported() {
        return userinfo_signing_alg_values_supported;
    }

    public void setUserinfo_signing_alg_values_supported(List<String> userinfo_signing_alg_values_supported) {
        this.userinfo_signing_alg_values_supported = userinfo_signing_alg_values_supported;
    }

    public List<String> getUserinfo_encryption_alg_values_supported() {
        return userinfo_encryption_alg_values_supported;
    }

    public void setUserinfo_encryption_alg_values_supported(List<String> userinfo_encryption_alg_values_supported) {
        this.userinfo_encryption_alg_values_supported = userinfo_encryption_alg_values_supported;
    }

    public List<String> getUserinfo_encryption_enc_values_supported() {
        return userinfo_encryption_enc_values_supported;
    }

    public void setUserinfo_encryption_enc_values_supported(List<String> userinfo_encryption_enc_values_supported) {
        this.userinfo_encryption_enc_values_supported = userinfo_encryption_enc_values_supported;
    }

    public List<String> getRequest_object_signing_alg_values_supported() {
        return request_object_signing_alg_values_supported;
    }

    public void setRequest_object_signing_alg_values_supported(List<String> request_object_signing_alg_values_supported) {
        this.request_object_signing_alg_values_supported = request_object_signing_alg_values_supported;
    }

    public List<String> getRequest_object_encryption_alg_values_supported() {
        return request_object_encryption_alg_values_supported;
    }

    public void setRequest_object_encryption_alg_values_supported(List<String> request_object_encryption_alg_values_supported) {
        this.request_object_encryption_alg_values_supported = request_object_encryption_alg_values_supported;
    }

    public List<String> getRequest_object_encryption_enc_values_supported() {
        return request_object_encryption_enc_values_supported;
    }

    public void setRequest_object_encryption_enc_values_supported(List<String> request_object_encryption_enc_values_supported) {
        this.request_object_encryption_enc_values_supported = request_object_encryption_enc_values_supported;
    }

    public List<String> getToken_endpoint_auth_methods_supported() {
        return token_endpoint_auth_methods_supported;
    }

    public void setToken_endpoint_auth_methods_supported(List<String> token_endpoint_auth_methods_supported) {
        this.token_endpoint_auth_methods_supported = token_endpoint_auth_methods_supported;
    }

    public List<String> getToken_endpoint_auth_signing_alg_values_supported() {
        return token_endpoint_auth_signing_alg_values_supported;
    }

    public void setToken_endpoint_auth_signing_alg_values_supported(List<String> token_endpoint_auth_signing_alg_values_supported) {
        this.token_endpoint_auth_signing_alg_values_supported = token_endpoint_auth_signing_alg_values_supported;
    }

    public List<String> getDisplay_values_supported() {
        return display_values_supported;
    }

    public void setDisplay_values_supported(List<String> display_values_supported) {
        this.display_values_supported = display_values_supported;
    }

    public List<String> getClaim_types_supported() {
        return claim_types_supported;
    }

    public void setClaim_types_supported(List<String> claim_types_supported) {
        this.claim_types_supported = claim_types_supported;
    }

    public List<String> getClaims_supported() {
        return claims_supported;
    }

    public void setClaims_supported(List<String> claims_supported) {
        this.claims_supported = claims_supported;
    }

    public String getService_documentation() {
        return service_documentation;
    }

    public void setService_documentation(String service_documentation) {
        this.service_documentation = service_documentation;
    }

    public List<String> getClaims_locales_supported() {
        return claims_locales_supported;
    }

    public void setClaims_locales_supported(List<String> claims_locales_supported) {
        this.claims_locales_supported = claims_locales_supported;
    }

    public List<String> getUi_locales_supported() {
        return ui_locales_supported;
    }

    public void setUi_locales_supported(List<String> ui_locales_supported) {
        this.ui_locales_supported = ui_locales_supported;
    }

    public String getClaims_parameter_supported() {
        return claims_parameter_supported;
    }

    public void setClaims_parameter_supported(String claims_parameter_supported) {
        this.claims_parameter_supported = claims_parameter_supported;
    }

    public String getRequest_parameter_supported() {
        return request_parameter_supported;
    }

    public void setRequest_parameter_supported(String request_parameter_supported) {
        this.request_parameter_supported = request_parameter_supported;
    }

    public String getRequest_uri_parameter_supported() {
        return request_uri_parameter_supported;
    }

    public void setRequest_uri_parameter_supported(String request_uri_parameter_supported) {
        this.request_uri_parameter_supported = request_uri_parameter_supported;
    }

    public String getRequire_request_uri_registration() {
        return require_request_uri_registration;
    }

    public void setRequire_request_uri_registration(String require_request_uri_registration) {
        this.require_request_uri_registration = require_request_uri_registration;
    }

    public String getOp_policy_uri() {
        return op_policy_uri;
    }

    public void setOp_policy_uri(String op_policy_uri) {
        this.op_policy_uri = op_policy_uri;
    }

    public String getOp_tos_uri() {
        return op_tos_uri;
    }

    public void setOp_tos_uri(String op_tos_uri) {
        this.op_tos_uri = op_tos_uri;
    }
}
