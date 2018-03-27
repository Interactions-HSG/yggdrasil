#!/bin/bash

while :
do

sleep 3

echo 'Sending positive event...'
curl -i -H 'Link: http://localhost:8080/artifacts/event-gen' -X POST http://localhost:8080/events/ -d 'positive'

sleep 3

echo 'Sending negative event...'
curl -i -H 'Link: http://localhost:8080/artifacts/event-gen' -X POST http://localhost:8080/events/ -d 'negative'

done
