#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${CAMUNDA_BASE_URL:-http://localhost:8080/engine-rest}"
USER="${CAMUNDA_USER:-moysha}"
PASSWORD="${CAMUNDA_PASSWORD:-2281337}"
OLD_PROCESS_KEY="${OLD_PROCESS_KEY:-order-process}"

echo "Looking for obsolete Camunda process definitions with key '${OLD_PROCESS_KEY}' at ${BASE_URL}"

definitions_json="$(curl -fsS -u "${USER}:${PASSWORD}" \
  "${BASE_URL}/process-definition?key=${OLD_PROCESS_KEY}")"

definition_ids="$(python3 -c 'import json,sys
for item in json.load(sys.stdin):
    print(item["id"])
' <<< "${definitions_json}")"

if [[ -z "${definition_ids}" ]]; then
  echo "Nothing to delete."
  exit 0
fi

while IFS= read -r definition_id; do
  [[ -z "${definition_id}" ]] && continue
  echo "Deleting ${definition_id}"
  curl -fsS -u "${USER}:${PASSWORD}" -X DELETE \
    "${BASE_URL}/process-definition/${definition_id}?cascade=true&skipCustomListeners=true&skipIoMappings=true"
  echo
done <<< "${definition_ids}"

echo "Obsolete process definitions removed. Current process key 'orderProcess' was not touched."
