#!/bin/bash

echo -e '\n\nCreating hue artifact...'
curl -i -X POST \
  http://localhost:8080/artifacts/ \
  -H 'content-type: text/turtle' \
  -H 'slug: hue1' \
  --data-binary '@light.ttl'

echo -e '\n\nAdding hue artifact to workspace...'
curl -i -X PUT \
  http://localhost:8080/workspaces/wksp1 \
  -H 'content-type: text/turtle' \
  -d '<http://localhost:8080/workspaces/wksp1> a <http://w3id.org/eve#Workspace> ;
<http://w3id.org/eve#hasName> "wksp1" ;
<http://w3id.org/eve#contains> <http://localhost:8080/artifacts/hue1> ;
<http://w3id.org/eve#contains> <http://localhost:8080/artifacts/event-gen> .'

