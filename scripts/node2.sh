curl -X POST 'http://localhost:8085/environments/' \
-H 'Slug: env2' \
-H 'X-Agent-WebID: http://andreiciortea.ro/#me'

curl -X POST 'http://localhost:8085/environments/env2/workspaces/' \
-H 'Slug: wksp2' \
-H 'X-Agent-WebID: http://andreiciortea.ro/#me'

#curl -X POST 'http://localhost:8085/environments/env2/workspaces/wksp2/artifacts/' \
#-H 'X-Agent-WebID: http://andreiciortea.ro/#me' \
#-H 'Slug: c2' \
#-H 'Content-Type: application/json' \
#-d '{
#    "artifactClass" : "http://example.org/Counter",
#    "artifactName" : "c2"
#}'

