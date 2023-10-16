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
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: intelliot'


echo "Create Workspace uc1"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/intelliot/sub \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: uc1'



echo "Create HIL Service"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/ \
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
              td:hasBase <'"${DEVICE_BASE}"'/services>;
              td:hasPropertyAffordance [ a td:PropertyAffordance, js:ArraySchema;
                  td:name "getOperators";
                  td:hasForm [
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/operators>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/operators/%7BoperatorId%7D>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions>;
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
                              js:propertyName "ai_session_id"
                            ], [ a js:StringSchema;
                              js:propertyName "camera_id"
                            ], [ a js:StringSchema;
                              js:propertyName "session_type";
                              js:enum "obstacle_avoidance_tractor"
                            ], [ a js:StringSchema;
                              js:propertyName "tractor_id"
                            ];
                          js:required "ai_session_id", "tractor_id", "camera_id", "session_type"
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions/%7BsessionId%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:readProperty, td:writeProperty
                    ];
                  td:isObservable false;
                  js:properties [ a js:StringSchema;
                      js:propertyName "creationTime"
                    ], [ a js:ObjectSchema;
                      js:propertyName "description";
                      js:properties [ a js:StringSchema;
                          js:propertyName "ai_session_id"
                        ], [ a js:StringSchema;
                          js:propertyName "camera_id"
                        ], [ a js:StringSchema;
                          js:propertyName "session_type";
                          js:enum "obstacle_avoidance_tractor"
                        ], [ a js:StringSchema;
                          js:propertyName "tractor_id"
                        ];
                      js:required "ai_session_id", "tractor_id", "camera_id", "session_type"
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/operators>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/operators/%7BoperatorId%7D>;
                      hctl:forContentType td:invokeAction
                    ]
                ], [ a td:ActionAffordance;
                  td:name "createSession";
                  td:hasForm [
                      <http://www.w3.org/2011/http#methodName> "POST";
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "ai_session_id"
                        ], [ a js:StringSchema;
                          js:propertyName "camera_id"
                        ], [ a js:StringSchema;
                          js:propertyName "session_type";
                          js:enum "obstacle_avoidance_tractor"
                        ], [ a js:StringSchema;
                          js:propertyName "tractor_id"
                        ];
                      js:required "ai_session_id", "tractor_id", "camera_id", "session_type"
                    ];
                  td:hasOutputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "creationTime"
                        ], [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "ai_session_id"
                            ], [ a js:StringSchema;
                              js:propertyName "camera_id"
                            ], [ a js:StringSchema;
                              js:propertyName "session_type";
                              js:enum "obstacle_avoidance_tractor"
                            ], [ a js:StringSchema;
                              js:propertyName "tractor_id"
                            ];
                          js:required "ai_session_id", "tractor_id", "camera_id", "session_type"
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions%7B?callbackUrl%7D>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ];
                  td:hasInputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "ai_session_id"
                        ], [ a js:StringSchema;
                          js:propertyName "camera_id"
                        ], [ a js:StringSchema;
                          js:propertyName "session_type";
                          js:enum "obstacle_avoidance_tractor"
                        ], [ a js:StringSchema;
                          js:propertyName "tractor_id"
                        ];
                      js:required "ai_session_id", "tractor_id", "camera_id", "session_type"
                    ];
                  td:hasOutputSchema [ a js:ObjectSchema;
                      js:properties [ a js:StringSchema;
                          js:propertyName "creationTime"
                        ], [ a js:ObjectSchema;
                          js:propertyName "description";
                          js:properties [ a js:StringSchema;
                              js:propertyName "ai_session_id"
                            ], [ a js:StringSchema;
                              js:propertyName "camera_id"
                            ], [ a js:StringSchema;
                              js:propertyName "session_type";
                              js:enum "obstacle_avoidance_tractor"
                            ], [ a js:StringSchema;
                              js:propertyName "tractor_id"
                            ];
                          js:required "ai_session_id", "tractor_id", "camera_id", "session_type"
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions/%7BsessionId%7D>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions/%7BaiSessionId%7D/takeover>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions/%7BaiSessionId%7D/reject>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions/%7BaiSessionId%7D/done>;
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
                      hctl:hasTarget <'"${DEVICE_BASE}"'/services/sessions/%7BaiSessionId%7D/reassign>;
                      hctl:forContentType "application/json";
                      hctl:hasOperationType td:invokeAction
                    ]
                ] .


'
