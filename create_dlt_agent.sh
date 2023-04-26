while [[ "$#" -gt 0 ]]
  do
    case $1 in
      -a|--agent)
        AGENT_ID=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;
      --hyper)
        HYPERMAS_BASE=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;
    esac
    shift
  done


if [[ $REQUIRED_PARAM_COUNTER -ne 2 ]]; then
    echo "$(basename $0)  --hyper <HyperMAS base URL>  -a/--agent <agent id>"
    exit 1
fi


curl --location --request POST ''"${HYPERMAS_BASE}"'/agents/' \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: '"${AGENT_ID}"'' \
--header 'Content-Type: text/plain' \
--data-raw '
td_dlt("http://localhost:8080/workspaces/uc3/artifacts/dlt-client").

!start.
+!start: true <-
.print("start");
?td_dlt(DLTClientTDUrl);
.map.create(Request);
.map.put(Request, "url", "http://example.com");
.map.put(Request, "method", "POST");
.map.create(RequestHeaders);
.map.put(RequestHeaders, "Content-Type", "text/plain");
.map.put(Request, "headers", RequestHeaders);
.map.put(Request, "body", "abc");
.map.create(Response);
.map.put(Response, "statusCode", 200);
.map.create(ResponseHeaders);
.map.put(ResponseHeaders, "Content-Type", "text/plain");
.map.put(Response, "headers", RequestHeaders);
.map.put(Response, "body", "abc");
.map.create(JsonMessage);
.map.put(JsonMessage, "request", Request);
.map.put(JsonMessage, "response", Response);
org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(JsonMessage, Message);
.print(Message);
org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "sendTransaction", Message, R);
.print("end").



'
