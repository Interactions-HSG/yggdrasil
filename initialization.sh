while [[ "$#" -gt 0 ]]
  do
    case $1 in
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


if [[ $REQUIRED_PARAM_COUNTER -ne 2 ]]; then
    echo "$(basename $0)  --hyper <HyperMAS base URL> --device <Edge device base URL>"
    exit 1
fi

echo "HYPERMAS_BASE=$HYPERMAS_BASE"
echo "DEVICE_BASE=$DEVICE_BASE"

echo "Create Workspace intelliot"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: intelliot'


echo "Create Workspace uc3"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/intelliot/sub \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: uc3'



echo "Create Camera AI"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc3/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: camera-ai' \
--header 'Content-Type: text/turtle' \
--data-raw '@prefix td: <https://www.w3.org/2019/wot/td#> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix dct: <http://purl.org/dc/terms/> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix js: <https://www.w3.org/2019/wot/json-schema#> .

<'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/camera-ai> a td:Thing;
  td:title "aiService";
  td:hasSecurityConfiguration [ a wotsec:NoSecurityScheme
    ];
  td:hasBase <'"${DEVICE_BASE}"'/camera-ai>;
  td:hasActionAffordance [ a td:ActionAffordance;
      td:name "getGrabspot";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "cameraId"
        ], [ a js:StringSchema;
          td:name "cameraHostname"
        ], [ a js:StringSchema;
          td:name "storageId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/camera-ai/AI_Service/GET_grabspot%7B?storageId,cameraHostname,cameraId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ArraySchema;
          js:items [ a js:ObjectSchema;
              js:properties [ a js:NumberSchema;
                  js:propertyName "ycoordinate"
                ], [ a js:NumberSchema;
                  js:propertyName "confidence"
                ], [ a js:NumberSchema;
                  js:propertyName "angle"
                ], [ a js:NumberSchema;
                  js:propertyName "xcoordinate"
                ];
              js:required "xcoordinate", "ycoordinate", "angle", "confidence"
            ]
        ]
    ], [ a td:ActionAffordance;
      td:name "computeEngravingArea";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "cameraId"
        ], [ a js:StringSchema;
          td:name "cameraHostname"
        ], [ a js:StringSchema;
          td:name "storageId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/camera-ai/AI_Service/compute_engravingArea%7B?storageId,cameraHostname,cameraId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ArraySchema;
          js:items [ a js:ObjectSchema;
              js:properties [ a js:NumberSchema;
                  js:propertyName "ycoordinate"
                ], [ a js:NumberSchema;
                  js:propertyName "radius-mm"
                ], [ a js:NumberSchema;
                  js:propertyName "confidence"
                ], [ a js:NumberSchema;
                  js:propertyName "xcoordinate"
                ];
              js:required "xcoordinate", "ycoordinate", "radius-mm", "confidence"
            ]
        ]
    ] .
'


echo "Create HIL Service"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc3/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: hil-service' \
--header 'Content-Type: text/turtle' \
--data-raw '@prefix td: <https://www.w3.org/2019/wot/td#> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix dct: <http://purl.org/dc/terms/> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix js: <https://www.w3.org/2019/wot/json-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/hil-service> a td:Thing;
  td:title "hilService";
  td:hasSecurityConfiguration [ a wotsec:APIKeySecurityScheme;
      wotsec:in "HEADER";
      wotsec:name "X-API-Key"
    ];
  td:hasBase <'"${DEVICE_BASE}"'/hil-service/service>;
  td:hasPropertyAffordance [ a td:PropertyAffordance, js:ArraySchema;
      td:name "getOperators";
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/operators>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/operators/%7BoperatorId%7D>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions>;
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
                  js:propertyName "sessionType";
                  js:enum "wood_piece_picking"
                ], [ a js:StringSchema;
                  js:propertyName "aiSessionId"
                ], [ a js:StringSchema;
                  js:propertyName "robotId"
                ];
              js:required "aiSessionId", "robotId", "cameraId", "sessionType"
            ], [ a js:StringSchema;
              js:propertyName "robotId"
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
            "workResult", "operatorId", "cameraId", "robotId", "hilAppIpAddress", "hyperMasCallbackUrl",
            "numberReassignments", "description"
        ]
    ], [ a td:PropertyAffordance, js:ObjectSchema;
      td:name "getSessions";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "sessionId"
        ];
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions/%7BsessionId%7D>;
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
              js:propertyName "sessionType";
              js:enum "wood_piece_picking"
            ], [ a js:StringSchema;
              js:propertyName "aiSessionId"
            ], [ a js:StringSchema;
              js:propertyName "robotId"
            ];
          js:required "aiSessionId", "robotId", "cameraId", "sessionType"
        ], [ a js:StringSchema;
          js:propertyName "robotId"
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
        "workResult", "operatorId", "cameraId", "robotId", "hilAppIpAddress", "hyperMasCallbackUrl",
        "numberReassignments", "description"
    ];
  td:hasActionAffordance [ a td:ActionAffordance;
      td:name "createOperator";
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/operators>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/operators/%7BoperatorId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ]
    ], [ a td:ActionAffordance;
      td:name "createSession";
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "cameraId"
            ], [ a js:StringSchema;
              js:propertyName "sessionType";
              js:enum "wood_piece_picking"
            ], [ a js:StringSchema;
              js:propertyName "aiSessionId"
            ], [ a js:StringSchema;
              js:propertyName "robotId"
            ];
          js:required "aiSessionId", "robotId", "cameraId", "sessionType"
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "creationTime"
            ], [ a js:ObjectSchema;
              js:propertyName "description";
              js:properties [ a js:StringSchema;
                  js:propertyName "cameraId"
                ], [ a js:StringSchema;
                  js:propertyName "sessionType";
                  js:enum "wood_piece_picking"
                ], [ a js:StringSchema;
                  js:propertyName "aiSessionId"
                ], [ a js:StringSchema;
                  js:propertyName "robotId"
                ];
              js:required "aiSessionId", "robotId", "cameraId", "sessionType"
            ], [ a js:StringSchema;
              js:propertyName "robotId"
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
            "workResult", "operatorId", "cameraId", "robotId", "hilAppIpAddress", "hyperMasCallbackUrl",
            "numberReassignments", "description"
        ]
    ], [ a td:ActionAffordance;
      td:name "createSessionWithCallback";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "callbackUrl"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions%7B?callbackUrl%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "cameraId"
            ], [ a js:StringSchema;
              js:propertyName "sessionType";
              js:enum "wood_piece_picking"
            ], [ a js:StringSchema;
              js:propertyName "aiSessionId"
            ], [ a js:StringSchema;
              js:propertyName "robotId"
            ];
          js:required "aiSessionId", "robotId", "cameraId", "sessionType"
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "creationTime"
            ], [ a js:ObjectSchema;
              js:propertyName "description";
              js:properties [ a js:StringSchema;
                  js:propertyName "cameraId"
                ], [ a js:StringSchema;
                  js:propertyName "sessionType";
                  js:enum "wood_piece_picking"
                ], [ a js:StringSchema;
                  js:propertyName "aiSessionId"
                ], [ a js:StringSchema;
                  js:propertyName "robotId"
                ];
              js:required "aiSessionId", "robotId", "cameraId", "sessionType"
            ], [ a js:StringSchema;
              js:propertyName "robotId"
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
            "workResult", "operatorId", "cameraId", "robotId", "hilAppIpAddress", "hyperMasCallbackUrl",
            "numberReassignments", "description"
        ]
    ], [ a td:ActionAffordance;
      td:name "deleteAllSessions";
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "DELETE";
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions/%7BsessionId%7D>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions/%7BaiSessionId%7D/takeover>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions/%7BaiSessionId%7D/reject>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions/%7BaiSessionId%7D/done>;
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
          hctl:hasTarget <'"${DEVICE_BASE}"'/hil-service/service/sessions/%7BaiSessionId%7D/reassign>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ]
    ] .
'

echo "Create Robot Controller"
curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc3/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: robot-controller' \
--header 'Content-Type: text/turtle' \
--data-raw '@prefix td: <https://www.w3.org/2019/wot/td#> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix dct: <http://purl.org/dc/terms/> .
@prefix eve: <http://w3id.org/eve#> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix js: <https://www.w3.org/2019/wot/json-schema#> .
@prefix htv: <http://www.w3.org/2011/http#> .
@prefix intelliot: <https://intelliot.org/things#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/robot-controller> a td:Thing;
  td:title "URx Robot Controller";
  td:hasSecurityConfiguration [ a wotsec:APIKeySecurityScheme;
      wotsec:in "HEADER";
      wotsec:name "X-API-Key"
    ];
  td:hasBase <'"${DEVICE_BASE}"'/robot-controller/>;
  td:hasPropertyAffordance [ a td:PropertyAffordance, intelliot:ReadStatus, js:ObjectSchema;
      td:name "readStatus";
      td:hasForm [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/status>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:readProperty
        ];
      td:isObservable false;
      js:properties [ a js:BooleanSchema, intelliot:InMovement;
          js:propertyName "inMovement"
        ];
      js:required "inMovement"
    ], [ a td:PropertyAffordance, intelliot:ReadOperator, js:ObjectSchema, intelliot:OperatorProfile;
      td:name "readOperator";
      td:hasForm [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/operator>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:readProperty
        ];
      td:isObservable false;
      js:properties [ a js:StringSchema, foaf:Name;
          js:propertyName "name"
        ], [ a js:StringSchema, foaf:Mbox;
          js:propertyName "email"
        ];
      js:required "name", "email"
    ], [ a td:PropertyAffordance, intelliot:ReadHandles, js:ObjectSchema, intelliot:Handle;
      td:name "readHandles";
      td:hasForm [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/handle>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:readProperty
        ];
      td:isObservable false;
      js:properties [ a js:StringSchema, intelliot:RobotHandle;
          js:propertyName "robot"
        ], [ a js:StringSchema, intelliot:CameraHandle;
          js:propertyName "camera"
        ];
      js:required "robot", "camera"
    ], [ a td:PropertyAffordance, intelliot:ReadPose, js:ObjectSchema;
      td:name "readPose";
      td:hasForm [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/pose>;
          hctl:forContentType "application/joint+json";
          hctl:hasOperationType td:readProperty
        ], [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/pose>;
          hctl:forContentType "application/tcp+json";
          hctl:hasOperationType td:readProperty
        ], [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/pose>;
          hctl:forContentType "application/namedpose+json";
          hctl:hasOperationType td:readProperty
        ], [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/pose>;
          hctl:forContentType "application/ai+json";
          hctl:hasOperationType td:readProperty
        ];
      td:isObservable false;
      js:properties [ a js:DataSchema;
          js:propertyName "value";
          js:oneOf [ a js:ObjectSchema, intelliot:JointCoordinates, intelliot:PoseValue;
              js:contentMediaType "application/joint+json";
              js:properties [ a js:IntegerSchema, intelliot:Joint1Coordinate;
                  js:propertyName "j1"
                ], [ a js:IntegerSchema, intelliot:Joint2Coordinate;
                  js:propertyName "j2"
                ], [ a js:IntegerSchema, intelliot:Joint3Coordinate;
                  js:propertyName "j3"
                ], [ a js:IntegerSchema, intelliot:Joint4Coordinate;
                  js:propertyName "j4"
                ], [ a js:IntegerSchema, intelliot:Joint5Coordinate;
                  js:propertyName "j5"
                ], [ a js:IntegerSchema, intelliot:Joint6Coordinate;
                  js:propertyName "j6"
                ];
              js:required "j1", "j2", "j3", "j4", "j5", "j6"
            ], [ a js:ObjectSchema, intelliot:PoseValue, intelliot:TCPCoordinate;
              js:contentMediaType "application/tcp+json";
              js:properties [ a js:IntegerSchema, intelliot:AlphaCoordinate;
                  js:propertyName "alpha"
                ], [ a js:IntegerSchema, intelliot:XCoordinate;
                  js:propertyName "x"
                ], [ a js:IntegerSchema, intelliot:YCoordinate;
                  js:propertyName "y"
                ], [ a js:IntegerSchema, intelliot:ZCoordinate;
                  js:propertyName "z"
                ], [ a js:IntegerSchema, intelliot:BetaCoordinate;
                  js:propertyName "beta"
                ], [ a js:IntegerSchema, intelliot:GammaCoordinate;
                  js:propertyName "gamma"
                ];
              js:required "x", "y", "z", "alpha", "beta", "gamma"
            ], [ a js:StringSchema, intelliot:PoseValue, intelliot:NamedPose;
              js:enum "milling_machine_load", "dump_workpiece", "engraver_load", "home";
              js:contentMediaType "application/namedpose+json"
            ], [ a js:ObjectSchema, intelliot:AICoordinate, intelliot:PoseValue;
              js:contentMediaType "application/ai+json";
              js:properties [ a js:IntegerSchema, intelliot:AlphaCoordinate;
                  js:propertyName "alpha"
                ], [ a js:IntegerSchema, intelliot:XCoordinate;
                  js:propertyName "x"
                ], [ a js:IntegerSchema, intelliot:YCoordinate;
                  js:propertyName "y"
                ];
              js:required "x", "y", "alpha"
            ]
        ];
      js:required "value"
    ], [ a td:PropertyAffordance, intelliot:ReadGripper, js:ObjectSchema;
      td:name "readGripper";
      td:hasForm [
          htv:methodName "GET";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/gripper>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:readProperty
        ];
      td:isObservable false;
      js:properties [ a js:StringSchema, intelliot:Gripper;
          js:propertyName "status"
        ]
    ];
  td:hasActionAffordance [ a td:ActionAffordance, intelliot:Register;
      td:name "register";
      td:hasForm [
          htv:methodName "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/operator>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema, intelliot:OperatorProfile;
          js:properties [ a js:StringSchema, foaf:Name;
              js:propertyName "name"
            ], [ a js:StringSchema, foaf:Mbox;
              js:propertyName "email"
            ];
          js:required "name", "email"
        ]
    ], [ a td:ActionAffordance, intelliot:Deregister;
      td:name "deregister";
      td:hasForm [
          htv:methodName "DELETE";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/operator>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ]
    ], [ a td:ActionAffordance, intelliot:SetGripper;
      td:name "setGripper";
      td:hasForm [
          htv:methodName "PUT";
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/services/gripper>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema, intelliot:Gripper;
              js:propertyName "status"
            ]
        ]
    ];
  td:hasEventAffordance [ a td:EventAffordance, intelliot:SetPose, intelliot:SubscribePose;
      td:name "setPose";
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/pose>;
          hctl:forContentType "application/joint+json";
          hctl:hasOperationType td:subscribeEvent, td:unsubscribeEvent
        ], [
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/pose>;
          hctl:forContentType "application/tcp+json";
          hctl:hasOperationType td:subscribeEvent, td:unsubscribeEvent
        ], [
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/pose>;
          hctl:forContentType "application/namedpose+json";
          hctl:hasOperationType td:subscribeEvent, td:unsubscribeEvent
        ], [
          hctl:hasTarget <'"${DEVICE_BASE}"'/robot-controller/pose>;
          hctl:forContentType "application/ai+json";
          hctl:hasOperationType td:subscribeEvent, td:unsubscribeEvent
        ];
      td:hasSubscriptionSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema, intelliot:CallbackLocation;
              js:propertyName "callback"
            ], [ a js:DataSchema;
              js:propertyName "value";
              js:oneOf [ a js:ObjectSchema, intelliot:JointCoordinates, intelliot:PoseValue;
                  js:contentMediaType "application/joint+json";
                  js:properties [ a js:IntegerSchema, intelliot:Joint1Coordinate;
                      js:propertyName "j1"
                    ], [ a js:IntegerSchema, intelliot:Joint2Coordinate;
                      js:propertyName "j2"
                    ], [ a js:IntegerSchema, intelliot:Joint3Coordinate;
                      js:propertyName "j3"
                    ], [ a js:IntegerSchema, intelliot:Joint4Coordinate;
                      js:propertyName "j4"
                    ], [ a js:IntegerSchema, intelliot:Joint5Coordinate;
                      js:propertyName "j5"
                    ], [ a js:IntegerSchema, intelliot:Joint6Coordinate;
                      js:propertyName "j6"
                    ];
                  js:required "j1", "j2", "j3", "j4", "j5", "j6"
                ], [ a js:ObjectSchema, intelliot:PoseValue, intelliot:TCPCoordinate;
                  js:contentMediaType "application/tcp+json";
                  js:properties [ a js:IntegerSchema, intelliot:AlphaCoordinate;
                      js:propertyName "alpha"
                    ], [ a js:IntegerSchema, intelliot:XCoordinate;
                      js:propertyName "x"
                    ], [ a js:IntegerSchema, intelliot:YCoordinate;
                      js:propertyName "y"
                    ], [ a js:IntegerSchema, intelliot:ZCoordinate;
                      js:propertyName "z"
                    ], [ a js:IntegerSchema, intelliot:BetaCoordinate;
                      js:propertyName "beta"
                    ], [ a js:IntegerSchema, intelliot:GammaCoordinate;
                      js:propertyName "gamma"
                    ];
                  js:required "x", "y", "z", "alpha", "beta", "gamma"
                ], [ a js:StringSchema, intelliot:PoseValue, intelliot:NamedPose;
                  js:enum "milling_machine_load", "dump_workpiece", "engraver_load", "home";
                  js:contentMediaType "application/namedpose+json"
                ], [ a js:ObjectSchema, intelliot:AICoordinate, intelliot:PoseValue;
                  js:contentMediaType "application/ai+json";
                  js:properties [ a js:IntegerSchema, intelliot:AlphaCoordinate;
                      js:propertyName "alpha"
                    ], [ a js:IntegerSchema, intelliot:XCoordinate;
                      js:propertyName "x"
                    ], [ a js:IntegerSchema, intelliot:YCoordinate;
                      js:propertyName "y"
                    ];
                  js:required "x", "y", "alpha"
                ]
            ];
          js:required "value"
        ];
      td:hasNotificationSchema [ a js:ObjectSchema, intelliot:PoseNotification;
          js:properties [ a js:StringSchema, intelliot:PoseUpdate;
              js:propertyName "message";
              js:enum "aborted", "completed"
            ];
          js:required "message"
        ]
    ] .
'
echo "Create Engraver"

curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc3/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: engraver' \
--header 'Content-Type: text/turtle' \
--data-raw '@prefix td: <https://www.w3.org/2019/wot/td#> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix dct: <http://purl.org/dc/terms/> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix js: <https://www.w3.org/2019/wot/json-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/engraver> a td:Thing, <http://w3id.org/eve#Artifact>, <http://example.org/intellIoT#EngraverMachine>;
  td:title "Engraver machine";
  td:hasSecurityConfiguration [ a wotsec:NoSecurityScheme
    ];
  td:hasPropertyAffordance [ a td:PropertyAffordance, js:ObjectSchema;
      td:name "getJob";
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/api/job>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:readProperty, td:writeProperty
        ];
      td:isObservable false;
      js:properties [
          js:propertyName "percentageCompleted"
        ], [ a js:IntegerSchema;
          js:propertyName "secondsToFinish"
        ], [ a js:StringSchema;
          js:propertyName "state";
          js:enum "paused", "waiting", "available", "working", "finished", "error", "unconnected"
        ]
    ], [ a td:PropertyAffordance, js:ObjectSchema;
      td:name "getSpec";
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/api/spec>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:readProperty, td:writeProperty
        ];
      td:isObservable false;
      js:properties [ a js:NumberSchema;
          js:propertyName "workingAreaHeightMillimeter"
        ], [ a js:StringSchema;
          js:propertyName "model"
        ], [
          js:propertyName "type"
        ], [ a js:StringSchema;
          js:propertyName "laserClass"
        ], [ a js:NumberSchema;
          js:propertyName "workingAreaLengthMillimeter"
        ], [ a js:NumberSchema;
          js:propertyName "workingAreaWidthMillimeter"
        ]
    ], [ a td:PropertyAffordance, js:ObjectSchema;
      td:name "getConfiguration";
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/api/configuration>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction, td:readProperty, td:writeProperty
        ];
      td:isObservable false;
      js:properties [ a js:StringSchema;
          js:propertyName "host"
        ], [ a js:StringSchema;
          js:propertyName "authenticationMode";
          js:enum "LaserCutter", "MillingMachine"
        ];
      js:required "host", "authenticationMode"
    ];
  td:hasActionAffordance [ a td:ActionAffordance;
      td:name "createEngraveText";
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/api/job/text>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:BooleanSchema;
              js:propertyName "laserOn"
            ], [ a js:StringSchema;
              js:propertyName "positionReference"
            ], [ a js:StringSchema;
              js:propertyName "variant"
            ], [ a js:NumberSchema;
              js:propertyName "x"
            ], [ a js:NumberSchema;
              js:propertyName "fontsize"
            ], [ a js:NumberSchema;
              js:propertyName "y"
            ], [ a js:ArraySchema;
              js:propertyName "text";
              js:items [ a js:StringSchema
                ]
            ], [ a js:StringSchema;
              js:propertyName "font"
            ]
        ]
    ], [ a td:ActionAffordance;
      td:name "deleteJob";
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "DELETE";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/api/job>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [
              js:propertyName "percentageCompleted"
            ], [ a js:IntegerSchema;
              js:propertyName "secondsToFinish"
            ], [ a js:StringSchema;
              js:propertyName "state";
              js:enum "paused", "waiting", "available", "working", "finished", "error", "unconnected"
            ]
        ]
    ], [ a td:ActionAffordance;
      td:name "getConfiguration";
      td:hasForm [
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/api/configuration>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction, td:readProperty, td:writeProperty
        ];
      td:hasInputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "host"
            ], [ a js:StringSchema;
              js:propertyName "authenticationMode";
              js:enum "LaserCutter", "MillingMachine"
            ];
          js:required "host", "authenticationMode"
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:StringSchema;
              js:propertyName "host"
            ], [ a js:StringSchema;
              js:propertyName "authenticationMode";
              js:enum "LaserCutter", "MillingMachine"
            ];
          js:required "host", "authenticationMode"
        ]
    ];
  dct:description "An engraver machine used on the IntellIoT project" .
'

echo "Create Actuators"

curl --location --request POST ${HYPERMAS_BASE}/workspaces/uc3/artifacts/ \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: actuators' \
--header 'Content-Type: text/turtle' \
--data-raw '@prefix td: <https://www.w3.org/2019/wot/td#> .
@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
@prefix dct: <http://purl.org/dc/terms/> .
@prefix wotsec: <https://www.w3.org/2019/wot/security#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix js: <https://www.w3.org/2019/wot/json-schema#> .

<'"${HYPERMAS_BASE}"'/workspaces/uc3/artifacts/actuators> a td:Thing, <http://w3id.org/eve#Artifact>;
  td:title "Engraver Actuators";
  td:hasSecurityConfiguration [ a wotsec:NoSecurityScheme
    ];
  td:hasActionAffordance [ a td:ActionAffordance;
      td:name "open";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "machineId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/actuator-api/lid/open%7B?machineId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:BooleanSchema;
              js:propertyName "demo"
            ], [ a js:StringSchema;
              js:propertyName "status"
            ];
          js:required "status", "demo"
        ]
    ], [ a td:ActionAffordance;
      td:name "close";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "machineId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/actuator-api/lid/close%7B?machineId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:BooleanSchema;
              js:propertyName "demo"
            ], [ a js:StringSchema;
              js:propertyName "status"
            ];
          js:required "status", "demo"
        ]
    ], [ a td:ActionAffordance;
      td:name "liftup";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "machineId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/actuator-api/table/liftup%7B?machineId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:BooleanSchema;
              js:propertyName "demo"
            ], [ a js:StringSchema;
              js:propertyName "status"
            ];
          js:required "status", "demo"
        ]
    ], [ a td:ActionAffordance;
      td:name "lowerdown";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "machineId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/actuator-api/table/lowerdown%7B?machineId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:BooleanSchema;
              js:propertyName "demo"
            ], [ a js:StringSchema;
              js:propertyName "status"
            ];
          js:required "status", "demo"
        ]
    ], [ a td:ActionAffordance;
      td:name "pushstart";
      td:hasUriTemplateSchema [ a js:StringSchema;
          td:name "machineId"
        ];
      td:hasForm [
          <http://www.w3.org/2011/http#methodName> "POST";
          hctl:hasTarget <'"${DEVICE_BASE}"'/engraver-laser/actuator-api/push-start-button%7B?machineId%7D>;
          hctl:forContentType "application/json";
          hctl:hasOperationType td:invokeAction
        ];
      td:hasOutputSchema [ a js:ObjectSchema;
          js:properties [ a js:BooleanSchema;
              js:propertyName "demo"
            ], [ a js:StringSchema;
              js:propertyName "status"
            ];
          js:required "status", "demo"
        ]
    ];
  dct:description "Actuators for the engraver" .
'
