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

process_robot("engraver_load").

available_storage_area("1").
available_storage_area("2").
available_storage_area("3").
available_storage_area("4").

best_diameter(1000).

best_storage(0).

!start.

+!start: true <-
    .print("start");
    ?callback(Callback);
    ?camera_hostname(Hostname);
    ?camera_id(Camera);
    ?text_width(TextWidth);
    ?x(X);
    ?y(Y);
    ?process_robot(Process);
    ?callback(Callback);
    .print("before compute storage area");
    ?compute_storage_area(TextWidth, X, Y, Storage);
    .print("storage area computed: ", Storage);
    ?grabspot(AIUrl, Storage, Hostname, Camera,  Grabspot);
    .print("grabspot received");
    .map.get(Grabspot, "confidence", Confidence);
    .map.get(Grabspot, "angle", Alpha);
    .map.get(Grabspot, "xcoordinate", XCoordinate);
    .map.get(Grabspot, "ycoordinate", YCoordinate);
    ?confidence(ConfidenceLevel);
    ?normalize_values(Alpha, XCoordinate, YCoordinate, NewAlpha, NewX, NewY);
    .print("NewAlpha = ", NewAlpha, ", NewX = ", NewX, ", NewY = ", NewY);
    .print("before use Mr Beam");
    ?actuators_td_url(ActuatorsUrl);
    ?engraver_td_url(EngraverUrl);
    Text = "IntellIoT";
    print("text to print: ", Text);
    //!move_piece_to_engraver(RobotUrl, Process, Callback);
    !print_mr_beam(ActuatorsUrl, EngraverUrl, Text);
    //!move_piece_back(RobotUrl, Process, Callback);
    .print("end").

+?compute_storage_area(Width, X, Y, StorageArea): ai_td_url(AIUrl) <-
    .print("start compute storage area");
    .findall(Z, available_storage_area(Z), L);
    .print("L: ", L);
    for (available_storage_area(ST)){
        .print("Test storage area: ", ST);
        .map.create(Headers);
        .map.put(Headers, "Content-Type", "application/json");
        .map.create(UriVariables);
        .map.put(UriVariables, "storageId", ST);
        ?camera_hostname(CameraHostname);
        ?camera_id(CameraId);
        .map.put(UriVariables,"cameraHostname", CameraHostname);
        .map.put(UriVariables,"cameraId", CameraId);
        ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", {}, Headers, UriVariables, Response);
        !process_storage_response(ST, Response);

    }
    RDiameter = Width + X + Y + 20;
    ?select_storage_area(RDiameter, StorageArea);
    .print("Storage area: ", StorageArea);
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

+?select_storage_area(RDiameter, BStorage): true <-
for (storage_area_diameter(S, D)){
    ?best_diameter(BDiameter);
    ?best_storage(BestStorage);
    ?new_selected_storage_area(RDiameter, BDiameter, BestStorage, S, NewBDiameter, NewBestStorage);
    -+best_diameter(NewBDiameter);
    -+best_storage(NewBestStorage);
}
?best_storage(BStorage);
if (BStorage == 0){
    .print("start goal fails");
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

+?read_property_with_DLT(TDUrl, Method,Headers, UriVariables, Response): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.readProperty(TDUrl, Method, Headers, UriVariables, Response);
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

+?grabspot(AIUrl, Storage, Hostname, Camera,  Grabspot): ai_td_url(AIUrl)<-
    //.map.create(Headers);
    .map.create(Headers);
    .map.put(Headers, "Content-Type", "application/json");
    //.map.create(UriVariables);
    .map.create(UriVariables);
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
    B = .map.key(Grabspot, "error_code");
    //?has_key(Grabspot, "error_code", B);
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


+!print_mr_beam(ActuatorsUrl, EngraverUrl, Text):
camera_engraver_hostname(CameraEngraverHostname) & camera_engraver_id(CameraEngraverId) & storage(Storage)
<-
    .print("print Mr Beam");
    !use_actuator(ActuatorsUrl, "lowerdown");
    //?compute_engraving_area(Storage, CameraEngraverHostname, CameraEngraverId, X_MrBeam, Y_MrBeam, TextWidth);
    !use_actuator(ActuatorsUrl, "close");
    //!engraver(EngraverUrl, ActuatorsUrl, Text, X_MrBeam, Y_MrBeam, TextWidth);
    !engraver(EngraverUrl, ActuatorsUrl, Text, 100, 180, 50);
    !use_actuator(ActuatorsUrl, "open");
    !use_actuator(ActuatorsUrl, "liftup");
    .print("end print mr beam").

+!engraver(EngraverUrl, ActuatorsUrl, Text, X_MrBeam, Y_MrBeam, TextWidth): true<-
    ?make_json_term(["text", "font", "variant","textWidth", "alignment", "positionReference","x", "y", "laserOn"], [[Text], "ABeeZee", "regular", TextWidth, "left", "center", X_MrBeam, Y_MrBeam, false], EngravingBody);
    //org.hyperagents.yggdrasil.jason.json.getTermAsJson(EngravingBodyJson, EngravingBodyString);
    .map.create(EngravingHeaders);
    .map.put(EngravingHeaders, "Content-Type","application/json");
    ?invoke_action_with_DLT(EngraverUrl, "createEngraveText", EngravingBody, EngravingHeaders, EngravingResponse);
    !exit(EngravingResponse, start);
    !wait(EngraverUrl, "waiting", 1000); //To create
    !use_actuator(ActuatorsUrl, "pushstart");
    !wait(EngraverUrl, "available", 1000).

+!use_actuator(ActuatorsUrl, Task): true <-
    .map.create(Body);
    .map.put(Body, "a", "a");
    .map.create(Headers);
    .map.put(Headers, "Content-Type", "application/json");
    ?invoke_action_with_DLT(ActuatorsUrl, Task, Body, Headers, Response);
    !exit(Response, start);
    .print("end use actuator").

+!wait(EngraverUrl, Status, Time): true <-
    .wait(Time);
    .print("wait for status: ", Status);
    //?read_property_with_DLT(EngraverUrl, "getJob", JobResponse);
    .map.create(Headers);
    .map.create(UriVariables);
    ?read_property_with_DLT(TDUrl, "getJob",Headers, UriVariables, JobResponse);
    //org.hyperagents.yggdrasil.jason.wot.readProperty(EngraverUrl, "getJob", JobResponse);
    !exit(JobResponse, start );
    //.map.get(JobResponse, "body", JobBody);
    ?get_body_as_json(JobResponse, JobBody);
    printJson(JobBody);
    .map.get(JobBody, "state", State);
    .print(State);
    if (not State == Status){
        !wait(EngraverUrl, Status, Time);
    }
    .print("end wait").

+?compute_engraving_area(StorageId, CameraHostname, CameraId, X_MrBeam, Y_MrBeam, TextWidth): ai_td_url(AIUrl) <-
    .map.create(Headers);
    .map.put(Headers, "My-Custom-Header", "My-Custom-Value"); //TODO: change
    .print("headers: ", Headers);
    .map.create(UriVariables);
    .map.put(UriVariables, "storageId", StorageId);
    .map.put(UriVariables, "cameraHostname", CameraHostname);
    .map.put(UriVariables, "cameraId", CameraId);
    ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", "", Headers, UriVariables, EAReply);
    .print("EA reply: ", EAReply);
    !exit(EAReply, start);
    //.map.get(EAReply, "body", EA);
    ?get_body_as_json(EAReply, EA);
    .print("EA", EA);
    B = .map.key(EA, "error_description");
    if (B){ //TODO: refactor without if
        .print("exit");
        .fail_goal(start);
    } else {
        .map.get(EA, "confidence", Confidence);
        .map.get(EA, "radius-mm", Radius);
        .map.get(EA, "xcoordinate", X);
        .map.get(EA, "ycoordinate", Y);
        X_MrBeam = 100 + Y;
        Y_MrBeam = 308 - X;
        TextWidth = 1.6 * Radius;
    }
    .print("engraving area computed").




+!gripper(RobotUrl, Status, Time): true <-
    .map.create(Headers);
    .map.put(Headers, "Content-Type", "application/json");
    ?make_json_term(["status"], [Status], Content);
    .print("content: ", Content);
    org.hyperagents.yggdrasil.jason.wot.invokeAction(RobotUrl, "setGripper", Content, Headers, Response);
    !exit(Response, start);
    .wait(Time)
    .print("end set gripper").





+!pose(RobotUrl, HeaderValue, PoseValue): true <- //TODO: refactor without if.
    .map.create(Headers);
    .map.put(Headers, "Content-Type", HeaderValue);
    -+content_type(HeaderValue);
    !pose_sub(RobotUrl, HeaderValue, PoseValue);
    .print("end pose").

+!pose_sub(RobotUrl, HeaderValue, PoseValue): content_type(CT) & CT=="application/ai+json" <-
        org.hyperagents.yggdrasil.jason.wot.invokeAction(RobotUrl, "setAIPose", PoseValue, Response);
        printJson(Response);
        !exit(Response, start).

+!pose_sub(RobotUrl, HeaderValue, PoseValue): content_type(CT) & CT=="application/namedpose+json" <-
        org.hyperagents.yggdrasil.jason.wot.invokeAction(RobotUrl, "setNamedPose", PoseValue, Response);
        !exit(Response, start).

+!pose_sub(RobotUrl, HeaderValue, PoseValue): content_type(CT) & CT=="application/joint+json" <-
        org.hyperagents.yggdrasil.jason.wot.invokeAction(RobotUrl, "setJointPose", PoseValue, Response);
        !exit(Response, start).

+!pose_sub(RobotUrl, HeaderValue, PoseValue): content_type(CT) & CT=="application/tcp+json" <-
        org.hyperagents.yggdrasil.jason.wot.invokeAction(RobotUrl, "setTcpPose", PoseValue, Response);
        !exit(Response, start).

+!move_piece_to_engraver(RobotUrl, Process, Callback): true <-
    ?make_json_term(["value", "callback"], ["home", Callback], PoseValueHome);
    ?create_named_pose_process(Process, Callback, PoseValueTransport);
    !gripper(RobotUrl, "close", 3000); //close gripper time = 3000, check position
    ?make_json_term(["value", "callback"], ["home", Callback], PoseValueHome);
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    .print("Before setting machine to use");
    ?create_named_pose_process(Process, Callback, PoseValueTransport);
    .print("The machine was selected");
    printJson(PoseValueTransport);
    !pose(RobotUrl,  "application/namedpose+json", PoseValueTransport);
    .print("The robot is at the  machine");
    !gripper(RobotUrl, "open", 1000); //open gripper, check position
    .print("The gripper is opened");
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    .print("The robot is at home").

+!move_piece_back(RobotUrl, Process, Callback): true <-
    ?make_json_term(["value", "callback"], ["home", Callback], PoseValueHome);
    ?create_named_pose_process(Process, Callback, PoseValueTransport);
    !pose(RobotUrl, "application/namedpose+json", PoseValueTransport);
    !gripper(RobotUrl, "close", 3000);
    ?default_x(Storage, DefaultX);
    ?default_y(Storage, DefaultY);
    ?default_alpha(Storage, DefaultAlpha);
    ?create_pose_ai(DefaultX, DefaultY, DefaultAlpha, Callback, PoseDefaultStorage);
    org.hyperagents.yggdrasil.jason.json.getTermAsJson(PoseDefaultStorage, PoseDefaultStorageString);
    !pose(RobotUrl, "application/ai+json", PoseDefaultStorage); //transport workpiece
    !gripper(RobotUrl, "open", 1000); //open gripper, check position
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    .print("the robot is back at the initial position").

+?create_pose_ai(X, Y, Alpha, Callback, PoseStorage): true <-
    ?make_json_term(["x","y","alpha"], [X,Y,Alpha], Value);
    .print("pose ai value: ",Value);
    ?make_json_term(["value","callback"],[Value, Callback], PoseStorage);
    .print("pose ai: ", PoseStorage).





'
