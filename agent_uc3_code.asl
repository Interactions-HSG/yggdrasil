ai_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/camera-ai").
hil_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/hil-service").
robot_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/robot-controller").
actuators_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/actuators").
engraver_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/engraver").
milling_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/milling").
dlt_client_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/dlt-client").
iobox_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/iobox").
goal_interface_td_url("http://edge.fritz.box:8888/workspaces/uc3/artifacts/goal-interface").

camera_hostname("camera-storage.fritz.box").
camera_id("workpieceStorage").
camera_engraver_hostname("camera-engraver.fritz.box").
camera_engraver_id("laserEngraver").
camera_milling_hostname("camera-milling.fritz.box").
camera_milling_id("millingMachine").
actual_confidence(100).
confidence_storage(95).
confidence_hil(95).
callback("http://example.org/callback").
ai_session_id(1007).
default_x(1, 0.14).
default_x(2, 0.41).
default_x(3, 0.7).
default_x(4, 0.98).
default_y(1, 0.37).
default_y(2, 0.37).
default_y(3, 0.37).
default_y(4, 0.37).
default_alpha(1, 0).
default_alpha(2, 0).
default_alpha(3, 0).
default_alpha(4, 0).
loop_index(0).
process_robot_put("engraver_load").
process_robot_back("engraver_load").
storage(1).
used_storage(1).
storage_engraver(1).
available_storage_area(1).
available_storage_area(2).
available_storage_area(3).
available_storage_area(4).
best_diameter(1000).
best_storage(0).
confidence_received(0).
storage_area_diameter(0).
storage_area_number(4).

//Information Engraving
process("laser").
text("IntellIoT").
text_width(50).
font("ABeeZee").
variant("regular").
alignment("left").
position_reference("center").
x(0).
y(0).
test(true).
is_working(false).

//!start.

+message(Id, Message): true <- //check
    .map.get(Message, "text", Text);
    -+text(Text);
    .map.get(Message,"font", Font);
    -+font(Font);
    .map.get(Message, "process", Process);
    -+process(Process);
    .map.get(Message, "variant", Variant);
    -+variant(Variant);
    .map.get(Message, "alignment", Alignment);
    -+alignment(Alignment);
    .map.get(Message, "positionReference", PositionReference);
    -+position_reference(PositionReference);
    .map.get(Message, "posx", X);
    -+x(X);
    .map.get(Message, "posy", Y);
    -+y(Y);
    .map.get(Message, "textWidth", TextWidth);
    -+text_width(TextWidth);
    .map.get(Message, "test", Test);
    -+test(Test);
    .map.get(Message, "confidence", GIConfidence);
    -+confidence_hil(GIConfidence);
    !check_goal.

+!check_goal: text_width(Width) & Width<=500 <-
    .print("Width: ", Width)
    !update_process_robot;
    !start.

+!check_goal: text_width(Width) & Width>500 <-
    .print("Width: ", Width)
    !send_message_goal_interface("rejected", "The text width is superior to 500.").

-!check_goal: true <-
    .print("Width: ", Width)
    !send_message_goal_interface("failed", "check_goal failed").

+!start: is_working(B) & not B <-
    !send_message_goal_interface("accepted");
    -+is_working(true);
    .print("start");
    -+best_storage(0);
    !reinitialize;
    ?actuators_td_url(ActuatorsUrl);
    ?engraver_td_url(EngraverUrl);
    ?robot_td_url(RobotUrl);
    !update_callback;
    ?callback(Callback);
    ?camera_hostname(Hostname);
    ?camera_id(Camera);
    ?text(Text);
    ?text_width(TextWidth);
    ?x(X);
    ?y(Y);
    ?process(Process);
    ?process_robot_put(ProcessRobotPut);
    ?process_robot_back(ProcessRobotBack);
    .print("before compute area");
    ?compute_storage_area(TextWidth, X, Y, Storage);
    .print("storage area: ", Storage);
    -+used_storage(Storage);
    ?grabspot(Storage, Grabspot);
    .map.get(Grabspot, "confidence", Confidence);
    .map.get(Grabspot, "angle", Alpha);
    .map.get(Grabspot, "xcoordinate", XCoordinate);
    .map.get(Grabspot, "ycoordinate", YCoordinate);
    -+actual_confidence(Confidence);
    ?normalize_values(Alpha, XCoordinate, YCoordinate, NewAlpha, NewX, NewY);
    .print("NewAlpha = ", NewAlpha, ", NewX = ", NewX, ", NewY = ", NewY);
    ?create_pose_ai(NewX, NewY, NewAlpha, Callback, PoseStorage);
    !pose(RobotUrl, "application/ai+json", PoseStorage);
    !use_hil;
    .print("move to engraver");
    !move_piece_to_engraver(ProcessRobotPut, Callback);
    .print("before print");
    !print(Process, Text);
    .print("print done");
    .print("move piece back");
    !move_piece_back(ProcessRobotBack, Callback);
    -+best_storage(0);
    -+is_working(false);
    !send_message_goal_interface("completed");
    .print("end").

+!start: is_working(B) & B <-
    !send_message_goal_interface("busy").

+!reinitialize: true <-
    !engraver_table_up;
    !robot_start.

+?compute_storage_area(TextWidth, X, Y, Storage): true <- //TODO: the process may be wrong (to select smaller piece with a correct diameter)
    ?storage_area_number(N);
    RDiameter = TextWidth + X + Y + 20;
    -+required_diameter(RDiameter);
    !compute_storage_area_explore(1, N);
    !test_best_storage;
    ?best_storage(Storage).

+!test_best_storage: best_storage(S) & S == 0 <-
    .print("no storage found");
    -+is_working(false);
    .fail_goal(start).

-!test_best_storage: true <-
    .print("best storage defined").

+!compute_storage_area_explore(I, N): I<N & ai_td_url(AIUrl) <-
    ?create_json(["Content-Type"], ["application/json"], Headers);
    ?camera_hostname(CameraHostname);
    ?camera_id(CameraId);
    ?create_json(["storageId", "cameraHostname", "cameraId"], [I, CameraHostname, CameraId], UriVariables);
    ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", {}, Headers, UriVariables, Response);
    .print("current storage number: ", I);
    .print("current response: ", Response);
    !process_storage_response(I, Response);
    !compute_storage_area_explore(I+1, N).

+!compute_storage_area_explore(I, N): I>=N <-
    .print("storage area computed").

+!process_storage_response(StorageNumber, Response): true <-
    !exit(Response, process_storage_response);
    ?get_body_as_json(Response, Body);
    .map.get(Body, "confidence", C);
    -+confidence_received(C);
    .print("new confidence: ", C);
    !add_storage_area_diameter(StorageNumber, C,  Body).

+!add_storage_area_diameter(StorageNumber, C, Body): C > 95  <-
    .map.get(Body, "radius", R);
    D = R * 2;
    !check_diameter(StorageNumber, D).

+!add_storage_area_diameter(StorageNumber, C, Body):  C <= 95 <-
    .print("do nothing add storage diameter").

-!add_storage_area_diameter(StorageNumber, C, Body): true <-
    .print("do nothing add storage diameter").

+!check_diameter(StorageNumber, D): required_diameter(RDiameter) & best_diameter(BDiameter) & D>=RDiameter & D<BDiameter <-
    -+best_diameter(D);
    -+best_storage(StorageNumber).

-!check_diameter(StorageNumber, D): true <-
    .print("current diameter: ", D);
    .print("current storage number: ", StorageNumber);
    .print("do nothing in check diameter").

+?grabspot(Storage, Grabspot): ai_td_url(AIUrl) & camera_hostname(Hostname)
    & camera_id(Camera)
    <-
    ?create_json(["Content-Type"], ["Content-Type"], Headers);
    ?create_json(["storageId", "cameraHostname", "cameraId"], [Storage, Hostname, Camera], UriVariables);
    ?invoke_action_with_DLT(AIUrl, "getGrabspot", {}, Headers, UriVariables, Response);
    !exit(Response, start);
    ?get_body_as_json(Response, Grabspot);
    B = .map.key(Grabspot, "error_code");
    !conditional_exit_goal(B, start). //To check

+?normalize_values(Alpha, XCoordinate, YCoordinate, NewAlpha, NewX, NewY): true <-
    X1 = XCoordinate/1000;
    .print("x1: ", X1);
    Y1 = YCoordinate/1000;
    .print("y1: ", Y1);
    ?normalize_boundaries(Alpha, -20, 25, NewAlpha);
    .print("new alpha: ", NewAlpha);
    ?normalize_boundaries(X1, 0.08, 1.05, NewX);
    .print("new x: ", NewX);
    ?normalize_boundaries(Y1, 0.365, 0.5, NewY);
    .print("new y: ", NewY).

+?normalize_boundaries(X, Low, High, NewX): X<Low <-
    .print("too low, low-x=", Low-X);
    NewX=Low.

+?normalize_boundaries(X, Low, High, NewX): X>High <-
    .print("too high x-high=", X-High);
    NewX=High.

+?normalize_boundaries(X, Low, High, NewX): X>=Low & X<=High <-
    .print("correct");
    NewX=X.

+?compute_engraving_area(StorageId, CameraHostname, CameraId, X, Y, X_MrBeam, Y_MrBeam): ai_td_url(AIUrl) & process(Process) & Process == "laser"<-
    ?create_json(Headers);
    ?create_json(["storageId", "cameraHostname", "cameraId"], [StorageId, CameraHostname, CameraId], UriVariables);
    ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", {}, Headers, UriVariables, EAReply);
    !exit(EAReply, start);
    ?get_body_as_json(EAReply, EA);
    B = .map.key(EA, "error_description");
    !conditional_exit_goal(B, start);
    .map.get(EA, "confidence", Confidence);
    .map.get(EA, "radius", Radius);
    .map.get(EA, "xcoordinate", X_Camera);
    .map.get(EA, "ycoordinate", Y_Camera);
    X_MrBeam = 100 + Y_Camera + X; //Initial formula 100 + Y_Camera
    Y_MrBeam = 308 - X_Camera + Y. //Initial formula: 308 - X_Camera

+?compute_engraving_area_milling(StorageId, CameraHostname, CameraId, X, Y, X_Milling, Y_Milling): ai_td_url(AIUrl) & process(Process) & Process == "milling" <-
    ?create_json(Headers);
    ?create_json(["storageId", "cameraHostname", "cameraId"], [StorageId, CameraHostname, CameraId], UriVariables);
    ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", {}, Headers, UriVariables, EAReply);
    !exit(EAReply, start);
    ?get_body_as_json(EAReply, EA);
    B = .map.key(EA, "error_description");
    !conditional_exit_goal(B, start);
    .map.get(EA, "confidence", Confidence);
    .map.get(EA, "radius", Radius);
    .map.get(EA, "xcoordinate", X_Camera);
    .map.get(EA, "ycoordinate", Y_Camera);
    X_Milling= 140 + X; //140 + Y_Camera + X; Initial formula 100 + Y_Camera
    Y_Milling= Y_Camera + 28 + Y. //100 - X_Camera + Y. //Initial formula: 308 - X_Camera


 +!use_hil: actual_confidence(C) & hil_td_url(HILUrl) & confidence_hil(CM) & C<CM <-
    .print("use hil");
    RobotId =  "UR5";
    CameraId = "robot-camera";
    SessionType = "wood_piece_picking";
    ?generate_random_session_id(100000, AISessionId);
    .term2string(AISessionId, AISessionIdString);
    .print("AI session Id: ", AISessionIdString);
    !create_hil_session(HILUrl, AISessionIdString, RobotId, CameraId, SessionType);
    ?hil_content_loop(AISessionIdString, HILContent).

+!use_hil: actual_confidence(C) & confidence_hil(CM) & C>=CM <-
    .print("do not use hil").

+?generate_random_session_id(C, Random): true <-
    .random(N);
    M = C * N;
    org.hyperagents.yggdrasil.jason.math.floor(M, R);
    Random = R + 1.

+!create_hil_session(HILUrl, AISessionId, RobotId, CameraId, SessionType): true <-
    ?create_json(["Content-Type"], ["application/json"], Headers);
    ?create_json(["aiSessionId", "robotId", "cameraId", "sessionType"], [AISessionId, RobotId, CameraId, SessionType], SessionInfo);
    ?invoke_action_with_DLT(HILUrl, "createSession", SessionInfo, Headers, Response);
    !exit(Response, start);
    .print("hil session created").

+?hil_content_loop(AISessionId, HILContent): hil_td_url(HILUrl) <-
    ?hil_content(AISessionId, HILCurrentContent);
    .print("hil current content: ", HILCurrentContent);
    .map.get(HILCurrentContent, "status", Status);
    ?hil_content_status(AISessionId, HILCurrentContent, Status, HILContent).

+?hil_content_status(AISessionId,HILCurrentContent, Status, HILContent): not Status == "finished" <-
    .wait(1000);
    ?hil_content_loop(AISessionId, _).

+?hil_content_status(AISessionId, HILCurrentContent, Status, HILContent): Status == "finished" <-
    HILContent = HILCurrentContent;
    org.hyperagents.yggdrasil.jason.json.getTermAsJson(HILContent, HILStr);
    .print("The HIL session has finished: ", HILStr).

+?hil_content(AISessionId, HILContent): hil_td_url(HILUrl) <-
    ?create_json(["Content-Type"], ["application/json"], Headers);
    ?create_json(["sessionId"], [AISessionId], UriVariables);
    ?read_property_with_DLT(HILUrl, "getSession", Headers, UriVariables, Response);
    !exit(Response, start);
    ?get_body_as_json(Response, HILContent).


+!robot_start: robot_td_url(RobotUrl) <-
    ?callback(Callback);
    ?create_json(["value", "callback"], ["home", Callback], PoseValueHome);
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    !gripper(RobotUrl, "open", 1000).

+!move_piece_to_engraver(Process, Callback): robot_td_url(RobotUrl) <-
    .print("robot process: ", Process);
    !check_process(Process);
    .print("process checked");
    ?create_json(["value", "callback"], ["home", Callback], PoseValueHome);
    .print("pose value home created");
    ?create_named_pose_process(Process, Callback, PoseValueTransport);
    .print("pose process created");
    !gripper(RobotUrl, "close", 3000); //close gripper time = 3000, check position
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    .print("Before setting machine to use");
    .print("The machine was selected");
    !pose(RobotUrl,  "application/namedpose+json", PoseValueTransport);
    .print("The robot is at the  machine");
    !gripper(RobotUrl, "open", 1000); //open gripper, check position
    .print("The gripper is opened");
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    .print("The robot is at home").

+!check_process(Process): Process == "engraver-load" <-
    !engraver_table_up.

+!check_process(Process): Process == "milling_machine_place" <-
    .print("go to milling machine").

-check_process(Process): true <-
    .print("do not need to move table up").

+!move_piece_back(Process, Callback): robot_td_url(RobotUrl) & used_storage(Storage) <-
    ?create_json(["value", "callback"], ["home", Callback], PoseValueHome);
    ?create_named_pose_process(Process, Callback, PoseValueTransport);
    !pose(RobotUrl, "application/namedpose+json", PoseValueTransport);
    !gripper(RobotUrl, "close", 3000);
    ?default_x(Storage, DefaultX);
    ?default_y(Storage, DefaultY);
    ?default_alpha(Storage, DefaultAlpha);
    ?create_pose_ai(DefaultX, DefaultY, DefaultAlpha, Callback, PoseDefaultStorage);
    !pose(RobotUrl, "application/ai+json", PoseDefaultStorage); //transport workpiece
    !gripper(RobotUrl, "open", 1000); //open gripper, check position
    !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
    .print("the robot is back at the initial position").

+?create_pose_ai(X, Y, Alpha, Callback, PoseStorage): true <-
    ?create_json(["x","y","alpha"], [X,Y,Alpha], Value);
    ?create_json(["value","callback"],[Value, Callback], PoseStorage).

+?create_named_pose_process(ProcessRobot, Callback, NamedPose): true <-
    ?create_json(["value", "callback"], [ProcessRobot, Callback], NamedPose).

+!pose(RobotUrl, HeaderValue, PoseValue): true <-
    ?create_json(["Content-Type"], [HeaderValue], Headers);
    !pose_sub(RobotUrl, HeaderValue, Headers, PoseValue).

+!pose_sub(RobotUrl, HeaderValue, Headers, PoseValue): HeaderValue == "application/ai+json" <-
    ?invoke_action_with_DLT(RobotUrl, "setAIPose", PoseValue, Headers, Response);
    !exit(Response, start).

+!pose_sub(RobotUrl, HeaderValue, Headers, PoseValue): HeaderValue == "application/namedpose+json" <-
    ?invoke_action_with_DLT(RobotUrl, "setNamedPose", PoseValue, Headers, Response);
    !exit(Response, start).

+!pose_sub(RobotUrl, HeaderValue, Headers, PoseValue): HeaderValue == "application/joint+json" <-
    ?invoke_action_with_DLT(RobotUrl, "setJointPose", PoseValue, Headers, Response);
    !exit(Response, start).

+!pose_sub(RobotUrl, HeaderValue, Headers, PoseValue): HeaderValue == "application/tcp+json" <-
    ?invoke_action_with_DLT(RobotUrl, "setTcpPose", PoseValue, Headers, Response);
    !exit(Response, start).

+!gripper(RobotUrl, Status, Time): true <-
    ?create_json(["Content-Type"], ["application/json"], Headers);
    ?create_json(["status"], [Status], Content);
    ?invoke_action_with_DLT(RobotUrl, "setGripper", Content, Headers, Response);
    !exit(Response, start);
    .wait(Time).

+!print(Process, Text): Process == "laser" <-
    !print_mr_beam(Text).

+!print(Process, Text): Process == "milling" <-
    !print_milling;
    .print("end print milling").

+!engraver_table_up: actuators_td_url(ActuatorsUrl) <-
    ?create_json(Headers);
    ?create_json(UriVariables);
    ?read_property_with_DLT(ActuatorsUrl, "tableStatus", Headers, UriVariables, Response);
    ?get_body_as_json(Response, Body);
    .map.get(Body, "status", Status);
    !check_status_table(Status).

+!check_status_table(Status): Status == "up" <-
    .print("the table is opened").

+!check_status_table(Status): actuators_td_url(ActuatorsUrl) & not (Status == "up") <-
    .print("the table is closed");
    !use_actuator(ActuatorsUrl, "open");
    !use_actuator(ActuatorsUrl, "liftup");
    .print("the table is opened").

+!print_mr_beam(Text): actuators_td_url(ActuatorsUrl) &
    engraver_td_url(EngraverUrl) & camera_engraver_hostname(CameraEngraverHostname)
    & camera_engraver_id(CameraEngraverId) & storage_engraver(StorageEngraver) & text_width(TextWidth) & x(X) & y(Y)
    <-
    .print("print Mr Beam");
    !use_actuator(ActuatorsUrl, "lowerdown");
    ?compute_engraving_area(StorageEngraver, CameraEngraverHostname, CameraEngraverId, X, Y, X_MrBeam, Y_MrBeam);
    !use_actuator(ActuatorsUrl, "close");
    !engraver(EngraverUrl, ActuatorsUrl, X_MrBeam, Y_MrBeam);
    !use_actuator(ActuatorsUrl, "open");
    !use_actuator(ActuatorsUrl, "liftup");
    .print("end print mr beam").

+!print_milling: milling_td_url(MillingUrl)  & camera_milling_hostname(CameraEngraverHostname) &
    camera_milling_id(CameraEngraverId) & storage_engraver(Storage) & text_width(TextWidth) & x(X) & y(Y) <-
    .print("print milling");
    ?create_json(["machineId"], ["511"], UriVariables);
    !use_milling_actuator("closeClamp", UriVariables);
    .print("Before compute engraving area milling.");
    ?compute_engraving_area_milling(Storage, CameraEngraverHostname, CameraEngraverId,X, Y, X_Milling, Y_Milling);
    .print("After compute engraving area milling.");
    !engraver_milling(MillingUrl, X_Milling, Y_Milling);
    !use_milling_actuator("stopSpindle", UriVariables);
    !use_milling_actuator("openClamp", UriVariables);
    .print("end print milling").

+!use_actuator(ActuatorsUrl,Task): true <-
    ?create_json(Body);
    ?create_json(["Content-Type"], ["application/json"], Headers);
    ?invoke_action_with_DLT(ActuatorsUrl, Task, Body, Headers, Response);
    !exit(Response, start).

+!use_milling_actuator(Task, UriVariables): milling_td_url(MillingUrl) <-
    ?create_json(Body);
    ?create_json(["Content-Type"], ["application/json"], Headers);
    ?invoke_action_with_DLT(MillingUrl, Task, Body, Headers, UriVariables, Response);
    !exit(Response, start).

+!engraver(EngraverUrl, ActuatorsUrl, X_MrBeam, Y_MrBeam): text(Text) & text_width(TextWidth) & font(Font) & variant(Variant) & alignment(Alignment) & position_reference(PositionReference) & test(Test) <-
    ?opposite(Test, LaserOn);
    ?create_json(["text", "font", "variant","textWidth", "alignment", "positionReference","x", "y", "laserOn"], [[Text], Font, Variant, TextWidth, Alignment, PositionReference, X_MrBeam, Y_MrBeam, LaserOn], EngravingBody);
    ?create_json(["Content-Type"], ["application/json"], EngravingHeaders);
    ?invoke_action_with_DLT(EngraverUrl, "createEngraveText", EngravingBody, EngravingHeaders, EngravingResponse);
    !exit(EngravingResponse, start);
    !wait(EngraverUrl, "waiting", 1000);
    !use_actuator(ActuatorsUrl, "pushstart");
    !wait(EngraverUrl, "available", 1000).

+!engraver_milling(MillingUrl, X_Milling, Y_Milling): text(Text) & text_width(TextWidth) & font(Font) & variant(Variant) & alignment(Alignment) & position_reference(PositionReference) & test(Test) <-
    .print("Engraver Milling");
    ?opposite(Test, LaserOn);
    ?create_json(["text", "font", "variant","textWidth", "alignment", "positionReference","x", "y", "laserOn", "noDrilling"], [[Text], Font, Variant, TextWidth, Alignment, PositionReference, X_Milling, Y_Milling, LaserOn, Test], EngravingBody);
    ?create_json(["Content-Type"], ["application/json"], EngravingHeaders);
    .print("Before engrave text milling");
    ?invoke_action_with_DLT(MillingUrl, "createEngraveText", EngravingBody, EngravingHeaders, EngravingResponse);
    !exit(EngravingResponse, start);
    .print("Before wait milling");
    !wait(MillingUrl, "available", 1000).

+!wait(EngraverUrl, Status, Time): engraver_td_url(EngraverUrl) <-
    .wait(Time);
    !check_temperature(40, 60);
    ?create_json(Headers);
    ?create_json(UriVariables);
    ?read_property_with_DLT(EngraverUrl, "getJob",Headers, UriVariables, JobResponse);
    !exit(JobResponse, start );
    ?get_body_as_json(JobResponse, JobBody);
    .map.get(JobBody, "state", State);
    !continue_wait(State, Status, Time).

+!continue_wait(State, Status, Time): not (State == Status) <-
    !wait(EngraverUrl, Status, Time).

+!continue_wait(State, Status, Time):  (State == Status) <-
    .print("stop waiting").

+!check_temperature(IntermediateTemperature, MaxTemperature): true <-
    ?get_mirocard_id(DeviceId);
    ?get_temperature(DeviceId, Temperature);
    !test_temperature(Temperature, MaxTemperature).

-!check_temperature(IntermediateTemperature,MaxTemperature): true <-
    .print("do nothing").

+!test_temperature(Temperature, IntermediateTemperature,MaxTemperature): Temperature>MaxTemperature <-
    .fail_goal(start).

+!test_temperature(Temperature, IntermediateTemperature,MaxTemperature): Temperature<=MaxTemperature & Temperature>IntermediateTemperature <-
    !send_warning(Temperature).

+!test_temperature(Temperature, MaxTemperature): Temperature<=MaxIntermediateTemperature <-
    .print("temperature is OK").

+!send_warning(Temperature): true <-
    .concat("Warning! The temperature is: ", Temperature, Message);
    !send_message_goal_interface("intermediate", Message).

+?get_mirocard_id(DeviceId): iobox_td_url(IOBoxTDUrl) <-
    ?create_json([], [], Headers);
    ?create_json([], [], UriVariables);
    ?read_property_with_DLT(IOBoxTDUrl, "getBLEDevices",Headers, UriVariables, Response);
    ?get_body_as_json(Response, ResponseBody);
    .findall(K, .map.key(ResponseBody,K), LL);
    .nth(0, LL, Key);
    .map.get(ResponseBody, Key, Value);
    .map.get(Value, "name", DeviceId).

+?get_temperature(DeviceId, Temperature): iobox_td_url(IOBoxTDUrl) <-
    ?create_json([], [], Headers);
    ?create_json(["deviceId"], [DeviceId], UriVariables);
    ?read_property_with_DLT(IOBoxTDUrl, "getTemperature",Headers, UriVariables, Response);
    ?get_body_as_json(Response, ResponseBody);
    .map.get(ResponseBody, "temperature", Temperature).

+?get_body_as_json(Response, Body): true <-
    .map.get(Response, "response", R);
    .map.get(R, "body", B);
     org.hyperagents.yggdrasil.jason.json.createTermFromJson(B, Body).

+?create_json(L1, L2, Json): true <-
    org.hyperagents.yggdrasil.jason.json.createMapTerm(L1, L2, Json).

-?create_json(L1, L2, Json): true <-
    .print("L1: ", L1);
    .print("L2: ", L2);
    .print("Json: ", Json).

+?create_json(Json): true <-
    org.hyperagents.yggdrasil.jason.json.createMapTerm([], [], Json).

+!send_message_goal_interface(Status, Message): goal_interface_td_url(GoalInterfaceTDUrl) <-
    ?create_json(["status", "custom"], [Status, Message], Body);
    ?create_json([], [], Headers);
    ?create_json([], [], UriVariables);
    !invoke_action_with_DLT(GoalInterfaceTDUrl, "sendNotification", Body, Headers, UriVariables).

-!send_message_goal_interface(Status, Message): true <-
    .print("Message not sent").

+!send_message_goal_interface(Status): goal_interface_td_url(GoalInterfaceTDUrl) <-
    ?create_json(["status"], [Status], Body);
    ?create_json([], [], Headers);
    ?create_json([], [], UriVariables);
    !invoke_action_with_DLT(GoalInterfaceTDUrl, "sendNotification", Body, Headers, UriVariables).

-!send_message_goal_interface(Status): true <-
    .print("Message not sent").

+!invoke_action_with_DLT(TDUrl, Method, Body, Headers, UriVariables): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, UriVariables, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    !send_transaction(DLTClientTDUrl, Message).

+?invoke_action_with_DLT(TDUrl, Method, Body, Headers, UriVariables, Response): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, UriVariables, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    !send_transaction(DLTClientTDUrl, Message).

+?invoke_action_with_DLT(TDUrl, Method, Body, Headers, Response): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    !send_transaction(DLTClientTDUrl, Message).

+?read_property_with_DLT(TDUrl, Method,Headers, UriVariables, Response): dlt_client_td_url(DLTClientTDUrl) <-
    org.hyperagents.yggdrasil.jason.wot.readProperty(TDUrl, Method, Headers, UriVariables, Response);
    org.hyperagents.yggdrasil.jason.dlt.getAsDLTMessage(Response, Message);
    !send_transaction(DLTClientTDUrl, Message).

+!send_transaction(DLTClientTDUrl, Message): true <-
    .print("send transaction to DLT");
    .print("Message: ", Message);
    org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "sendTransaction", Message, R).

-!send_transaction(DLTClientTDUrl, Message): true <-
    .print("interaction not stored in DLT").

+!update_callback: true <-
    getAgHypermediaName(Name);
    .concat(Name, "/message", Callback);
    -+callback(Callback).

+?get_callback(Callback): true <-
    getAgHypermediaName(Name);
    concat(Name, "/message", Callback).

+!exit(Response, Goal): true <-
    ?get_status(Response, Code);
    !exit_code(Code, Goal).

+?get_status(Response, Status): true <-
    .map.get(Response, "response", R);
    .map.get(R, "statusCode", Status).

+!exit_code(Code, Goal): Code > 299 <-
    .print("exit");
    -+is_working(false);
    !send_message_goal_interface("failed", "exit_code failed");
    .fail_goal(Goal).

+!exit_code(Code, Goal): Code <= 299 <-
    .print("no exit").

+!conditional_exit_goal(B, Goal):  B <-
    .print("exit");
    -+is_working(false);
    !send_message_goal_interface("failed", "conditional_exit_goal failed");
    .fail_goal(Goal).

+!conditional_exit_goal(B, Goal): not B <-
    .print("no exit").

+?opposite(B1, B2): B1 <-
    B2 = false.

+?opposite(B1, B2): not B1 <-
    B2 = true.

+!update_process_robot: process(Process) & Process == "laser" <-
    -+process_robot_put("engraver_load");
    -+process_robot_back("engraver_load").

+!update_process_robot: process(Process) & Process == "milling" <-
    -+process_robot_put("milling_machine_place");
    -+process_robot_back("milling_machine_pick").
