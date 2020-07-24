curl -X POST 'http://localhost:8080/environments/' \
-H 'Slug: env1' \
-H 'X-Agent-WebID: http://andreiciortea.ro/#me'

curl -X POST 'http://localhost:8080/environments/env1/workspaces/' \
-H 'Slug: wksp1' \
-H 'X-Agent-WebID: http://andreiciortea.ro/#me'

curl -X POST 'http://localhost:8080/environments/env1/workspaces/wksp1/artifacts/' \
-H 'Slug: leubot1' \
-H 'Content-Type: text/turtle' \
-H 'X-Agent-WebID: http://andreiciortea.ro/#me' \
--data-binary '@leubot1.ttl'

# curl -X POST 'http://localhost:8080/environments/env1/workspaces/wksp1/artifacts/' \
# -H 'X-Agent-WebID: http://andreiciortea.ro/#me' \
# -H 'Slug: c1' \
# -H 'Content-Type: application/json' \
# -d '{
#     "artifactClass" : "http://example.org/Counter"
# }'
