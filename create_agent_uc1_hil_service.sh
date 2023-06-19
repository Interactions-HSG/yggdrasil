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
--data-raw 'tractor_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc1/artifacts/tractor-controller").

            hil_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc1/artifacts/hil-service").

            dlt_client_td_url("'"${HYPERMAS_BASE}"'/workspaces/uc1/artifacts/dlt").

            task("undefined").
            current_ai_session_id(0).

            exit_code(200).

            !start.

            +!start : true <-
                !set_mode("idle");
                !use_hil;
                ?get_current_state(TractorUrl, State);
                while (not (State=="idle")){
                    ?get_current_state(TractorUrl, State);
            }


            +message(M, Waypoints): true <-
                ?tractor_td_url(TractorUrl);
                !drive(TractorUrl, Waypoints).

            +!drive(TractorUrl, Waypoints): true <-
                !travel_to_waypoint(TractorUrl, Waypoints);
                .print("end").

            +!travel_to_waypoint(TractorUrl, Waypoints): true <-
                .print("waypoints: ", Waypoints);
                map.create(H);
                .map.put(H, "Content-Type", "application/json");
                .map.create(U);
                .map.put(U, "restart", false);
                ?invoke_action_with_DLT(TractorUrl, "putWpoMissionGoals", Waypoints, H, U, Result).


            +!go_to_waypoint(TractorUrl, Waypoint): true <-
                getTermAsJson(Waypoint, JsonBody);
                .map.create(H);
                .map.put(H, "Content-Type", "application/json");
                .map.create(U);
                .map.put(U, "restart", false);
                ?invoke_action_with_DLT(TractorUrl, "putWpoMissionGoals", Waypoint, H, U, Result).


            +!check_state : tractor_td_url(TractorUrl) <- //TODO: check state of tractor and use of HIL Service
            .print("check");
            ?get_current_state(TractorUrl, State);
            if (State == "wpo_err"){ //TODO: check state
                !set_mode("idle");
                !create_hil_session;
                ?get_current_state(TractorUrl, State);
                while (not (State=="idle")){
                    ?get_current_state(TractorUrl, State);
                }
                .fail_goal(drive);
                //!invoke_hil_session;
                //!set_mode("mano"); //TODO: check whether this is done by the agent or the HIL application.
            }
            if (State == "mano"){
                ?hil_td_url(HILUrl);
                ?current_ai_session_id(AISessionId);
                ?hil_content_loop(HILUrl, AISessionId, HILContent);
                .map.get(HILContent, "status", S);
                if (S == "finished"){
                    !set_mode("wpo")
                }
            }
            if (not (State == "wpo_fin")){
                !check_state;
            }
            .print("end check").

            +!set_mode(Mode): tractor_td_url(TractorUrl)
             <-
             .map.create(H);
             .map.create(U);
             .map.put(U, "mode", Mode);
            ?invoke_action_with_DLT(TractorUrl, "putMode", null, H, U, Result). //TODO: check body

            +?get_current_state(TractorUrl, State): true <-
            invokeAction(TractorUrl, "getCurrentMode", Response);
            .map.get(Response, "body", State);
            .print("end current state").


            +?invoke_action_with_DLT(TDUrl, Method, Body, Headers, UriVariables, Response): dlt_client_td_url(DLTClientTDUrl) <-
                org.hyperagents.yggdrasil.jason.wot.invokeAction(TDUrl, Method, Body, Headers, UriVariables, Response);
                .print(Response);
                org.hyperagents.yggdrasil.jason.wot.dlt.getAsDLTMessage(Response, Message);
                .print("message: ", Message);
                //org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "POST", Message, R);
                .print("action performed").

            +?read_property_with_DLT(TDUrl, Method, Headers, UriVariables, Response): dlt_client_td_url(DLTClientTDUrl) <-
                org.hyperagents.yggdrasil.jason.wot.readProperty(TDUrl, Method, Headers, UriVariables, Response);
                .print(Response);
                org.hyperagents.yggdrasil.jason.wot.dlt.getAsDLTMessage(Response, Message);
                .print("message: ", Message);
                //org.hyperagents.yggdrasil.jason.wot.invokeAction(DLTClientTDUrl, "POST", Message, R);
                .print("property read").

            +!use_hil: true <-
                    TractorId =  "tractor"; //TODO: update
                    CameraId = "robot-tractor"; //TODO: update
                    SessionType = "session"; //TODO: update
                    ?generate_random_session_id(100000, AISessionId);
                    .term2string(AISessionId, AISessionIdString);
                    .print(AISessionIdString);
                    !create_hil_session(HILUrl, AISessionIdString, RobotId, CameraId, SessionType);
                    ?hil_content_loop(HILUrl, AISessionIdString, HILContent).

            +!create_hil_session(HILUrl, AISessionId, TractorId, CameraId, SessionType): true <-
            .map.create(Headers);
            .map.put(Headers, "Content-Type", "application/json");
            ?make_json_term(["aiSessionId", "tractorId", "cameraId", "sessionType"], [AISessionId, TractorId, CameraId, SessionType], SessionInfo);
            //org.hyperagents.yggdrasil.jason.json.getTermAsJson(SessionJson, SessionInfo);
            org.hyperagents.yggdrasil.jason.wot.invokeAction(HILUrl, "createSession", SessionInfo, Headers, Response);
            !exit(Response, drive);
            .print("hil session created").

            +?hil_content_loop(HILUrl, AISessionId, HILContent): true <-
                ?hil_content(HILUrl, AISessionId, HILCurrentContent);
                .print("hil current content: ", HILCurrentContent);
                .map.get(HILCurrentContent, "status", Status);
                if (not Status == "finished"){
                    .wait(1000);
                    ?hil_content_loop(HILUrl, AISessionId, _);
                } else {
                    HILContent = HILCurrentContent;
                    org.hyperagents.yggdrasil.jason.json.getTermAsJson(HILContent, HILStr);
                    .print("The HIL session has finished: ", HILStr);
                }.

            +?make_json_term(AttributeList, ValueList, Json): true <-
                createMapTerm(AttributeList, ValueList, Json); //TODO: check if action exists
                .print("make json term: ", Json).

            +!exit(Response, Goal): true <- //TODO: refactor without if
                ?get_status(Response, Code);
                .print("status code: ",Code);
                -+exit_code(Code);
                !exit_sub(Goal);
                .print("end exit").

            +!exit_sub(Goal): exit_Code(C) & C > 299 <-
                print("exit");
                .fail_goal(Goal).






'
