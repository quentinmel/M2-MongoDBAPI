#!/usr/bin/env bash
set -uo pipefail

JAEGER_URL="${JAEGER_URL:-http://localhost:16686}"
failures=0

check() {
  local description="$1"
  shift
  if "$@"; then
    printf 'OK - %s\n' "$description"
  else
    printf 'ÉCHEC - %s\n' "$description"
    failures=$((failures + 1))
  fi
}

services_json="$(curl --fail --silent --show-error --max-time 10 "$JAEGER_URL/api/services" 2>/dev/null)" || services_json=''
backend_traces="$(curl --fail --silent --show-error --max-time 10 --get \
  --data-urlencode 'service=medislot-api' \
  --data-urlencode 'limit=100' \
  --data-urlencode 'lookback=24h' \
  "$JAEGER_URL/api/traces" 2>/dev/null)" || backend_traces=''

check 'Jaeger connaît medislot-api et medislot-web' \
  python3 -c 'import json,sys; s=set(json.load(sys.stdin).get("data",[])); sys.exit(0 if {"medislot-api","medislot-web"} <= s else 1)' <<<"$services_json"

check 'une trace du back contient des spans de base de données' \
  python3 -c 'import json,sys; traces=json.load(sys.stdin).get("data",[]); spans=[s for t in traces for s in t.get("spans",[])]; tags=[{x.get("key"):x.get("value") for x in s.get("tags",[])} for s in spans]; sys.exit(0 if any((x.get("db.system")=="postgresql" or x.get("db.system.name")=="postgresql" or "db.statement" in x or "db.query.text" in x) for x in tags) else 1)' <<<"$backend_traces"

check 'une réservation est liée et ses spans n’exposent que des attributs techniques' \
  python3 -c 'import json,sys; traces=json.load(sys.stdin).get("data",[]); ok=False
for t in traces:
 names={p.get("serviceName") for p in t.get("processes",{}).values()}
 spans=t.get("spans",[])
 business=[s for s in spans if s.get("operationName")=="medislot.reservation"]
 attrs=[{x.get("key"):x.get("value") for x in s.get("tags",[])} for s in business]
 web_ids={pid for pid,p in t.get("processes",{}).items() if p.get("serviceName")=="medislot-web"}
 web_spans=[s for s in spans if s.get("processID") in web_ids]
 urls=[x.get("value","") for s in web_spans for x in s.get("tags",[]) if x.get("key") in ("http.url","url.full")]
 custom_keys={k for a in attrs for k in a if k.startswith("medislot.")}
 safe=custom_keys <= {"medislot.slot.id","medislot.appointment.id"}
 if {"medislot-api","medislot-web"} <= names and any("medislot.slot.id" in a and "medislot.appointment.id" in a for a in attrs) and safe and all("?" not in str(url) for url in urls): ok=True
sys.exit(0 if ok else 1)' <<<"$backend_traces"

if (( failures == 0 )); then
  printf '3 OK, 0 ÉCHEC\n'
else
  printf '0 OK, %s ÉCHEC\n' "$failures"
  exit 1
fi
