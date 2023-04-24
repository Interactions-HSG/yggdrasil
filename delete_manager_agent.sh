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

echo "HYPERMAS_BASE=$HYPERMAS_BASE"

echo "Delete manager agent"
curl --location --request DELETE ${HYPERMAS_BASE}/agents/manager \
--header 'X-Agent-WebID: http://example.org/agent' \
