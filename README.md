# Yggdrasil

[![codecov](https://codecov.io/gh/Interactions-HSG/yggdrasil/graph/badge.svg?token=DhAW8ZB5zr)](https://codecov.io/gh/Interactions-HSG/yggdrasil)

A platform for [Hypermedia Multi-Agent Systems (MAS)](https://hyperagents.org/) [1] built with
[Vert.x](https://vertx.io/). The current implementation provides two core functionalities:

* it allows to program and deploy hypermedia environments for autonomous agents that conform to the
  _Agents & Artifacts_ meta-model [2]
* it partially implements the [W3C WebSub recommendation](https://www.w3.org/TR/2018/REC-websub-20180123/)
  and can act as a WebSub hub

#### References

[1] Andrei Ciortea, Olivier Boissier, Alessandro Ricci. 2019. Engineering World-Wide Multi-Agent Systems
with Hypermedia. In: Weyns D., Mascardi V., Ricci A. (eds) Engineering Multi-Agent Systems. EMAS 2018.
Lecture Notes in Computer Science, vol 11375. Springer, Cham. https://doi.org/10.1007/978-3-030-25693-7_15

[2] Alessandro Ricci, Michele Piunti, and Mirko Viroli. 2011. Environment Programming in multi-agent
systems: an artifact-based perspective. Autonomous Agents and Multi-Agent Systems, 23(2):158-192.

## Prerequisites

* JDK 21+

## Building the project

To build the project, just use:

```shell
./gradlew
```

The default Gradle task `shadowJar` generates a fat-jar in the `build/libs` directory.

## Running Yggdrasil

To start an Yggdrasil node:

```shell
java -jar build/libs/yggdrasil-0.0.0-SNAPSHOT-all.jar -conf conf/localhost_memory_config.json
```

The configuration file is optional. Open your browser to
[http://localhost:8080](http://localhost:8080). You should see an `Yggdrasil v0.0.0` message.

## Running Yggdrasil as a Docker container

Build the image with the current context and creates the image `yggdrasil`:

```shell
docker-compose build
```

Run with docker-compose (by default, it exposes the port `8899` of the host machine):

```shell
docker-compose up
```

Use `docker-compose-dev.yml` for an environment that contains both libraries and sources:

```shell
docker-compose -f docker-compose.yml -f docker-compose-dev.yml build
docker-compose -f docker-compose.yml -f docker-compose-dev.yml up
```

## HTTP API Overview

The HTTP API implements CRUD operations for 2 types of resources:

* workspaces (URI template: `/workspaces/<wksp_id>`)
* artifacts (URI template: `/artifacts/<art_id>`)

`POST` and `PUT` requests use [Turtle](http://www.w3.org/TR/2014/REC-turtle-20140225/) payloads
and the current implementation only validates the payload's syntax.

`POST` requests can use the `Slug` header (see [RFC 5023](https://tools.ietf.org/html/rfc5023#section-9.7))
to hint at a preferred IRI for a resource to be created. If the IRI is not already in use, it will
be minted to the created resource.

When creating a resource via `POST`, the resource to be created is identified in the Turtle payload
via a null relative IRI:

```shell
curl -i -X POST \
  http://localhost:8080/workspaces/ \
  -H 'content-type: text/turtle' \
  -H 'slug: wksp1' \
  -d '<> a <https://purl.org/hmas/Workspace> .'
```

When retrieving the representation of a resource from Yggdrasil, the HTTP response contains 2 `Link`
header fields that advertise a WebSub hub that clients can subscribe to in order to receive
notifications whenever the resource is updated (see the
[W3C WebSub recommendation](https://www.w3.org/TR/2018/REC-websub-20180123/)).
Sample request:

```shell
GET /workspaces/wksp1 HTTP/1.1
Host: yggdrasil.andreiciortea.ro

HTTP/1.1 200 OK
Content-Type: text/turtle
Link: <http://yggdrasil.andreiciortea.ro/hub>; rel="hub"
Link: <http://yggdrasil.andreiciortea.ro/workspaces/wksp1>; rel="self"

<http://yggdrasil.andreiciortea.ro/workspaces/wksp1>
  a <https://purl.org/hmas/Workspace> ;
  <https://purl.org/hmas/hasName> "wksp1" ;
  <https://purl.org/hmas/contains>
    <http://85.204.10.233:8080/artifacts/hue1> ,
    <http://yggdrasil.andreiciortea.ro/artifacts/event-gen> .
```

Using the discovered hub and topic IRIs, a client can subscribe for notification via a `POST` request
that contains a JSON payload with the following fields (see the
[W3C WebSub recommendation](https://www.w3.org/TR/2018/REC-websub-20180123/)):

* `hub.mode`
* `hub.topic`
* `hub.callback`

When a resource is updated, Yggdrasil issues `POST` requests with the (updated) resource
representation to all registered callbacks.
