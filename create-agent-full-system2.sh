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
            milling_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/milling").
            dlt_client_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/dlt-client").

            //Camera and Storage Information

            camera_hostname("camera-storage.fritz.box").

            camera_id("workpieceStorage").

            camera_engraver_hostname("camera-engraver.fritz.box").

            camera_engraver_id("laserEngraver").

            actual_confidence(100).

            confidence(90).

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



            loop_index(0).


            x(10).
            y(10).

            process_robot("engraver_load").

            available_storage_area("1").
            available_storage_area("2").
            available_storage_area("3").
            available_storage_area("4").

            best_diameter(1000).

            best_storage(0).

            confidence_received(0).

            storage_area_diameter(0).



            //Information Engraving
            process("laser").
            text("IntellIoT").
            text_width(10).
            font("ABeeZee").
            variant("regular").
            alignment("left").
            position_reference("center").
            x(10).
            y(10).
            test(true).


            /*List of zones:
             - Start zone (receive and process message, or directly start)
             - Sequence zone (defines the interaction based on the sequence diagram)
             - Camera zone
             - HIL zone
             - Robot zone
             - Engraver and actuators zone
             - JSON zone
             - WoT zone
             - Helper zone


            */


            //Start zone

            !start.


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
                !update_process_robot;
                !start.



            +!start: true <-
                .print("start");
                //!print_parameters;
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
                ?process_robot(ProcessRobot);
                .print("before compute storage area");
                ?compute_storage_area(TextWidth, X, Y, Storage);
                .print("storage area computed: ", Storage);
                -+storage(Storage);
                ?grabspot(Storage, Grabspot);
                .print("grabspot received");
                .map.get(Grabspot, "confidence", Confidence);
                .map.get(Grabspot, "angle", Alpha);
                .map.get(Grabspot, "xcoordinate", XCoordinate);
                .map.get(Grabspot, "ycoordinate", YCoordinate);
                +actual_confidence(Confidence);
                ?normalize_values(Alpha, XCoordinate, YCoordinate, NewAlpha, NewX, NewY);
                .print("NewAlpha = ", NewAlpha, ", NewX = ", NewX, ", NewY = ", NewY);
                ?create_pose_ai(NewX, NewY, NewAlpha, Callback, PoseStorage);
                org.hyperagents.yggdrasil.jason.json.getTermAsJson(PoseStorage, PoseStorageString);
                !pose(RobotUrl, "application/ai+json", PoseStorage);
                !use_hil;
                .print("move piece to engraver");
                !move_piece_to_engraver(ProcessRobot, Callback);
                .print("before print");
                !print(Process, Text);
                !print_mr_beam(Text);
                .print("printing done");
                .print("move piece back");
                !move_piece_back(ProcessRobot, Callback);
                .print("end").


            +!print_parameters: true <-
            ?process(Process);
            .print("process: ",Process);
            ?text(Text);
            .print("text: ",Text);
            ?text_width(TextWidth);
            .print("text width: ",TextWidth);
            ?font(Font);
            .print("font: ",Font);
            ?variant(Variant);
            .print("variant: ",Variant);
            ?alignment(Alignment);
            .print("alignment: ", Alignment);
            ?position_reference(PositionReference);
            .print("position reference: ",PositionReference);
            ?x(X);
            .print("x: ",X);
            ?y(Y);
            .print("y: ",Y);
            ?test(Test);
            .print("test: ",Test).


            // Camera zone

            +?compute_storage_area(TextWidth, X, Y, Storage): true <-
                .findall(Z, available_storage_area(Z), L);
                .length(L, N);
                ?compute_storage_area_list(TextWidth, X, Y, L, 0, N, StorageArea).

           +?compute_storage_area_list(Width, X, Y, L, I, N, StorageArea): I<N & ai_td_url(AIUrl) <-
               .nth(I, L, ST);
               ?create_json(["Content-Type"], ["application/json"], Headers);
               ?camera_hostname(CameraHostname);
               ?camera_id(CameraId);
               ?create_json(["storageId", "cameraHostname", "cameraId"], [ST, CameraHostname, CameraId], UriVariables);
               ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", {}, Headers, UriVariables, Response);
               .print("current storage number: ", ST);
               .print("current response: ", Response);
               !process_storage_response(ST, Response).

           +!process_storage_response(StorageNumber, Response): true <-
               !exit(Response, process_storage_response);
               ?get_body_as_json(Response, Body);
               .map.get(Body, "confidence", C);
               -+confidence_received(C);
               !add_storage_area_diameter(StorageNumber, Body).

           +!add_storage_area_diameter(StorageNumber, Body): confidence_retrieved(C) & C>95 <-
               .map.get(Body, "radius", R1);
               R = R1 * 2;
               -+storage_area_diameter(StorageNumber, R).

            +!add_storage_area_diameter(StorageNumber, Body): confidence_retrieved(C) & C<=95 <-
                -+storage_area_diameter(StorageNumber, 0).

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
                X1 = X/1000;
                Y1 = Y/1000;
                ?normalize_boundaries(Alpha, -20, 25, NewAlpha);
                ?normalize_boundaries(X1, 0.08, 1.05, NewX);
                ?normalize_boundaries(Y1, 0.365, 0.5, NewY).

            +?normalize_boundaries(X, Low, High, NewX): X<Low <-
                NewX=Law.

            +?normalize_boundaries(X, Low, High, NewX): X>High <-
                NewX=High.

             +?normalize_boundaries(X, Low, High, NewX): X>=Low & X<=High <-
                NewX=High.


            +?compute_engraving_area(StorageId, CameraHostname, CameraId, X_MrBeam, Y_MrBeam, TextWidth): ai_td_url(AIUrl) <-
                ?create_json(Headers);
                ?create_json(["storageId", "cameraHostname", "cameraId"], [StorageId, CameraHostname, CameraId], UriVariables);
                ?invoke_action_with_DLT(AIUrl, "computeEngravingArea", {}, Headers, UriVariables, EAReply);
                !exit(EAReply, start);
                ?get_body_as_json(EAReply, EA);
                B = .map.key(EA, "error_description");
                !conditional_exit_goal(B, start);
                .map.get(EA, "confidence", Confidence);
                .map.get(EA, "radius-mm", Radius);
                .map.get(EA, "xcoordinate", X);
                .map.get(EA, "ycoordinate", Y);
                X_MrBeam = 100 + Y;
                Y_MrBeam = 308 - X;
                TextWidth = 1.6 * Radius.



            // HIL zone

            +!use_hil: actual_confidence(C) & confidence_min(CM) & C<CM <-
                .print("use hil");
                RobotId =  "UR5";
                CameraId = "robot-camera";
                SessionType = "wood_piece_picking";
                ?generate_random_session_id(100000, AISessionId);
                .term2string(AISessionId, AISessionIdString);
                .print("AI session Id: ", AISessionIdString);
                !create_hil_session(HILUrl, AISessionIdString, RobotId, CameraId, SessionType);
                ?hil_content_loop(HILUrl, AISessionIdString, HILContent).

            +!use_hil: actual_confidence(C) & confidence_min(CM) & C>=CM <-
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

            +?hil_content_loop1(AISessionId, HILContent): hil_td_url(HILUrl) <-
                ?hil_content(HILUrl, AISessionId, HILCurrentContent);
                .print("hil current content: ", HILCurrentContent);
                .map.get(HILCurrentContent, "status", Status);
                if (not Status == "finished"){
                    .wait(1000);
                    ?hil_content_loop(AISessionId, _);
                } else {
                    HILContent = HILCurrentContent;
                    org.hyperagents.yggdrasil.jason.json.getTermAsJson(HILContent, HILStr);
                    .print("The HIL session has finished: ", HILStr);
                }.

            +?hil_content_loop(AISessionId, HILContent): hil_td_url(HILUrl) <-
                ?hil_content(HILUrl, AISessionId, HILCurrentContent);
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

            // Robot zone

            +!move_piece_to_engraver(Process, Callback): robot_td_url(RobotUrl) <-
                ?create_json(["value", "callback"], ["home", Callback], PoseValueHome);
                ?create_named_pose_process(Process, Callback, PoseValueTransport);
                !gripper(RobotUrl, "close", 3000); //close gripper time = 3000, check position
                ?create_json(["value", "callback"], ["home", Callback], PoseValueHome);
                !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
                .print("Before setting machine to use");
                ?create_named_pose_process(Process, Callback, PoseValueTransport);
                .print("The machine was selected");
                !pose(RobotUrl,  "application/namedpose+json", PoseValueTransport);
                .print("The robot is at the  machine");
                !gripper(RobotUrl, "open", 1000); //open gripper, check position
                .print("The gripper is opened");
                !pose(RobotUrl, "application/namedpose+json", PoseValueHome); //moving home
                .print("The robot is at home").

            +!move_piece_back(Process, Callback): robot_td_url(RobotUrl) <-
                ?create_json(["value", "callback"], ["home", Callback], PoseValueHome);
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
                ?create_json(["x","y","alpha"], [X,Y,Alpha], Value);
                ?create_json(["value","callback"],[Value, Callback], PoseStorage).

            +?create_named_pose_process(Process, Callback, NamedPose): true <-
                Value = "engraver_load";
                ?create_json(["value", "callback"], [Value, Callback], NamedPose).


            +!pose(RobotUrl, HeaderValue, PoseValue): true <-
                ?create_json(["Content-Type"], [HeaderValue], Headers);
                !pose_sub(RobotUrl, HeaderValue, PoseValue).

            +!pose_sub(RobotUrl, HeaderValue, PoseValue): HeaderValue == "application/ai+json" <-
                    ?invoke_action_with_DLT(RobotUrl, "setAIPose", PoseValue, Response);
                    printJson(Response);
                    !exit(Response, start).

            +!pose_sub(RobotUrl, HeaderValue, PoseValue): HeaderValue == "application/namedpose+json" <-
                    ?invoke_action_with_DLT(RobotUrl, "setNamedPose", PoseValue, Response);
                    !exit(Response, start).

            +!pose_sub(RobotUrl, HeaderValue, PoseValue): HeaderValue == "application/joint+json" <-
                    ?invoke_action_with_DLT(RobotUrl, "setJointPose", PoseValue, Response);
                    !exit(Response, start).

            +!pose_sub(RobotUrl, HeaderValue, PoseValue): HeaderValue == "application/tcp+json" <-
                    ?invoke_action_with_DLT(RobotUrl, "setTcpPose", PoseValue, Response);
                    !exit(Response, start).

            +!gripper(RobotUrl, Status, Time): true <-
                ?create_json(["Content-Type"], ["application/json"], Headers);
                ?create_json(["status"], [Status], Content);
                ?invoke_action_with_DLT(RobotUrl, "setGripper", Content, Headers, Response);
                !exit(Response, start);
                .wait(Time).


            //Engraver zone

            +!print(Process, Text): Process == "laser" <-
            !print_mr_beam.

            +!print(Process, Text): Process == "milling" <-
            !print_milling.

            +!print_mr_beam(Text): actuators_td_url(ActuatorsUrl) &
            engraver_td_url(EngraverUrl) & camera_engraver_hostname(CameraEngraverHostname)
            & camera_engraver_id(CameraEngraverId) & storage(Storage)
            <-
                .print("print Mr Beam");
                !use_actuator(ActuatorsUrl, "lowerdown");
                ?compute_engraving_area(Storage, CameraEngraverHostname, CameraEngraverId, X_MrBeam, Y_MrBeam, TextWidth);
                !use_actuator(ActuatorsUrl, "close");
                !engraver(EngraverUrl, ActuatorsUrl, Text, X_MrBeam, Y_MrBeam, TextWidth);
                !use_actuator(ActuatorsUrl, "open");
                !use_actuator(ActuatorsUrl, "liftup");
                .print("end print mr beam").

            +!print_milling: actuators_td_url(ActuatorsUrl) & //update
            engraver_td_url(EngraverUrl) & camera_engraver_hostname(CameraEngraverHostname)
            & camera_engraver_id(CameraEngraverId) & storage(Storage) <-
                .print("print milling");
                !use_actuator(ActuatorsUrl, "lowerdown");
                ?compute_engraving_area(Storage, CameraEngraverHostname, CameraEngraverId, X_MrBeam, Y_MrBeam, TextWidth);
                !use_actuator(ActuatorsUrl, "close");
                !engraver(EngraverUrl, ActuatorsUrl, Text, X_MrBeam, Y_MrBeam, TextWidth);
                !use_actuator(ActuatorsUrl, "open");
                !use_actuator(ActuatorsUrl, "liftup");
                .print("end print milling").

            +!use_actuator(ActuatorsUrl, Task): true <-
                ?create_json(Body);
                ?create_json(["Content-Type"], ["application/json"], Headers);
                ?invoke_action_with_DLT(ActuatorsUrl, Task, Body, Headers, Response);
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

            +!wait(EngraverUrl, Status, Time): true <-
                .wait(Time);
                ?create_json(Headers);
                ?create_json(UriVariables);
                ?read_property_with_DLT(TDUrl, "getJob",Headers, UriVariables, JobResponse);
                !exit(JobResponse, start );
                ?get_body_as_json(JobResponse, JobBody);
                .map.get(JobBody, "state", State);
                !continue_wait(State, Status, Time).

            +!continue_wait(State, Status, Time): not (State == Status) <-
                !wait(EngraverUrl, Status, Time).

            +!continue_wait(State, Status, Time):  (State == Status) <-
                .print("stop waiting").

            // JSON zone

            +?get_body_as_json(Response, Body): true <-
            .map.get(Response, "response", R);
            .map.get(R, "body", B);
            org.hyperagents.yggdrasil.jason.json.createTermFromJson(B, Body).

            +?create_json(L1, L2, Json): true <-
                org.hyperagents.yggdrasil.jason.json.createMapTerm(L1, L2, Json).

            +?create_json(Json): true <-
                org.hyperagents.yggdrasil.jason.json.createMapTerm([], [], Json).

            //WoT zone

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



            //Helper zone

            +!update_callback: true <-
                getAgHypermediaName(Name);
                .concat(Name, "/message", Callback);
                -+callback(Callback).

            +?get_callback(Callback): true <-
                getAgHypermediaName(Name);
                .concat(Name, "/message", Callback).

            +!exit(Response, Goal): true <-
                ?get_status(Response, Code);
                !exit_code(Code, Goal).

            +?get_status(Response, Status): true <-
                .map.get(Response, "response", R);
                .map.get(R, "statusCode", Status).

            +!exit_code(Code, Goal): Code > 299 <-
                .print("exit");
                .fail_goal(Goal).

            +!exit_code(Code, Goal): Code <= 299 <-
                .print("do not exit").

            +!conditional_exit_goal(B, Goal):  B <-
                .print("exit");
                .fail_goal(Goal).

            +!conditional_exit_goal(B, Goal): not B <-
                .print("do not exit").



            +?opposite(B1, B2): B1 <-
                B2 = false.

            +?opposite(B1, B2): not B1 <-
                B2 = true.

            +!update_process_robot: process(Process) & Process == "laser" <-
                -+process_robot("engraver_load").

            +!update_process_robot: process(Process) & Process == "milling" <-
                -+process_robot("milling_machine_load").









'
