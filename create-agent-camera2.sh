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
      --device)
        DEVICE_BASE=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;
    esac
    shift
  done


if [[ $REQUIRED_PARAM_COUNTER -ne 3 ]]; then
    echo "$(basename $0)  --hyper <HyperMAS base URL> --device <Edge device base URL> -a/--agent <agent id>"
    exit 1
fi


curl --location --request POST ''"${HYPERMAS_BASE}"'/agents/' \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: '"${AGENT_ID}"'' \
--header 'Content-Type: text/plain' \
--data-raw 'ai_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/camera-ai").
hil_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/hil-service").
robot_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/robot-controller").
actuators_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/actuators").
engraver_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/engraver").
dlt_client_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/dlt-client").

camera_hostname("camera-storage.fritz.box").

camera_id("workpieceStorage").

camera_engraver_hostname("camera-engraver.fritz.box").

camera_engraver_id("laserEngraver").

text("IntellIoT").

fontsize(20).

process("laser").

storage("1").
callback("http://example.org/callback").

ai_session_id(1007).

default_x("1", 0.14).
default_x("2", 0.41).
default_x("3", 0.7).
default_x("4", 0.98).

default_y("1", 0.37).
default_y("2", 0.37).
default_y("3", 0.37).
default_y("4", 0.37).

default_alpha("1", 0).
default_alpha("2", 0).
default_alpha("3", 0).
default_alpha("4", 0).

confidence(90).

loop_index(0).

text_width(10).

x(10).
y(10).

!start.

+!start: true <-
    .print("start");
    ?callback(Callback);
    ?camera_hostname(Hostname);
    ?camera_id(Camera);
    ?text_width(TextWidth);
    ?x(X);
    ?y(Y);
    .print("before compute storage area");
    ?compute_storage_area(TextWidth, X, Y, Storage);
    .print("storage area computed");
    ?grabspot(AIUrl, Storage, Hostname, Camera,  Grabspot);
    .print("grabspot received");
    .map.get(Grabspot, "confidence", Confidence);
    .map.get(Grabspot, "angle", Alpha);
    .map.get(Grabspot, "xcoordinate", X);
    .map.get(Grabspot, "ycoordinate", Y);
    ?confidence(ConfidenceLevel);
    ?normalize_values(Alpha, X, Y, NewAlpha, NewX, NewY);
    .print("NewAlpha = ", NewAlpha, ", NewX = ", NewX, ", NewY = ", NewY).

+?compute_storage_area(Width, X, Y, StorageArea): ai_td_url(AIUrl) <-
    for (available_storage_area(X) & X){
        ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", "", Headers, UriVariables, Response);
        !process_storage_response(X, Response);

    }
    RDiameter = Width + X + Y + 20
    ?select_storage_area(RDiameter, StorageArea)
    .print("end compute storage area").

+!process_storage_response(StorageNumber, Response): true <-
    !exit(Response, process_storage_response);
    ?get_body_as_json(Response, Body);
    .map.get(Body, "confidence", C);
    if (C>95){
        .map.get(Body, "radius", R1);
        R = R1 * 2;
        +storage_area_diameter(StorageNumber, R)

    } else {
        +storage_area_diameter(StorageNumber, 0);

    }.


+?new_selected_storage_area(RDiameter, BDiameter, CurrentBestStorage, StorageAreaToTest, NewBDiameter, NewBestStorage): true <-
    ?storage_area_diameter(StorageAreaToTest, CDiameter);
    if (CDiameter>RDiameter & CDiameter <BDiameter){
        NewBDiameter = CDiameter;
        NewBestStorage = StorageAreaToTest;
    } else {
        NewBDiameter = BDiameter;
        NewBestStorage = CurrentBestStorage;
    }
    .print("end new selected storage area").

+?select_storage_area(RDiameter, BestStorage): true <-
BDiameter = 1000;
BestStorage = 0;
for (storage_area_diameter(S, D)){
    ?new_selected_storage_area(D, BDiameter, BestStorage, S, NewBDiameter, NewBestStorage);
    BDiameter = NewBDiameter;
    BestStorage = NewBestStorage;
}
if (BestStorage == 0){
    .fail_goal(start);
}

.print("storage area selected").

+!invoke_action_with_DLT(TDUrl, Method, Body, Headers, UriVariables): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, UriVariables, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "sendTransaction", Message, R).

+?invoke_action_with_DLT(TDUrl, Method, Body, Headers, UriVariables, Response): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, UriVariables, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "sendTransaction", Message, R).


+?invoke_action_with_DLT(TDUrl, Method, Body, Headers, Response): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "sendTransaction", Message, R).

+?normalize_values(Alpha, X, Y, NewAlpha, NewX, NewY): true <-
    X1 = X/1000;
    Y1 = Y/1000;
    ?normalize_boundaries(Alpha, -20, 25, NewAlpha);
    ?normalize_boundaries(X1, 0.08, 1.05, NewX);
    ?normalize_boundaries(Y1, 0.365, 0.5, NewY).

+?normalize_boundaries(X, Low, High, NewX): true <-
    if (X < Low){
        NewX = Low;
    } else {
        if (X > High){
            NewX = High;
        } else {
            NewX = X;
        }
    }.

+?grabspot(AIUrl, Storage, Hostname, Camera,  Grabspot): true <-
    //.map.create(Headers);
    Headers = {};
    .map.put(Headers, "Content-Type", "application/json");
    //.map.create(UriVariables);
    UriVariables = {};
    .map.put(UriVariables, "storageId", Storage);
    .map.put(UriVariables, "cameraHostname", Hostname);
    .map.put(UriVariables, "cameraId", Camera);
    .print(UriVariables);
    //org.hyperagents.yggdrasil.jason.wot.invokeAction(AIUrl, "getGrabspot", "", Headers, UriVariables, Response);
    ?invoke_action_with_DLT(AIUrl, "getGrabspot", "", Headers, UriVariables, Response);
    !exit(Response, start);
    //.map.get(Response, "body", GrabspotString);
    ?get_body_as_json(Response, Grabspot);
    .print("grabspot: ", Grabspot);
    //.map.key(Grabspot, "error_code", B);
    ?has_key(Grabspot, "error_code", B);
    .print("has error code: ", B);
    if (B){ //TODO: refactor without if
        .print("exit");
        .fail_goal(start);
    }
    .print("grabspot computed");
    .print("end camera").

+!exit(Response, Goal): true <- //TODO: refactor without if
?get_status(Response, Code);
.print("status code: ",Code);
if (Code > 299){
.print("exit");
.fail_goal(Goal);
}
.print("end exit").

+?get_status(Response, Status): true <-
.map.get(Response, "response", R);
.map.get(R, "statusCode", Status).

+?make_json_term(AttributeList, ValueList, Json): true <-
    createMapTerm(AttributeList, ValueList, Json);
    .print("make json term: ", Json).

+?make_json_string(AttributeList, ValueList, JsonString): true <-
    createMapTerm(AttributeList, ValueList, Json);
    .print("json created: ", Json);
    org.hyperagents.yggdrasil.jason.json.getTermAsJson(Json, JsonString);
    .print("json string: ", JsonString).

+?get_body(Response, Body): true <-
.map.get(Response, "response", R);
.map.get(R, "body", Body).

+?get_body_as_json(Response, Body): true <-
.map.get(Response, "response", R);
.map.get(R, "body", B);
org.hyperagents.yggdrasil.jason.json.createTermFromJson(B, Body).






'
