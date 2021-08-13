curl -X POST 'http://localhost:8080/environments/61/workspaces/102/artifacts/' \
-H 'X-Agent-WebID: http://example.com/lemee/jeremy#me' \
-H 'Content-Type: application/json' \
--data-raw '{
    "artifactClass" : "http://example.org/Maze1",
    "artifactName" : "maze1"
}'
