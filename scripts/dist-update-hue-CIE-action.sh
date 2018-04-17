#!/bin/bash

echo -e '\n\nUpdating hue artifact with set color action...'
curl -i -X PUT \
  http://localhost:8085/artifacts/hue1 \
  -H 'content-type: text/turtle' \
  --data-binary '@light-CIE.ttl'

