
curl --location --request POST 'http://localhost:8080/agents/' \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: 'manager' \
--header 'Content-Type: text/plain' \
--data-raw '
!start.

+!start: true <-
  Callback = "http://example.org/callback"
  ?make_json_term(["value", "callback"], ["home", Callback], PoseValueHome);
  .print("end").

+?make_json_term(AttributeList, ValueList, Json): true <-
    createMapTerm(AttributeList, ValueList, Json);
    .print("make json term: ", Json).

'











'
