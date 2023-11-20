while [[ "$#" -gt 0 ]]
  do
    case $1 in
      --hyper)
        HYPERMAS_BASE=$2
         ((REQUIRED_PARAM_COUNTER++))
        ;;

    esac
    shift
  done


if [[ $REQUIRED_PARAM_COUNTER -ne 1 ]]; then
    echo "$(basename $0)  --hyper <HyperMAS base URL> --device <Edge device base URL> --dlt <DLT Client Url> --iobox <IOBox --interface <Interface>"
    exit 1
fi

echo "Delete DLT"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/dlt-client \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete IOBox"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/iobox \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete Goal Interface"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/goal-interface \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete Milling Machine"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/milling \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete Actuators"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/actuators \
--header 'X-Agent-WebID: http://example.org/agent' \


echo "Delete Engraver"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/engraver \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete Robot Controller"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/robot-controller \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete Camera AI"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/camera-ai \
--header 'X-Agent-WebID: http://example.org/agent' \

echo "Delete HIL Service"
curl --location --request POST "${HYPERMAS_BASE}"/workspaces/uc1/artifacts/hil-service \
--header 'X-Agent-WebID: http://example.org/agent' \


echo "Delete Workspace uc3"
curl --location --request DELETE ${HYPERMAS_BASE}/workspaces/uc3 \
--header 'X-Agent-WebID: http://example.org/agent'

echo "Delete Workspace intelliot"
curl --location --request DELETE ${HYPERMAS_BASE}/workspaces/intelliot \
--header 'X-Agent-WebID: http://example.org/agent'
