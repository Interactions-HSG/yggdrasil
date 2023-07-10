while [[ "$#" -gt 0 ]]
  do
    case $1 in
      --hyper)
        HYPERMAS_BASE=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;
      --avl)
        AVL_BASE=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;
      --hil)
        HIL_BASE=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;
    esac
    shift
  done


if [[ $REQUIRED_PARAM_COUNTER -ne 3 ]]; then
    echo "$(basename $0)  --hyper <HyperMAS base URL> --device <Edge device base URL> --dlt <DLT Client Url>"
    exit 1
fi

echo "HYPERMAS_BASE=$HYPERMAS_BASE"
echo "AVL_BASE"=$AVL_BASE

echo "Create Workspace intelliot"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: intelliot'


echo "Create Workspace uc1"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/intelliot/sub \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: uc1'



echo "Create HIL Service"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc1/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: hil-service' \
--header 'Content-Type: text/turtle' \
--data-raw '@prefix td: <https://www.w3.org/2019/wot/td#> .
            @prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
            @prefix dct: <http://purl.org/dc/terms/> .
            @prefix wotsec: <https://www.w3.org/2019/wot/security#> .
            @prefix js: <https://www.w3.org/2019/wot/json-schema#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

            <'"${HYPERMAS_BASE}"'/workspaces/uc1/artifacts/hil_service> a td:Thing;
              td:title "HIL Service";
              td:hasSecurityConfiguration [ a wotsec:APIKeySecurityScheme;
                  wotsec:in "HEADER";
                  wotsec:name "X-API-Key"
                ];
              td:hasBase <'"${HIL_BASE}"'/services>;
              td:hasPropertyAffordance [ a td:PropertyAffordance, js:ArraySchema;
                  td:name "getOperators";
                  td:hasForm [
                      hctl:hasTarget <'"${HIL_BASE}"'/services/operators>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:readProperty, td:writeProperty
                    ];
                  td:isObservable false;
                  js:items [ a js:ObjectSchema;
                      js:properties [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "operatorMacAddress"
                            ], [ a js:StringSchema;
                              js:propertyName "operatorIpAddress"
                            ];
                          js:required "operatorIpAddress", "operatorMacAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorId"
                        ];
                      js:required "description", "operatorId"
                    ]
                ], [ a td:PropertyAffordance, js:ObjectSchema;
                  td:name "getOperatorFromId";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "operatorId"
                    ];
                  td:hasForm [
                      hctl:hasTarget <'"${HIL_BASE}"'services/operators/%7BoperatorId%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:readProperty, td:writeProperty
                    ];
                  td:isObservable false;
                  js:properties [ a js:ObjectSchema;
                      js:propertyName "description";
                      js:properties [ a js:StringSchema;
                          js:propertyName "operatorMacAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorIpAddress"
                        ];
                      js:required "operatorIpAddress", "operatorMacAddress"
                    ], [ a js:StringSchema;
                      js:propertyName "operatorId"
                    ];
                  js:required "description", "operatorId"
                ], [ a td:PropertyAffordance, js:ArraySchema;
                  td:name "getAllSessions";
                  td:hasForm [
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:readProperty, td:writeProperty
                    ];
                  td:isObservable false;
                  js:items [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "creationTime"
                        ], [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "cameraId"
                            ], [ a js:StringSchema;
                              js:propertyName "tractorId"
                            ], [ a js:StringSchema;
                              js:propertyName "sessionType";
                              js:enum "obstacle_avoidance_tractor"
                            ], [ a js:StringSchema;
                              js:propertyName "aiSessionId"
                            ];
                          js:required "aiSessionId", "tractorId", "cameraId", "sessionType"
                        ], [ a js:StringSchema;
                          js:propertyName "workResult";
                          js:enum "unfinished", "completed", "failed"
                        ], [ a js:StringSchema;
                          js:propertyName "finishedAt"
                        ], [ a js:StringSchema;
                          js:propertyName "willExpireAt"
                        ], [ a js:StringSchema;
                          js:propertyName "hilAppIpAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "cameraId"
                        ], [ a js:StringSchema;
                          js:propertyName "hyperMasCallbackUrl"
                        ], [ a js:StringSchema;
                          js:propertyName "tractorId"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorId"
                        ], [ a js:StringSchema;
                          js:propertyName "takenOverAt"
                        ], [ a js:StringSchema;
                          js:propertyName "status";
                          js:enum "preparing", "expired", "started", "finished", "unstarted", "failed"
                        ], [ a js:IntegerSchema;
                          js:propertyName "numberReassignments"
                        ];
                      js:required "creationTime", "willExpireAt", "takenOverAt", "finishedAt", "status",
                        "workResult", "operatorId", "cameraId", "tractorId", "hilAppIpAddress", "hyperMasCallbackUrl",
                        "numberReassignments", "description"
                    ]
                ], [ a td:PropertyAffordance, js:ObjectSchema;
                  td:name "getSession";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "sessionId"
                    ];
                  td:hasForm [
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions/%7BsessionId%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:readProperty, td:writeProperty
                    ];
                  td:isObservable false;
                  js:properties [ a js:StringSchema;
                      js:propertyName "creationTime"
                    ], [ a js:ObjectSchema;
                      js:propertyName "description";
                      js:properties [ a js:StringSchema;
                          js:propertyName "cameraId"
                        ], [ a js:StringSchema;
                          js:propertyName "tractorId"
                        ], [ a js:StringSchema;
                          js:propertyName "sessionType";
                          js:enum "obstacle_avoidance_tractor"
                        ], [ a js:StringSchema;
                          js:propertyName "aiSessionId"
                        ];
                      js:required "aiSessionId", "tractorId", "cameraId", "sessionType"
                    ], [ a js:StringSchema;
                      js:propertyName "workResult";
                      js:enum "unfinished", "completed", "failed"
                    ], [ a js:StringSchema;
                      js:propertyName "finishedAt"
                    ], [ a js:StringSchema;
                      js:propertyName "willExpireAt"
                    ], [ a js:StringSchema;
                      js:propertyName "hilAppIpAddress"
                    ], [ a js:StringSchema;
                      js:propertyName "cameraId"
                    ], [ a js:StringSchema;
                      js:propertyName "hyperMasCallbackUrl"
                    ], [ a js:StringSchema;
                      js:propertyName "tractorId"
                    ], [ a js:StringSchema;
                      js:propertyName "operatorId"
                    ], [ a js:StringSchema;
                      js:propertyName "takenOverAt"
                    ], [ a js:StringSchema;
                      js:propertyName "status";
                      js:enum "preparing", "expired", "started", "finished", "unstarted", "failed"
                    ], [ a js:IntegerSchema;
                      js:propertyName "numberReassignments"
                    ];
                  js:required "creationTime", "willExpireAt", "takenOverAt", "finishedAt", "status",
                    "workResult", "operatorId", "cameraId", "tractorId", "hilAppIpAddress", "hyperMasCallbackUrl",
                    "numberReassignments", "description"
                ];
              td:hasActionAffordance [ a td:ActionAffordance;
                  td:name "createOperator";
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/operators>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "operatorMacAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorIpAddress"
                        ];
                      js:required "operatorIpAddress", "operatorMacAddress"
                    ];
                  td:hasOutputSchema [ a js:ObjectSchema;
                      js:properties [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "operatorMacAddress"
                            ], [ a js:StringSchema;
                              js:propertyName "operatorIpAddress"
                            ];
                          js:required "operatorIpAddress", "operatorMacAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorId"
                        ];
                      js:required "description", "operatorId"
                    ]
                ], [ a td:ActionAffordance;
                  td:name "deleteOperator";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "operatorId"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "DELETE";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/operators/%7BoperatorId%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ]
                ], [ a td:ActionAffordance;
                  td:name "createSession";
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "cameraId"
                        ], [ a js:StringSchema;
                          js:propertyName "tractorId"
                        ], [ a js:StringSchema;
                          js:propertyName "sessionType";
                          js:enum "obstacle_avoidance_tractor"
                        ], [ a js:StringSchema;
                          js:propertyName "aiSessionId"
                        ];
                      js:required "aiSessionId", "tractorId", "cameraId", "sessionType"
                    ];
                  td:hasOutputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "creationTime"
                        ], [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "cameraId"
                            ], [ a js:StringSchema;
                              js:propertyName "tractorId"
                            ], [ a js:StringSchema;
                              js:propertyName "sessionType";
                              js:enum "obstacle_avoidance_tractor"
                            ], [ a js:StringSchema;
                              js:propertyName "aiSessionId"
                            ];
                          js:required "aiSessionId", "tractorId", "cameraId", "sessionType"
                        ], [ a js:StringSchema;
                          js:propertyName "workResult";
                          js:enum "unfinished", "completed", "failed"
                        ], [ a js:StringSchema;
                          js:propertyName "finishedAt"
                        ], [ a js:StringSchema;
                          js:propertyName "willExpireAt"
                        ], [ a js:StringSchema;
                          js:propertyName "hilAppIpAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "cameraId"
                        ], [ a js:StringSchema;
                          js:propertyName "hyperMasCallbackUrl"
                        ], [ a js:StringSchema;
                          js:propertyName "tractorId"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorId"
                        ], [ a js:StringSchema;
                          js:propertyName "takenOverAt"
                        ], [ a js:StringSchema;
                          js:propertyName "status";
                          js:enum "preparing", "expired", "started", "finished", "unstarted", "failed"
                        ], [ a js:IntegerSchema;
                          js:propertyName "numberReassignments"
                        ];
                      js:required "creationTime", "willExpireAt", "takenOverAt", "finishedAt", "status",
                        "workResult", "operatorId", "cameraId", "tractorId", "hilAppIpAddress", "hyperMasCallbackUrl",
                        "numberReassignments", "description"
                    ]
                ], [ a td:ActionAffordance;
                  td:name "createSessionWithCallback";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "callbackUrl"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions%7B?callbackUrl%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "cameraId"
                        ], [ a js:StringSchema;
                          js:propertyName "tractorId"
                        ], [ a js:StringSchema;
                          js:propertyName "sessionType";
                          js:enum "obstacle_avoidance_tractor"
                        ], [ a js:StringSchema;
                          js:propertyName "aiSessionId"
                        ];
                      js:required "aiSessionId", "tractorId", "cameraId", "sessionType"
                    ];
                  td:hasOutputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "creationTime"
                        ], [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "cameraId"
                            ], [ a js:StringSchema;
                              js:propertyName "tractorId"
                            ], [ a js:StringSchema;
                              js:propertyName "sessionType";
                              js:enum "obstacle_avoidance_tractor"
                            ], [ a js:StringSchema;
                              js:propertyName "aiSessionId"
                            ];
                          js:required "aiSessionId", "tractorId", "cameraId", "sessionType"
                        ], [ a js:StringSchema;
                          js:propertyName "workResult";
                          js:enum "unfinished", "completed", "failed"
                        ], [ a js:StringSchema;
                          js:propertyName "finishedAt"
                        ], [ a js:StringSchema;
                          js:propertyName "willExpireAt"
                        ], [ a js:StringSchema;
                          js:propertyName "hilAppIpAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "cameraId"
                        ], [ a js:StringSchema;
                          js:propertyName "hyperMasCallbackUrl"
                        ], [ a js:StringSchema;
                          js:propertyName "tractorId"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorId"
                        ], [ a js:StringSchema;
                          js:propertyName "takenOverAt"
                        ], [ a js:StringSchema;
                          js:propertyName "status";
                          js:enum "preparing", "expired", "started", "finished", "unstarted", "failed"
                        ], [ a js:IntegerSchema;
                          js:propertyName "numberReassignments"
                        ];
                      js:required "creationTime", "willExpireAt", "takenOverAt", "finishedAt", "status",
                        "workResult", "operatorId", "cameraId", "tractorId", "hilAppIpAddress", "hyperMasCallbackUrl",
                        "numberReassignments", "description"
                    ]
                ], [ a td:ActionAffordance;
                  td:name "deleteAllSessions";
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "DELETE";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ]
                ], [ a td:ActionAffordance;
                  td:name "deleteSession";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "sessionId"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "DELETE";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions/%7BsessionId%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ]
                ], [ a td:ActionAffordance;
                  td:name "takeover";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "aiSessionId"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions/%7BaiSessionId%7D/takeover>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "operatorMacAddress"
                        ], [ a js:StringSchema;
                          js:propertyName "operatorIpAddress"
                        ];
                      js:required "operatorIpAddress", "operatorMacAddress"
                    ];
                  td:hasOutputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "decision";
                          js:enum "standby", "rejected", "accepted"
                        ], [ a js:StringSchema;
                          js:propertyName "subscribeTo"
                        ];
                      js:required "decision", "subscribeTo"
                    ]
                ], [ a td:ActionAffordance;
                  td:name "reject";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "aiSessionId"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions/%7BaiSessionId%7D/reject>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ]
                ], [ a td:ActionAffordance;
                  td:name "done";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "aiSessionId"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions/%7BaiSessionId%7D/done>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:StringSchema;
                      js:enum "unfinished", "completed", "failed"
                    ]
                ], [ a td:ActionAffordance;
                  td:name "reassign";
                  td:hasUriTemplateSchema [ a js:StringSchema;
                      td:name "aiSessionId"
                    ];
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${HIL_BASE}"'/services/sessions/%7BaiSessionId%7D/reassign>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ]
                ] .


'

echo "Create Tractor Controller"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc1/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: tractor-controller' \
--header 'Content-Type: text/turtle' \
--data-raw '    @prefix td: <https://www.w3.org/2019/wot/td#> .
                @prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                @prefix wotsec: <https://www.w3.org/2019/wot/security#> .
                @prefix js: <https://www.w3.org/2019/wot/json-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                <'"${HYPERMAS_BASE}"'/workspaces/uc1/artifacts/tractor_controller> a td:Thing;
                  td:title "Tractor Controller";
                  td:hasSecurityConfiguration [ a wotsec:APIKeySecurityScheme;
                      wotsec:in "HEADER";
                      wotsec:name "X-API-Key"
                    ];
                  td:hasBase <'"${AVL_BASE}"'>;
                  td:hasPropertyAffordance [ a td:PropertyAffordance;
                      td:name "getCurrentMode";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/eTractorMode/currentMode>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false
                    ], [ a td:PropertyAffordance, js:ArraySchema;
                      td:name "getAllModes";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/eTractorModes>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:items [ a js:StringSchema;
                          js:enum "cancel", "estop", "rto_mov", "idle", "mano", "wpo_mov", "error", "rto", "undefined",
                            "wpo_fin", "mano_mov", "errack", "wpo_err", "rto_err", "wpo", "rto_fin"
                        ]
                    ], [ a td:PropertyAffordance;
                      td:name "isProcessing";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/waypoint_operated/isProcessing>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false
                    ], [ a td:PropertyAffordance, js:ArraySchema;
                      td:name "currentGoal";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/waypoint_operated/currentGoal>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:items [ a js:ObjectSchema;
                          js:properties [ a js:StringSchema;
                              js:propertyName "task";
                              js:enum "spray", "plough", "sing", "dance", "undefined"
                            ], [ a js:NumberSchema;
                              js:propertyName "heading";
                              js:minimum 0.0E0;
                              js:maximum 3.59E2
                            ], [ a js:NumberSchema;
                              js:propertyName "latitude";
                              js:minimum 9.0E1
                            ], [ a js:NumberSchema;
                              js:propertyName "longitude";
                              js:minimum 1.8E2
                            ]
                        ]
                    ], [ a td:PropertyAffordance;
                      td:name "isRoutineProcessing";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/routine_operated/isProcessing>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "currentCmd";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/routine_operated/currentCmd>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:StringSchema;
                          js:propertyName "task";
                          js:enum "spray", "plough", "sing", "dance", "undefined"
                        ], [ a js:ObjectSchema;
                          js:propertyName "drive_cmds";
                          js:properties [ a js:NumberSchema;
                              js:propertyName "set_velocity";
                              js:minimum 0.0E0;
                              js:maximum 2.0E0
                            ], [ a js:NumberSchema;
                              js:propertyName "set_curvature";
                              js:minimum -2.0E-1;
                              js:maximum 2.0E-1
                            ], [ a js:IntegerSchema;
                              js:propertyName "set_vehicle_state";
                              js:minimum "0"^^xsd:int;
                              js:maximum "3"^^xsd:int
                            ], [ a js:IntegerSchema;
                              js:propertyName "sys_cond";
                              js:minimum "0"^^xsd:int;
                              js:maximum "4"^^xsd:int
                            ]
                        ], [ a js:NumberSchema;
                          js:propertyName "execDuration";
                          js:minimum 1.0E-1;
                          js:maximum 1.0E1
                        ]
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "getVcuMsg";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/vcuMsg>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:IntegerSchema;
                          js:propertyName "propulsion_act_value"
                        ], [ a js:IntegerSchema;
                          js:propertyName "curverture_max_value"
                        ], [ a js:IntegerSchema;
                          js:propertyName "propulsion_max_value"
                        ], [ a js:IntegerSchema;
                          js:propertyName "propulsion_state";
                          js:minimum "0"^^xsd:int;
                          js:maximum "4"^^xsd:int
                        ], [ a js:ObjectSchema;
                          js:propertyName "header";
                          js:properties [ a js:StringSchema;
                              js:propertyName "frameId"
                            ], [ a js:ObjectSchema;
                              js:propertyName "stamp";
                              js:properties [ a js:IntegerSchema;
                                  js:propertyName "nsecs"
                                ], [ a js:IntegerSchema;
                                  js:propertyName "secs"
                                ];
                              js:required "secs", "nsecs"
                            ], [ a js:IntegerSchema;
                              js:propertyName "seq"
                            ]
                        ], [ a js:IntegerSchema;
                          js:propertyName "vehicle_state";
                          js:minimum "0"^^xsd:int;
                          js:maximum "4"^^xsd:int
                        ], [ a js:IntegerSchema;
                          js:propertyName "curverture_act_value"
                        ], [ a js:IntegerSchema;
                          js:propertyName "curverture_state"
                        ]
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "getImu";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          js:enum "left", "back", "front", "right";
                          td:name "imu_position"
                        ];
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/imu/%7Bimu_position%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:ArraySchema;
                          js:propertyName "linear_acceleration_covariance";
                          js:items [ a js:NumberSchema
                            ]
                        ], [ a js:ObjectSchema;
                          js:propertyName "orientation";
                          js:properties [ a js:NumberSchema;
                              js:propertyName "w"
                            ], [ a js:NumberSchema;
                              js:propertyName "x"
                            ], [ a js:NumberSchema;
                              js:propertyName "y"
                            ], [ a js:NumberSchema;
                              js:propertyName "z"
                            ]
                        ], [ a js:ArraySchema;
                          js:propertyName "orientation_covariance";
                          js:items [ a js:NumberSchema
                            ]
                        ], [ a js:ArraySchema;
                          js:propertyName "angular_velocity_covariance";
                          js:items [ a js:NumberSchema
                            ]
                        ], [ a js:ObjectSchema;
                          js:propertyName "linear_acceleration";
                          js:properties [ a js:NumberSchema;
                              js:propertyName "x"
                            ], [ a js:NumberSchema;
                              js:propertyName "y"
                            ], [ a js:NumberSchema;
                              js:propertyName "z"
                            ]
                        ], [ a js:ObjectSchema;
                          js:propertyName "header";
                          js:properties [ a js:StringSchema;
                              js:propertyName "frameId"
                            ], [ a js:ObjectSchema;
                              js:propertyName "stamp";
                              js:properties [ a js:IntegerSchema;
                                  js:propertyName "nsecs"
                                ], [ a js:IntegerSchema;
                                  js:propertyName "secs"
                                ];
                              js:required "secs", "nsecs"
                            ], [ a js:IntegerSchema;
                              js:propertyName "seq"
                            ]
                        ], [ a js:ObjectSchema;
                          js:propertyName "angular_velocity";
                          js:properties [ a js:NumberSchema;
                              js:propertyName "x"
                            ], [ a js:NumberSchema;
                              js:propertyName "y"
                            ], [ a js:NumberSchema;
                              js:propertyName "z"
                            ]
                        ];
                      js:required "header", "orientation", "orientation_covariance", "angular_velocity",
                        "angular_velocity_covariance", "linear_acceleration", "linear_acceleration_covariance"
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "getGps";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/gps>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:NumberSchema;
                          js:propertyName "altitude"
                        ], [ a js:NumberSchema;
                          js:propertyName "latitude";
                          js:minimum 9.0E1
                        ], [ a js:IntegerSchema;
                          js:propertyName "COVARIANCE_TYPE_KNOWN"
                        ], [ a js:IntegerSchema;
                          js:propertyName "COVARIANCE_TYPE_UNKNOWN"
                        ], [ a js:ObjectSchema;
                          js:propertyName "header";
                          js:properties [ a js:StringSchema;
                              js:propertyName "frameId"
                            ], [ a js:ObjectSchema;
                              js:propertyName "stamp";
                              js:properties [ a js:IntegerSchema;
                                  js:propertyName "nsecs"
                                ], [ a js:IntegerSchema;
                                  js:propertyName "secs"
                                ];
                              js:required "secs", "nsecs"
                            ], [ a js:IntegerSchema;
                              js:propertyName "seq"
                            ]
                        ], [ a js:IntegerSchema;
                          js:propertyName "COVARIANCE_TYPE_APPROXIMATED"
                        ], [ a js:IntegerSchema;
                          js:propertyName "COVARIANCE_TYPE_DIAGONAL_KNOWN"
                        ], [ a js:ArraySchema;
                          js:propertyName "position_covariance";
                          js:items [ a js:NumberSchema
                            ]
                        ], [ a js:ObjectSchema;
                          js:propertyName "status";
                          js:properties [ a js:IntegerSchema;
                              js:propertyName "STATUS_GBAS_FIX"
                            ], [ a js:IntegerSchema;
                              js:propertyName "STATUS_SBAS_FIX"
                            ], [ a js:IntegerSchema;
                              js:propertyName "SERVICE_GPS"
                            ], [ a js:IntegerSchema;
                              js:propertyName "service"
                            ], [ a js:IntegerSchema;
                              js:propertyName "SERVICE_GLONASS"
                            ], [ a js:IntegerSchema;
                              js:propertyName "SERVICE_COMPASS"
                            ], [ a js:IntegerSchema;
                              js:propertyName "STATUS_NO_FIX"
                            ], [ a js:IntegerSchema;
                              js:propertyName "STATUS_FIX"
                            ], [ a js:IntegerSchema;
                              js:propertyName "SERVICE_GALILEO"
                            ], [ a js:IntegerSchema;
                              js:propertyName "status"
                            ]
                        ], [ a js:NumberSchema;
                          js:propertyName "longitude";
                          js:minimum 1.8E2
                        ], [ a js:IntegerSchema;
                          js:propertyName "position_covariance_type"
                        ]
                    ], [ a td:PropertyAffordance, js:StringSchema;
                      td:name "getCameraHandleRosTopic";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          js:enum "left", "back", "front", "right";
                          td:name "cam_position"
                        ];
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/CameraHandleRosTopic/%7Bcam_position%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false
                    ], [ a td:PropertyAffordance, js:StringSchema;
                      td:name "getCameraHandleRosTopic";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          js:enum "left", "back", "front", "right";
                          td:name "cam_position"
                        ];
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/CameraHandleRosTopic/%7Bcam_position%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "getHeading";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/heading>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:StringSchema;
                          js:propertyName "heading";
                          js:enum "SE", "S", "SW", "E", "NE", "W", "NW", "N"
                        ], [ a js:ObjectSchema;
                          js:propertyName "header";
                          js:properties [ a js:StringSchema;
                              js:propertyName "frameId"
                            ], [ a js:ObjectSchema;
                              js:propertyName "stamp";
                              js:properties [ a js:IntegerSchema;
                                  js:propertyName "nsecs"
                                ], [ a js:IntegerSchema;
                                  js:propertyName "secs"
                                ];
                              js:required "secs", "nsecs"
                            ], [ a js:IntegerSchema;
                              js:propertyName "seq"
                            ]
                        ], [ a js:IntegerSchema;
                          js:propertyName "angle"
                        ];
                      js:required "header", "heading", "angle"
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "getVelocity";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/velocity>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:NumberSchema;
                          js:propertyName "linear_velocity"
                        ], [ a js:ObjectSchema;
                          js:propertyName "header";
                          js:properties [ a js:StringSchema;
                              js:propertyName "frameId"
                            ], [ a js:ObjectSchema;
                              js:propertyName "stamp";
                              js:properties [ a js:IntegerSchema;
                                  js:propertyName "nsecs"
                                ], [ a js:IntegerSchema;
                                  js:propertyName "secs"
                                ];
                              js:required "secs", "nsecs"
                            ], [ a js:IntegerSchema;
                              js:propertyName "seq"
                            ]
                        ], [ a js:NumberSchema;
                          js:propertyName "angular_velocity"
                        ];
                      js:required "header"
                    ], [ a td:PropertyAffordance, js:ArraySchema;
                      td:name "getImplements";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/implements>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:items [ a js:StringSchema;
                          js:enum "packer", "sprayer", "mower", "spreader", "plough", "undefined"
                        ]
                    ], [ a td:PropertyAffordance, js:StringSchema;
                      td:name "getCurrentImplement";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/currImplement>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:enum "packer", "sprayer", "mower", "spreader", "plough", "undefined"
                    ], [ a td:PropertyAffordance, js:StringSchema;
                      td:name "readRoot";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false
                    ], [ a td:PropertyAffordance, js:ObjectSchema;
                      td:name "getHeartbeat";
                      td:hasForm [
                          hctl:hasTarget <'"${AVL_BASE}"'/ros_hb>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:readProperty, td:writeProperty
                        ];
                      td:isObservable false;
                      js:properties [ a js:BooleanSchema;
                          js:propertyName "data"
                        ]
                    ];
                  td:hasActionAffordance [ a td:ActionAffordance;
                      td:name "putCurrentMode";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          js:enum "cancel", "estop", "rto_mov", "idle", "mano", "wpo_mov", "error", "rto", "undefined",
                            "wpo_fin", "mano_mov", "errack", "wpo_err", "rto_err", "wpo", "rto_fin";
                          td:name "mode"
                        ], [ a js:StringSchema;
                          td:name "feedback_callback_url"
                        ];
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/eTractorMode/currentMode%7B?mode,%20feedback_callback_url%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "putWpoMissionGoals";
                      td:hasUriTemplateSchema [ a js:BooleanSchema;
                          td:name "restart"
                        ];
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/waypoint_operated/missionGoals%7B?restart%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasInputSchema [ a js:ArraySchema;
                          js:items [ a js:ObjectSchema;
                              js:properties [ a js:StringSchema;
                                  js:propertyName "task";
                                  js:enum "spray", "plough", "sing", "dance", "undefined"
                                ], [ a js:NumberSchema;
                                  js:propertyName "heading";
                                  js:minimum 0.0E0;
                                  js:maximum 3.59E2
                                ], [ a js:NumberSchema;
                                  js:propertyName "latitude";
                                  js:minimum 9.0E1
                                ], [ a js:NumberSchema;
                                  js:propertyName "longitude";
                                  js:minimum 1.8E2
                                ]
                            ]
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "putWpoMissionGoalsWithFeedback";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          td:name "feedback_callback_url"
                        ], [ a js:BooleanSchema;
                          td:name "restart"
                        ];
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/waypoint_operated/missionGoals%7B?restart,feedback_callback_url%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasInputSchema [ a js:ArraySchema;
                          js:items [ a js:ObjectSchema;
                              js:properties [ a js:StringSchema;
                                  js:propertyName "task";
                                  js:enum "spray", "plough", "sing", "dance", "undefined"
                                ], [ a js:NumberSchema;
                                  js:propertyName "heading";
                                  js:minimum 0.0E0;
                                  js:maximum 3.59E2
                                ], [ a js:NumberSchema;
                                  js:propertyName "latitude";
                                  js:minimum 9.0E1
                                ], [ a js:NumberSchema;
                                  js:propertyName "longitude";
                                  js:minimum 1.8E2
                                ]
                            ]
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "putWpoCancel";
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/waypoint_operated/cancel>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "putMissionCmds";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          td:name "feedback_callback_url"
                        ];
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/routine_operated/missionCmds%7B?feedback_callback_url%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasInputSchema [ a js:ObjectSchema;
                          js:properties [ a js:StringSchema;
                              js:propertyName "task";
                              js:enum "spray", "plough", "sing", "dance", "undefined"
                            ], [ a js:ObjectSchema;
                              js:propertyName "drive_cmds";
                              js:properties [ a js:NumberSchema;
                                  js:propertyName "set_velocity";
                                  js:minimum 0.0E0;
                                  js:maximum 2.0E0
                                ], [ a js:NumberSchema;
                                  js:propertyName "set_curvature";
                                  js:minimum -2.0E-1;
                                  js:maximum 2.0E-1
                                ], [ a js:IntegerSchema;
                                  js:propertyName "set_vehicle_state";
                                  js:minimum "0"^^xsd:int;
                                  js:maximum "3"^^xsd:int
                                ], [ a js:IntegerSchema;
                                  js:propertyName "sys_cond";
                                  js:minimum "0"^^xsd:int;
                                  js:maximum "4"^^xsd:int
                                ]
                            ], [ a js:NumberSchema;
                              js:propertyName "execDuration";
                              js:minimum 1.0E-1;
                              js:maximum 1.0E1
                            ]
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "putRtoCancel";
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/routine_operated/cancel>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "startRTPSStream";
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/startRTPSStream/%7Bcam_position%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasOutputSchema [ a js:ObjectSchema;
                          js:properties [ a js:BooleanSchema;
                              js:propertyName "was_succes"
                            ], [ a js:StringSchema;
                              js:propertyName "description"
                            ]
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "stopRTPSStream";
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/sensors/stopRTPSStream/%7Bcam_position%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasOutputSchema [ a js:ObjectSchema;
                          js:properties [ a js:BooleanSchema;
                              js:propertyName "was_succes"
                            ], [ a js:StringSchema;
                              js:propertyName "description"
                            ]
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "putCurrentImplement";
                      td:hasUriTemplateSchema [ a js:StringSchema;
                          js:enum "packer", "sprayer", "mower", "spreader", "plough", "undefined";
                          td:name "implement"
                        ];
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/implements/implement%7B?implement%7D>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasOutputSchema [ a js:ObjectSchema;
                          js:properties [ a js:StringSchema;
                              js:propertyName "curr_implement";
                              js:enum "packer", "sprayer", "mower", "spreader", "plough", "undefined"
                            ], [ a js:BooleanSchema;
                              js:propertyName "was_sucess"
                            ]
                        ]
                    ], [ a td:ActionAffordance;
                      td:name "manualDriveCmds";
                      td:hasForm [
                          <http://www.w3.org/2011/http#methodName> "PUT";
                          hctl:hasTarget <'"${AVL_BASE}"'/manualDriveCmds>;
                          hctl:forContentType "application/json";
                          hctl:hasOperationType td:invokeAction
                        ];
                      td:hasInputSchema [ a js:ObjectSchema;
                          js:properties [ a js:NumberSchema;
                              js:propertyName "set_velocity";
                              js:minimum 0.0E0;
                              js:maximum 2.0E0
                            ], [ a js:NumberSchema;
                              js:propertyName "set_curvature";
                              js:minimum -2.0E-1;
                              js:maximum 2.0E-1
                            ], [ a js:IntegerSchema;
                              js:propertyName "set_vehicle_state";
                              js:minimum "0"^^xsd:int;
                              js:maximum "3"^^xsd:int
                            ], [ a js:IntegerSchema;
                              js:propertyName "sys_cond";
                              js:minimum "0"^^xsd:int;
                              js:maximum "4"^^xsd:int
                            ]
                        ];
                      td:hasOutputSchema []
                    ] .
'
