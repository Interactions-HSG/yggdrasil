# Yggdrasil

[![codecov](https://codecov.io/gh/Interactions-HSG/yggdrasil/graph/badge.svg?token=DhAW8ZB5zr)](https://codecov.io/gh/Interactions-HSG/yggdrasil)

A platform for [Hypermedia Multi-Agent Systems (MAS)](https://hyperagents.org/) [1] built with [Vert.x](https://vertx.io/).
The current implementation provides two core functionalities:

* it allows programming and deploying hypermedia environments for autonomous agents that conform to the _Agents & Artifacts_ metamodel [2]
* it partially implements the [W3C WebSub recommendation](https://www.w3.org/TR/2018/REC-websub-20180123/) and can act as a WebSub hub

#### References

[1] Andrei Ciortea, Olivier Boissier, Alessandro Ricci. 2019. Engineering World-Wide Multi-Agent Systems
with Hypermedia. In: Weyns D., Mascardi V., Ricci A. (eds) Engineering Multi-Agent Systems. EMAS 2018.
Lecture Notes in Computer Science, vol 11375. Springer, Cham. [https://doi.org/10.1007/978-3-030-25693-7_15](https://doi.org/10.1007/978-3-030-25693-7_15)

[2] Alessandro Ricci, Michele Piunti, and Mirko Viroli. 2011. Environment Programming in multi-agent
systems: an artifact-based perspective. Autonomous Agents and Multi-Agent Systems, 23(2):158–192.

## Prerequisites

* JDK 21+
* Gradle 8.9+

## Building the project

To build the project use:

```shell
./gradlew
```

The default Gradle task `shadowJar` generates a "fat" JAR file in the `build/libs` directory.

## Running Yggdrasil

To start a Yggdrasil node:

```shell
java -jar build/libs/yggdrasil-0.0.0-SNAPSHOT-all.jar -conf conf/localhost_memory_config_td.json
```

The configuration file is optional.
Open your browser to [http://localhost:8080](http://localhost:8080).
You should see a description of the platform like the following:

```
@base <http://localhost:8080/> .
@prefix hmas: <https://purl.org/hmas/> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix htv: <http://www.w3.org/2011/http#> .
@prefix jacamo: <https://purl.org/hmas/jacamo/> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix td: <https://www.w3.org/2019/wot/td#> .

<> a td:Thing, hmas:HypermediaMASPlatform;
  td:title "Yggdrasil Node";
  td:hasSecurityConfiguration [ a wotsec:NoSecurityScheme
    ];
  td:hasActionAffordance [ a td:ActionAffordance, jacamo:CreateWorkspace;
      td:name "createWorkspace";
      td:hasForm [
          htv:methodName "POST";
          hctl:hasTarget <workspaces/>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ]
    ] .
```

## Running Yggdrasil as a Docker container

Run and build the Yggdrasil image in the project directory context
(by default, the service is exposed on port `8899` of the host machine). The docker config can be found in the /conf folder:

```shell
docker compose up
```

## HTTP API Overview

The HTTP API implements CRUD operations for three types of resources:

* workspaces and sub-workspaces (URI template: `/workspaces/<wksp_id>`)
* artifacts (URI template: `/workspaces/<wksp_id>/artifacts/<art_id>`)
* body artifacts (URI template: `/workspaces/<wksp_id>/artifacts/<agt_id>`)

### Caveats

The `POST` requests for creating a workspace (URI `/workspaces/`),
a sub-workspace (URI `/workspaces/<wksp_id>`) or an artifact (URI `/workspaces/<wksp_id>/artifact/`)
can receive [Turtle](http://www.w3.org/TR/2014/REC-turtle-20140225/) payloads other than their default payloads,
and the current implementation only validates the payload's syntax.

When creating a resource via `POST`,
the resource to be created can be identified in the Turtle payload via a null relative IRI:

```shell
curl -i -X POST \
  http://localhost:8080/workspaces/ \
  -H 'content-type: text/turtle' \
  -H 'slug: wksp1' \
  -d '<> a <https://purl.org/hmas/Workspace> .'
```

The `POST` requests for creating a workspace (URI `/workspaces/`) or a sub-workspace (URI `/workspaces/<wksp_id>`)
can use the `Slug` header (see [RFC 5023](https://tools.ietf.org/html/rfc5023#section-9.7))
to hint at a preferred IRI for a resource to be created.
If the IRI is not already in use, it will be minted to the created resource,
otherwise, a new random URI will be generated.

All `PUT` requests (for updating a workspace, URI `/workspaces/<wksp_id>`,
for updating an artifact, URI `/workspaces/<wksp_id>/artifacts/<art_id>`,
and for updating an agent body, URI `/workspaces/<wksp_id>/agents/<agt_id>`),
can receive [Turtle](http://www.w3.org/TR/2014/REC-turtle-20140225/) payloads other than their default payloads,
and the current implementation only validates the payload's syntax.

For more information, see the [documentation website](https://interactions-hsg.github.io/yggdrasil/).

### WebSub

When retrieving the representation of a resource from Yggdrasil,
the HTTP response contains 2 `Link` header fields
that advertise a WebSub hub that clients can subscribe to receive notifications whenever the resource is updated
(see the [W3C WebSub recommendation](https://www.w3.org/TR/2018/REC-websub-20180123/)).
A sample request follows:

```shell
GET /workspaces/test HTTP/1.1
Host: yggdrasil.andreiciortea.ro

HTTP/1.1 200 OK
Content-Type: text/turtle
Link: <http://yggdrasil.andreiciortea.ro/hub/>; rel="hub"
Link: <http://yggdrasil.andreiciortea.ro/workspaces/test>; rel="self"

@prefix hmas: <https://purl.org/hmas/> .
@prefix td: <https://www.w3.org/2019/wot/td#> .
@prefix htv: <http://www.w3.org/2011/http#> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix dct: <http://purl.org/dc/terms/> .
@prefix js: <https://www.w3.org/2019/wot/json-schema#> .
@prefix saref: <https://w3id.org/saref#> .

<http://localhost:8080/workspaces/test> a td:Thing, hmas:Workspace;
  td:hasActionAffordance [ a td:ActionAffordance;
      td:name "joinWorkspace";
      td:hasForm [
          htv:methodName "POST";
          hctl:hasTarget <http://localhost:8080/workspaces/test/join>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ]
    ], [ a td:ActionAffordance;
      td:hasForm [
          htv:methodName "POST";
          hctl:hasTarget <http://localhost:8080/workspaces/test/artifacts/>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "artifactName"
            ], [ a js:ArraySchema;
              js:propertyName "initParams"
            ], [ a js:StringSchema;
              js:propertyName "artifactClass";
              js:enum <http://example.org/Adder>, <http://example.org/Counter>
            ];
          js:required "artifactName", "artifactClass"
        ];
      td:name "makeArtifact"
    ], [ a td:ActionAffordance;
      td:name "focus";
      td:hasForm [
          htv:methodName "POST";
          hctl:hasTarget <http://localhost:8080/workspaces/test/focus>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "artifactName"
            ], [ a js:StringSchema;
              js:propertyName "callbackIri"
            ];
          js:required "artifactName", "callbackIri"
        ]
    ], [ a td:ActionAffordance;
      td:name "quitWorkspace";
      td:hasForm [
          htv:methodName "POST";
          hctl:hasTarget <http://localhost:8080/workspaces/test/leave>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ]
    ], [ a td:ActionAffordance;
      td:name "createSubWorkspace";
      td:hasForm [
          htv:methodName "POST";
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction;
          hctl:hasTarget <http://localhost:8080/workspaces/test>
        ]
    ] ;
  td:title "test";
  td:hasSecurityConfiguration [ a wotsec:NoSecurityScheme
    ];
  hmas:isHostedOn <http://localhost:8080/>;
  hmas:contains <http://localhost:8080/workspaces/test/agents/test>;
  hmas:contains <http://localhost:8080/workspaces/sub> .

<http://localhost:8080/> a hmas:HypermediaMASPlatform .
<http://localhost:8080/workspaces/sub> a hmas:Workspace .
<http://localhost:8080/workspaces/test/agents/test> a hmas:Artifact .
```

Using the discovered hub and topic IRIs,
a client can subscribe for notifications via a `POST` request that contains a JSON payload with the following fields
(see the [W3C WebSub recommendation](https://www.w3.org/TR/2018/REC-websub-20180123/)) to the "hub" URL:

* `hub.mode` (could be either "subscribe" or "unsubscribe")
* `hub.topic` (the URI of the resource to subscribe to or unsubscribe from)
* `hub.callback` (the URI to be notified by the Yggdrasil platform upon notifications)

When a resource is created or updated,
Yggdrasil issues a `POST` request with the resource representation to all registered callbacks.
When a resource is deleted,
Yggdrasil issues an empty `POST` request to all registered callbacks.
When an agent starts or ends performing an action on an artifact in a workspace,
Yggdrasil issues a JSON `POST` request to all registered callbacks with the following fields:

* `eventType` (can be either "actionRequested," or "actionSucceeded," or "actionFailed")
* `artifactName` (the name of the artifact on which the action is done)
* `actionName` (the name of the action done on the artifact)
* `cause` (present only if the "eventType" field is set to "actionFailed," the cause of failure)

For more information, see the [documentation website](https://interactions-hsg.github.io/yggdrasil/).
