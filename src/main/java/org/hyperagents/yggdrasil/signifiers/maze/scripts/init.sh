curl -X POST 'http://localhost:8080/environments/' \
-H 'Slug: 61' \
-H 'X-Agent-WebID: http://example.com/lemee/jeremy#me'

curl -X POST 'http://localhost:8080/environments/61/workspaces/' \
-H 'Slug: 102' \
-H 'X-Agent-WebID: http://example.com/lemee/jeremy#me'
