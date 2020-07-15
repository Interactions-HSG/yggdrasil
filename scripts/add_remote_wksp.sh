curl -X PUT 'http://localhost:8080/environments/env1' \
-H 'Content-Type: text/turtle' \
-H 'X-Agent-WebID: http://andreiciortea.ro/#me' \
--data-binary '@env1.ttl'
