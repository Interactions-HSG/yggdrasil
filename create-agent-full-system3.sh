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
    echo "$(basename $0)  --hyper <HyperMAS base URL> -a/--agent <agent id>"
    exit 1
fi


curl --location --request POST ''"${HYPERMAS_BASE}"'/agents/' \
--header 'X-Agent-WebID: http://example.org/agent' \
--header 'Slug: '"${AGENT_ID}"'' \
--header 'Content-Type: text/plain' \
--data-binary @agent_uc3_code.asl
