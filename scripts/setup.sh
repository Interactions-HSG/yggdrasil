#!/bin/bash

echo -e '\n\nCreating environment...'
curl -i -X POST \
  http://localhost:8080/environments/ \
  -H 'content-type: text/turtle' \
  -H 'slug: env1' \
  -d '<> a <http://w3id.org/eve#Environment> ;
<http://w3id.org/eve#contains> <http://localhost:8080/workspaces/www> .'

sleep 1

echo -e '\n\nCreating workspace...'
curl -i -X POST \
  http://localhost:8080/workspaces/ \
  -H 'content-type: text/turtle' \
  -H 'slug: wksp1' \
  -d '<> a <http://w3id.org/eve#Workspace> ;
<http://w3id.org/eve#hasName> "www" .'

echo -e '\n\nCreating robot2 artifact...'
curl -i -X POST \
  http://localhost:8080/artifacts/ \
  -H 'content-type: text/turtle' \
  -H 'slug: robot2' \
  -d '<> a <http://w3id.org/eve#Artifact> ;
<http://w3id.org/eve#hasName> "robot2" ;
<http://w3id.org/eve#hasCartagoArtifact> "ThingArtifact" .'

echo -e '\n\nAdding robot2 artifact to workspace...'
curl -i -X PUT \
  http://localhost:8080/workspaces/www \
  -H 'content-type: text/turtle' \
  -d '<http://localhost:8080/workspaces/www> a <http://w3id.org/eve#Workspace> ;
<http://w3id.org/eve#hasName> "www" ;
<http://w3id.org/eve#contains> <http://localhost:8080/artifacts/robot2> .'

echo -e '\n\nCreating robot artifact...'
curl -i -X POST \
  http://localhost:8080/artifacts/robot2 \
  -H 'content-type: text/turtle' \
  -H 'slug: hue1' \
  --data-binary '@robot.ttl'

echo -e '\n\nAdding robot artifact to workspace...'
curl -i -X PUT \
  http://localhost:8080/workspaces/www \
  -H 'content-type: text/turtle' \
  -d '<http://localhost:8080/workspaces/wksp1> a <http://w3id.org/eve#Workspace> ;
<http://w3id.org/eve#hasName> "www" ;
<http://w3id.org/eve#contains> <http://localhost:8080/artifacts/robot2> .'
