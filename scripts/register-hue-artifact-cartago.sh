#!/bin/bash

echo -e '\n\nCreating hue artifact...'
curl -i -X POST \
  http://localhost:8080/artifacts/ \
  -H 'content-type: text/turtle' \
  -H 'slug: hue1' \
  -d '<> a <http://w3id.org/eve#Artifact> ;
<http://w3id.org/eve#hasName> "hue1" ;
<http://w3id.org/eve#hasCartagoArtifact> "emas.HueArtifact" ;
<http://w3id.org/eve#hasInitParam> <http://192.168.0.101/api/YqqaHVH8QF-o7iPm6L7ax9jRtu-NTxBAysr4-UQc/lights/3/state> .'

echo -e '\n\nAdding hue artifact to workspace...'
curl -i -X PUT \
  http://localhost:8080/workspaces/wksp1 \
  -H 'content-type: text/turtle' \
  -d '<http://localhost:8080/workspaces/wksp1> a <http://w3id.org/eve#Workspace> ;
<http://w3id.org/eve#hasName> "wksp1" ;
<http://w3id.org/eve#contains> <http://localhost:8080/artifacts/hue1> ;
<http://w3id.org/eve#contains> <http://localhost:8080/artifacts/event-gen> .'

