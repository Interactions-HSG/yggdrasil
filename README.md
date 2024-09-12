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
Yggdrasil currently supports the HMAS and TD ontologies. If none is specified in the configuration file it defaults to TD.

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

## Configuration Files
Configuration files are used to clearly specify the exact startup parameters for a given Yggdrasil instance.
Check out the conf/ folder for some exemplary configuration files. Configuration files are added as a cli param with the
"-conf" flag, and either a full path or dynamic path starting from the source directory.

You can create your own configuration files by creating a json file. There are three top level keys that can be specified
in the configuration json: "http-config", "notification-config" and "environment-config".

### HTTP-CONFIG
The Http-config specifies the host port and baseUri that are used for the specific yggdrasil instance.
```json
{
  "http-config" : {
    "host" : "localhost",
    "port" : 8080,
    "base-uri" : "http://localhost:8080/"
  }
}
```
The above config illustrates the default values that are used if the http-config is not specified in the configuration.

### NOTIFICATION-CONFIG
The notification-config specifies wether or not the given yggdrasil instance implements the websub protocol and enables
users to subscribe to entities to receive update messagese if they change.
If it is not enabled the HttpNotificationVerticle will not be deployed.
```json
{
  "notification-config" : {
    "enabled" : false
  }
}
```
The above config illustrates the default values that are used if the notification-config is not specified in the configuration.

### ENVIRONMENT-CONFIG
The environment-config specifies how the environment should look like upon launch, further it specifies if CArtAgO should be used to enable
e.g. virtual artifacts. You can also specify the wanted ontology for the given yggdrasil instance in the environment config.

#### Environment settings
```json
{
  "environment-config" : {
    "ontology" : "td",
    "enabled" : false
  }
}
```
The above config illustrates the default values that are used if the environment-config is not specified in the configuration.
We currently support the Thing Description (TD) and Hypermedia Multi Agent System (HMAS) ontologies. They can be selected
through the "ontology" parameter.

Enabling the environment will deploy the CartagoVerticle and enable the given yggdrasil instance to host virtual artifacts
and workspaces, including the functionality of focusing on artifacts and being notified on changes in observable properties and signals.

#### Virtual artifacts
To use artificial artifacts, the application needs to know where they are situated. For that you can use the additional
parameter "known-artifacts" to inform the given yggdrasil instance of the available artifacts.
```json
{
  "environment-config" : {
    "known-artifacts" : [
      {
        "class" : "http://example.org/Counter",
        "template" : "org.hyperagents.yggdrasil.cartago.artifacts.CounterTD"
      }
    ]
  }
}
```
The "known-artifacts" key takes a JsonArray as its value. Each Json Object contains the class and template for an artifact.
The class is then used as a key to identify the artifacts at runtime. The template is simply the java class of the wanted artifact.
When a virtual artifact is in the "known-artifacts" array users are then able to create instances of this class by using the makeArtifact endpoint
in a workspace (needs to be a workspace that has an underlying CArtAgO workspace). Correctly specified artifacts can then be used to remotely
execute operations.

### Workspaces
The last top level key "workspaces" can be used to specify the layout that the given yggdrasil instance should start out with.
On deployment the program will read in the specifications and create the workspaces and artifacts as well as their relations as specified.
```json
{
  "environment-config" : {
    "workspaces" : [
      {
        "name" : "w1",
        "representation" : "path/to/file/containingEntireRepresentation"
      },
      {
        "name" : "w0",
        "parent-name" : "w1",
        "artifacts" : [
          {
            "name" : "c0",
            "class" : "http://example.org/Counter",
            "init-params" : [
              5
            ],
            "metadata" :  "path/to/file/containgingAdditionalTriple"
          }
        ],
        "agents" : [
          {
            "name" : "test",
            "agent-uri" : "http://localhost:8080/agents/test",
            "callback-uri" : "http://localhost:8081",
            "focused-artifacts" : [
              "c0"
            ],
            "metadata" : "path/to/file/containgingAdditionalTriple"
          }
        ]
      }
    ]
  }
}
```
The "workspaces" key again takes as value a JsonArray where each JsonObject specifies a Workspace.
If you have the environment enabled, a workspace only needs a name and every other variable is optional.
If the environment is disabled the "representation" parameter is also mandatory.

##### Environment disabled
If the environment is disabled, we do not use CArtAgO and therefore provide less functionality. This also means
the setup is a little simpler. Currently, you can only specify workspaces and artifacts. In both cases the name and the
representation are mandatory fields. A simple configuration could look like this:
```json
{
  "environment-config" : {
    "workspaces" : [
      {
        "name" : "test",
        "representation" : "src/test/resources/td/test_workspace_td.ttl"
      },
      {
        "name" : "sub",
        "parent-name" : "test",
        "artifacts" : [
          {
            "name" : "c0",
            "representation" : "src/test/resources/td/c0_counter_artifact.ttl"
          }
        ],
        "representation" : "src/test/resources/td/sub_workspace_td.ttl"
      }
    ]
  }
}
```
Optionally you can also define parent - child relationships between workspaces.

#### Environment enabled
If the environment is enabled, the given yggdrasil instance will provide more funcionality in interacting with the environment.
This is reflected in the more extendable configuration file. It is still possible to specifiy a workspace with a name and representation.
If you do it this way NO underlying CArtAgO workspace will be created!

##### workspaces
The following configurations will all create an underlying CArtAgO workspace instance.

To start with a workspace you only need to specify a name. This will instantiate a top-level workspace and its representation
will have the default signifiers that are available to workspaces. Optionally you can also use the "metadata" key to specifiy a files
from which you want to add additional triples to the workspace representation. Also, the "parent-name" key is still available and works as expected.

You have now created workspaces that either contain other workspaces or are empty.

To fill the workspaces with entities you can add the "artifacts" and "agents" keys to the workspace json objects. These specify the entities and their
relations inside the workspaces.
```json
[
  {
    "name": "w1",
    "representation": "path/to/file/containingEntireRepresentation"
  },
  {
    "name": "w0",
    "parent-name": "w1",
    "artifacts": [
      {
        "name": "c0",
        "class": "http://example.org/Counter",
        "init-params": [
          5
        ],
        "metadata": "path/to/file/containingAdditionalTriples"
      },
      {
        "name" : "c1",
        "representation" : "path/to/files/containingEntireRepresentation"
      }
    ],
    "agents": [
      {
        "name": "test",
        "agent-uri": "http://localhost:8080/agents/test",
        "callback-uri": "http://localhost:8081",
        "focused-artifacts": [
          "c0"
        ],
        "metadata": "path/to/files/containingAdditionalTriples"
      }
    ]
  }
]
```
##### artifacts
With the environment enabled artifacts can still be statically instantiated with the "representation" key. If this key is
specified the other keys will be disregarded and no virtual artifact (CArtAgO instance) will be created.

If instead the "class" key is specified then the application will check the "known-artifacts" array and if the "class" is
present it will create an instance of the specified template class. It is possible to instantiate artifacts with parameters,
these are specified with the optional "init-params" key. Additional metadata can again be specified through the "metadata" parameter.

##### agents
Enabling the enviroment allows the creation of body artifacts and the ability of agents to join and participate in the environment.
An agent consists of a name which will be given to the body artifact, an agent-uri which should point to the WebID of the agent and a callback-uri
which should point to an endpoint of the agent that is capable of handling the update messages sent from yggdrasil.

Specifying an agent in a workspace will automatically create a body artifact in said workspace as well as join the agent in the CArtAgO workspace.
This will be verifiable by checking the workspace and observing that it contains a body artifact of said agent.

It is also possible to specify which artifacts the agent should be focused on by adding the optional "focused-artifacts" param and a list of artifact names.
Note that these artifacts must exist in the same workspace.

Additional metadata can again be specified through the "metadata" parameter. Agents cannot be instantiated in workspaces that are NOT created ontop of CArtAgO.

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
